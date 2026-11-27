import { expect, test } from "@playwright/test";

// Smoke tests: the few checks that prove a deployed app works at all.
// CI runs them against Azure after every deploy, so they must stay READ-ONLY: never create,
// edit, or buy anything here, because the Azure database is real and kept across deploys.
// They also run locally with the rest of e2e/, against the seeded demo data.

test("the app reports it is healthy", async ({ request }) => {
    const response = await request.get("/actuator/health");

    expect(response.ok()).toBeTruthy();
    expect(await response.json()).toMatchObject({ status: "UP" });
});

test("a customer can search shops by tag and open a catalog", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveTitle(/Mini-Shopify/);

    await page.getByPlaceholder("Search by shop name or tag").fill("coffee");
    await page.getByRole("button", { name: "Search" }).click();

    const firstShop = page.locator(".shop-card").filter({ hasText: "coffee" }).first();
    await expect(firstShop).toBeVisible();

    const shopLink = firstShop.getByRole("link");
    const shopName = (await shopLink.textContent())?.trim() ?? "";
    await shopLink.click();

    await expect(page).toHaveURL(/\/shops\/\d+\/catalog/);
    await expect(page.getByRole("heading", { level: 1 })).toHaveText(shopName);
    await expect(page.getByRole("heading", { name: "Products" })).toBeVisible();
});

test("a search with no match says so", async ({ page }) => {
    await page.goto("/?query=no-shop-is-called-this-xyz");

    await expect(page.getByText("No shops found.")).toBeVisible();
});
