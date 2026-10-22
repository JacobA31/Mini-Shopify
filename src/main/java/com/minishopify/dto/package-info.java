/**
 * Form and request objects (e.g. {@code CreateShopForm}) that controllers bind HTML forms
 * to, annotated with Jakarta Bean Validation constraints ({@code @NotBlank}, {@code @Min}, ...).
 *
 * <p>Keeping forms separate from entities stops users from setting fields they should not
 * (ids, inventory counts on other shops, ...) and keeps validation messages out of the model.
 */
package com.minishopify.dto;
