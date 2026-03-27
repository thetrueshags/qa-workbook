package com.staysync.tests;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * API-level tests for the StaySync booking endpoints.
 *
 * These tests talk directly to the REST API — no browser required.
 * REST Assured gives us a fluent, readable style:
 *
 *     given()        — set up the request  (headers, body, params)
 *       .when()      — perform the action  (GET, POST, PUT, DELETE)
 *       .then()      — assert the result   (status code, body fields)
 *
 * WORKSHOP INSTRUCTIONS
 * ---------------------
 * 1. Read through the two completed tests to understand the pattern.
 * 2. Fill in the skeleton tests marked with TODO.
 * 3. Run with:  mvn test -Dtest=BookingApiTest
 * 4. Some tests are EXPECTED to reveal bugs in StaySync — that is the
 *    point!  Note which tests expose missing validation or wrong
 *    calculations and discuss with your team.
 */
public class BookingApiTest {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost:8080";
    }

    // ---------------------------------------------------------------
    //  COMPLETED EXAMPLES — follow this pattern for the skeleton tests
    // ---------------------------------------------------------------

    /**
     * Verify that GET /api/bookings returns HTTP 200 and a JSON array.
     */
    @Test
    void testGetAllBookings() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/api/bookings")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", instanceOf(java.util.List.class));
    }

    /**
     * Verify that GET /api/bookings/1 returns a single booking with the
     * fields we expect every booking to have.
     */
    @Test
    void testGetBookingById() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/api/bookings/1")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("id", equalTo(1))
            .body("guest_name", notNullValue())
            .body("room_id", notNullValue())
            .body("check_in_date", notNullValue())
            .body("check_out_date", notNullValue())
            .body("total_price", notNullValue());
    }

    // ---------------------------------------------------------------
    //  SKELETON TESTS — trainees fill these in
    // ---------------------------------------------------------------

    /**
     * POST /api/bookings with a valid JSON body should return 201 and
     * echo back the created booking with an assigned id.
     *
     * Hint: use .body() on the given() side to send JSON, and check
     * the response status code and that an "id" field is present.
     */
    @Test
    void testCreateBooking() {
        // TODO: Send a POST request to /api/bookings with a JSON body like:
        //
        //   {
        //     "guest_name": "Jane Doe",
        //     "room_id": 2,
        //     "check_in_date": "2026-05-01",
        //     "check_out_date": "2026-05-04"
        //   }
        //
        // Then assert:
        //   - Status code is 201
        //   - Response body has a non-null "id"
        //   - "guest_name" in the response matches what you sent
    }

    /**
     * POST /api/bookings where check_out_date is BEFORE check_in_date.
     *
     * A well-behaved API should reject this with a 400 Bad Request.
     * StaySync might not — and that is the bug you are looking for!
     *
     * If the test PASSES (you get 400), the validation is correct.
     * If it FAILS (you get 201), you have found the missing-validation bug.
     */
    @Test
    void testCreateBookingWithInvalidDates() {
        // TODO: Send a POST to /api/bookings with check_out BEFORE check_in:
        //
        //   {
        //     "guest_name": "Time Traveler",
        //     "room_id": 1,
        //     "check_in_date": "2026-06-10",
        //     "check_out_date": "2026-06-05"
        //   }
        //
        // Then assert:
        //   - Status code is 400  (expect this to FAIL — that is the bug!)
    }

    /**
     * Create two bookings for the SAME room on overlapping dates.
     *
     * A correct system should reject the second booking (409 Conflict
     * or 400 Bad Request).  StaySync may accept both — that is the
     * double-booking bug you are hunting.
     */
    @Test
    void testDoubleBookSameRoom() {
        // TODO:
        // Step 1 — Create the first booking:
        //   POST /api/bookings
        //   {
        //     "guest_name": "Alice",
        //     "room_id": 3,
        //     "check_in_date": "2026-07-01",
        //     "check_out_date": "2026-07-05"
        //   }
        //   Assert status 201.
        //
        // Step 2 — Create a second booking for the SAME room and
        //          overlapping dates:
        //   POST /api/bookings
        //   {
        //     "guest_name": "Bob",
        //     "room_id": 3,
        //     "check_in_date": "2026-07-03",
        //     "check_out_date": "2026-07-07"
        //   }
        //   Assert status is NOT 201 (e.g., 409 or 400).
        //   If the second POST also returns 201, you found the bug!
    }

    /**
     * Cancel an existing booking via PUT /api/bookings/{id}/cancel.
     *
     * After cancellation, a subsequent GET for that booking should show
     * a "CANCELLED" status.
     */
    @Test
    void testCancelBooking() {
        // TODO:
        // Step 1 — Create a booking (POST /api/bookings) and capture
        //          the returned "id".
        //
        // Step 2 — Cancel it:
        //   PUT /api/bookings/{id}/cancel
        //   Assert status 200.
        //
        // Step 3 — Fetch the booking again:
        //   GET /api/bookings/{id}
        //   Assert that "status" equals "CANCELLED".
    }

    /**
     * Create a booking and verify the total_price is calculated
     * correctly.
     *
     * If the room costs $150/night and the stay is 3 nights
     * (check-in May 1 -> check-out May 4), total should be $450.
     *
     * StaySync has an off-by-one bug in its price calculation — your
     * test should catch it!
     */
    @Test
    void testPriceCalculation() {
        // TODO:
        // Step 1 — Look up the nightly rate for a room:
        //   GET /api/rooms/1
        //   Extract the "price_per_night" value.
        //
        // Step 2 — Create a booking for that room with known dates:
        //   POST /api/bookings
        //   {
        //     "guest_name": "Calculator",
        //     "room_id": 1,
        //     "check_in_date": "2026-08-10",
        //     "check_out_date": "2026-08-13"
        //   }
        //   (That is 3 nights: Aug 10, 11, 12)
        //
        // Step 3 — Assert:
        //   expected_total = price_per_night * 3
        //   actual_total   = response body "total_price"
        //   These should be equal.  If they differ, you found the
        //   off-by-one pricing bug!
    }
}
