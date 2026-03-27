# QA Workbook — The StaySync Workshop

## Welcome

You've just joined **StaySync**, a hotel booking platform. Congratulations on your first day!

Unfortunately, things aren't going great. Customers are complaining about wrong prices. Bookings are duplicated. The last deployment failed. Monitoring dashboards are empty. The previous QA engineer left without much documentation.

Your job? Figure out what's going on, find the problems, and help the team get back on track.

## What This Workshop Is

This is a hands-on workbook designed to get you comfortable with the tools and thinking that make a great QA engineer. Each exercise is a realistic work scenario — the kind of thing you'd actually encounter on the job.

You won't be building anything from scratch. Instead, you'll be **investigating**, **testing**, **reviewing**, and **fixing** an existing system — which is what QA work actually looks like most of the time.

## The System

StaySync is a simple hotel booking platform with these components:

```
┌─────────────┐     ┌─────────────────┐     ┌────────────┐
│   Browser    │────>│  Booking API    │────>│ PostgreSQL │
│   (Guest)    │<────│  (Spring Boot)  │<────│  Database   │
└─────────────┘     └─────────────────┘     └────────────┘
       │                     │
       │              ┌──────┴──────┐
       │              │  Prometheus  │
       │              │  (Metrics)   │
       │              └─────────────┘
       │
  Selenium Tests
```

- **Booking API** — A Java Spring Boot REST API that manages hotel bookings
- **PostgreSQL** — Stores bookings, rooms, and guest data
- **Docker** — Packages and runs the application
- **Kubernetes** — Orchestrates the deployment
- **Prometheus** — Collects metrics from the application
- **Azure Pipelines** — Builds, tests, and deploys the code

## Exercises

| #  | Exercise | Skills | Time |
|----|----------|--------|------|
| 00 | [Getting Started](exercises/00-getting-started.md) | System understanding, QA mindset | 15 min |
| 01 | [SQL Investigation](exercises/01-sql-investigation.md) | SQL, data analysis, root cause thinking | 30 min |
| 02 | [Exploratory Testing](exercises/02-exploratory-testing.md) | Test design, bug reporting, critical thinking | 30 min |
| 03 | [Your First Selenium Tests](exercises/03-selenium-tests.md) | Selenium, test automation, Java (reading/writing) | 45 min |
| 04 | [Docker — Running Locally](exercises/04-docker-basics.md) | Docker, environment troubleshooting | 30 min |
| 05 | [Kubernetes — Deployment Review](exercises/05-kubernetes-review.md) | Kubernetes, risk assessment, config review | 30 min |
| 06 | [Monitoring & Observability](exercises/06-monitoring.md) | Prometheus, Grafana, Datadog concepts, alerting | 45 min |
| 07 | [Pipeline Detective](exercises/07-pipeline-detective.md) | Azure Pipelines, CI/CD, shift-left thinking | 30 min |
| 08 | [Integration Test Plan](exercises/08-integration-test-plan.md) | Test planning, cross-system thinking, communication | 30 min |

Exercises are designed to be worked through in order, but each one is mostly self-contained. Your facilitator will guide you through which exercises to focus on.

## Setup

**First time here?** Follow the [Setup Guide](exercises/SETUP.md) — it walks you through installing everything and verifying it works.

### Quick Start (if you've already done the setup)

```bash
git clone https://github.com/thetrueshags/qa-workbook.git
cd qa-workbook

# Start the database (needed for exercises 01-03)
docker compose -f database/docker-compose.yml up -d

# Verify it's running
docker ps
```

Further setup instructions are included in each exercise as needed.

## Tips for Success

- **Ask "what could go wrong?"** — This is the core QA question. Ask it constantly.
- **Document what you find** — Write things down. Screenshots, notes, queries. Good QA leaves a trail.
- **Don't just find bugs — explain them** — Anyone can say "it's broken." A good QA says *why* it's broken, *who* it affects, and *how bad* it is.
- **Be curious** — Poke around. Try unexpected inputs. Read error messages carefully.
- **It's okay to not know something** — That's what this workshop is for. Ask questions.
