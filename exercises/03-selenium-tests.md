# Exercise 03: Your First Selenium Tests

## Scenario

The team has decided: bugs found in Exercise 01 and 02 need automated regression tests. That way, once the developers fix them, they stay fixed. You've been asked to contribute to the test automation suite.

The `selenium-tests` project already has some structure and a couple of working tests. Your job is to read the existing code, understand the patterns, and add new tests following the same approach.

---

## Background

The test suite uses:

- **REST Assured** — a Java library for testing REST APIs. It gives you a fluent, readable way to make HTTP requests and assert on responses.
- **JUnit 5** — the test framework. If you've seen `@Test` annotations before, you know the basics.
- **Maven** — builds the project and manages dependencies.

You don't need to be a Java expert for this. The existing tests are templates: read them, understand the pattern, then follow it. The hard part isn't the Java — it's knowing what to test and what to assert.

---

## Setup

1. Open the `selenium-tests` folder in your IDE (IntelliJ, VS Code, Eclipse — whatever you prefer).
2. Look at `pom.xml` to see what libraries are included.
3. Look at `BaseTest.java` — this is the parent class for all tests. It sets up the base URL and any shared configuration.
4. If the app is running, you can execute tests right away:

    ```bash
    cd selenium-tests
    mvn test
    ```

    If the app isn't running, you can still read the code and write tests — you just won't be able to run them yet.

---

## Tasks

### Task 1 — Understand the existing tests

Before writing anything, read what's already there.

**Open `BookingApiTest.java`** and study the two completed tests:

- `testGetAllBookings()` — Makes a GET request to `/api/bookings` and verifies the response.
- `testGetBookingById()` — Fetches a specific booking and checks its fields.

Pay attention to:

- How is the request built? (`given()`, `.when()`, `.then()`)
- How are assertions written? (`.statusCode()`, `.body()`)
- How are JSON paths used to check nested fields? (e.g., `"bookings[0].id"`)

**Open `RoomApiTest.java`** and read those tests too. Notice the similarities.

**Key pattern** (this is the REST Assured style you'll follow):

```java
given()
    .contentType(ContentType.JSON)
    .body(requestBody)
.when()
    .post("/api/bookings")
.then()
    .statusCode(200)
    .body("id", notNullValue())
    .body("status", equalTo("CONFIRMED"));
```

Take a few minutes to get comfortable with this. The syntax reads almost like English, which is intentional.

---

### Task 2 — Complete the skeleton tests

The test files contain several methods with `TODO` comments. These are your assignments. Pick **at least three** and implement them.

Here's what each one should do:

#### `testCreateBooking()`

Create a valid booking and verify the response.

- Send a POST to `/api/bookings` with valid `guestId`, `roomId`, `checkInDate`, and `checkOutDate`.
- Assert that the status code is 200.
- Assert that the response contains an `id` (not null).
- Assert that the `status` is `"CONFIRMED"`.
- Assert that the `guestId` and `roomId` in the response match what you sent.

Hint — building the request body:

```java
String requestBody = """
    {
        "guestId": 1,
        "roomId": 2,
        "checkInDate": "2026-07-01",
        "checkOutDate": "2026-07-04"
    }
    """;
```

#### `testCreateBookingWithInvalidDates()`

What happens when you send a `checkOutDate` that's before `checkInDate`?

- Send a POST with check-out before check-in.
- What SHOULD happen? (Think about it before you run the test.)
- What ACTUALLY happens? (Run the test and find out.)
- Write the test to document the **current behavior**, even if it's wrong. Add a comment explaining what the correct behavior should be.

This is an important concept: sometimes you write tests that assert broken behavior, so you have a clear record of the bug. When the fix lands, the test will fail — and that's the signal to update it.

#### `testDoubleBookSameRoom()`

Can you book the same room for the same dates twice?

- Create a booking for a specific room and date range.
- Create another booking for the exact same room and dates.
- What should the second request return? (Hint: it should fail.)
- What does it actually return?
- Write the test accordingly.

#### `testPriceCalculation()`

This is the one that should expose the off-by-one pricing bug from Exercises 01 and 02.

- First, GET the room details to find the `price_per_night`.
- Create a booking for that room with known dates (e.g., 3 nights).
- Calculate the expected total: `nights * pricePerNight`.
- Compare it to the `totalPrice` in the response.
- Does it match?

Think carefully about how to write this test. Should it assert the CORRECT price (and fail until the bug is fixed)? Or should it assert the CURRENT wrong price (and pass, documenting the bug)?

#### `testCancelBooking()`

- Create a booking.
- Cancel it using `PUT /api/bookings/{id}/cancel`.
- Verify the response shows `status` as `"CANCELLED"`.
- Retrieve the booking with a GET and verify the status persists.
- What happens if you try to cancel it a second time?

---

### Task 3 — Think about test design

After implementing your tests, step back and think about the big picture.

**Categorize your tests:**

| Test | Verifying correct behavior | Documenting a bug |
|------|---------------------------|-------------------|
| testCreateBooking | | |
| testCreateBookingWithInvalidDates | | |
| testDoubleBookSameRoom | | |
| testPriceCalculation | | |
| testCancelBooking | | |

**Consider maintenance:**
- When the pricing bug gets fixed, which test(s) will need to change?
- When double-booking prevention is added, which test(s) will need to change?
- How can you write tests that are easy to update when behavior changes?

**Identify gaps:**
- What scenarios are NOT covered by any test in the suite?
- If you had time to add 5 more tests, what would they be?
- Are there any tests that would require a different approach (e.g., performance tests, security tests)?

---

## Reflection Questions

1. **What's the difference between a test that verifies correct behavior and one that documents a bug?** Both have value, but they serve different purposes. How do you decide which approach to use?

2. **When you found a bug through a test, did you write the test to PASS (asserting current broken behavior) or to FAIL (asserting correct behavior)?** There are legitimate arguments for both. Teams handle this differently — what matters is being intentional about the choice. What are the tradeoffs?

3. **What are the limits of API testing?** Think about what kinds of bugs you CAN catch with API tests, and what kinds you CAN'T. What other types of testing would complement these?

4. **How do these automated tests relate to the exploratory testing you did in Exercise 02?** Could automation replace exploratory testing? Could exploratory testing replace automation? Or do they serve different purposes?

---

## Bonus Challenges

- **Parameterized tests**: REST Assured and JUnit 5 support parameterized tests. Rewrite `testCreateBookingWithInvalidDates()` to test multiple invalid date combinations using `@ParameterizedTest` and `@CsvSource`.
- **Test data management**: Your tests create bookings in the database. What happens when you run the tests twice? Do they still pass? Think about test isolation — how can you make tests independent of each other and of existing data?
- **Response time assertions**: REST Assured can assert on response time. Add a check that key endpoints respond within an acceptable threshold (e.g., under 500ms). This is a lightweight way to catch performance regressions.
- **Extract a test utility**: If you notice repeated code across your tests (e.g., creating a booking), extract it into a helper method in `BaseTest.java` or a utility class. Good test code follows the same principles as good application code.
