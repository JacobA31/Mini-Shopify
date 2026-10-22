# Contributing

How we work on Mini-Shopify. TAs grade the **process** (60% of each milestone), so following
this matters as much as the code.

## Getting started

**Prerequisites:** JDK 17 or newer, Git, and an IDE (IntelliJ IDEA recommended).
You do **not** need to install Maven; use the `./mvnw` wrapper (`mvnw.cmd` on Windows).

```bash
git clone https://github.com/JacobA31/Mini-Shopify.git
cd Mini-Shopify
./mvnw spring-boot:run        # start the app at http://localhost:8080
./mvnw test                   # run all tests
./mvnw verify                 # what CI runs: compile, test, and package target/mini-shopify.jar
```

While running locally, the H2 database console is at http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:mem:minishopify`, user `sa`, empty password).

In IntelliJ, use *File → Open* on the `pom.xml` and open it as a project. Then run
`MiniShopifyApplication`.

## Workflow: from issue to production

1. **Pick an issue** from the Kanban *Backlog* column, assign yourself, and move it to *In progress*.
   If there is no issue for what you're about to do, create one first (use the *User story* template).
2. **Create a branch** from an up-to-date `main`:
   ```bash
   git switch main && git pull
   git switch -c feature/12-create-shop-form     # <type>/<issue#>-<short-description>
   ```
   Types: `feature/`, `fix/`, `test/`, `docs/`, `chore/`.
3. **Commit small and often** with clear messages (`Add ShopService.createShop with validation`).
4. **Open a pull request** into `main` when the feature works and has tests. Fill in the PR template,
   write `Closes #12` so the issue closes on merge, and **request review from at least two teammates**.
5. **Reviewers** check out the branch or read the diff, leave comments, and *Approve* or *Request changes*.
   Try to review within 24 hours. Small PRs get reviewed faster.
6. **Once there are 2 approvals and CI is green, the PR author merges** (squash merge). This triggers CI on
   `main`, which re-tests, builds, and **deploys to Azure automatically**. Because the author merges, every
   team member deploys regularly. The course expects at least 2–3 deploys per week.
7. Check that the deploy succeeded (*Actions* tab, or *Deployments* on the repo page) and that your
   feature works on the live site. Move the issue to *Completed* if GitHub didn't do it automatically.

**Never push directly to `main`.** Branch protection should block it anyway (see setup below).

### Keeping your branch up to date

```bash
git switch main && git pull
git switch feature/12-create-shop-form
git merge main            # resolve conflicts, run ./mvnw verify, push
```

## Code conventions

- Follow the layering in [`docs/architecture.md`](docs/architecture.md):
  controller → service → repository, with entities in `model`.
- Tests mirror the main package: `service/ShopService` → `src/test/.../service/ShopServiceTest`.
- Thymeleaf views go in a folder per feature (`templates/shops/list.html`) and use the shared
  fragments in `templates/fragments/layout.html`.
- **If you change an entity, update the UML class diagram and DB schema in the README in the same PR.**
- Don't add UI links to pages that don't exist yet.
- Formatting comes from `.editorconfig` (4 spaces, LF). IntelliJ picks it up automatically.

## Weekly scrum

Every week (suggestion: **Monday**, rotating who does it) one person opens an issue with the
**Weekly scrum** template, titled `Weekly scrum – YYYY-MM-DD`. Each member then adds **their own comment**
answering, in 1–2 sentences each, with links to issues/PRs:

- What did I do this week?
- What will I do next week?
- What is holding me back?

## Kanban

The *Projects* tab has a board with **Backlog**, **In progress**, and **Completed** columns. Every piece
of work is an issue on the board. Before each milestone, update the *Current state* and *Plan for next
sprint* sections of the README to match the board.

---

## One-time setup

Done once at the start of the project. Most of these need **admin access to the repository**
(the repo owner), so tick them off here as they're done.

### GitHub

- [ ] **Add every team member as a collaborator** with write access
      (*Settings → Collaborators → Add people*).
- [ ] Issues are enabled (*Settings → General → Features → Issues*).
- [ ] Create a label named `scrum` (*Issues → Labels → New label*), used by the weekly scrum template.
- [ ] **Create the Kanban board:** *Projects → New project → Board*, with columns
      `Backlog`, `In progress`, `Completed`. Link it to this repository.
- [ ] **Protect `main`** (*Settings → Branches → Add branch ruleset*, or *Add classic branch protection rule*)
      targeting `main`:
  - Require a pull request before merging, with **2 required approvals**
  - Dismiss stale approvals when new commits are pushed
  - Require status checks to pass: **`Build & test`** (appears after CI has run once)
  - Block force pushes
- [ ] *Settings → General → Pull Requests*: allow **squash merging**, and enable
      *Automatically delete head branches*.

### Azure (production)

The workflow in `.github/workflows/ci-cd.yml` deploys on every push to `main`, once the variable and
secret below exist. Until then, the deploy job is skipped and only build and tests run.

- [ ] In the [Azure portal](https://portal.azure.com) (the *Azure for Students* subscription works),
      **create a Web App**:
  - Publish: **Code**, Runtime stack: **Java 17**, Java web server stack: **Java SE (Embedded Web Server)**
  - Operating system: **Linux**, Region: e.g. **Canada Central**
  - Pricing plan: **Free F1** works, but it's slow to start and sleeps when idle. **Basic B1** is more
    reliable for demos.
  - On the *Deployment* tab, **leave GitHub Actions continuous deployment disabled**. We already have our
    own workflow, and enabling it would make Azure commit a second one.
- [ ] In the new Web App: *Settings → Configuration → General settings* → turn **SCM Basic Auth Publishing
      Credentials** **On** and save. This is required to download a publish profile.
- [ ] *Overview → Download publish profile*. In GitHub: *Settings → Secrets and variables → Actions*:
  - **Secrets** tab → New repository secret `AZURE_WEBAPP_PUBLISH_PROFILE` = the full contents of the
    downloaded file
  - **Variables** tab → New repository variable `AZURE_WEBAPP_NAME` = the Web App's name
- [ ] Optional: *Monitoring → Health check* path `/actuator/health`.
- [ ] Re-run the latest workflow on `main` (*Actions → CI/CD → Run workflow*). Then open
      `https://<app-name>.azurewebsites.net` and put the URL in the README.

**Troubleshooting:** if the deploy succeeds but the site shows an *Application Error*, open
*Log stream* in the Web App. If the logs show Spring Boot started on port 8080 but the site still
doesn't respond, add an application setting `SERVER_PORT` = `80`
(*Settings → Environment variables*).
