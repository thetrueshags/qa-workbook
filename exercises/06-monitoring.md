# Exercise 06: Monitoring and Observability

## Scenario

StaySync has been running in production for about a month. The team set up "monitoring" early on and felt good about it. But there's a pattern emerging: customers keep discovering problems before the engineering team does. Last week a database connection issue went unnoticed for nearly an hour -- until support tickets started flooding in. The week before, response times degraded gradually over two days, and nobody noticed until a customer tweeted about it.

You've been asked to review the monitoring setup and figure out why the team is always the last to know.

---

## Background

**Metrics**: Numerical measurements about your system, collected over time. Examples: number of HTTP requests per second, error rate, average response time, CPU usage, memory consumption, database connection pool size. Metrics let you answer questions like "how is the system performing right now?" and "is this getting better or worse?"

**Prometheus**: An open-source monitoring system that collects metrics by "scraping" -- it reaches out to your application at regular intervals and pulls the latest numbers. Prometheus stores this time-series data and lets you query it. It also evaluates alert rules.

**Alert rules**: Conditions that, when true, fire a notification. For example: "if the error rate is above 5% for more than 5 minutes, send a page to the on-call engineer." Good alerts catch real problems early. Bad alerts either miss problems (thresholds too high) or cry wolf constantly (thresholds too low).

**Prometheus `for` duration**: When an alert rule includes `for: 10m`, Prometheus waits for the condition to be continuously true for 10 minutes before actually firing the alert. This prevents alerts from firing on brief, harmless blips. But set it too long and you're just... waiting while customers suffer.

**Observability**: A broader concept than monitoring. Observability is the ability to understand what's happening inside your system from the outside, using three pillars: **metrics** (numbers over time), **logs** (detailed event records), and **traces** (the path of a single request through your system). Monitoring tells you THAT something is wrong. Observability helps you figure out WHY.

**Datadog**: A commercial observability platform that brings metrics, logs, traces, and alerting into one product. Many companies use it instead of (or alongside) Prometheus. The concepts are the same even if the tools differ.

---

## Tasks

### Task 1 --- Review the Prometheus configuration

Open `monitoring/prometheus.yml` and understand what it does.

- What "jobs" are configured? What does each one scrape?
- What endpoint does Prometheus scrape on the StaySync API? (Check the `metrics_path` value, and cross-reference with `app/src/main/resources/application.properties` to confirm the app actually exposes that endpoint.)
- How often does Prometheus scrape? (Check `scrape_interval`.) Is 15 seconds reasonable, or would you change it?
- What metrics would you expect a Spring Boot application with Actuator to expose? (Examples: HTTP request counts, response times, JVM memory usage, active database connections.)

Now think about what's NOT being scraped:
- Is the database being monitored? PostgreSQL can expose metrics through an exporter, but is one configured?
- If the Prometheus instance itself goes down, who monitors the monitor?

---

### Task 2 --- Review the alert rules

Open `monitoring/alerts.yml` and evaluate each alert rule. For every alert, ask: "Would this actually catch the problem in time to matter?"

**Alert 1: InstanceDown**

```yaml
- alert: InstanceDown
  expr: up{job="staysync-api"} == 0
  for: 30m
  labels:
    severity: warning
```

Walk through this scenario:
- The StaySync API crashes at 2:00 PM.
- Prometheus detects it's down immediately (the scrape fails).
- The `for: 30m` means Prometheus waits 30 minutes with the API continuously down before firing the alert.
- The alert fires at 2:30 PM.
- The on-call engineer gets a "warning" (not an urgent page), so they check it when they get around to it.
- Meanwhile, every customer trying to book a hotel since 2:00 PM has seen errors.

Questions:
- Is 30 minutes a reasonable wait time for an API being completely down? What would you set it to?
- Is "warning" the right severity? If your primary API is unreachable, is that a warning or is that critical?
- How many bookings might be lost in 30 minutes? What's the business impact?

**Alert 2: HighErrorRate**

```yaml
- alert: HighErrorRate
  expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.5
  for: 5m
  labels:
    severity: warning
```

This fires when more than 50% of requests are returning 5xx errors, sustained for 5 minutes.

Questions:
- 50% means HALF of all requests are failing. Is that a reasonable threshold for a booking platform?
- At what error rate would YOU want to be alerted? 1%? 5%? 10%? Think about what your customers are experiencing.
- If 10% of booking requests are failing, that might be dozens of lost bookings per hour. Is that a "warning"?
- Should there be different thresholds for different severity levels? (For example: >1% = warning, >5% = critical?)

**Alert 3: HighResponseTime**

```yaml
- alert: HighResponseTime
  expr: http_server_requests_seconds_sum / http_server_requests_seconds_count > 2
  for: 5m
  labels:
    severity: warning
```

This fires when the average response time exceeds 2 seconds for 5 minutes.

Questions:
- Is 2 seconds a reasonable threshold? What's the customer experience at 2-second response times?
- This measures the average across ALL endpoints. Could a slow health-check endpoint mask a very slow booking endpoint? Or vice versa?
- Should different endpoints have different thresholds? (A search might tolerate 500ms; a booking confirmation should be under 200ms.)
- Once again: is "warning" the right severity?

---

### Task 3 --- What's missing?

The current alert rules cover three scenarios: instance down, high error rate, and slow responses. For a hotel booking platform, what else should be monitored?

Think about each of these categories and write at least one alert rule for each (plain English is fine; YAML is a bonus):

**Database connectivity**:
- What happens if the API can't reach the database? Is there currently an alert for this?
- Spring Boot Actuator exposes database health through the `/actuator/health` endpoint. The Prometheus metrics include `hikaricp_connections_active` (active DB connections) and similar metrics.
- Write an alert that fires when database connections are exhausted or when the connection pool is full for more than 2 minutes.

**Disk space / storage**:
- Databases grow. Logs grow. What happens when the disk fills up?
- Write an alert for disk usage approaching capacity (e.g., above 80%).

**Business metrics**:
- HTTP error rates tell you about technical failures, but not about business logic failures. A booking endpoint could return 200 OK but silently fail to actually create the booking (if there's a bug in the logic).
- What business-level metrics would you want? Examples: bookings created per hour, booking success rate, revenue processed.
- Write an alert that fires if the number of successful bookings drops below a historical baseline.

**Memory and CPU**:
- JVM applications can have memory leaks that slowly degrade performance.
- Write an alert for JVM heap usage consistently above 90%.

**Certificate and endpoint expiry**:
- SSL certificates expire. API keys expire. Domains expire.
- How would you monitor for these?

For each alert you propose, specify:
- What condition triggers it
- How long to wait before firing (`for` duration)
- What severity it should be
- Who should be notified and how

---

### Task 4 --- Datadog concepts

Many teams use Datadog instead of (or in addition to) Prometheus. The concepts are the same; the implementation differs. If the team were considering a migration to Datadog, think through these questions:

**Dashboards**:
- What dashboards would you create? Think about different audiences: the engineering team needs different views than the product team or the executives.
- Design a "QA team dashboard." What would be on it? Consider: test pass rates, deployment frequency, error rates before and after deployments, response time trends, open bugs vs. closed bugs.
- Design a "production health dashboard." What are the top 5 graphs you'd put front and center?

**Monitors (Datadog's term for alerts)**:
- Datadog supports anomaly detection (alerting when a metric deviates from its historical pattern, not just when it crosses a fixed threshold). Where would this be useful for StaySync?
- Datadog supports composite monitors (alerting when multiple conditions are true simultaneously). Give an example: when would you want an alert that fires only if BOTH the error rate is elevated AND response times are high?

**Logs**:
- What log messages would you want to search for and alert on?
- What's the relationship between a log entry that says "Database connection timeout" and a Prometheus metric showing zero active connections?

---

## Reflection Questions

1. **What's the difference between "monitoring" and "observability"?** The team has monitoring -- Prometheus is scraping metrics and there are alert rules defined. But do they have observability? Can they answer "why is the error rate high?" or just "the error rate IS high"? What would they need to add?

2. **Why should QA care about monitoring? Isn't that an ops/SRE responsibility?** Think about it from several angles: How does monitoring data inform your test strategy? How can alert quality be "tested"? When a customer reports a bug, how does monitoring help you reproduce it?

3. **How can monitoring data help you write better tests?** If you know that the booking endpoint has a 2% error rate in production, that tells you something about what to test. If you know response times spike every day at 3 PM, that points you toward load testing. Production monitoring and testing are not separate activities -- they're two views of the same goal.

4. **An alert that nobody acts on is worse than no alert at all. Why?** Think about alert fatigue. If the team gets 50 "warning" alerts a day and most of them are false positives or low-priority, what happens when a real critical alert fires?

---

## Bonus Challenges

- **Fix the alert rules**: Rewrite `monitoring/alerts.yml` with improved thresholds, appropriate severity levels, and reasonable `for` durations. Add at least two new alerts for scenarios that aren't currently covered.

- **Write a runbook**: For each alert, write a brief "runbook" -- a document that tells the on-call engineer what to do when the alert fires. Include: what the alert means, what to check first, common causes, and how to resolve them. This is a real thing SRE teams create, and QA engineers often help write them.

- **Design an SLO**: An SLO (Service Level Objective) is a target for reliability -- for example, "99.9% of booking requests will succeed within 500ms." Define SLOs for StaySync. What metrics would you track? What alerts would you derive from them? How do SLOs change the conversation from "is the system up?" to "are customers happy?"

- **Trace the monitoring gap**: The scenario says customers found problems before the team did. For each of the three existing alerts, construct a realistic scenario where the problem would be bad enough for customers to complain but NOT bad enough to trigger the current alert. This demonstrates exactly why the thresholds are wrong.
