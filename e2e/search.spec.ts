import { expect, test } from "@playwright/test";

// A customer searches shops by tag on the home page and opens a shop's catalog.
// Uses the seeded demo data: Rideau Roasters is tagged "coffee" and sells House blend beans.
// The test only reads data so it can use on the seeded shop instead of creating its own.

test("a customer can find a shop by tag and open its catalog", async ({ page }) => {
    await page.goto("/");

    await page.getByRole("textbox", { name: "Search by shop name or tag" }).fill("coffee");
    await page.getByRole("button", { name: "Search" }).click();

    const shopLink = page.getByRole("link", { name: "Rideau Roasters" });
    await expect(shopLink).toBeVisible();
    await shopLink.click();

    await expect(page).toHaveURL(/\/shops\/\d+\/catalog/);
    await expect(page.getByRole("heading", { level: 1, name: "Rideau Roasters" })).toBeVisible();
    await expect(page.getByRole("heading", { name: "House blend beans" })).toBeVisible();
});
