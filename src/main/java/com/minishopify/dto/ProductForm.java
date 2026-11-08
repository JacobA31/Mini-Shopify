package com.minishopify.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductForm {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 750)
    private String description;

    @NotNull(message = "Price is required")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @NotNull(message = "Inventory is required")
    @Min(value = 0)
    private Integer inventory;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getInventory() { return inventory; }
    public void setInventory(Integer inventory) { this.inventory = inventory; }
}
