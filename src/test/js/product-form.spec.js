const { loadPage } = require("./page");

// Edit product page: the red × removes the current picture when the form is saved
describe("product-form.js", () => {
    let page;

    beforeEach(() => {
        page = loadPage("products/edit.html");
    });

    it("keeps the current image until × is clicked", () => {
        page.runScript("product-form.js");

        expect(page.document.getElementById("current-image").hidden).toBeFalse();
        expect(page.document.getElementById("remove-image").value).not.toBe("true");
    });

    it("hides the current image and marks it for removal when × is clicked", () => {
        page.runScript("product-form.js");

        page.document.getElementById("remove-image-button").click();

        expect(page.document.getElementById("current-image").hidden).toBeTrue();
        expect(page.document.getElementById("remove-image").value).toBe("true");
    });

    it("does nothing on a product without an image", () => {
        // Without a current image the page has no × button; the script must not throw
        page.document.getElementById("current-image").remove();

        expect(() => page.runScript("product-form.js")).not.toThrow();
    });
});
