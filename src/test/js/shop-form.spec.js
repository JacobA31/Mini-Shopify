const { loadPage } = require("./page");

// Uses src/main/resources/templates/shops/new.html; keep its form/tag IDs in sync with these tests.
describe("shop-form.js", () => {
    let page;
    let tagInput;
    let tagList;
    let addButton;

    beforeEach(() => {
        page = loadPage("shops/new.html");
        page.runScript("shop-form.js");
        tagInput = page.document.getElementById("tag-input");
        tagList = page.document.getElementById("tag-list");
        addButton = page.document.getElementById("add-tag-button");
    });

    afterEach(() => {
        page.window.close();
    });

    it("adds a chip and a hidden tags input when Add tag is clicked", () => {
        tagInput.value = "Toys";
        addButton.click();

        expect(tagList.querySelectorAll(".tag-chip").length).toBe(1);
        expect(tagList.querySelector(".tag-chip span").textContent).toBe("Toys");
        expect(tagList.querySelector('input[type="hidden"][name="tags"]').value).toBe("Toys");
        expect(tagInput.value).toBe("");
    });

    it("trims whitespace from a tag before displaying and submitting it", () => {
        tagInput.value = "  Toys  ";
        addButton.click();

        expect(tagList.querySelector(".tag-chip span").textContent).toBe("Toys");
        expect(tagList.querySelector('input[name="tags"]').value).toBe("Toys");
    });

    it("ignores a duplicate regardless of case and clears the tag box", () => {
        tagInput.value = "Toys";
        addButton.click();
        tagInput.value = "toys";
        addButton.click();

        expect(tagList.querySelectorAll(".tag-chip").length).toBe(1);
        expect(tagList.querySelectorAll('input[name="tags"]').length).toBe(1);
        expect(tagList.querySelector('input[name="tags"]').value).toBe("Toys");
        expect(tagInput.value).toBe("");
    });

    it("removes both the chip and its hidden input when its × button is clicked", () => {
        tagInput.value = "Toys";
        addButton.click();
        tagList.querySelector(".remove-tag").click();

        expect(tagList.querySelectorAll(".tag-chip").length).toBe(0);
        expect(tagList.querySelectorAll('input[name="tags"]').length).toBe(0);
    });

    it("adds text left in the tag box when the form is submitted", () => {
        const form = page.document.getElementById("shop-form");
        // jsdom cannot navigate after submission; dispatch the event and prevent navigation.
        form.addEventListener("submit", (event) => event.preventDefault());
        tagInput.value = "Toys";

        form.dispatchEvent(new page.window.Event("submit", { bubbles: true, cancelable: true }));

        expect(tagList.querySelectorAll(".tag-chip").length).toBe(1);
        expect(tagList.querySelector(".tag-chip span").textContent).toBe("Toys");
        expect(tagList.querySelector('input[name="tags"]').value).toBe("Toys");
        expect(tagInput.value).toBe("");
    });
});
