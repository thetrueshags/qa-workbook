package com.staysync.tests;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * API tests for the StaySync room endpoints.
 *
 * These cover the read-only room catalogue and the availability check.
 *
 * WORKSHOP INSTRUCTIONS
 * ---------------------
 * 1. Review the two completed tests.
 * 2. Fill in the skeleton test at the bottom.
 * 3. Run with:  mvn test -Dtest=RoomApiTest
 */
public class RoomApiTest {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost:8080";
    }

    // ---------------------------------------------------------------
    //  COMPLETED EXAMPLES
    // ---------------------------------------------------------------

    /**
     * GET /api/rooms should return 200 and a non-empty list of rooms.
     * Each room should have at least an id and a room_number.
     */
    @Test
    void testGetAllRooms() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/api/rooms")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", not(empty()))
            .body("[0].id", notNullValue())
            .body("[0].room_number", notNullValue());
    }

    /**
     * GET /api/rooms/available should return 200 and a JSON array.
     * (The array may be empty if every room is booked, but the
     * endpoint itself must respond correctly.)
     */
    @Test
    void testGetAvailableRooms() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/api/rooms/available")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", instanceOf(java.util.List.class));
    }

    // ---------------------------------------------------------------
    //  SKELETON TEST — trainees fill this in
    // ---------------------------------------------------------------

    /**
     * After booking a room for certain dates, that room should no
     * longer appear in the available-rooms list for those dates.
     *
     * This is an integration-style test: it combines the bookings
     * endpoint with the rooms/available endpoint.
     */
    @Test
    void testRoomAvailabilityAfterBooking() {
        // TODO:
        // Step 1 — Pick a room that is currently available:
        //   GET /api/rooms/available
        //   Extract the "id" of the first room in the response.
        //
        // Step 2 — Book that room:
        //   POST /api/bookings
        //   {
        //     "guest_name": "Availability Tester",
        //     "room_id": <the id from step 1>,
        //     "check_in_date": "2026-09-01",
        //     "check_out_date": "2026-09-03"
        //   }
        //   Assert status 201.
        //
        // Step 3 — Check availability again:
        //   GET /api/rooms/available
        //   Assert that the room you just booked does NOT appear in
        //   the returned list (filter by room id).
        //
        //   Hint with REST Assured:
        //     .body("id", not(hasItem(roomId)))
        //
        //   If the room still shows as available, the availability
        //   logic may have a bug!
    }
}
