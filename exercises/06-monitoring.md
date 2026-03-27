# Exercise 06: Monitoring and Observability

## Scenario

StaySync has been running in production for about a month. The team set up "monitoring" early on and felt good about it. But there's a pattern emerging: customers keep discovering problems before the engineering team does. Last week a database connection issue went unnoticed for nearly an hour -- until support tickets started flooding in. The week before, response times degraded gradually over two days, and nobody noticed until a customer tweeted about it.

You've been asked to review the monitoring setup and figure out why the team is always the last to know. But this time, you're not just reading config files -- you're going to run the monitoring stack and see it in action.

---

## Background

**Metrics**: Numerical measurements about your system, collected over time. Examples: number of HTTP requests per second, error rate, average response time, CPU usage, memory consumption, database connection pool size. Metrics let you answer questions like "how is the system performing right now?" and "is this getting better or worse?"

**Prometheus**: An open-source monitoring system that collects metrics by "scraping" -- it reaches out to your application at regular intervals and pulls the latest numbers. Prometheus stores this time-series data and lets you query it. It also evaluates alert rules.

**PromQL**: Prometheus Query Language. Think of it like SQL, but for time-series data instead of database rows. Where SQL asks "which bookings were created today?", PromQL asks "how many HTTP requests per second happened in the last 5 minutes?" You'll use it in this exercise.

**Alert rules**: Conditions that, when true, fire a notification. For example: "if the error rate is above 5% for more than 5 minutes, send a page to the on-call engineer." Good alerts catch real problems early. Bad alerts either miss problems (thresholds too high) or cry wolf constantly (thresholds too low).

**Prometheus `for` duration**: When an alert rule includes `for: 10m`, Prometheus waits for the condition to be continuously true for 10 minutes before actually firing the alert. This prevents alerts from firing on brief, harmless blips. But set it too long and you're just... waiting while customers suffer.

**Grafana**: An open-source visualization platform. Prometheus stores the metrics and lets you query them, but Grafana makes them visual -- dashboards, graphs, gauges. Most teams use Grafana (or something like it) as the thing they actually look at day-to-day.

**Observability**: A broader concept than monitoring. Observability is the ability to understand what's happening inside your system from the outside, using three pillars: **metrics** (numbers over time), **logs** (detailed event records), and **traces** (the path of a single request through your system). Monitoring tells you THAT something is wrong. Observability helps you figure out WHY.

**Datadog**: A commercial observability platform that brings metrics, logs, traces, and alerting into one product. Many companies use it instead of (or alongside) Prometheus. The concepts are the same even if the tools differ.

---

## Tasks

### Task 1 --- Start the monitoring stack

The monitoring directory contains a docker-compose file with everything you need: the PostgreSQL database, the StaySync API, Prometheus, and Grafana.

Start it up:

```bash
docker compose -f monitoring/docker-compose.yml up --build -d
```

Wait a minute or two for everything to start (the API takes a moment to build and boot). You can check the status with:

```bash
docker compose -f monitoring/docker-compose.yml ps
```

All four services should show as running. If the API is restarting, give it another 30 seconds -- it may be waiting for the database to accept connections.

Once everything is up, verify the services are reachable:

- **API**: Open http://localhost:8080/actuator/health -- you should see `{"status":"UP"}`
- **Prometheus**: Open http://localhost:9090 -- you should see the Prometheus UI
- **Grafana**: Open http://localhost:3000 -- you should see the Grafana home page (no login required)

---

### Task 2 --- Explore Prometheus

Open the Prometheus UI at http://localhost:9090.

**Check the targets**

Go to Status > Targets (or navigate to http://localhost:9090/targets). This page shows you every service Prometheus is scraping and whether the scrape is succeeding.

- Is the `staysync-api` target showing as UP?
- Is the `prometheus` target (Prometheus monitoring itself) showing as UP?
- If either shows as DOWN, what does the error message tell you? (This is exactly the kind of troubleshooting QA engineers do -- the config says one thing, reality says another.)

**Run some PromQL queries**

Go back to the main Prometheus page (the "Graph" tab). The query box at the top accepts PromQL. Try these queries one at a time -- type (or paste) the query and click "Execute." Switch between the "Table" view (current values) and the "Graph" view (values over time).

**Query 1**: `up`

This is the simplest possible query. It returns 1 for every target that is currently reachable, and 0 for any target that is down. You should see entries for `staysync-api` and `prometheus`.

- What would it mean if `up{job="staysync-api"}` returned 0?

**Query 2**: `http_server_requests_seconds_count`

This shows the total number of HTTP requests the API has handled, broken down by method, URI, and status code. You'll probably see requests to `/actuator/prometheus` (that's Prometheus scraping the API) and maybe `/actuator/health`.

- Notice the labels: `method`, `uri`, `status`. These let you slice the data. Try: `http_server_requests_seconds_count{status="200"}` to see only successful requests.
- If this query returns nothing, the API might still be starting up. Wait 30 seconds and try again.

**Query 3**: `jvm_memory_used_bytes`

This shows how much memory the JVM is using, broken down by memory area (heap, non-heap, etc.). Switch to the Graph view to see it over time.

- Is memory usage stable, or trending upward? (It's just started, so there's no trend yet -- but in production, an upward trend could signal a memory leak.)

**Generate some traffic, then query again**

The metrics are more interesting when there's actual traffic. Open a new terminal and make some API calls:

```bash
# List all rooms
curl http://localhost:8080/api/rooms

# Create a booking
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"roomId": 1, "guestName": "Test Guest", "checkIn": "2026-04-01", "checkOut": "2026-04-03"}'

# List all bookings
curl http://localhost:8080/api/bookings

# Try an endpoint that might not exist (to generate a 404)
curl http://localhost:8080/api/nonexistent
```

Now go back to Prometheus and run `http_server_requests_seconds_count` again. You should see new entries for the endpoints you just called. Look at the `status` label -- you should see 200s from the successful requests and a 404 from the nonexistent endpoint.

**Try to write an error rate query**

This is harder. The error rate is the proportion of requests that returned a 5xx status. In PromQL:

```promql
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])
```

- `rate(...[5m])` calculates the per-second rate over the last 5 minutes
- `{status=~"5.."}` is a regex matcher: any status starting with 5 (500, 503, etc.)
- Dividing errors by total gives you the error rate as a fraction (0.05 = 5%)

Try running this query. If you haven't generated any 5xx errors, the result will be empty (which is good -- no errors!). Can you think of a way to cause a 5xx error? (Hint: try sending a booking with invalid data or a room ID that doesn't exist.)

---

### Task 3 --- Explore Grafana

Open Grafana at http://localhost:3000. You're logged in automatically as an admin (we configured anonymous auth so you don't have to deal with passwords during the exercise).

**Add Prometheus as a data source**

Grafana doesn't know where your metrics are until you tell it.

1. Click the gear icon (Configuration) in the left sidebar, then "Data sources"
2. Click "Add data source"
3. Select "Prometheus"
4. In the URL field, enter: `http://prometheus:9090` (Grafana reaches Prometheus over the Docker network, not localhost)
5. Scroll down and click "Save & test" -- you should see a green "Data source is working" message

**Create a dashboard**

1. Click the "+" icon in the left sidebar, then "New dashboard"
2. Click "Add visualization"

**Panel 1: Request count over time**

- Select your Prometheus data source
- In the query editor, enter: `rate(http_server_requests_seconds_count[5m])`
- This shows the per-second rate of requests, broken down by endpoint and status
- Give the panel a title like "Request Rate"
- Click "Apply" to save the panel

**Panel 2: JVM memory usage**

- Add another panel (click the "Add" button at the top of the dashboard)
- Query: `jvm_memory_used_bytes{area="heap"}`
- Title: "JVM Heap Memory"
- Under "Standard options," set the unit to "bytes (IEC)" so the Y-axis shows MB/GB instead of raw numbers
- Click "Apply"

**Panel 3: Error rate (optional challenge)**

- Add another panel
- Query: `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))`
- Title: "Error Rate"
- Under "Standard options," set the unit to "Percent (0.0-1.0)"
- Click "Apply"

Save the dashboard (Ctrl+S or the save icon). Give it a name like "StaySync Overview."

Take a moment to look at what you've built. This is what the on-call engineer would look at when they get paged at 2 AM. Does it give them enough information to quickly understand what's happening? What would you add?

---

### Task 4 --- Review the Prometheus configuration

Now that you've seen the live system, go back to the config files with fresh eyes.

Open `monitoring/prometheus.yml` and understand what it does.

- What "jobs" are configured? What does each one scrape?
- What endpoint does Prometheus scrape on the StaySync API? (Check the `metrics_path` value, and cross-reference with `app/src/main/resources/application.properties` to confirm the app actually exposes that endpoint.)
- How often does Prometheus scrape? (Check `scrape_interval`.) Is 15 seconds reasonable, or would you change it?
- What metrics would you expect a Spring Boot application with Actuator to expose? (Examples: HTTP request counts, response times, JVM memory usage, active database connections.)

Now think about what's NOT being scraped:
- Is the database being monitored? PostgreSQL can expose metrics through an exporter, but is one configured?
- If the Prometheus instance itself goes down, who monitors the monitor?

---

### Task 5 --- Review the alert rules

Open `monitoring/alerts.yml` and evaluate each alert rule. For every alert, ask: "Would this actually catch the problem in time to matter?"

You can also see the alert rules live in Prometheus: go to http://localhost:9090/alerts. This shows you the current state of each alert (inactive, pending, or firing). Since your system is healthy, they should all be inactive.

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

Try it live: stop the API container with `docker stop staysync-monitoring-api`. Go to the Prometheus alerts page and watch the InstanceDown alert change from "inactive" to "pending." (It won't reach "firing" unless you wait 30 minutes -- which is exactly the problem.)

Start it back up when you're done: `docker start staysync-monitoring-api`

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

### Task 6 --- What's missing?

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

### Task 7 --- Datadog concepts

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

## Cleanup

When you're done with the exercise, stop the monitoring stack:

```bash
docker compose -f monitoring/docker-compose.yml down
```

Add `-v` if you also want to remove the database volume:

```bash
docker compose -f monitoring/docker-compose.yml down -v
```

---

## Reflection Questions

1. **What's the difference between "monitoring" and "observability"?** The team has monitoring -- Prometheus is scraping metrics and there are alert rules defined. But do they have observability? Can they answer "why is the error rate high?" or just "the error rate IS high"? What would they need to add?

2. **Why should QA care about monitoring? Isn't that an ops/SRE responsibility?** Think about it from several angles: How does monitoring data inform your test strategy? How can alert quality be "tested"? When a customer reports a bug, how does monitoring help you reproduce it?

3. **How can monitoring data help you write better tests?** If you know that the booking endpoint has a 2% error rate in production, that tells you something about what to test. If you know response times spike every day at 3 PM, that points you toward load testing. Production monitoring and testing are not separate activities -- they're two views of the same goal.

4. **An alert that nobody acts on is worse than no alert at all. Why?** Think about alert fatigue. If the team gets 50 "warning" alerts a day and most of them are false positives or low-priority, what happens when a real critical alert fires?

5. **You've now seen monitoring from both sides -- the config files AND the live dashboards. Which gave you more insight?** Think about what you learned from reading `prometheus.yml` and `alerts.yml` versus what you learned from actually running queries and building dashboards. How does hands-on experience change the way you'd review a monitoring setup in a PR?

---

## Bonus Challenges

- **Fix the alert rules**: Rewrite `monitoring/alerts.yml` with improved thresholds, appropriate severity levels, and reasonable `for` durations. Add at least two new alerts for scenarios that aren't currently covered.

- **Write a runbook**: For each alert, write a brief "runbook" -- a document that tells the on-call engineer what to do when the alert fires. Include: what the alert means, what to check first, common causes, and how to resolve them. This is a real thing SRE teams create, and QA engineers often help write them.

- **Design an SLO**: An SLO (Service Level Objective) is a target for reliability -- for example, "99.9% of booking requests will succeed within 500ms." Define SLOs for StaySync. What metrics would you track? What alerts would you derive from them? How do SLOs change the conversation from "is the system up?" to "are customers happy?"

- **Trace the monitoring gap**: The scenario says customers found problems before the team did. For each of the three existing alerts, construct a realistic scenario where the problem would be bad enough for customers to complain but NOT bad enough to trigger the current alert. This demonstrates exactly why the thresholds are wrong.

- **Break something and watch**: Intentionally cause problems (stop the database, send malformed requests in a loop, set JVM memory limits low) and observe how the metrics react in Prometheus and Grafana. This builds intuition for what "trouble" looks like in monitoring data -- the kind of intuition that makes QA engineers invaluable during incidents.
