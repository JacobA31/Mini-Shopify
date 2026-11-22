// Edit product page: the red × on the current image marks it for removal when the form is saved
const currentImage = document.getElementById("current-image");
const removeButton = document.getElementById("remove-image-button");
const undoButton = document.getElementById("undo-remove-image");
const removedMessage = document.getElementById("image-removed-message");
const removeImageField = document.getElementById("remove-image");

if (currentImage && removeButton && undoButton && removedMessage && removeImageField) {
    function showRemoved(removed) {
        removeImageField.value = removed ? "true" : "false";
        currentImage.hidden = removed;
        removedMessage.hidden = !removed;
    }

    removeButton.addEventListener("click", () => {
        showRemoved(true);
        undoButton.focus();
    });
    undoButton.addEventListener("click", () => {
        showRemoved(false);
        removeButton.focus();
    });

    // Keep the choice if the form comes back with a validation error
    showRemoved(removeImageField.value === "true");
}
