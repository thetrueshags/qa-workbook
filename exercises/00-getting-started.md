# Exercise 00: Getting Started

## Scenario

It's your first day on the QA team for **StaySync**, a hotel booking platform. Before you can test anything, you need to understand what you're testing. The best testers aren't the ones who find the most bugs on day one — they're the ones who take time to understand the system deeply enough to find the bugs nobody else thinks to look for.

Your goal in this exercise is to build a mental model of StaySync: what it does, how it's built, and where the risks might be.

---

## Background

StaySync is a hotel booking platform with a REST API backend, a PostgreSQL database, and (eventually) a front-end. The codebase lives in this repository. You have access to everything — source code, database schemas, configuration files, deployment scripts.

Nobody is going to walk you through every file. That's realistic. In QA, you often inherit a system with incomplete documentation, and your ability to orient yourself quickly is one of your most valuable skills.

---

## Tasks

### Task 1 — Read the README

Start with the project README at the repository root.

- What does it tell you about how to run the application?
- What does it NOT tell you that you wish it did?
- Are there any claims in the README you'd want to verify?

### Task 2 — Map the folder structure

Look at the top-level folders in the project. For each one, open it and figure out what it contains.

| Folder | What's in it? | What's it for? |
|--------|--------------|----------------|
| `app/` | | |
| `database/` | | |
| `docker/` | | |
| `selenium-tests/` | | |
| `k8s/` | | |
| `monitoring/` | | |
| `pipelines/` | | |

Don't skip the ones that seem "not your job." QA engineers who understand deployment, monitoring, and infrastructure find entire categories of bugs that others miss.

### Task 3 — Draw the architecture

On paper, a whiteboard, or whatever you have handy, draw a diagram showing:

- The main components of the system (app, database, etc.)
- How they connect to each other
- What ports they use
- Where data flows when a user makes a booking

This doesn't need to be pretty. It needs to be accurate. You'll update it as you learn more.

### Task 4 — Read the application code

Open the `app/src` folder and browse through the source code. You don't need to understand every line of Java — that's not the point. You're looking for the big picture.

Try to answer these questions:

- **Entities**: What are the main domain objects? (Hint: look for model/entity classes.) What fields does each one have?
- **Endpoints**: What API endpoints does the app expose? What HTTP methods do they use? What do they accept and return?
- **Business logic**: Where does the interesting logic live? Are there services or controllers that do calculations, validations, or decision-making?
- **What's missing?** Are there things you'd expect a hotel booking system to have that aren't here?

### Task 5 — Read the database schema

Open `database/init.sql` and study the schema.

- What tables exist?
- What are the relationships between them? (Look for foreign keys.)
- What constraints are defined? (NOT NULL, UNIQUE, CHECK, etc.)
- What constraints are conspicuously ABSENT?

Sketch a quick entity-relationship diagram if it helps you think.

---

## Reflection Questions

Take a few minutes to think about these. There are no wrong answers — these are about building your QA instincts.

1. **What areas of this system would you want to test first, and why?** Think about where the highest risk is. Where could bugs cause the most damage to users? To the business?

2. **If you could only run 5 tests before a release, what would they be?** This forces you to think about what matters most. Real QA work constantly involves prioritization.

3. **What documentation is missing that would help you do your job?** This is a practical question. If you could ask the dev team to write one document, what would it be?

---

## Bonus Challenges

- Compare the database schema to the application entities. Are they perfectly aligned, or are there differences? What are the implications?
- Look at the Docker and deployment configuration. What environment variables does the app need? What happens if one is wrong?
- Check the `monitoring/` folder. What's being monitored? What ISN'T being monitored that should be?
- Look at `pipelines/`. What does the CI/CD pipeline do? Is there a test stage? What happens if tests fail?
