/**
 * Domain model: JPA {@code @Entity} classes (e.g. Shop, Product, Order).
 *
 * <p>These classes are the source of truth for the database schema (Hibernate generates
 * the tables from them) and are the only classes shown in the UML class diagram in the
 * README. Whenever you add or change an entity, update both diagrams in the README in
 * the same pull request.
 *
 * <p>Entities hold data and simple invariants (e.g. "inventory cannot go negative").
 * They must not depend on services, repositories, or controllers.
 */
package com.minishopify.model;
