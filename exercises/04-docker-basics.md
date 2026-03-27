# Exercise 04: Docker --- Running Locally

## Scenario

You need to get the full StaySync system running on your local machine so you can test it properly. The team uses Docker for local development -- it lets everyone run the same stack (the API, the database, monitoring) without installing everything natively.

Someone on the team set up the Docker configuration a while ago and said it "worked on my machine." Your job is to get it working on YOUR machine. But here's the thing: don't just blindly run commands and hope for the best. **Read the configuration first.** You'll be amazed how many issues you can spot before anything runs.

This is a real QA superpower: catching problems by reading configs and code, not just by clicking buttons and waiting for things to break.

---

## Background

A few concepts before we dive in:

**Container**: A lightweight, isolated environment for running an application. Think of it like a shipping container for software -- everything the app needs is packaged inside, so it runs the same way everywhere. Unlike a full virtual machine, a container shares the host's operating system, making it fast and efficient.

**Docker image**: A snapshot of everything needed to run an application (code, runtime, libraries, configuration). Containers are running instances of images.

**Dockerfile**: The recipe for building a Docker image. It's a step-by-step script: start with a base image, copy files in, run commands, define how to start the app.

**docker-compose**: A tool for defining and running multiple containers together. Instead of starting each container manually, you describe all of them in one YAML file and bring them all up with a single command.

**Multi-stage build**: A Dockerfile technique where you use one stage to build the application (with all the build tools) and a separate stage to run it (with only the runtime). This keeps the final image small and clean.

---

## Tasks

### Task 1 --- Read before you run

Before typing a single Docker command, open the configuration files and understand what they describe.

**Open `docker/docker-compose.yml` and study it.**

For each service defined in the file, answer:
- What is the service named?
- What image does it use (or how is it built)?
- What ports does it expose?
- What environment variables does it set?
- What volumes does it mount?

Fill in a table like this:

| Service | Image / Build | Ports | Purpose |
|---------|--------------|-------|---------|
| | | | |
| | | | |
| | | | |

**Now open `app/Dockerfile` and read through it.**

- How many stages does the build have? What are they called?
- What base image does each stage use?
- What happens in each stage?
- What port does the final container expose?

Don't rush this. When you read infrastructure files carefully, you develop an instinct for spotting misconfigurations -- and that instinct will serve you throughout your career.

---

### Task 2 --- Spot the issues (before running anything)

Now read through both files again, this time with a critical eye. You're looking for things that don't add up -- mismatches, missing pieces, typos, and assumptions that might not hold.

**Issue hunt in `docker/docker-compose.yml`:**

1. **Look at the `SPRING_DATASOURCE_URL` environment variable for the `staysync-api` service.** It contains a hostname that the application uses to connect to the database. In Docker Compose networking, containers communicate with each other using their **service names** as hostnames (not `localhost`, because each container has its own `localhost`). What hostname does the URL currently use? What SHOULD it use?

2. **Look at service startup order.** When you run `docker compose up`, all services start roughly at the same time. The API needs the database to be available. Is there anything in the compose file that tells Docker about this dependency? What might happen if the API starts and tries to connect before the database is ready?

3. **Look at the Prometheus volume mount very carefully.** It maps a file from the `monitoring/` folder into the Prometheus container. Compare the filename in the volume path to the actual files in the `monitoring/` folder. Letter by letter. Do they match?

Write down every issue you find. For each one, note:
- What the problem is
- What would happen when you try to run it
- What the fix should be

**Issue hunt in `app/Dockerfile`:**

4. **Compare the base images for the two stages.** The build stage uses one Java version, and the runtime stage uses another. What are they? Why might this cause problems?

---

### Task 3 --- Fix and run

Now let's see if your analysis was right.

**Step 1**: Before making any changes, try running the compose file as-is and observe what happens:

```bash
docker compose -f docker/docker-compose.yml up --build
```

Watch the output carefully. Do you see errors? Warnings? Which services start and which fail? Do the failures match the issues you predicted in Task 2?

**Step 2**: Fix the issues you found, one at a time. After each fix, run the compose file again and see if the behavior changes.

Recommended fix order:
1. Fix the volume path typo first (this prevents Prometheus from starting)
2. Fix the database URL (this prevents the API from connecting to the database)
3. Add a `depends_on` for the database (this doesn't guarantee the database is ready, but it sets the startup order)

**Step 3**: Once the services are starting, verify they're actually working:

- Can you reach the API? Try: `curl http://localhost:8080/actuator/health`
- Can you reach Prometheus? Open `http://localhost:9090` in your browser
- Can you connect to the database? Use your database client from Exercise 01

**Step 4**: Note any issues that remain even after your fixes. Some problems are structural and can't be fixed with a single-line change. That's okay -- document them.

---

### Task 4 --- Examine the Dockerfile

Take another look at `app/Dockerfile`, now that you've seen the build in action.

1. **The version mismatch**: The build stage compiles the application using one Java version. The runtime stage runs it using a different, older Java version. Specifically:
   - What version does `maven:3.9-eclipse-temurin-21` use for building?
   - What version does `eclipse-temurin:17-jre` use for running?
   - Java 21 can produce bytecode and use features that Java 17 doesn't support. What would happen if the application used a Java 21 feature? Would the build succeed? Would the application start?

2. **The `-DskipTests` flag**: The Maven build command includes `-DskipTests`. This tells Maven to compile the code but skip running any unit tests.
   - Why might someone add this flag? (Think about build speed, or tests that don't pass yet.)
   - What's the risk? If tests are skipped during the Docker build, where else should they run?
   - Check the CI/CD pipeline (`pipelines/azure-pipelines.yml`). Does it also skip tests? What does that tell you?

3. **Build context**: The `docker-compose.yml` sets the build context to `../app`. This means Docker can access everything in the `app/` folder during the build. Is that reasonable? Are there any files that shouldn't be in the Docker image?

---

## Reflection Questions

1. **Why is "reading configs before running them" a valuable QA habit?** Think about the time you spent reading vs. the time you would have spent debugging blind. Which issues did you catch by reading that would have been hard to diagnose from error messages alone?

2. **How many of these issues would you have found by reading only?** How many by running only? Are there issues that you can ONLY find one way or the other?

3. **In a real deployment, what's the impact of each issue you found?** Rank them from "mild annoyance" to "production outage." Consider:
   - The `localhost` issue: The API silently can't reach the database. Every request fails.
   - The volume typo: Monitoring doesn't work. You're flying blind.
   - The missing `depends_on`: Intermittent startup failures. The kind that "works sometimes."
   - The Java version mismatch: A ticking time bomb. Works until someone uses a Java 21 feature.

4. **Who should be responsible for Docker configuration -- developers, DevOps, or QA?** Is there a reason all three should care?

---

## Bonus Challenges

- **Add a healthcheck to docker-compose**: Docker supports a `healthcheck` directive that periodically checks if a service is healthy. Add one for the API service that hits the `/actuator/health` endpoint. What's the difference between Docker's `depends_on` and a healthcheck-based dependency?

- **Make the Dockerfile more robust**: Fix the Java version mismatch. Should you upgrade the runtime to 21, or downgrade the build to 17? What are the tradeoffs?

- **Add the alert rules**: The Prometheus container has the metrics config, but the alert rules from `monitoring/alerts.yml` aren't mounted. How would you add them? (You'll review whether those alert rules are any good in Exercise 06.)

- **Trace a full request**: With all services running, make a booking through the API (use curl or Postman). Then check the database to see if the data was saved. Then check Prometheus to see if the request was recorded as a metric. This end-to-end trace is the foundation of integration testing, which you'll do formally in Exercise 08.
