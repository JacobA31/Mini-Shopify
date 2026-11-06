# Mini-Shopify

[![CI/CD](https://github.com/JacobA31/Mini-Shopify/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/JacobA31/Mini-Shopify/actions/workflows/ci-cd.yml)

SYSC 4806 term project: a small e-commerce platform built with Spring MVC.

- **Live app:** _TBD (add the Azure URL once deployed)_
- **Kanban board:** _TBD (add the GitHub Projects link)_
- **Architecture:** [`docs/architecture.md`](docs/architecture.md)
- **How we work:** [`CONTRIBUTING.md`](CONTRIBUTING.md)

## About

Merchants create a shop by filling in a form with the shop's name, its categories/tags, and other details.
They then upload products with a description, picture, and inventory count. Customers find a shop by
name or by category/tag, browse its product catalog, add one or more products to a shopping cart, and
check out. Purchases are simulated, but they can never exceed the available inventory.

## Team

| Name | GitHub | Main area |
|------|--------|-----------|
|      |        |           |
|      |        |           |
|      |        |           |
|      |        |           |

## Running locally

Requires JDK 17+. Maven is not required; the wrapper downloads it.

```bash
./mvnw spring-boot:run     # http://localhost:8080
./mvnw verify              # compile, run tests, and build target/mini-shopify.jar
java -jar target/mini-shopify.jar
```

## Milestones

| Milestone           | Demo date | Goal                                              |
|---------------------|-----------|---------------------------------------------------|
| 1: Early prototype  | Nov 12    | One use case working end to end; CI + Azure live  |
| 2: Alpha release    | Nov 26    | Several related features usable together          |
| 3: Final demo       | Dec 10    | Feature-complete, no dangling links               |

## Current state

_Keep this in sync with the Kanban board before every milestone._

**Sprint 1 (in progress)**

- Done: project template (Spring Boot app skeleton, layered package structure, example tests)
- Done: CI pipeline (build and test on every PR and push to `main`)
- To do: Azure Web App setup (the deploy workflow is ready and waiting for credentials)

## Plan for next sprint

_To be agreed by the team and turned into issues on the board. See the suggested split in
[`docs/architecture.md`](docs/architecture.md#suggested-work-split)._

## Models (UML class diagram)

```mermaid
classDiagram
    class Shop {
        Long id
        String name
        String description
    }
    class Tag {
        Long id
        String name
    }
    Shop "*" -- "*" Tag : tags
```

## Database schema

```mermaid
erDiagram
    SHOPS {
        BIGINT id PK
        VARCHAR(255) name
        VARCHAR(255) description
    }
    TAGS {
        BIGINT id PK
        VARCHAR(100) name UK
    }
    SHOP_TAGS {
        BIGINT shop_id PK, FK
        BIGINT tag_id PK, FK
    }

    SHOPS ||--o{ SHOP_TAGS : has
    TAGS ||--o{ SHOP_TAGS : labels
```
