/**
 * Web layer: Spring MVC {@code @Controller} classes that handle HTTP requests and return
 * Thymeleaf view names from {@code src/main/resources/templates}.
 *
 * <p>Controllers stay thin: read the request, call a service, put the result in the
 * {@code Model}, and pick a view. No business rules and no repository calls here.
 * If JSON endpoints are needed later (e.g. for JavaScript on a page), put the
 * {@code @RestController} classes in a {@code controller.api} sub-package.
 */
package com.minishopify.controller;
