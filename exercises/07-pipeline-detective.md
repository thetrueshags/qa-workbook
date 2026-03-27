# Exercise 07: Pipeline Detective

## Scenario

The StaySync deployment pipeline has been "working" for months -- code gets built and deployed to production on every push to main. But bugs keep making it to production. Last sprint, three separate issues reached customers that should have been caught before deployment. The team lead is frustrated and has asked you to investigate the pipeline.

Your mission: figure out why the pipeline isn't catching quality issues. Treat this like a detective case. The pipeline is the suspect. The evidence is in `pipelines/azure-pipelines.yml`.

---

## Background

**CI/CD (Continuous Integration / Continuous Delivery)**: The practice of automatically building, testing, and deploying code whenever changes are pushed. The goal is to catch problems early and deploy safely.

**Pipeline**: The automated sequence of stages that code passes through on its way to production. Think of it as an assembly line with quality checkpoints.

**Azure Pipelines**: Microsoft's CI/CD platform. The pipeline is defined in a YAML file that describes what happens at each stage. Each stage contains jobs, and each job contains steps.

**A healthy pipeline typically flows like this**:

```
Code Push
   |
   v
[Build] --> Compile the code, create artifacts
   |
   v
[Test] --> Run unit tests, integration tests, static analysis
   |
   v
[Deploy to Staging] --> Deploy to a non-production environment
   |
   v
[Test in Staging] --> Run smoke tests, E2E tests against staging
   |
   v
[Approval Gate] --> A human reviews and approves
   |
   v
[Deploy to Production] --> Deploy to the real environment
   |
   v
[Smoke Test Production] --> Quick verification that prod is working
```

Each stage acts as a quality gate. If any stage fails, the pipeline stops and the code doesn't advance. This is the principle of "shift-left testing" -- catching problems as early as possible, when they're cheapest to fix.

---

## Tasks

### Task 1 --- Map the pipeline

Open `pipelines/azure-pipelines.yml` and create a map of what it does.

**First, the trigger**:
- What branch triggers this pipeline?
- Does it run on pull requests, or only on pushes to main?

**Then, the stages**:
Draw the stages in order, including what depends on what. For each stage, list:
- The stage name and display name
- What it depends on (the `dependsOn` field)
- Every step within it and what that step does

Your diagram might look something like this (fill in the real values):

```
Stage: _______ ("_______")
  |  Step 1: _______
  |  Step 2: _______
  |
  v  (depends on: _______)
Stage: _______ ("_______")
  |  Step 1: _______
  |
  v  (depends on: _______)
Stage: _______ ("_______")
  |  Step 1: _______
```

---

### Task 2 --- Find the order problem

Look at your diagram from Task 1. Now compare it to the healthy pipeline flow from the Background section.

- In what order do Build, Test, and Deploy run in this pipeline?
- What order SHOULD they run in?
- What is the consequence of the current order? Walk through a specific scenario:
  1. A developer pushes code with a bug to main.
  2. The Build stage runs. What happens?
  3. The Deploy stage runs. What happens?
  4. The Test stage runs. What happens?
  5. The tests fail and reveal the bug. But the code is already in production. Now what?

This is not a hypothetical. This is exactly the kind of thing that happens when pipeline stages are in the wrong order. Tests that run AFTER deployment are an autopsy, not a prevention.

---

### Task 3 --- Look at the details

Beyond the ordering problem, there are several issues hiding in the details of each stage. Go through each one.

**Build stage**:

1. **Skipped tests**: Look at the Maven build step. It uses `-DskipTests`. This means the code is compiled but NOT tested during the build.
   - Why is this a problem? The build stage is the first quality gate, and it's wide open.
   - Even if tests run later (which they do, sort of), skipping them here means you build and push a Docker image that might contain broken code.
   - Is there ever a legitimate reason to skip tests in a build? (Build speed? Flaky tests? Think about the tradeoffs.)

2. **Image tagging**: The Docker step tags the image with `$(Build.BuildId)`. This is a unique identifier for each pipeline run -- something like `20240315.1`.
   - This is actually a GOOD practice. Why? (Think about traceability.)
   - But now look at the Deploy stage...

**Deploy stage**:

3. **Tag mismatch**: The Deploy stage uses `$(dockerRegistry)/$(imageName):latest` as the container image. But the Build stage tagged the image with `$(Build.BuildId)`, not `latest`.
   - What image does the Deploy stage actually deploy? Is it the one that was just built?
   - If someone pushed a different image tagged `latest` between the Build and Deploy stages, what would happen?
   - What tag should the Deploy stage use to guarantee it deploys the exact image that was just built and (theoretically) tested?

4. **No staging environment**: The Deploy stage goes directly to production. There's no staging or pre-production environment.
   - What would a staging deployment allow you to verify?
   - What types of problems can only be found in a production-like environment?

5. **No approval gate**: There's no human approval required before deploying to production.
   - For a booking platform handling real customer data and real money, should a human review and approve the deployment?
   - When might automatic deployment be acceptable, and when should it require approval?

6. **No failure notifications**: If any stage fails, who finds out? Is there a notification step?
   - In the current setup, the pipeline would fail silently. The developer might not even know their deployment failed unless they check the Azure Pipelines dashboard.

**Test stage**:

7. **Wrong position**: You already identified this in Task 2, but also consider: the tests run against the source code in the repository, not against the deployed application. These are unit tests, not integration tests or end-to-end tests. Even if the order were correct, is this sufficient?

---

### Task 4 --- Redesign the pipeline

Now that you've found the problems, design the pipeline you think StaySync should have. You can sketch this on paper, describe it in plain text, or write it as YAML if you want the challenge.

Your redesigned pipeline should address:

**Stage order**: What stages do you need, and in what order?

Consider this structure:
1. **Build & Unit Test**: Compile the code AND run unit tests. If tests fail, stop here.
2. **Static Analysis / Linting**: Run code quality checks (optional but recommended).
3. **Build Docker Image**: Create the container image, tagged with the build ID.
4. **Deploy to Staging**: Deploy to a staging environment.
5. **Integration Tests in Staging**: Run API tests, Selenium tests against the staging environment.
6. **Approval Gate**: A team lead or QA engineer reviews and approves.
7. **Deploy to Production**: Deploy the exact same image to production.
8. **Smoke Tests**: Run a quick sanity check against production.
9. **Notification**: Report success or failure to the team.

**For each stage, answer**:
- What does it do?
- What triggers it? (automatic, or manual approval?)
- What happens if it fails? Who is notified?
- How does it connect to the testing work from earlier exercises? (Where do the SQL checks from Exercise 01 fit? The Selenium tests from Exercise 03? The monitoring checks from Exercise 06?)

**Tag consistency**: How do you ensure the image built in stage 3 is the exact image deployed in stages 4 and 7?

**Rollback plan**: What happens if the production deployment causes problems? How do you roll back? Should the pipeline support automatic rollback if smoke tests fail?

---

## Reflection Questions

1. **The pipeline technically "works" -- it builds and deploys code. Why is "working" not the same as "correct"?** A pipeline that deploys untested code to production isn't broken in the mechanical sense. It does what it's told to do. But it's fundamentally wrong in terms of what it SHOULD do. What's the lesson here about "works on my machine" vs. "works correctly"?

2. **How does "shift-left testing" apply here?** Shift-left means moving testing earlier in the development process. The current pipeline is the opposite -- testing is shifted as far RIGHT as possible (after deployment). What's the cost of finding bugs late vs. early? Think in terms of time, money, customer impact, and team morale.

3. **As a QA engineer, what's your role in pipeline design?** Some teams treat the pipeline as "DevOps territory." But who better to define where quality gates should go than the person responsible for quality? How would you advocate for pipeline changes in a team where CI/CD isn't traditionally "your job"?

4. **How do the pipeline issues connect to issues you found in other exercises?** The `-DskipTests` flag here echoes the same flag in the Dockerfile (Exercise 04). The `latest` tag connects to the Kubernetes deployment (Exercise 05). The lack of monitoring integration connects to Exercise 06. What pattern do you see?

---

## Bonus Challenges

- **Write the fixed pipeline**: Rewrite `pipelines/azure-pipelines.yml` with the correct stage order, proper testing, staging deployment, approval gates, and notifications. Use Azure Pipelines YAML syntax. (It's okay to look up the syntax -- real engineers do this constantly.)

- **Add Selenium tests to the pipeline**: In the earlier exercises, you worked with Selenium tests in the `selenium-tests/` folder. Write a pipeline stage that runs these tests against the staging environment after deployment. What does the stage need? (A running browser? A URL to test against? A way to report results?)

- **Design a rollback strategy**: Write a pipeline stage that runs smoke tests after production deployment and automatically triggers a rollback if they fail. What does "rollback" mean in Kubernetes? (Hint: `kubectl rollout undo`.) What are the risks of automatic rollback?

- **Calculate the blast radius**: The current pipeline deploys every push to main directly to production with no tests. Estimate: in a team of 5 developers each pushing 2-3 times a day, how many untested deployments happen per week? If even 5% of those introduce a bug, how many bugs reach production per month? Now calculate the same numbers with a proper pipeline that catches 90% of issues before deployment.
