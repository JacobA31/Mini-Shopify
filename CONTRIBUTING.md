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

The local database is in memory, so it starts empty on every run. A few demo shops and products
are added automatically on startup (see `config/DemoDataSeeder`). To start with an empty database
instead, run `./mvnw spring-boot:run -Dspring-boot.run.arguments=--minishopify.seed.enabled=false`.
Tests never get the demo data.

In IntelliJ, use *File → Open* on the `pom.xml` and open it as a project. Then run
`MiniShopifyApplication`.

### Client-side tests

The Java tests check the HTML the server sends, but they never run the JavaScript or click through
pages in a browser. Two more kinds of tests do that. They need Node 22 or newer:

| Kind | Tool | Where | Tests |
|------|------|-------|-------|
| JavaScript unit tests | Jasmine + jsdom | `src/test/js/*.spec.js` | the page scripts in `static/js`, against the real templates |
| End-to-end browser tests | Playwright | `e2e/**/*.spec.ts` | whole user flows in Chromium, Firefox, and WebKit |

```bash
npm ci                         # once, and after package.json changes
npx playwright install         # once: downloads the browsers

npm run test:js                # Jasmine
npm run test:js:watch          # Jasmine, re-run whenever a script, template, or spec is saved
npm test                       # same Jasmine configuration as test:js
npm test -- --watch             # same watch behavior as test:js:watch
npm run test:e2e               # Playwright: uses the app on :8080, or starts target/mini-shopify.jar
npm run test:e2e:ui            # Playwright's UI mode: pick tests, watch them run, step through traces
npm run codegen                # record clicks on the running app and get a test written for you
npm run report                 # open the last Playwright HTML report
```

- **Jasmine:** `const page = loadPage("shops/new.html")` builds a fresh page from the real template,
  then `page.runScript("shop-form.js")` runs the script in it; check `page.document` afterwards
  (see `src/test/js/page.js` and `product-form.spec.js`).
- **Playwright:** use `getByRole`, `getByLabel`, and `getByText` locators rather than CSS classes.
  Tests run in parallel against one shared database, so a test that changes data should create its
  own shop and products (with a unique name) rather than use the demo data.
- **`e2e/smoke/`** also runs against the live app on Azure after every deploy. Tests there must be
  **read-only**: never create, edit, or buy anything.

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
- **If you change an entity, add a migration (see below) and update the UML class diagram and DB schema
  in the README in the same PR.**
- Don't add UI links to pages that don't exist yet.
- Formatting comes from `.editorconfig` (4 spaces, LF). IntelliJ picks it up automatically.

## Changing the database schema

The tables are created and changed by **Flyway migrations** in `src/main/resources/db/migration`,
not by Hibernate. On startup, Flyway runs every migration the database hasn't seen yet, then Hibernate
checks that the `@Entity` classes match the tables (`ddl-auto=validate`). If they don't match, **the app
won't start** and every `@SpringBootTest` fails with a `SchemaManagementException` naming the missing
table or column.

So whenever you add or change an entity field, add a migration in the same PR:

1. Create `src/main/resources/db/migration/V<next number>__<what_it_does>.sql`, for example
   `V2__add_product_image.sql`. Two underscores after the number. Check `main` for the latest number
   first; if someone merges the same number before you, rename yours to the next one.
2. Write the SQL for the change, for example:
   ```sql
   alter table products add column image blob;
   alter table products add column image_content_type varchar(100);
   ```
   New `not null` columns need a `default`, because the live database already has rows.
3. Run `./mvnw verify`. The tests run every migration on a fresh in-memory database and validate it
   against your entities.
4. Update the UML class diagram and DB schema in the README.

**Never edit or rename a migration that is already on `main`.** The live database has already run
it, and Flyway will refuse to start the app because the file's checksum changed. Fix mistakes with
a new migration instead.

Tip: to see the `create table` SQL Hibernate expects for your entities (a good starting point for the
migration), build the JAR and have Hibernate write it to `schema.sql`:
```bash
./mvnw -q package -DskipTests
java -jar target/mini-shopify.jar --spring.main.web-application-type=none \
  --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=none --minishopify.seed.enabled=false \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
  --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=schema.sql
```
Copy only the parts for your change into the migration, and don't commit `schema.sql`.

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

- [x] **Add every team member as a collaborator** with write access
      (*Settings → Collaborators → Add people*).
- [x] Issues are enabled (*Settings → General → Features → Issues*).
- [x] Create a label named `scrum` (*Issues → Labels → New label*), used by the weekly scrum template.
- [x] **Create the Kanban board:** *Projects → New project → Board*, with columns
      `Backlog`, `In progress`, `Completed`. Link it to this repository.
- [x] **Protect `main`** (*Settings → Branches → Add branch ruleset*, or *Add classic branch protection rule*)
      targeting `main`:
  - Require a pull request before merging, with **2 required approvals**
  - Dismiss stale approvals when new commits are pushed
  - Require status checks to pass: **`Build & test`** (appears after CI has run once)
  - Block force pushes
- [x] *Settings → General → Pull Requests*: allow **squash merging**, and enable
      *Automatically delete head branches*.

### Azure (production)

The workflow in `.github/workflows/ci-cd.yml` deploys on every push to `main`, once the variable and
secret below exist. Until then, the deploy job is skipped and only build and tests run.

- [x] In the [Azure portal](https://portal.azure.com) (the *Azure for Students* subscription works),
      **create a Web App**:
  - Publish: **Code**, Runtime stack: **Java 17**, Java web server stack: **Java SE (Embedded Web Server)**
  - Operating system: **Linux**, Region: e.g. **Canada Central**
  - Pricing plan: **Free F1** works, but it's slow to start and sleeps when idle. **Basic B1** is more
    reliable for demos.
  - On the *Deployment* tab, **leave GitHub Actions continuous deployment disabled**. We already have our
    own workflow, and enabling it would make Azure commit a second one.
- [x] In the new Web App: *Settings → Configuration → General settings* → turn **SCM Basic Auth Publishing
      Credentials** **On** and save. This is required to download a publish profile.
- [x] *Overview → Download publish profile*. In GitHub: *Settings → Secrets and variables → Actions*:
  - **Secrets** tab → New repository secret `AZURE_WEBAPP_PUBLISH_PROFILE` = the full contents of the
    downloaded file
  - **Variables** tab → New repository variable `AZURE_WEBAPP_NAME` = the Web App's name
- [ ] *Settings → Environment variables* → add the application setting `SPRING_PROFILES_ACTIVE` = `azure`.
      This switches the app to `application-azure.properties`, which keeps the database in an H2 file on
      App Service's persistent `/home` disk, so data survives restarts and redeploys. Keep the App Service
      at **one instance** (no scale-out): two instances can't share the H2 file.
- [ ] Optional: *Monitoring → Health check* path `/actuator/health`.
- [x] Re-run the latest workflow on `main` (*Actions → CI/CD → Run workflow*). Then open
      `https://<app-name>.azurewebsites.net` and put the URL in the README.

**Troubleshooting:** if the deploy succeeds but the site shows an *Application Error*, open
*Log stream* in the Web App. If the logs show Spring Boot started on port 8080 but the site still
doesn't respond, add an application setting `SERVER_PORT` = `80`
(*Settings → Environment variables*).

If the logs show `Database may be already in use`, two copies of the app are trying to open the H2 file
at once (for example, during a deploy). Restart the Web App. If it keeps happening, see the persistence
decision in [`docs/architecture.md`](docs/architecture.md#open-decisions).

If the logs show `Validate failed: Migrations have failed validation` or a checksum mismatch, a migration
that was already deployed has been edited. Revert that edit and put the change in a new migration.
