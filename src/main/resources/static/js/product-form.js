// Edit product page: the red × hides the current image and marks it for removal on save
document.getElementById("remove-image-button")?.addEventListener("click", () => {
    document.getElementById("remove-image").value = "true";
    document.getElementById("current-image").hidden = true;
});
