# Exercise 01: SQL Investigation

## Scenario

You arrive at your desk to find three customer complaints forwarded by the support team. Each one describes something that went wrong with a booking. Your job is to investigate the underlying data — figure out what happened, why, and what could prevent it from happening again.

This is a core QA skill: using the database to understand what's actually going on beneath the surface of a bug report. Users tell you symptoms. The data tells you causes.

---

## Background

StaySync stores all its data in a PostgreSQL database. Guests make bookings for rooms at hotels, and the system tracks check-in dates, check-out dates, prices, and booking statuses.

The three complaints you need to investigate:

1. **Michael Torres**: "I was charged for 2 nights but I booked 3 nights!"
2. **Sarah Connor**: "I keep getting booking confirmation emails for trips I didn't book."
3. **Room 103 incident**: "The hotel said my room was already occupied when I arrived."

---

## Setup

The database should already be running if you followed the Quick Start in the README. Connect to it using your preferred database client (pgAdmin, DBeaver, DataGrip, or even `psql` on the command line).

**Connection details:**
| Setting  | Value         |
|----------|---------------|
| Host     | localhost     |
| Port     | 5432          |
| Database | staysync      |
| User     | staysync      |
| Password | staysync123   |

Before you begin, take a moment to orient yourself. Run a few queries to see what's in each table:

```sql
SELECT * FROM guests LIMIT 10;
SELECT * FROM rooms LIMIT 10;
SELECT * FROM bookings LIMIT 10;
```

---

## Tasks

### Task 1 — Michael's billing complaint

Michael Torres says he booked 3 nights but was only charged for 2. Let's find out.

**Step 1**: Find Michael Torres in the guests table.

```sql
SELECT * FROM guests WHERE first_name = 'Michael' AND last_name = 'Torres';
```

**Step 2**: Find his bookings and calculate what the price should be.

```sql
SELECT
    b.*,
    r.price_per_night,
    (b.check_out_date - b.check_in_date) AS calculated_nights,
    (b.check_out_date - b.check_in_date) * r.price_per_night AS expected_total
FROM bookings b
JOIN rooms r ON b.room_id = r.id
WHERE b.guest_id = (
    SELECT id FROM guests
    WHERE first_name = 'Michael' AND last_name = 'Torres'
);
```

**Step 3**: Compare the `expected_total` to the `total_price` stored in the booking.

- Do the numbers match?
- If not, what's the discrepancy?
- Can you figure out what formula the application used to calculate the price? (Hint: look at the application code from Exercise 00.)

Write down your findings. You'll reference them later.

---

### Task 2 — Sarah's duplicate emails

Sarah Connor says she's receiving booking confirmations for trips she never booked. That's unsettling for a customer.

**Step 1**: Find Sarah Connor in the guests table.

```sql
SELECT * FROM guests WHERE first_name = 'Sarah' AND last_name = 'Connor';
```

- How many rows come back?
- Look carefully at the data. What's the same? What's different?

**Step 2**: Find all bookings associated with each of her records.

```sql
SELECT g.id AS guest_id, g.email, b.*
FROM guests g
JOIN bookings b ON g.id = b.guest_id
WHERE g.first_name = 'Sarah' AND g.last_name = 'Connor';
```

- Which bookings belong to which guest record?
- Can you see how the duplicate records could cause email problems?

**Step 3**: Check if there are other duplicate guests in the system.

```sql
SELECT first_name, last_name, COUNT(*) AS count
FROM guests
GROUP BY first_name, last_name
HAVING COUNT(*) > 1;
```

- How widespread is this problem?
- What would you recommend to fix it?

---

### Task 3 — Room 103 conflict

A guest arrived at room 103 to find it already occupied. Someone else had been checked in. This is a serious problem — let's find out what happened.

**Step 1**: Find all bookings for room 103.

```sql
SELECT b.*, g.first_name, g.last_name
FROM bookings b
JOIN guests g ON b.guest_id = g.id
WHERE b.room_id = (
    SELECT id FROM rooms WHERE room_number = '103'
)
ORDER BY b.check_in_date;
```

**Step 2**: Look at the dates carefully. Do any bookings overlap?

Two bookings overlap if one starts before the other ends. Visually, you're looking for:

```
Booking A:  |--------|
Booking B:       |--------|
                 ^ overlap
```

**Step 3**: Write a query that detects ALL overlapping bookings across ALL rooms.

This is harder. Give it a try before looking at the hint.

<details>
<summary>Hint</summary>

You need to join the bookings table to itself, comparing every pair of bookings for the same room:

```sql
SELECT
    a.id AS booking_a,
    b.id AS booking_b,
    a.room_id,
    a.check_in_date AS a_checkin,
    a.check_out_date AS a_checkout,
    b.check_in_date AS b_checkin,
    b.check_out_date AS b_checkout
FROM bookings a
JOIN bookings b ON a.room_id = b.room_id
    AND a.id < b.id
    AND a.check_in_date < b.check_out_date
    AND b.check_in_date < a.check_out_date
WHERE a.status != 'CANCELLED'
    AND b.status != 'CANCELLED';
```

</details>

---

### Task 4 — Go further

Now investigate on your own. No hints this time — these are the kinds of data quality checks a QA engineer should think about proactively.

**Check for impossible dates:**
- Are there any bookings where the check-out date is on or before the check-in date?

**Check for missing data:**
- Are there any bookings with NULL values in fields that shouldn't be null?
- Are there guests with missing email addresses or names?

**Check for stale data:**
- Are there bookings with a `CONFIRMED` status where the check-out date has already passed? These should probably be `COMPLETED` or `NO_SHOW`.

**Think about prevention:**
- What database-level constraints (CHECK, UNIQUE, NOT NULL, FOREIGN KEY) could prevent each issue you found?
- Write the ALTER TABLE statements you'd recommend. Even if you don't run them, writing them clarifies your thinking.

---

## Reflection Questions

1. **How many of these data issues could have been prevented by application code?** Think about validation in the API layer — checking inputs before they reach the database.

2. **How many could have been prevented by database constraints?** The database is the last line of defense. If the app has a bug, constraints can still stop bad data from being saved.

3. **If you were writing a test plan, what data validation tests would you add?** Think about both positive tests (valid data is accepted) and negative tests (invalid data is rejected).

4. **Which of these three customer complaints represents the biggest business risk, and why?**

---

## Bonus Challenges

- Write a "data health check" — a single SQL script that runs all of the checks above and produces a summary report. This is a real thing QA teams do.
- Look at the application code for the booking creation endpoint. Trace the path from the HTTP request to the database INSERT. Where SHOULD validation happen? Where DOES it happen?
- If you found duplicate guest records, write a query that identifies which one is the "original" (earliest created) and which are duplicates. How would you safely merge them?
