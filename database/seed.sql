-- ============================================================
-- StaySync Hotel Booking Platform — Seed Data
-- ============================================================
-- This dataset contains INTENTIONAL data-quality issues
-- that QA trainees should discover through SQL exploration.
-- ============================================================

-- -----------------------------------------------------------
-- Rooms
-- -----------------------------------------------------------
INSERT INTO rooms (room_number, type, price_per_night, is_available) VALUES
('101', 'SINGLE',  89.00,  true),
('102', 'SINGLE',  89.00,  true),
('103', 'DOUBLE', 149.00,  true),
('104', 'DOUBLE', 149.00,  true),
('105', 'DOUBLE', 149.00,  true),   -- first instance of room 105
('105', 'SINGLE',  99.00,  true),   -- BUG: duplicate room number with different type/price
('106', 'SUITE',  299.00,  true),
('107', 'SINGLE',  89.00,  false),
('108', 'DOUBLE', 149.00,  true),
('109', 'SUITE',  299.00,  true),
('110', 'DOUBLE', 159.00,  true);

-- -----------------------------------------------------------
-- Guests
-- -----------------------------------------------------------
INSERT INTO guests (first_name, last_name, email, phone) VALUES
('James',    'Whitfield',  'j.whitfield@email.com',    '555-0101'),
('Maria',    'Gonzalez',   'maria.g@email.com',        '555-0102'),
('Sarah',    'Connor',     'sconnor@email.com',        '555-0103'),  -- first Sarah Connor
('Sarah',    'Connor',     'sconnor@email.com',        '555-0199'),  -- BUG: duplicate guest, same email, different phone
('David',    'Kim',        'dkim@email.com',           '555-0105'),
('Emily',    'Zhang',      'ezhang@email.com',         '555-0106'),
('Robert',   'Okafor',     'r.okafor@email.com',       '555-0107'),
('Priya',    'Sharma',     'p.sharma@email.com',       '555-0108'),
('Michael',  'Torres',     'mtorres@email.com',        '555-0109'),
('Aisha',    'Mohammed',   'aisha.m@email.com',        '555-0110'),
('Lucas',    'Andersen',   'l.andersen@email.com',     '555-0111'),
('Olivia',   'Bennett',    'o.bennett@email.com',      '555-0112'),
('Chen',     'Wei',        'chen.wei@email.com',       '555-0113'),
('Sofia',    'Rossi',      's.rossi@email.com',        '555-0114'),
('Nathan',   'Dubois',     'n.dubois@email.com',       '555-0115');

-- -----------------------------------------------------------
-- Bookings
-- -----------------------------------------------------------

-- Normal bookings (no issues) --------------------------------

-- James Whitfield: 2 nights in room 101 (SINGLE $89) — checked out
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(1, 1, '2024-01-10', '2024-01-12', 178.00, 'CHECKED_OUT');

-- Maria Gonzalez: 4 nights in room 106 (SUITE $299) — checked out
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(2, 7, '2024-01-20', '2024-01-24', 1196.00, 'CHECKED_OUT');

-- David Kim: 1 night in room 102 (SINGLE $89) — checked out
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(5, 2, '2024-02-05', '2024-02-06', 89.00, 'CHECKED_OUT');

-- Emily Zhang: 3 nights in room 108 (DOUBLE $149) — cancelled
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(6, 9, '2024-02-14', '2024-02-17', 447.00, 'CANCELLED');

-- Priya Sharma: 5 nights in room 109 (SUITE $299) — checked out
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(8, 10, '2024-03-01', '2024-03-06', 1495.00, 'CHECKED_OUT');

-- Aisha Mohammed: 2 nights in room 110 (DOUBLE $159) — checked out
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(10, 11, '2024-04-10', '2024-04-12', 318.00, 'CHECKED_OUT');

-- Chen Wei: 1 night in room 101 (SINGLE $89) — confirmed (upcoming)
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(13, 1, '2024-06-15', '2024-06-16', 89.00, 'CONFIRMED');

-- Sofia Rossi: 3 nights in room 109 (SUITE $299) — confirmed (upcoming)
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(14, 10, '2024-07-01', '2024-07-04', 897.00, 'CONFIRMED');

-- Problematic bookings (intentional bugs) --------------------

-- BUG: checkout before checkin
-- Robert Okafor: room 104 (DOUBLE $149), check-in Mar 15 but check-out Mar 12
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(7, 4, '2024-03-15', '2024-03-12', 447.00, 'CONFIRMED');

-- BUG: overlapping confirmed bookings for the same room (room 103, id=3)
-- Lucas Andersen: room 103, Mar 20 – Mar 25
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(11, 3, '2024-03-20', '2024-03-25', 745.00, 'CONFIRMED');

-- Olivia Bennett: room 103, Mar 23 – Mar 27 (overlaps with Lucas's booking)
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(12, 3, '2024-03-23', '2024-03-27', 596.00, 'CONFIRMED');

-- BUG: wrong total_price (off-by-one night calculation)
-- Michael Torres: 3 nights in room 103 (DOUBLE $149), Apr 5 – Apr 8
-- Correct price = 3 × 149 = 447.00, but recorded as 298.00 (2 × 149)
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(9, 3, '2024-04-05', '2024-04-08', 298.00, 'CHECKED_OUT');

-- BUG: NULL total_price
-- Nathan Dubois: 2 nights in room 107 (SINGLE $89), May 1 – May 3
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(15, 8, '2024-05-01', '2024-05-03', NULL, 'CONFIRMED');

-- BUG: CONFIRMED status for a date far in the past (stale booking, never checked in)
-- Sarah Connor (first): 2 nights in room 102, Jan 5 – Jan 7 2024, still CONFIRMED
INSERT INTO bookings (guest_id, room_id, check_in_date, check_out_date, total_price, status) VALUES
(3, 2, '2024-01-05', '2024-01-07', 178.00, 'CONFIRMED');
