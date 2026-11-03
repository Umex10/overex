-- =============================================================================
-- Test data for the e2e profile only (application-e2e.properties).
--
-- Runs on every backend start, right after Hibernate has created the empty
-- schema. The Playwright tests rely on exactly these rows: change a title,
-- an order or a count here and the matching test must change too.
--
-- Fixed UUIDs, so a test can open a known event (/events/<id>).
-- Dates are relative to now(), so "upcoming" stays upcoming on every run.
--
--   Category   Events
--   Concert    8   (events 01, 02, 04, 05, 07, 08, 10, 11)
--   Festival   4   (events 03, 06, 09, 12)
--   Theater    0   -> the "no events" state
--
-- 12 events = 2 pages (PAGE_SIZE 10). Sorted by start (default "earliest
-- first"), the number in the title is the position in the list.
--
-- Featured event = the sale that opens next: event 07 (sale in 2 days).
-- Event 10 opens later (in 5 days); every other sale has started already.
-- =============================================================================

INSERT INTO event_categories (id, title) VALUES
  ('00000000-0000-0000-0000-000000000001', 'Concert'),
  ('00000000-0000-0000-0000-000000000002', 'Festival'),
  ('00000000-0000-0000-0000-000000000003', 'Theater'),
  ('00000000-0000-0000-0000-000000000004', 'Tennis');

INSERT INTO events (id, category_id, title, description, venue, city,
                    starts_at, sale_starts_at, image_url, status,
                    created_at, updated_at)
VALUES
  ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001',
   'Event 01 – Jazz Night', 'An evening of live jazz.', 'Porgy & Bess', 'Wien',
   now() + interval '10 days', now() - interval '20 days',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a', 'PUBLISHED', now(), now()),

  ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001',
   'Event 02 – Rock Classics', 'The greatest rock songs, live.', 'Gasometer', 'Wien',
   now() + interval '12 days', now() - interval '18 days',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a', 'PUBLISHED', now(), now()),

  ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002',
   'Event 03 – Summer Beats', 'Three stages, one weekend.', 'Donauinsel', 'Wien',
   now() + interval '14 days', now() - interval '16 days',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a', 'PUBLISHED', now(), now()),

  ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001',
   'Event 04 – Piano Evening', 'Classical piano works.', 'Musikverein', 'Wien',
   now() + interval '16 days', now() - interval '14 days',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a', 'PUBLISHED', now(), now()),

  ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001',
   'Event 05 – Indie Showcase', 'Four new indie bands.', 'Arena', 'Wien',
   now() + interval '18 days', now() - interval '12 days',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a', 'PUBLISHED', now(), now()),

  ('10000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000002',
   'Event 06 – Electro Open Air', 'Electronic music under the sky.', 'Messe', 'Graz',
   now() + interval '20 days', now() - interval '10 days',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a', 'PUBLISHED', now(), now()),

  ('10000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000001',
   'Event 07 – Acoustic Sessions', 'Unplugged and up close.', 'Posthof', 'Linz',
   now() + interval '22 days', now() + interval '2 days',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a', 'PUBLISHED', now(), now()),

  ('10000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000001',
   'Event 08 – Symphony No. 9', 'Beethoven with full orchestra.', 'Konzerthaus', 'Wien',
   now() + interval '24 days', now() - interval '6 days',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a', 'PUBLISHED', now(), now()),

  ('10000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002',
   'Event 09 – Lake Festival', 'Music by the lake.', 'Seebühne', 'Bregenz',
   now() + interval '26 days', now() - interval '4 days',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a', 'PUBLISHED', now(), now()),

  ('10000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000001',
   'Event 10 – Hip-Hop Live', 'The best of the local scene.', 'Ottakringer Brauerei', 'Wien',
   now() + interval '28 days', now() + interval '5 days',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a', 'PUBLISHED', now(), now()),

  ('10000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000001',
   'Event 11 – Blues Brothers Tribute', 'A night full of blues.', 'Stadtwerkstatt', 'Linz',
   now() + interval '30 days', now() - interval '2 days',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a', 'PUBLISHED', now(), now()),

  ('10000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000002',
   'Event 12 – Winter Festival', 'Music in the snow.', 'Bergisel', 'Innsbruck',
   now() + interval '32 days', now() - interval '1 day',
   'https://images.unsplash.com/photo-1501386761578-eac5c94b800a', 'PUBLISHED', now(), now());
