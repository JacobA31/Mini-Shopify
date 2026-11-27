import { expect, test, type Locator, type Page } from "@playwright/test";

// End-to-end tests for the customer side of Mini-Shopify: catalog -> cart -> checkout.
//
// The checkout test buys stock, so it creates its own shop with a unique name instead of using the
// demo data. The other two tests only put things in the cart, which lives in the browser session, 
// so they can safely read the seeded demo shops.

/** Search by tag from the home page, the way a customer would, and open a shop's catalog. */
async function openShop(page: Page, tag: string, shopName: string): Promise<void> {
    await page.goto("/");
    await page.getByRole("textbox", { name: "Search by shop name or tag" }).fill(tag);
    await page.getByRole("button", { name: "Search" }).click();
    await page.getByRole("link", { name: shopName }).click();
    await expect(page.getByRole("heading", { level: 1, name: shopName })).toBeVisible();
}

type NewProduct = { name: string; price: string; inventory: number };

/** Creates a shop and its products through the merchant forms, and returns the catalog URL. */
async function createShopWithProducts(page: Page, shopName: string, products: NewProduct[]): Promise<string> {
    await page.goto("/shops/new");
    await page.getByLabel("Shop name").fill(shopName);
    await page.getByRole("button", { name: "Create shop" }).click();
    await page.getByRole("link", { name: "Add products to this shop" }).click();

    // The add-product URL is /shops/{id}/products/new
    const shopId = new URL(page.url()).pathname.split("/")[2];
    for (const product of products) {
        await page.goto(`/shops/${shopId}/products/new`);
        await page.getByLabel("Product name").fill(product.name);
        await page.getByLabel("Price").fill(product.price);
        await page.getByLabel("Inventory").fill(String(product.inventory));
        await page.getByRole("button", { name: "Add product" }).click();
        await expect(page).toHaveURL(new RegExp(`/shops/${shopId}/products$`));
    }
    return `/shops/${shopId}/catalog`;
}

/** One product card on the catalog page, found by its heading. */
function productCard(page: Page, productName: string): Locator {
    return page
        .getByRole("listitem")
        .filter({ has: page.getByRole("heading", { name: productName, exact: true }) });
}

async function addToCart(page: Page, productName: string, quantity: number): Promise<void> {
    const card = productCard(page, productName);
    await card.getByLabel("Quantity").fill(String(quantity));
    await card.getByRole("button", { name: "Add to cart" }).click();
}

/** Reads "N in stock" from a product card. */
async function stockOf(page: Page, productName: string): Promise<number> {
    const text = await productCard(page, productName).getByText(/\d+ in stock/).textContent();
    return Number(text!.match(/\d+/)![0]);
}

test("a customer adds products, changes a quantity, and checks out", async ({ page }) => {
    const catalog = await createShopWithProducts(page, `Playwright Cart ${crypto.randomUUID().slice(0, 8)}`, [
        { name: "Bookmark set", price: "5.00", inventory: 10 },
        { name: "Used paperback", price: "6.00", inventory: 5 },
    ]);

    // Add two products from the catalog
    await page.goto(catalog);
    await addToCart(page, "Bookmark set", 2);
    await expect(page.getByText("Added 2 × Bookmark set to your cart.")).toBeVisible();
    await addToCart(page, "Used paperback", 1);

    // Change a quantity in the cart: 3 bookmarks ($5.00) + 1 paperback ($6.00) = $21.00
    await page.getByRole("link", { name: "Cart", exact: true }).click();
    const bookmarkRow = page.getByRole("row").filter({ hasText: "Bookmark set" });
    await bookmarkRow.getByLabel("Quantity").fill("3");
    await bookmarkRow.getByRole("button", { name: "Update" }).click();
    await expect(page.getByText("Total (4 items)")).toBeVisible();
    await expect(page.locator(".cart-total")).toHaveText("$21.00");

    // Check out
    await page.getByRole("button", { name: "Checkout" }).click();
    await expect(page).toHaveURL(/\/checkout\/confirmation\/\d+$/);
    await expect(page.getByRole("heading", { name: "Order confirmed" })).toBeVisible();
    await expect(page.locator(".cart-total")).toHaveText("$21.00");

    // The cart is emptied, and the shop's stock went down by exactly what was bought
    await page.getByRole("link", { name: "Cart", exact: true }).click();
    await expect(page.getByText("Your cart is empty.")).toBeVisible();
    await page.goto(catalog);
    await expect(productCard(page, "Bookmark set").getByText("7 in stock")).toBeVisible();
    await expect(productCard(page, "Used paperback").getByText("4 in stock")).toBeVisible();
});

test("the server never lets the cart hold more than the inventory", async ({ page }) => {
    await openShop(page, "outdoors", "Gatineau Trail Co.");
    const stock = await stockOf(page, "Daypack");

    // Taking all of it works...
    await addToCart(page, "Daypack", stock);
    await expect(page.getByText(`Added ${stock} × Daypack to your cart.`)).toBeVisible();

    // ...but one more is rejected, because the cart already holds the rest
    await addToCart(page, "Daypack", 1);
    await expect(page.getByText(`Only ${stock} of Daypack in stock.`)).toBeVisible();

    await page.getByRole("link", { name: "Cart", exact: true }).click();
    await expect(page.getByText(`Total (${stock} items)`)).toBeVisible();
});

test("the browser blocks quantities above the stock, and sold-out products have no button", async ({ page }) => {
    await openShop(page, "coffee", "Rideau Roasters");

    // The quantity box has max = stock, so the browser refuses to submit a bigger number
    const card = productCard(page, "Ceramic pour-over dripper");
    const stock = await stockOf(page, "Ceramic pour-over dripper");
    const quantity = card.getByLabel("Quantity");
    await quantity.fill(String(stock + 1));
    await card.getByRole("button", { name: "Add to cart" }).click();
    expect(await quantity.evaluate((input: HTMLInputElement) => input.validity.rangeOverflow)).toBe(true);
    await expect(page.getByText(/to your cart\./)).toHaveCount(0);

    // Out-of-stock products are shown but can't be added
    const soldOut = productCard(page, "Cold brew concentrate");
    await expect(soldOut.getByText("Out of stock")).toBeVisible();
    await expect(soldOut.getByRole("button", { name: "Add to cart" })).toHaveCount(0);
});
