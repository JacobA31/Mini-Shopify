/**
 * Domain model: JPA {@code @Entity} classes (e.g. Shop, Product, Order).
 *
 * <p>The tables are created by the Flyway migrations in {@code src/main/resources/db/migration},
 * and Hibernate checks on startup that these classes match them. Whenever you add or change an
 * entity, add a migration (see CONTRIBUTING.md) and update both diagrams in the README in the
 * same pull request. These are the only classes shown in the README's UML class diagram.
 *
 * <p>Entities hold data and simple invariants (e.g. "inventory cannot go negative").
 * They must not depend on services, repositories, or controllers.
 */
package com.minishopify.model;
