package com.minishopify.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

// The data (and it's validation) of the form filled out for creation of a shop
public class ShopForm {

    @NotBlank(message = "Shop name is required")
    private String name;

    private String description;

    // Tags are submitted individually by the form, for example ["coffee", "local", "bakery"]
    private List<String> tags = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }

}
