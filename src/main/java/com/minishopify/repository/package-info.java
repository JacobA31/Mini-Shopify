/**
 * Data access: Spring Data JPA repository interfaces, one per aggregate root
 * (e.g. {@code ShopRepository extends JpaRepository<Shop, Long>}).
 *
 * <p>Prefer derived query methods ({@code findByNameContainingIgnoreCase}) and fall back
 * to {@code @Query} only when needed. Repositories are only used by services, never
 * directly by controllers.
 */
package com.minishopify.repository;
