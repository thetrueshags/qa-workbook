# Exercise 02: Exploratory Testing

## Scenario

The development team is about to push a new release of StaySync. There's no formal test plan for this cycle — the team lead has asked you to spend 30 minutes doing exploratory testing on the booking API and report back what you find.

This is not unusual. In practice, exploratory testing is one of the most effective ways to find bugs, and it's a skill that separates good QA engineers from great ones.

---

## Background

Exploratory testing is not random clicking. It's a disciplined approach where you simultaneously:

- **Learn** about the system
- **Design** tests based on what you're learning
- **Execute** those tests immediately
- **Adapt** your approach based on results

The key mindset: curiosity. Every response from the system is information. Unexpected responses are the most interesting — follow them.

**Keep notes as you go.** Write down what you tried, what you expected, and what happened. These notes become your bug reports.

---

## Setup

You'll need a way to make HTTP requests. Any of these will work:

- **curl** (command line)
- **Postman** (GUI)
- **Insomnia** (GUI)
- Your IDE's built-in HTTP client

If the app is running (via Docker or locally), the API base URL is `http://localhost:8080`.

**Available endpoints:**

| Method | Endpoint                     | Description          |
|--------|------------------------------|----------------------|
| GET    | `/api/rooms`                 | List all rooms       |
| GET    | `/api/rooms/available`       | List available rooms |
| GET    | `/api/bookings`              | List all bookings    |
| GET    | `/api/bookings/{id}`         | Get a booking by ID  |
| POST   | `/api/bookings`              | Create a booking     |
| PUT    | `/api/bookings/{id}/cancel`  | Cancel a booking     |

**Sample booking request body (for POST):**

```json
{
    "guestId": 1,
    "roomId": 1,
    "checkInDate": "2026-06-01",
    "checkOutDate": "2026-06-04"
}
```

If the app isn't running, you can still do this exercise by reading the API code and predicting what would happen. That's a valid QA approach too — static analysis.

---

## Tasks

### Task 1 — Walk the happy path

Before you try to break anything, make sure the normal flow works. This gives you a baseline.

1. **List the rooms**: `GET /api/rooms` — What rooms are available? Pick one and note its ID and price per night.

2. **Create a booking**: `POST /api/bookings` — Use a valid guest ID, the room ID you picked, and reasonable future dates.

    ```bash
    curl -X POST http://localhost:8080/api/bookings \
      -H "Content-Type: application/json" \
      -d '{"guestId": 1, "roomId": 1, "checkInDate": "2026-06-01", "checkOutDate": "2026-06-04"}'
    ```

3. **Retrieve the booking**: `GET /api/bookings/{id}` — Does the returned data match what you sent?

4. **Cancel the booking**: `PUT /api/bookings/{id}/cancel` — Does the status change?

5. **Retrieve it again**: Is the status now CANCELLED?

Document anything that surprises you, even on the happy path.

---

### Task 2 — Push the boundaries

Now it's time to get creative. For each test below, document three things: what you sent, what you expected, and what actually happened.

**Date validation:**
- Book with `checkOutDate` before `checkInDate`
- Book with `checkInDate` in the past
- Book with `checkInDate` and `checkOutDate` on the same day (zero nights)
- Book with dates far in the future (year 2099)

**Referential integrity:**
- Book with a `guestId` that doesn't exist (e.g., 99999)
- Book with a `roomId` that doesn't exist
- Book with `guestId` of 0 or -1

**Double booking:**
- Book the same room for the same dates twice
- Book a room for dates that partially overlap an existing booking

**Missing and malformed data:**
- Send a booking with `guestId` missing
- Send a booking with no body at all
- Send a request with `Content-Type: application/json` but the body is `{not valid json}`
- Send a request with `Content-Type: text/plain`

**Unexpected input types:**
- Send `guestId` as a string: `"guestId": "one"`
- Send dates in a different format: `"checkInDate": "06/01/2026"`
- Send a negative `roomId`
- Send extremely long string values

**Cancellation edge cases:**
- Cancel a booking that's already cancelled
- Cancel a booking that doesn't exist
- Cancel with an ID of 0 or -1

Record every response. Pay attention to:
- HTTP status codes (are they appropriate?)
- Error messages (are they helpful? Do they leak internal details?)
- Consistency (does the API handle similar errors the same way?)

---

### Task 3 — Check the math

This one is targeted. From Exercise 01, you may already suspect there's a pricing bug.

1. Use `GET /api/rooms` to find a room and its `price_per_night`.
2. Create a booking for that room for a known number of nights (e.g., check-in June 1, check-out June 4 = 3 nights).
3. Calculate the expected total: `nights x price_per_night`.
4. Compare it to the `totalPrice` in the API response.

Does it match? If not, figure out exactly what formula the API is using.

Try a few different durations (1 night, 2 nights, 7 nights) and see if the pattern is consistent.

---

### Task 4 — Write bug reports

For each issue you found, write a brief bug report. Use this format:

```
**Title**: [One-line summary]

**Severity**: Critical / Major / Minor / Cosmetic

**Steps to reproduce**:
1. [Step one]
2. [Step two]
3. ...

**Expected result**: [What should happen]

**Actual result**: [What actually happened]

**Notes**: [Any additional context — related bugs, workarounds, etc.]
```

Severity guide:
- **Critical**: Data loss, security issue, or system crash. Blocks release.
- **Major**: Core functionality broken. A user would be unable to complete a key task.
- **Minor**: Something works but is incorrect or confusing. Workaround exists.
- **Cosmetic**: Appearance or wording issues. No functional impact.

Write your reports in a new file or in a notebook. You'll reference them in future exercises.

---

## Reflection Questions

1. **Which bugs would you consider release blockers?** If the team pushed this to production tomorrow, which issues could cause real harm to users or the business?

2. **Did finding one bug lead you to look for related bugs?** For example, if date validation is missing, did you then check other input validation? This instinct — following the thread — is one of the most important QA skills you can develop.

3. **How would you prioritize fixing these issues?** Not everything can be fixed at once. What would you fix first, second, and last?

4. **What's the difference between "the API returned an error" and "the API handled the error well"?** Did you see examples of both?

---

## Bonus Challenges

- **Session-based testing**: Set a timer for exactly 25 minutes. Test with full focus. When the timer goes off, spend 5 minutes organizing your notes into a debrief. This is a real technique called Session-Based Test Management (SBTM).
- **Test the GET endpoints too**: Can you filter rooms by availability? What happens with invalid query parameters? Are the list endpoints paginated? What happens if there are thousands of records?
- **Check response headers**: Are there any security headers (CORS, Content-Security-Policy, etc.)? Is the Content-Type always correct?
- **Compare code to behavior**: Read the controller and service code in `app/src`. When the API does something unexpected, find the exact line of code responsible. Understanding WHY a bug exists makes your bug report much more useful to developers.
