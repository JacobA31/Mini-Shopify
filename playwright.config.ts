import { defineConfig, devices } from "@playwright/test";

// End-to-end browser tests in e2e/.
//
// By default they run against the app at http://localhost:8080. If it isn't running yet, Playwright
// starts it from the JAR that `./mvnw verify` builds (in-memory database with the demo data).
// Set BASE_URL to test a deployed app instead, e.g. the Azure smoke test in CI:
//   BASE_URL=https://<app>.azurewebsites.net npm run test:smoke
// Only the read-only tests in e2e/smoke may run against Azure: its database is real and kept.
const baseURL = process.env.BASE_URL ?? "http://localhost:8080";
const isCI = !!process.env.CI;

export default defineConfig({
    testDir: "./e2e",
    // Fail the CI run if someone leaves a test.only in
    forbidOnly: isCI,
    // A test that fails and then passes on retry is reported as "flaky" instead of failing the build
    retries: isCI ? 2 : 0,
    reporter: isCI ? [["github"], ["html", { open: "never" }]] : [["list"], ["html", { open: "on-failure" }]],
    use: {
        baseURL,
        // Record a trace (DOM snapshots, network, console for every step) when a test is retried
        trace: "on-first-retry",
        screenshot: "only-on-failure",
    },
    projects: [
        { name: "chromium", use: { ...devices["Desktop Chrome"] } },
        { name: "firefox", use: { ...devices["Desktop Firefox"] } },
        { name: "webkit", use: { ...devices["Desktop Safari"] } },
    ],
    webServer: process.env.BASE_URL
        ? undefined
        : {
              command: "java -jar target/mini-shopify.jar",
              url: "http://localhost:8080/actuator/health",
              // Locally, reuse the app you already started with ./mvnw spring-boot:run
              reuseExistingServer: !isCI,
              timeout: 120_000,
          },
});
