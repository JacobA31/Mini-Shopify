import { expect, test } from "@playwright/test";

// A merchant creates a shop and adds a tag with the "Add tag" button, which is JavaScript
// It creates its own shop with a unique name and tag so it doesn't depend on the demo data.

test("a merchant can add a tag with the Add tag button and find the shop by it", async ({ page }) => {
    const unique = crypto.randomUUID().slice(0, 8);
    const shopName = `Playwright Shop ${unique}`;
    const tag = `tag-${unique}`;

    await page.goto("/shops/new");
    await page.getByLabel("Shop name").fill(shopName);
    await page.getByLabel("Tags").fill(tag);
    await page.getByRole("button", { name: "Add tag" }).click();

    await expect(page.getByRole("button", { name: `Remove ${tag}` })).toBeVisible();
    await expect(page.getByLabel("Tags")).toHaveValue("");

    await page.getByRole("button", { name: "Create shop" }).click();
    await expect(page.getByRole("heading", { name: "Shop created" })).toBeVisible();
    await expect(page.getByRole("listitem").filter({ hasText: tag })).toBeVisible();

    await page.goto("/");
    await page.getByRole("textbox", { name: "Search by shop name or tag" }).fill(tag);
    await page.getByRole("button", { name: "Search" }).click();
    await expect(page.getByRole("link", { name: shopName })).toBeVisible();
});
