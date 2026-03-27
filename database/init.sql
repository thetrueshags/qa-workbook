-- ============================================================
-- StaySync Hotel Booking Platform — Schema
-- ============================================================
-- NOTE FOR QA TRAINEES: This schema contains intentional
-- design issues. Part of the exercise is to identify them
-- by writing queries against the seeded data and by
-- inspecting the DDL itself.
-- ============================================================

CREATE TABLE rooms (
    id SERIAL PRIMARY KEY,
    room_number VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('SINGLE', 'DOUBLE', 'SUITE')),
    price_per_night DECIMAL(10,2) NOT NULL,
    is_available BOOLEAN DEFAULT true
);

CREATE TABLE guests (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20)
);

CREATE TABLE bookings (
    id SERIAL PRIMARY KEY,
    guest_id INTEGER REFERENCES guests(id),
    room_id INTEGER REFERENCES rooms(id),
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    total_price DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED', 'CANCELLED', 'CHECKED_IN', 'CHECKED_OUT')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
