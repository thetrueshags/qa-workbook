# Exercise 05: Kubernetes --- Deployment Review

## Scenario

The team wants to deploy StaySync to a Kubernetes cluster for production. They've written the Kubernetes manifests (the YAML files that describe what to deploy and how), and they've asked you to review them before anyone applies them to the cluster.

You don't need a running cluster for this exercise. This is purely a configuration review -- like a code review, but for infrastructure. You're reading YAML files and asking: "Is this production-ready? What could go wrong?"

This is one of the highest-value things a QA engineer can do. Catching a misconfiguration in review is cheap. Catching it during a production outage at 2 AM is expensive.

---

## Background

Kubernetes (often shortened to "K8s") orchestrates containers at scale. Here are the concepts you need for this exercise:

**Pod**: The smallest deployable unit in Kubernetes. A pod runs one or more containers together on the same node. If a pod dies, Kubernetes can create a new one.

**Deployment**: Manages a set of identical pods. You tell it "I want 3 replicas of this container" and it makes sure 3 are always running. If one crashes, it replaces it. If you push a new image, it rolls out the update gradually.

**Service**: A stable network endpoint that routes traffic to pods. Pods are ephemeral (they come and go), but a Service gives them a consistent address.

**ConfigMap**: Stores non-sensitive configuration as key-value pairs. Applications read their settings from here instead of hardcoding them.

**Secret**: Stores sensitive data (passwords, API keys, tokens). Similar to a ConfigMap, but the values are base64-encoded and Kubernetes treats them with extra care (access controls, encryption at rest). **ConfigMaps and Secrets are not interchangeable** -- passwords belong in Secrets, not ConfigMaps.

**StatefulSet**: Like a Deployment, but designed for applications that need stable storage and stable network identities. Databases are the classic use case.

**PersistentVolumeClaim (PVC)**: A request for storage that outlives any individual pod. Without a PVC, a database running in a pod loses ALL its data when the pod restarts.

---

## Tasks

### Task 1 --- Review the deployment (`k8s/deployment.yaml`)

Open the file and go through it line by line. For each setting, ask yourself: "Is this what I'd want in production?"

Here's a checklist of things to evaluate:

**Replicas**:
- How many replicas are configured?
- What happens if this single pod crashes or the node it's running on goes down?
- For a booking platform that customers depend on, what's a reasonable number of replicas?
- What does "High Availability" (HA) mean, and does this configuration provide it?

**Image tag**:
- What image tag is used? (Look at the `image:` field.)
- The tag `latest` means "whatever was most recently pushed." Why is this risky?
- Consider: if you deploy today with `latest`, it points to version X. Tomorrow someone pushes version Y. If the pod restarts (which Kubernetes does routinely), it pulls `latest` again -- and now you're running version Y without anyone deploying it intentionally.
- What should the tag be instead? (Think: something specific, traceable, immutable.)

**Container port**:
- What port is the `containerPort` set to?
- Now check `app/src/main/resources/application.properties`. What port does the application actually run on?
- Check `k8s/service.yaml`. What `targetPort` does the Service route traffic to?
- Do these all match? What happens when they don't?

**Health checks (liveness and readiness probes)**:
- Are there any liveness or readiness probes defined?
- A **liveness probe** tells Kubernetes "is this container still alive?" If it fails, Kubernetes kills and restarts the pod.
- A **readiness probe** tells Kubernetes "is this container ready to receive traffic?" If it fails, the Service stops sending traffic to that pod.
- Without probes, Kubernetes assumes the container is healthy as long as the process is running. But what if the process is running but the app is stuck? What if it started but can't connect to the database?
- What endpoint would you use for these probes? (Hint: check the actuator configuration in `application.properties`.)

**Resource requests and limits**:
- Are there any `resources` defined (CPU/memory requests and limits)?
- Without limits, a container can consume unlimited memory. What happens to other pods on the same node if one container has a memory leak?
- Without requests, the Kubernetes scheduler doesn't know how much capacity a pod needs. It might place too many pods on one node.

Write down every issue you find. You'll compile these into a review in Task 4.

---

### Task 2 --- Review the config (`k8s/configmap.yaml`)

Open the ConfigMap and examine what's stored in it.

- List every key-value pair. What is each one for?
- Look at the values carefully. Is there anything stored here that **should not** be in a ConfigMap?
- Remember the distinction: ConfigMaps are for non-sensitive configuration. Secrets are for sensitive data.
- Think about who can read a ConfigMap. In most Kubernetes setups, anyone with basic access to the namespace can read ConfigMaps. What are the implications?
- If this ConfigMap were accidentally committed to a public Git repository (which happens more often than you'd think), what would be exposed?
- What's the fix? What would a Kubernetes Secret for this data look like?

---

### Task 3 --- Review the database (`k8s/database.yaml`)

This file defines a StatefulSet for PostgreSQL and a Service to expose it.

- Using a StatefulSet instead of a Deployment is the right call for a database. Why? (Think about what databases need that stateless apps don't.)
- **Look at the storage situation.** Is there a PersistentVolumeClaim (PVC) defined? Where is PostgreSQL storing its data inside the container?
- Without a PVC, the database stores data in the container's ephemeral filesystem. What happens to ALL bookings, guests, and room data when the pod restarts? (And Kubernetes restarts pods routinely -- node maintenance, scaling events, updates.)
- Compare this to the Docker Compose file (`docker/docker-compose.yml`). How does the Docker setup handle database storage? What does the Compose file have that this Kubernetes manifest is missing?
- Look at the environment variables. The database password is hardcoded in the YAML. This has the same problem as the ConfigMap issue -- what would be better?

---

### Task 4 --- Write a review

Now compile your findings into a proper review, written as you would for a real code review or pull request.

For each issue, write:

| # | What you found | Why it's a problem | Recommended fix | Severity |
|---|---------------|-------------------|-----------------|----------|
| 1 | | | | |
| 2 | | | | |
| ... | | | | |

For severity, categorize each issue:
- **Critical**: Would cause an outage, data loss, or security breach in production
- **High**: Would cause significant operational problems or risk
- **Medium**: Bad practice that increases risk over time
- **Low**: Improvement that would make things cleaner or more maintainable

When you're done, rank the issues by severity. If the team could only fix five things before deploying, which five would you choose?

---

## Reflection Questions

1. **Which issue would you flag as the highest priority to fix before deployment?** There's a strong case for the missing PVC (data loss on restart), the password in the ConfigMap (security), and the single replica (no HA). How do you choose when everything seems urgent?

2. **If you could only fix three things, which three would make the biggest difference?** This is a real constraint QA engineers face. You rarely get to fix everything. Prioritization is the skill.

3. **How does QA add value by reviewing infrastructure, not just application code?** Many QA teams focus only on functional testing -- does the button work, does the API return the right data. But some of the most impactful bugs live in configuration files. A wrong port number, a missing volume mount, a password in plaintext -- these cause outages, data loss, and security incidents. What does that tell you about the scope of QA?

4. **How do the Kubernetes issues connect to what you found in other exercises?** The `containerPort` mismatch connects to the port in `application.properties`. The image tag `latest` connects to the pipeline in Exercise 07. The password issue connects to the same password in `docker-compose.yml`. QA is about seeing these connections.

---

## Bonus Challenges

- **Write the liveness and readiness probes**: Add proper probe definitions to the deployment YAML. Use the Spring Boot Actuator health endpoint. What path, port, and timing would you use?

- **Write the PVC**: Add a `volumeClaimTemplates` section to the StatefulSet. How much storage would you request for a hotel booking database? What access mode should it use?

- **Create the Secret**: Write a Kubernetes Secret manifest that stores the database password. Then update the ConfigMap and Deployment to reference the Secret for sensitive values. Remember that Secret values must be base64-encoded.

- **Write a production-ready deployment**: Take `deployment.yaml` and rewrite it with all the fixes: proper replica count, specific image tag, correct port, probes, resource limits. Compare your version to the original -- how many lines did you add?

- **Cross-reference with the pipeline**: Look at `pipelines/azure-pipelines.yml`. The Deploy stage applies `k8s/deployment.yaml`. It also overrides the image to use `latest`. Even if you fix the tag in the YAML, the pipeline puts it back. How would you fix this end-to-end?
