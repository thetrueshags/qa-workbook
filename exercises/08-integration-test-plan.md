# Exercise 08: Integration Test Plan

## Scenario

The team has been fixing bugs -- the pricing calculation from the SQL investigation, the duplicate guest records, the overlapping bookings. They've also been working on infrastructure improvements based on the issues you flagged in Docker, Kubernetes, monitoring, and the pipeline.

Now they want to do a proper release. Before the code goes out, someone needs to answer a simple but important question: **does everything work together?**

You've been asked to write the Integration Test Plan. This is the document that describes how you'll verify the entire system -- not just individual components, but how they interact. It's the difference between "the API works" and "the API works with the database, in Docker, monitored by Prometheus, deployed through the pipeline."

This exercise pulls together everything you've learned in the workshop. Every earlier exercise was testing a piece of the system. Now you're thinking about the whole picture.

---

## Background

**Unit testing**: Testing a single component in isolation. "Does this function return the right value?" Unit tests are fast, focused, and run by developers constantly. They're essential, but they can't tell you whether the components work together.

**Integration testing**: Testing how components interact with each other. "When the API saves a booking, does it actually appear in the database with the correct data?" Integration tests cross boundaries -- they involve real databases, real network calls, real configuration.

**End-to-end (E2E) testing**: Testing complete user workflows across the full system. "Can a user search for a hotel, select a room, make a booking, and receive a confirmation?" E2E tests exercise the entire stack from front to back.

**Test plan**: A communication document. Yes, it guides testing, but its primary purpose is to align the team on what will be tested, what won't be tested, how "done" is defined, and what risks remain. A good test plan answers the question your manager and your teammates are really asking: "Are we confident this release is safe?"

---

## Tasks

### Task 1 --- Identify the integration points

An integration point is any place where two components connect and exchange data. Start by listing every integration point in StaySync.

For each one, describe:
- What component talks to what
- What data flows between them
- What protocol or mechanism they use
- What could go wrong at this boundary

Here are some to get you started (there are more -- find them):

**API to Database**:
- The Spring Boot application connects to PostgreSQL via JDBC
- Data flows: bookings, guests, rooms (CRUD operations)
- What could go wrong: connection failures, schema mismatches, data type issues, constraint violations, connection pool exhaustion

**Docker to Application**:
- The Docker container runs the Java application
- Configuration flows through environment variables (database URL, credentials)
- What could go wrong: wrong environment variables, port mismatches, Java version incompatibilities (remember the Dockerfile issue from Exercise 04)

**Kubernetes to Docker Image**:
- Kubernetes pulls and runs the Docker image
- Configuration flows through ConfigMaps, Secrets, and the deployment manifest
- What could go wrong: image tag mismatch (Exercise 05 and 07), wrong container port, missing health checks, insufficient resources

**Prometheus to Application**:
- Prometheus scrapes the `/actuator/prometheus` endpoint
- Metrics data flows from the app to Prometheus
- What could go wrong: wrong scrape endpoint, network connectivity between containers, actuator not enabled

**Pipeline to Everything**:
- The CI/CD pipeline builds, tests, and deploys
- Artifacts flow from source code to Docker image to Kubernetes cluster
- What could go wrong: tests skipped, wrong image deployed, staging skipped (Exercise 07)

**Database Schema to Application Entities**:
- The JPA entities in the application must match the database schema
- What could go wrong: column name mismatches, data type mismatches, missing constraints that the app assumes exist

What other integration points exist? Think about:
- Monitoring alerts to notification channels
- Load balancer to application
- External systems (payment, email) if they existed

---

### Task 2 --- Define test scenarios

For each integration point, write at least two test scenarios: one happy path and one failure scenario.

Use this format:

```
Integration Point: [Component A] <-> [Component B]

Scenario 1 (Happy Path): [Name]
  Preconditions: [What must be true before the test]
  Steps:
    1. [Action]
    2. [Action]
    3. [Verification]
  Expected Result: [What success looks like]

Scenario 2 (Failure): [Name]
  Preconditions: [What must be true before the test]
  Steps:
    1. [Action that should fail or degrade gracefully]
    2. [Verification]
  Expected Result: [What correct failure handling looks like]
```

Here are examples to calibrate the level of detail:

**Integration Point: API to Database**

```
Scenario 1 (Happy Path): Create a booking and verify persistence
  Preconditions: Database is running, at least one guest and one available room exist
  Steps:
    1. POST /api/bookings with valid booking data
    2. Note the booking ID from the response
    3. Query the database directly: SELECT * FROM bookings WHERE id = [returned ID]
    4. Compare every field in the API response to the database record
  Expected Result: All fields match. The booking exists in the database with
  the correct guest_id, room_id, dates, status, and calculated total_price.

Scenario 2 (Failure): API handles database connection loss gracefully
  Preconditions: Application is running, database is running
  Steps:
    1. Stop the database container (docker stop staysync-db)
    2. POST /api/bookings with valid booking data
    3. Observe the API response
  Expected Result: API returns a meaningful error (503 Service Unavailable or
  500 Internal Server Error with a clear message), not a stack trace. The API
  itself remains running and recovers when the database comes back.
```

Now write scenarios for:
- API to Database: at least 3 more scenarios (think about data types, constraints, concurrent bookings)
- Docker to Application: environment variable correctness, port mapping
- Prometheus to Application: metrics are actually collected and queryable
- End-to-end booking flow: from API request through database storage through metric recording
- Pipeline deployment: the deployed image matches what was built

Challenge yourself to think about edge cases and failure modes. The goal is not to list every possible test, but to identify the tests that would catch the most important problems.

---

### Task 3 --- Write the test plan

Now assemble your work into a formal test plan. This is the document you'd hand to your team lead or present in a planning meeting.

Use this structure (fill in each section):

---

**StaySync Release Test Plan**

**Version**: [Your version number]
**Author**: [Your name]
**Date**: [Today's date]

**1. Scope**

What is being tested:
- [List the integration points and workflows under test]

What is NOT being tested (and why):
- [Be explicit about what you're excluding. For example: "Performance/load testing is out of scope for this release. UI testing is out of scope because there is no frontend yet." Being clear about what you're NOT testing is just as important as what you ARE testing. It manages expectations.]

**2. Test Environment**

What needs to be running:
- [List every component and how to start it]
- [Specify versions, configurations, test data requirements]

Tools needed:
- [Database client, curl/Postman, Docker, browser for Prometheus, etc.]

**3. Prerequisites**

Before testing begins:
- [ ] [Database is seeded with test data]
- [ ] [All Docker services are running and healthy]
- [ ] [Specific issues from earlier exercises have been fixed -- list which ones]
- [ ] [Test accounts / test data are prepared]

**4. Test Scenarios**

[Include your scenarios from Task 2, organized by integration point]

**5. Entry Criteria**

Testing will begin when:
- [All services start without errors]
- [Database schema is up to date]
- [The build pipeline has passed (when the pipeline is fixed)]
- [What else?]

**6. Exit Criteria**

The release is ready when:
- [All critical and high-priority test scenarios pass]
- [No known critical bugs remain open]
- [Monitoring is confirmed working (alerts fire correctly for test failures)]
- [What else? Be specific. "All tests pass" is not enough -- which tests, at what pass rate?]

**7. Risks and Mitigations**

| Risk | Impact | Mitigation |
|------|--------|------------|
| [Database connection issues during testing] | [Tests can't run] | [Verify DB health first, have restart procedure ready] |
| [Test data conflicts between testers] | [False failures] | [Each tester uses isolated test data] |
| [Flaky tests due to timing] | [Unreliable results] | [Add retries, investigate root cause] |
| [...] | [...] | [...] |

---

Don't skip the risks section. Every test plan has risks, and acknowledging them is a sign of maturity, not weakness.

---

### Task 4 --- Communicate it

You have 5 minutes in the team standup to present your test plan. Nobody is going to read a multi-page document in a standup. You need to distill it.

Write down:

**Your 3 key talking points** (what the team absolutely needs to know):
1. [What you're testing and why]
2. [What the biggest risks are]
3. [What you need from the team to proceed]

**Your 1-sentence summary** (the elevator pitch):
- If someone asks "what's the test plan?", you should be able to answer in one sentence that covers scope, approach, and confidence level.

**Questions you anticipate the team will ask** (and your answers):
- "How long will testing take?"
- "What if we find a critical bug?"
- "Can we ship without [specific test] passing?"

Being able to communicate a plan concisely is as important as writing the plan itself. A brilliant test plan that nobody reads has zero value.

---

## Reflection Questions

1. **What's the hardest part about integration testing compared to unit testing?** Think about: environment setup, data management, test isolation, debugging failures, execution time, flakiness. Integration tests are harder to write, slower to run, and harder to debug. Why are they still essential?

2. **How do you decide when you've tested "enough"?** There's no such thing as testing everything. You have to make judgments about risk, coverage, and time. What principles guide that decision? How do you communicate your confidence level to stakeholders?

3. **Why is the test plan a communication tool, not just a testing tool?** Who reads a test plan? Developers (to understand what will be tested and what they need to fix), managers (to understand timeline and risk), other QA engineers (to execute the tests), and future team members (to understand the testing strategy). Each audience needs something different from the same document.

4. **How do all the exercises in this workshop connect?** This is the big-picture question. Think about the journey:
   - Exercise 00 (Getting Started) gave you the mental model of the system.
   - Exercise 01 (SQL) showed you how data issues manifest and how to investigate them.
   - Exercises 02-03 (API and Selenium) gave you the tools to test the application.
   - Exercise 04 (Docker) showed you how the local environment is configured -- and misconfigured.
   - Exercise 05 (Kubernetes) showed you how the production environment is configured -- and misconfigured.
   - Exercise 06 (Monitoring) showed you how the team (fails to) detect problems.
   - Exercise 07 (Pipeline) showed you how code gets from commit to production -- with all the gaps.
   - Now, Exercise 08 (this one) asks you to think about how ALL of it works together.

   QA is not a single skill. It's the ability to see the connections between all of these things and ask: "What could go wrong at each boundary, and how would we know?"

---

## Bonus Challenges

- **Execute the plan**: If you have the Docker environment running from Exercise 04, actually run through your test scenarios. Track the results in a simple pass/fail table. How many pass? How many fail? Are the failures expected (known issues from earlier exercises) or surprises?

- **Automate a scenario**: Pick one of your integration test scenarios and write it as an automated test. It could be a shell script using curl, a Java test using RestAssured, or a Selenium test. The format matters less than the thinking: how do you set up the preconditions, execute the test, and verify the result programmatically?

- **Write a bug report**: For every test scenario that fails, write a proper bug report with: summary, steps to reproduce, expected result, actual result, severity, and which exercise originally identified the root cause. This connects your test plan back to the detective work you did throughout the workshop.

- **Design the regression suite**: If you could pick 10 automated tests that run on every deployment (the "smoke suite"), which 10 would you pick from all your scenarios? These are the tests that give you the most confidence in the least time. Defending that choice is a real skill -- it forces you to think about what matters most.

- **Retrospective**: Write a one-page retrospective on the entire workshop. What did you learn? What surprised you? What would you do differently? What questions do you still have? This isn't an exercise with a right answer -- it's how you consolidate learning and identify your next growth areas.
