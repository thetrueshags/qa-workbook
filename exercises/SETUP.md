# Pre-Workshop Setup Guide

**Workshop:** QA Workbook -- The StaySync Workshop
**What to do:** Complete this guide *before* the workshop day so you arrive with everything installed and working.
**Time needed:** 20-30 minutes (depending on what you already have installed)

If you hit a problem you can't solve, don't sweat it -- just note where you got stuck and we'll sort it out at the start of the workshop. But do give it a real try first; most issues are quick fixes.

---

## What You'll Need

Work through each tool below. Install it, then run the verification command to confirm it's working. Check the box when you're done.

---

### 1. Git

> Version control -- used to clone the workshop repository.

- [ ] **Install:** [https://git-scm.com/downloads](https://git-scm.com/downloads)
- [ ] **Verify:**
  ```bash
  git --version
  ```
  You should see something like `git version 2.x.x`. Any recent version is fine.

---

### 2. Docker Desktop

> Runs the application, database, and monitoring stack in containers. We use this heavily throughout the workshop (Exercises 01-04 and 06).

- **Windows:** [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/) (requires WSL2 -- the installer will guide you through enabling it)
- **Mac:** [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop/)
- **Linux:** [Docker Engine](https://docs.docker.com/engine/install/) + [Docker Compose plugin](https://docs.docker.com/compose/install/linux/)

- [ ] **Verify Docker is installed:**
  ```bash
  docker --version
  ```
- [ ] **Verify Docker Compose is available:**
  ```bash
  docker compose version
  ```
  You should see `Docker Compose version v2.x.x`. Note: it's `docker compose` (with a space), not the older `docker-compose`.

> **IMPORTANT:** Make sure Docker Desktop is actually *running*, not just installed. Look for the whale icon in your system tray (Windows) or menu bar (Mac). Many "connection refused" errors happen simply because Docker Desktop isn't started yet.

---

### 3. An IDE or Text Editor

> For reading source code and writing tests. You'll be working with Java code, SQL files, YAML configuration, and Dockerfiles.

- [ ] **Recommended:** [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/) (free) -- excellent Java support out of the box
- [ ] **Alternative:** [Visual Studio Code](https://code.visualstudio.com/) (free) -- if you go with VS Code, install these extensions:
  - [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) -- Java language support, debugging, and Maven integration
  - [Docker](https://marketplace.visualstudio.com/items?itemName=ms-azuretools.vscode-docker) -- syntax highlighting and management for Docker files

No verification command needed -- just make sure you can open it.

---

### 4. A Database Client

> For running SQL queries against the StaySync PostgreSQL database. Used primarily in Exercise 01 (SQL Investigation) but handy throughout.

- [ ] **Recommended:** [DBeaver Community Edition](https://dbeaver.io/download/) (free, works on Windows/Mac/Linux)
- [ ] **Alternatives:**
  - [DataGrip](https://www.jetbrains.com/datagrip/) (paid, 30-day trial)
  - [pgAdmin](https://www.pgadmin.org/download/) (free, PostgreSQL-specific)
  - VS Code with the [PostgreSQL extension](https://marketplace.visualstudio.com/items?itemName=ckolkman.vscode-postgres)

No verification command needed -- just make sure it opens. We'll connect to the database together during the workshop.

---

### 5. Java 17+ JDK

> The StaySync API is a Spring Boot 3.2 application built with Java 17. You'll need the JDK to compile and run the automated tests in Exercises 03 and 08.

- [ ] **Recommended:** [Eclipse Temurin 17 (Adoptium)](https://adoptium.net/temurin/releases/?version=17) -- a free, production-ready OpenJDK distribution
- [ ] **Verify:**
  ```bash
  java -version
  ```
  You should see output containing `openjdk version "17.x.x"` or similar. Java 17 or any higher version (19, 21, etc.) will work.

> **Tip for Windows users:** During installation, check the option to set `JAVA_HOME` automatically. If you installed Java but the command isn't found, you likely need to add it to your `PATH` environment variable.

---

### 6. Maven

> Build tool for Java projects. Used to compile the test suite and run automated tests.

- [ ] **Install:** [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi) (download the binary zip/tar.gz, extract it, and add the `bin` directory to your `PATH`)
- [ ] **Verify:**
  ```bash
  mvn -version
  ```
  You should see Maven version info *and* it should show Java 17+ as the runtime. If Maven shows an older Java version, your `JAVA_HOME` is pointing to the wrong JDK.

> **Tip:** If you installed IntelliJ IDEA, it bundles Maven -- but having it available on the command line is still useful for the workshop.

---

### 7. An API Testing Tool

> For exploratory API testing in Exercise 02. You'll be sending HTTP requests to the StaySync API and inspecting responses.

- [ ] **Recommended:** [Postman](https://www.postman.com/downloads/) (free)
- [ ] **Alternative:** [Insomnia](https://insomnia.rest/download) (free)
- [ ] **Also fine:** `curl` from the command line, if you're comfortable with it

No verification command needed -- just make sure it opens.

---

### 8. kubectl (Optional)

> Kubernetes command-line tool. Only needed if your facilitator confirms we'll be doing hands-on Kubernetes work in Exercise 05. If in doubt, skip this for now.

- [ ] **Install:** [https://kubernetes.io/docs/tasks/tools/](https://kubernetes.io/docs/tasks/tools/)
- [ ] **Verify:**
  ```bash
  kubectl version --client
  ```

---

## The Setup Test

Once everything is installed, run through this end-to-end smoke test to make sure it all works together. This takes about 5 minutes.

### Step 1: Clone the repository

```bash
git clone https://github.com/thetrueshags/qa-workbook.git
cd qa-workbook
```

- [ ] Repository cloned successfully

### Step 2: Start the database

```bash
docker compose -f database/docker-compose.yml up -d
```

This pulls the PostgreSQL 15 image (if you don't already have it) and starts the database container.

- [ ] Command completed without errors

### Step 3: Verify the container is running

```bash
docker ps
```

You should see a container named `staysync-db` with status `Up`.

- [ ] `staysync-db` container is listed and running

### Step 4: Test the database connection

Wait about 5-10 seconds for PostgreSQL to finish initializing, then run:

```bash
docker exec staysync-db psql -U staysync -d staysync -c "SELECT COUNT(*) FROM rooms;"
```

**Expected output:**

```
 count
-------
    11
(1 row)
```

If you see a count of 11, the database is initialized with the workshop data and everything is working.

- [ ] Query returned a count of 11

### Step 5: Connect your database client

Open DBeaver (or whichever client you chose) and create a new PostgreSQL connection with these settings:

| Setting  | Value         |
|----------|---------------|
| Host     | `localhost`   |
| Port     | `5432`        |
| Database | `staysync`    |
| User     | `staysync`    |
| Password | `staysync123` |

Try running a quick query: `SELECT * FROM rooms;`

- [ ] Database client connects and shows room data

### Step 6: Clean up

We'll start fresh on workshop day, so tear everything down:

```bash
docker compose -f database/docker-compose.yml down -v
```

The `-v` flag removes the data volume too, so you get a clean slate.

- [ ] Containers stopped and volumes removed

---

## Setup Checklist (Summary)

Copy this and check off each item:

- [ ] Git installed
- [ ] Docker Desktop installed **and running**
- [ ] Docker Compose available (`docker compose version`)
- [ ] IDE or text editor ready
- [ ] Database client installed
- [ ] Java 17+ JDK installed
- [ ] Maven installed
- [ ] API testing tool installed
- [ ] Smoke test completed (Steps 1-6 above)

---

## Troubleshooting

### "docker: command not found"

Docker Desktop isn't installed, or it's not on your system `PATH`. Reinstall Docker Desktop and make sure to let the installer update your PATH. On Windows, you may need to restart your terminal after installation.

### "Cannot connect to the Docker daemon"

Docker Desktop is installed but not currently running. Start Docker Desktop from your applications menu and wait for it to finish loading (the whale icon should stop animating). This is the most common setup issue -- Docker Desktop doesn't auto-start by default on most systems.

### "Port 5432 already in use"

Another PostgreSQL instance is already running on your machine and using port 5432. Options:
- Stop the other PostgreSQL service (on Windows: check Services; on Mac: `brew services stop postgresql`; on Linux: `sudo systemctl stop postgresql`)
- Or edit `database/docker-compose.yml` and change the port mapping from `"5432:5432"` to something like `"5433:5432"`, then use port 5433 in your database client

### "java: command not found"

The JDK isn't installed, or `JAVA_HOME` isn't set. On Windows, make sure the JDK's `bin` directory is in your `PATH` environment variable. On Mac/Linux, add `export JAVA_HOME=$(/usr/libexec/java_home)` (Mac) or `export JAVA_HOME=/path/to/jdk` (Linux) to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.).

### "mvn: command not found"

Maven's `bin` directory isn't on your `PATH`. After extracting Maven, add its `bin` folder to your system `PATH`. Restart your terminal afterward.

### "Permission denied" when running Docker (Linux/Mac)

On Linux, your user may not be in the `docker` group. Fix it with:
```bash
sudo usermod -aG docker $USER
```
Then log out and back in (or restart your machine) for the change to take effect.

### The database container starts but the query fails

If `docker exec` gives you an error like "relation does not exist," PostgreSQL may still be initializing. Wait 10-15 seconds and try again. If it persists, tear down and rebuild:
```bash
docker compose -f database/docker-compose.yml down -v
docker compose -f database/docker-compose.yml up -d
```

---

## Before the Workshop

### Do

- Complete this setup guide and the smoke test
- Skim the `README.md` in the repository root to get a sense of what the project is about
- Bring a notebook or open a notes document -- you'll want to write things down during exercises

### Don't

- **Don't read through the exercise files ahead of time** -- no spoilers! The discovery process is part of the learning.
- **Don't try to fix any bugs you notice** while setting up -- that's what the workshop is for.
- **Don't worry if you're not a Java expert** -- the exercises are designed so you can work through them by reading code, not writing it from scratch.

---

See you at the workshop. Come with everything installed, a curious mindset, and a willingness to break things.
