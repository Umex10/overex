/**
 * Mirror of the backend's e2e test data
 * (apps/backend/src/main/resources/db/e2e-seed.sql): change both together.
 *
 * Only what the tests look at is mirrored. The number in each title is the
 * event's position in the list when sorted by start date (the default,
 * "earliest first"); 12 events = 2 pages of 10.
 */

export const seedCategories = {
  concert: { id: "00000000-0000-0000-0000-000000000001", title: "Concert" },
  festival: { id: "00000000-0000-0000-0000-000000000002", title: "Festival" },
  /** Has no events: for the "no events" state. */
  theater: { id: "00000000-0000-0000-0000-000000000003", title: "Theater" },
  tennis: { id: "00000000-0000-0000-0000-000000000004", title: "Tennis" },
};

export const seedEvents = {
  jazzNight: {
    id: "10000000-0000-0000-0000-000000000001",
    title: "Event 01 – Jazz Night",
  },
  rockClassics: {
    id: "10000000-0000-0000-0000-000000000002",
    title: "Event 02 – Rock Classics",
  },
  summerBeats: {
    id: "10000000-0000-0000-0000-000000000003",
    title: "Event 03 – Summer Beats",
  },
  pianoEvening: {
    id: "10000000-0000-0000-0000-000000000004",
    title: "Event 04 – Piano Evening",
  },
  indieShowcase: {
    id: "10000000-0000-0000-0000-000000000005",
    title: "Event 05 – Indie Showcase",
  },
  electroOpenAir: {
    id: "10000000-0000-0000-0000-000000000006",
    title: "Event 06 – Electro Open Air",
  },
  /** Featured event: its sale opens next (in 2 days). */
  acousticSessions: {
    id: "10000000-0000-0000-0000-000000000007",
    title: "Event 07 – Acoustic Sessions",
  },
  symphonyNo9: {
    id: "10000000-0000-0000-0000-000000000008",
    title: "Event 08 – Symphony No. 9",
  },
  lakeFestival: {
    id: "10000000-0000-0000-0000-000000000009",
    title: "Event 09 – Lake Festival",
  },
  hipHopLive: {
    id: "10000000-0000-0000-0000-000000000010",
    title: "Event 10 – Hip-Hop Live",
  },
  bluesBrothersTribute: {
    id: "10000000-0000-0000-0000-000000000011",
    title: "Event 11 – Blues Brothers Tribute",
  },
  winterFestival: {
    id: "10000000-0000-0000-0000-000000000012",
    title: "Event 12 – Winter Festival",
  },
};
