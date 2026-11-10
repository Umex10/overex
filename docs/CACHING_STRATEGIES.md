# Caching-Strategien für Detailseiten (geplant, noch nicht aktiv)

Stand: 10.11.2026. Notiz für später: Wie die Detailseiten (`/events/[id]`,
`/my-events/[id]`) schneller laden könnten. Beide Varianten sind als Hook
vorbereitet, werden aber **derzeit nirgends verwendet**. Heute lädt jede
Detailseite ganz normal über `useGetEventQuery(id)` und zeigt solange ein
Skeleton.

| Hook | Datei | Status |
|---|---|---|
| `useSeedEventCache` | `apps/web/features/event/hooks/use-seed-event-cache.ts` | fertig, nicht eingebunden |
| `usePrefetchEvent` | `apps/web/features/event/hooks/use-prefetch-event.ts` | fertig, nicht eingebunden (enthält noch Einrückungs-Altlasten) |

---

## 1. Seed: den Cache aus der Liste füllen

**Idee:** Die Liste hat das komplette `EventDto` schon. Beim Klick wird es in den
Cache-Eintrag von `getEvent(id)` geschrieben. Die Detailseite findet die Daten
sofort, ganz ohne Request.

```ts
dispatch(eventApi.util.upsertQueryData("getEvent", event.id, event));
```

- **Vorteil:** sofort da, auf allen Geräten (Maus, Tastatur, Touch), kein
  zusätzlicher Request.
- **Nachteil:** Die Daten sind so alt wie die Liste.
- **Voraussetzung:** Die Liste liefert **genau dasselbe** wie der Detail-Endpoint
  (beides `EventDto`). Liefert die Liste später weniger Felder, geht Seed nicht
  mehr, dann Prefetch nehmen.
- Bei TanStack Query heißt das „Seeding the Query Cache“.

**Einbauen:**
```tsx
const seedEventCache = useSeedEventCache();
<Link href={`/events/${event.id}`} onClick={() => seedEventCache(event)}>
```
Die `EventCard` braucht dafür wieder einen optionalen Prop `onClick?: () => void`,
der an den `Link` weitergereicht wird.

**Wann ist die Liste veraltet?** Änderungen im selben Tab deckt die
Tag-Invalidierung ab (`invalidatesTags: ["Event"]`). Veraltet ist sie bei zwei
Tabs oder Geräten, bei einem Tab, der stundenlang offen ist, oder bei Änderungen
durch andere Nutzer.

---

## 2. Prefetch: Daten laden, bevor geklickt wird

**Idee:** Sobald absehbar ist, dass der Nutzer gleich klickt, wird
`GET /events/{id}` schon gestartet.

```ts
const prefetch = usePrefetch("getEvent");
prefetch(eventId); // gleiches Argument wie useGetEventQuery(eventId)!
```

- **Gleiches Argument** wie in `useGetEventQuery`, sonst landen die Daten in
  einem anderen Cache-Eintrag.
- **Kein eingebautes Debouncing:** Der Request startet sofort. Doppelte Requests
  für dieselbe ID gibt es nicht, solange einer läuft oder Daten im Cache sind.
  Optionen: `{ ifOlderThan: <Sekunden> }` oder `{ force: true }`.
- Ein Prefetch **abonniert** die Daten nicht. Sie verfallen nach
  `keepUnusedDataFor` (Standard 60 s), wenn keine Seite sie nutzt.
- Die Zielseite hängt sich an einen laufenden Prefetch an. Ohne Prefetch lädt sie
  selbst. **Daten fehlen also nie**, im schlimmsten Fall gibt es einen Ladezustand.
- Next.js `<Link>` prefetcht nur den **Code** der Seite, nicht die Daten.

**Hover-Intent** (das macht `usePrefetchEvent`): Timer bei `mouseenter`,
Abbruch bei `mouseleave`, 100 bis 200 ms. `onFocus` deckt die Tastatur ab.
Ein `onBlur` fehlt im Hook noch, ebenso eine Cleanup-Funktion bei `onFocus`.

- Auf dem Handy gibt es kein Hover. Möglich wären `onPointerDown` (kleiner
  Vorsprung) oder ein IntersectionObserver (viele Requests).
- Hover-Prefetch nur am Computer ist in Ordnung („Progressive Enhancement“).

**Prefetch ist nicht nur für Hover.** Er passt immer dann, wenn man den nächsten
Request kennt, aber die Daten noch nicht hat: die nächste Seite einer Pagination
(auf allen Geräten), das Dashboard nach dem Login, mehrstufige Formulare.

---

## 3. Welche Strategie für welche Seite?

**Merkregel:** Anzeigen → sofort zeigen und im Hintergrund nachladen.
Bearbeiten → frisch laden.

| Seite | Empfehlung |
|---|---|
| `/events/[id]` (anzeigen) | Seed + `useGetEventQuery(id, { refetchOnMountOrArgChange: true })`. Das ist „Stale-While-Revalidate“: sofort die Daten aus der Liste, im Hintergrund neu laden. Ist die Antwort gleich, behält RTK die alten Objekte, und React rendert nichts neu. |
| `/my-events/[id]` (bearbeiten) | **Kein Seed.** Frisch laden mit Skeleton. Hover-Prefetch nur, wenn das Laden spürbar langsam ist. |

**Warum kein Seed beim Bearbeiten?** `react-hook-form` liest `defaultValues` nur
beim ersten Rendern. Kommen später neuere Daten, ignoriert das Formular sie
(dann bearbeitet man den alten Stand) oder man ruft `form.reset()` auf (dann sind
die Eingaben weg). Neu laden im Hintergrund hilft in Formularen also nicht.

Frisch laden sorgt nur für einen aktuellen **Ausgangsstand**. Gegen Änderungen
**während** des Bearbeitens schützt das Optimistic Locking (`version`, 409).
Das ist inzwischen eingebaut.

**Achtung, „frisch“ ist heute nicht ganz garantiert:** Ohne
`refetchOnMountOrArgChange` nimmt `useGetEventQuery(id)` einen vorhandenen
Cache-Eintrag (z. B. vom Besuch vor 30 s). Wer auf der Edit-Seite wirklich immer
neu laden will, setzt dort `{ refetchOnMountOrArgChange: true }`.

---

## 4. Wann lädt RTK Query überhaupt neu?

`keepUnusedDataFor` ist **kein Ablaufdatum**, sondern ein Aufräum-Timer. Er
startet erst, wenn keine Komponente die Query mehr nutzt. Solange sie genutzt
wird, lädt RTK **nie von selbst** neu. Ein `staleTime` wie bei TanStack gibt es
nicht. Am nächsten kommt `refetchOnMountOrArgChange: <Sekunden>`.

RTK lädt nur neu bei:
- `refetchOnMountOrArgChange`
- `refetchOnFocus` / `refetchOnReconnect` (braucht `setupListeners(store.dispatch)`)
- `pollingInterval`
- Tag-Invalidierung (`invalidatesTags`)

---

## 5. Offene Punkte rund ums Thema

- `MyEventItem`: Der `Link` umschließt die Bearbeiten- und Löschen-Buttons. Ein
  Klick auf „Löschen“ navigiert mit, und Buttons in einem `<a>` sind kein gültiges
  HTML. Der Link sollte nur noch den Bearbeiten-Button umschließen.
- Optional: `refetchOnFocus` + `setupListeners`, damit ein lange offener Tab beim
  Zurückwechseln neu lädt.
- `getAccessTk` in `features/auth/auth-api.ts` hat `keepUnusedDataFor: 900` mit
  dem Kommentar „matches the access-token lifetime“. Das führt in die Irre: Die
  Query ist durch `AuthProvider` und das Shell-Layout immer in Benutzung, der
  Timer läuft also nie ab. Erneuert wird das Token durch den Reauth bei 401.
- Später, bei mehreren Nutzern: andere Clients über Änderungen informieren
  (Polling, SSE mit `SseEmitter` + `invalidateTags`, WebSockets). Bei mehreren
  Backend-Instanzen braucht es dafür Redis Pub/Sub o. Ä.
