const tagInput = document.getElementById("tag-input");
const tagList = document.getElementById("tag-list");
const addTagButton = document.getElementById("add-tag-button");
const shopForm = document.getElementById("shop-form");

if (tagInput && tagList && addTagButton && shopForm) {
    function bindRemoveButton(chip) {
        chip.querySelector(".remove-tag").addEventListener("click", () => chip.remove());
    }

    function addTag(value) {
        const tagName = value.trim();
        if (!tagName) {
            return;
        }

        const alreadyAdded = Array.from(tagList.querySelectorAll('input[name="tags"]'))
            .some((tagField) => tagField.value.toLowerCase() === tagName.toLowerCase());
        if (alreadyAdded) {
            tagInput.value = "";
            return;
        }

        const chip = document.createElement("div");
        chip.className = "tag-chip";

        const label = document.createElement("span");
        label.textContent = tagName;

        const removeButton = document.createElement("button");
        removeButton.type = "button";
        removeButton.className = "remove-tag";
        removeButton.setAttribute("aria-label", `Remove ${tagName}`);
        removeButton.textContent = "×";

        const hiddenField = document.createElement("input");
        hiddenField.type = "hidden";
        hiddenField.name = "tags";
        hiddenField.value = tagName;

        chip.append(label, removeButton, hiddenField);
        tagList.append(chip);
        bindRemoveButton(chip);
        tagInput.value = "";
        tagInput.focus();
    }

    tagList.querySelectorAll(".tag-chip").forEach(bindRemoveButton);

    addTagButton.addEventListener("click", () => addTag(tagInput.value));
    shopForm.addEventListener("submit", () => {
        if (tagInput.value.trim()) {
            addTag(tagInput.value);
        }
    });
}
