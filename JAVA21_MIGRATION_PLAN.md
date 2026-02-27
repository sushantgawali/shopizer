# Shopizer: Java 11 → Java 21 Migration Plan

**Project:** Shopizer E-Commerce Platform
**Current Stack:** Java 11 · Spring Boot 2.5.12 · Spring Framework 5.3.x
**Target Stack:** Java 21 (LTS) · Spring Boot 3.4.x · Spring Framework 6.2.x
**Document Date:** February 2026
**Estimated Total Duration:** 12–16 weeks

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current vs Target State](#2-current-vs-target-state)
3. [Risk Assessment](#3-risk-assessment)
4. [Migration Phases & Milestones](#4-migration-phases--milestones)
5. [Breaking Changes Reference](#5-breaking-changes-reference)
6. [Test Plan](#6-test-plan)
7. [Rollback Strategy](#7-rollback-strategy)
8. [CI/CD & Infrastructure Updates](#8-cicd--infrastructure-updates)
9. [Performance & New Features Opportunities](#9-performance--new-features-opportunities)
10. [Additional Considerations](#10-additional-considerations)

---

## 1. Executive Summary

This document outlines the complete migration plan for upgrading Shopizer from Java 11 / Spring Boot 2.5.12 to Java 21 (LTS) / Spring Boot 3.4.x. The migration is a **multi-phase, incremental process** structured to minimize risk and ensure no regression in existing functionality.

### Why Java 21?

| Version | LTS Until | Status |
|---------|-----------|--------|
| Java 11 | September 2026 | **Current — approaching EOL** |
| Java 17 | September 2029 | LTS (stepping stone only) |
| **Java 21** | **September 2031** | **Target — latest LTS** |

Java 21 provides the longest viable support window and unlocks significant performance improvements (virtual threads, improved GC) and language features (records, pattern matching, sealed classes).

### Why Spring Boot 3.4.x?

- **Required** for Java 21 virtual threads (Project Loom) support via `spring.threads.virtual.enabled=true`
- Built on Spring Framework 6.2.x and Hibernate 6.6.x
- Native image support via GraalVM
- Spring Security 6.4.x with modernized security API
- Active maintenance through 2026+

**The critical breaking point** in this migration is the Spring Boot 2.x → 3.x jump, which introduces the **`javax.*` → `jakarta.*` namespace change** — a non-trivial, project-wide change affecting every module.

---

## 2. Current vs Target State

### Core Platform

| Component | Current | Target | Breaking? |
|-----------|---------|--------|-----------|
| Java | 11 | **21** | Moderate |
| Spring Boot | 2.5.12 | **3.4.x** | **Yes — Major** |
| Spring Framework | 5.3.x | **6.2.x** | **Yes — Major** |
| Spring Security | 5.5.x | **6.4.x** | **Yes — Major** |
| Spring Data JPA | 2.5.x | **3.4.x** | Yes |
| Hibernate | 5.4.x | **6.6.x** | Yes |
| Tomcat (embedded) | 9.x | **10.1.x** | Yes (Servlet 5 → 6) |

### Dependencies

| Dependency | Current Version | Target Version | Notes |
|-----------|----------------|----------------|-------|
| Springfox Swagger | 2.9.2 | **REMOVE** | Not compatible with Spring Boot 3 |
| SpringDoc OpenAPI | — | **2.8.x** | Replacement for Springfox |
| jjwt | 0.8.0 | **0.12.x** | Breaking API changes |
| MapStruct | 1.3.0.Final | **1.6.x** | Minor changes |
| Drools / KIE | 7.32.0.Final | **9.x** | Major changes |
| Infinispan | 9.4.18.Final | **15.x** | Major changes; evaluate alternatives |
| MySQL Connector | `mysql:mysql-connector-java:8.0.21` | `com.mysql:mysql-connector-j:9.x` | GroupId/ArtifactId changed |
| AWS Java SDK | v1 (1.11.640) | **v2 (2.x)** | Major API rewrite; high effort |
| Google Cloud Storage | 1.74.0 | **2.x** | Major API changes |
| commons-fileupload | 1.3.3 | **REMOVE** | Spring Boot 3 removed support |
| commons-lang3 | 3.5 | **3.17.x** | Compatible |
| commons-io | 2.7 | **2.18.x** | Compatible |
| commons-validator | 1.5.1 | **1.9.x** | Compatible |
| Guava | 27.1-jre | **33.x-jre** | Compatible |
| Jackson | 2.13.4 | **2.18.x** (managed by Boot) | Compatible |
| Passay | 1.6.0 | **1.6.x** | Compatible |
| OWASP AntiSamy | 1.6.7 | **1.7.x** | Compatible |
| PayPal Merchant SDK | 2.6.109 | Evaluate | May need NVP/SOAP→REST migration |
| Stripe | 19.5.0 | **28.x** | Compatible API |
| Braintree | 2.73.0 | **3.x** | Minor updates |
| GeoIP2 | 2.7.0 | **4.x** | Minor updates |
| SpotBugs Maven Plugin | 3.1.8 | **4.9.x** | Compatible |

### Javax → Jakarta Namespace (All Modules Affected)

| Old Package | New Package |
|-------------|-------------|
| `javax.persistence.*` | `jakarta.persistence.*` |
| `javax.validation.*` | `jakarta.validation.*` |
| `javax.servlet.*` | `jakarta.servlet.*` |
| `javax.inject.*` | `jakarta.inject.*` |
| `javax.annotation.*` | `jakarta.annotation.*` |
| `javax.mail.*` | `jakarta.mail.*` |
| `javax.el.*` | `jakarta.el.*` |
| `javax.transaction.*` | `jakarta.transaction.*` |

---

## 3. Risk Assessment

### Risk Matrix

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| Drools 7 → 9 incompatibility | High | High | Evaluate Kogito/DMN alternatives or pin at 8.x |
| Infinispan 9 → 15 regression | High | Medium | Consider migrating to Caffeine or Redis cache |
| PayPal Merchant SDK abandoned | Medium | High | Migrate to PayPal REST SDK |
| AWS SDK v1 → v2 migration effort | Medium | Medium | Can defer; v1 still works on Java 21 |
| Springfox removal breaks API docs | High | Medium | Replace with SpringDoc (well-documented path) |
| Security config regression (Spring Security 6) | High | High | Comprehensive integration test suite required |
| jjwt 0.8 → 0.12 API changes | High | High | Token generation/parsing code requires full rewrite |
| Hibernate 6 query behavior changes | Medium | High | Run full integration tests against MySQL |
| commons-fileupload removal | High | Medium | Migrate to `spring.servlet.multipart.*` |
| CI/CD pipeline breakage | Low | Medium | Update in parallel before cutover |

### High-Risk Components

1. **jjwt token handling** — API completely changed between 0.8 and 0.12
2. **Drools rules engine** — `kie-spring` removed in v8+; major integration work
3. **Spring Security configuration** — `WebSecurityConfigurerAdapter` removed
4. **Hibernate 6 ID generation** — `use-new-id-generator-mappings` behavior changed; database integrity risk

---

## 4. Migration Phases & Milestones

### Overview Timeline

```
Week  1–2 : Phase 0 — Baseline & Preparation
Week  3–4 : Phase 1 — Spring Boot 2.5 → 2.7 (Bridge Upgrade)
Week  5–6 : Phase 2 — Safe Dependency Pre-upgrades
Week  7–9 : Phase 3 — javax → jakarta Migration + Spring Boot 3.0
Week 10–11: Phase 4 — Spring Boot 3.x Compatibility Fixes
Week 12   : Phase 5 — Java 21 Compiler Upgrade
Week 13–14: Phase 6 — Dependency Modernization
Week 15–16: Phase 7 — Final Validation & Production Readiness
```

---

### Phase 0: Baseline & Preparation (Weeks 1–2)

**Goal:** Establish a stable, documented baseline before any changes.

#### Tasks

- [ ] Create a dedicated migration branch: `git checkout -b java21-migration`
- [ ] Run full test suite and document pass/fail baseline:
  ```bash
  ./mvnw -B test -Dexcludes='**/*IntegrationTest.java' | tee test-baseline-unit.log
  ./mvnw -B test -pl sm-shop -am -Dtest='*IntegrationTest' -DfailIfNoTests=false | tee test-baseline-integration.log
  ```
- [ ] Generate API contract snapshot using Swagger UI (export OpenAPI JSON at `/v2/api-docs`)
- [ ] Run JaCoCo and export current coverage report as baseline
- [ ] Document all currently passing integration test endpoints
- [ ] Update Maven Wrapper to latest version (3.9.x):
  ```bash
  ./mvnw wrapper:wrapper -Dmaven=3.9.9
  ```
- [ ] Audit deprecated API usage:
  ```bash
  ./mvnw -B compile -Xlint:deprecation 2>&1 | grep -i "deprecated"
  ```
- [ ] Set up a parallel `java21-ci` GitHub Actions workflow (non-blocking, for tracking progress)
- [ ] Document database schema version (run `SHOW CREATE TABLE` for all entities)

#### Milestone 0 — Exit Criteria
- [ ] All existing tests pass on the baseline branch
- [ ] API contract snapshot (OpenAPI JSON) saved to `/docs/api-baseline-v3.2.7.json`
- [ ] Coverage baseline documented
- [ ] Migration branch created and CI configured

---

### Phase 1: Spring Boot 2.5 → 2.7 Bridge Upgrade (Weeks 3–4)

**Goal:** Move from Spring Boot 2.5.12 to 2.7.x as an intermediate step. Spring Boot 2.7 includes deprecation warnings that reveal what will be removed in 3.0.

#### Why This Step?
Spring Boot 2.7 is the last version before the 3.x line. It provides bridge APIs and deprecation warnings for everything removed in 3.0, making it the ideal diagnostic tool.

#### Tasks

- [ ] Update root `pom.xml` Spring Boot parent:
  ```xml
  <parent>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-parent</artifactId>
      <version>2.7.18</version>
  </parent>
  ```
- [ ] Run `./mvnw -B package -DskipTests` and resolve all compilation errors
- [ ] Run full test suite and fix all failures
- [ ] Identify all `@DeprecatedConfigurationProperty` warnings in `application.properties`
- [ ] Run with deprecation flag to identify code-level issues:
  ```bash
  ./mvnw -B compile -Xlint:deprecation 2>&1 | grep "\[deprecation\]"
  ```
- [ ] Fix Springfox warning (Springfox only works with Spring Boot 2.x; prepare for replacement)
- [ ] Update `spring.mvc.pathmatch.use-suffix-pattern=false` (required for Springfox + 2.6+, but will be removed)

#### Milestone 1 — Exit Criteria
- [ ] Application starts and passes smoke test on Spring Boot 2.7.18
- [ ] All unit and integration tests pass
- [ ] Zero compilation errors
- [ ] All deprecation warnings catalogued for Phase 3 resolution

---

### Phase 2: Safe Dependency Pre-upgrades (Weeks 5–6)

**Goal:** Upgrade dependencies that are compatible across Spring Boot 2.7 and 3.x before the major migration.

#### Tasks

**MapStruct** (1.3.0 → 1.6.x):
- [ ] Update `org.mapstruct.version` to `1.6.3`
- [ ] Update `mapstruct-processor` in annotation processor path accordingly
- [ ] Verify all mapper interfaces compile and tests pass

**jjwt** (0.8.0 → 0.12.x) — **Breaking Change**:
- [ ] Update dependency to split artifacts:
  ```xml
  <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-api</artifactId>
      <version>0.12.6</version>
  </dependency>
  <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-impl</artifactId>
      <version>0.12.6</version>
      <scope>runtime</scope>
  </dependency>
  <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-jackson</artifactId>
      <version>0.12.6</version>
      <scope>runtime</scope>
  </dependency>
  ```
- [ ] Rewrite JWT generation code (`Jwts.builder()` API changed)
- [ ] Rewrite JWT parsing code (`Jwts.parser()` → `Jwts.parserBuilder()` → `Jwts.parser()` with new chaining)
- [ ] Verify token generation, validation, and refresh all work correctly
- [ ] Verify existing tokens from prior version are handled gracefully (or implement token rotation)

**Commons Libraries**:
- [ ] `commons-lang3`: `3.5` → `3.17.0`
- [ ] `commons-io`: `2.7` → `2.18.0`
- [ ] `commons-collections4`: `4.1` → `4.5.0`
- [ ] `commons-validator`: `1.5.1` → `1.9.0`
- [ ] `guava`: `27.1-jre` → `33.4.0-jre`

**Payment & Third-party SDKs**:
- [ ] Stripe: `19.5.0` → `28.x` (review changelog for breaking changes)
- [ ] Braintree: `2.73.0` → `3.x`
- [ ] GeoIP2: `2.7.0` → `4.x`

**Build Plugins**:
- [ ] SpotBugs: `3.1.8` → `4.9.3`
- [ ] Maven Release Plugin: `2.5.3` → `3.1.1`
- [ ] Nexus Staging Plugin: `1.6.7` → `1.7.0`

#### Milestone 2 — Exit Criteria
- [ ] All tests pass with updated dependencies on Spring Boot 2.7.18
- [ ] JWT token generation and validation fully functional with new API
- [ ] No new CVEs introduced (run `./mvnw -B dependency-check:check`)

---

### Phase 3: javax → jakarta + Spring Boot 3.0 (Weeks 7–9)

**Goal:** The biggest, most impactful phase. Migrate the entire codebase from `javax.*` to `jakarta.*` and upgrade to Spring Boot 3.0.

#### Why This Is Hard
Every `@Entity`, `@Column`, `@NotNull`, `@RequestMapping`, `@Inject`, `@PostConstruct` — every JPA, validation, servlet, and injection annotation — uses the old `javax.*` package. All five modules are affected.

#### Tasks

**Automated Migration (run first)**:
- [ ] Use the Spring Boot Migrator or OpenRewrite to automate the namespace changes:
  ```bash
  # Using OpenRewrite (recommended)
  ./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run \
    -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:LATEST \
    -Drewrite.activeRecipes=org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_0
  ```
- [ ] Review all files changed by the migration tool — do not blindly accept every change

**Manual Migration Tasks**:
- [ ] Update Spring Boot parent to `3.0.x` (use `3.0.13` as initial target):
  ```xml
  <parent>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-parent</artifactId>
      <version>3.0.13</version>
  </parent>
  ```
- [ ] Update compiler version property: `<java.version>21</java.version>` (or `17` temporarily)
- [ ] Remove `javax.inject` dependency; replace with `jakarta.inject:jakarta.inject-api:2.0.1`
- [ ] Remove `javax.annotation-api`; now included in `jakarta.annotation-api`
- [ ] Replace `javax.mail` with `jakarta.mail` (via `org.eclipse.angus:angus-mail`)
- [ ] Replace `javax.servlet-api` with `jakarta.servlet:jakarta.servlet-api`
- [ ] Remove `javax.el` dependency (bundled in Tomcat 10+)
- [ ] Replace `commons-fileupload` with Spring Boot's built-in multipart support:
  ```properties
  # application.properties
  spring.servlet.multipart.enabled=true
  spring.servlet.multipart.max-file-size=9MB
  spring.servlet.multipart.max-request-size=10MB
  ```
  And remove all `CommonsMultipartResolver` bean definitions.

**MySQL Connector Update**:
- [ ] Replace deprecated groupId/artifactId:
  ```xml
  <!-- Remove -->
  <groupId>mysql</groupId>
  <artifactId>mysql-connector-java</artifactId>

  <!-- Add -->
  <groupId>com.mysql</groupId>
  <artifactId>mysql-connector-j</artifactId>
  <version>9.1.0</version>
  ```

**Hibernate 6 Changes**:
- [ ] Remove `spring.jpa.properties.hibernate.use-new-id-generator-mappings=true`
  (Hibernate 6 uses the new strategy by default; this property is gone)
- [ ] Review all `@GeneratedValue(strategy = GenerationType.AUTO)` annotations — behavior changed
  **Risk:** If the database was created under Hibernate 5, AUTO used a `hibernate_sequence` table. Hibernate 6 defaults to `SEQUENCE` per-entity. Verify ID generation doesn't break existing data.
- [ ] Update Hibernate dialect config if explicitly set:
  ```properties
  # Remove if present — auto-detected in Hibernate 6
  # spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
  ```
- [ ] Review all native queries for syntax compatibility with Hibernate 6

**Springfox → SpringDoc OpenAPI**:
- [ ] Remove Springfox dependencies from all `pom.xml` files:
  ```xml
  <!-- Remove these -->
  <artifactId>springfox-swagger2</artifactId>
  <artifactId>springfox-swagger-ui</artifactId>
  ```
- [ ] Add SpringDoc OpenAPI:
  ```xml
  <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>2.8.3</version>
  </dependency>
  ```
- [ ] Replace all Springfox annotations:
  | Old (Springfox) | New (SpringDoc) |
  |----------------|-----------------|
  | `@Api` | `@Tag` |
  | `@ApiOperation` | `@Operation` |
  | `@ApiParam` | `@Parameter` |
  | `@ApiResponse` | `@ApiResponse` (io.swagger.v3) |
  | `@ApiModel` | `@Schema` |
  | `@ApiModelProperty` | `@Schema` |
  | `@EnableSwagger2` | Remove (auto-configured) |
  | `SwaggerConfiguration` bean | Configure via `application.properties` |
- [ ] Verify Swagger UI loads at `/swagger-ui/index.html` (path changed from `/swagger-ui.html`)
- [ ] Remove `spring.mvc.pathmatch.use-suffix-pattern` property

**Spring XML Configuration**:
- [ ] Review `shopizer-core-context.xml` and `shopizer-servlet-context.xml` for `javax.*` namespace references
- [ ] Verify Spring XML namespace URIs are still valid in Spring 6

#### Milestone 3 — Exit Criteria
- [ ] Project compiles on Spring Boot 3.0.x with Java 17 or 21
- [ ] Application starts (may have runtime failures — that's OK for this milestone)
- [ ] Zero `javax.*` imports remain in main source (verified with grep)
- [ ] API documentation loads at `/swagger-ui/index.html`
- [ ] All module POMs updated

---

### Phase 4: Spring Boot 3.x Compatibility Fixes (Weeks 10–11)

**Goal:** Fix all runtime failures introduced by Phase 3. Focus on Spring Security 6, Actuator, and Infinispan.

#### Spring Security 6.x Migration

- [ ] Remove all `extends WebSecurityConfigurerAdapter` — this class is removed
- [ ] Rewrite security configuration as `@Bean`-based:
  ```java
  // OLD (Spring Security 5)
  @Configuration
  public class SecurityConfig extends WebSecurityConfigurerAdapter {
      @Override
      protected void configure(HttpSecurity http) throws Exception {
          http.antMatchers("/api/v1/auth/**").permitAll()...
      }
  }

  // NEW (Spring Security 6)
  @Configuration
  @EnableWebSecurity
  public class SecurityConfig {
      @Bean
      public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
          http
              .authorizeHttpRequests(auth -> auth
                  .requestMatchers("/api/v1/auth/**").permitAll()
                  .anyRequest().authenticated()
              )
              ...
          return http.build();
      }
  }
  ```
- [ ] Replace all `antMatchers()` → `requestMatchers()`
- [ ] Replace `authorizeRequests()` → `authorizeHttpRequests()`
- [ ] Replace `mvcMatchers()` → `requestMatchers()` (uses MVC matching by default)
- [ ] Review `PasswordEncoder` configuration (still compatible, no change needed)
- [ ] Review JWT filter chain — `OncePerRequestFilter` implementation still valid
- [ ] Test all secured and public endpoints after migration
- [ ] Review CORS configuration — `CorsConfigurationSource` approach unchanged, but verify

#### Infinispan Assessment

**Decision Point:** Infinispan 9.x → 15.x is a massive jump with significant API changes and limited Spring Boot 3 integration support.

- [ ] Evaluate migrating from Infinispan to **Caffeine** (already in Spring Boot's managed dependencies):
  ```xml
  <dependency>
      <groupId>com.github.ben-manes.caffeine</groupId>
      <artifactId>caffeine</artifactId>
  </dependency>
  ```
  Caffeine is the recommended in-process cache for Spring Boot 3 and provides comparable performance for single-node deployments.
- [ ] If distributed caching is required, evaluate **Redis** via `spring-boot-starter-data-redis`
- [ ] If Infinispan must be kept: upgrade to `15.x` and update to the new Spring Boot integration:
  ```xml
  <dependency>
      <groupId>org.infinispan</groupId>
      <artifactId>infinispan-spring-boot3-starter-embedded</artifactId>
      <version>15.1.x.Final</version>
  </dependency>
  ```
- [ ] Migrate Infinispan configuration from XML to programmatic configuration (required in 14+)

#### Drools Assessment

**Decision Point:** Drools 7 → Drools 9 (via `kie-spring` removal) is highly disruptive.

- [ ] Evaluate Drools 8.x (`org.drools:drools-core:8.x`) which removes `kie-spring` in favor of Quarkus/CDI
- [ ] Consider migrating rules to **Spring component-based rule engine** if rules are simple
- [ ] If staying on Drools: use the Drools KIE API directly without the removed Spring integration
- [ ] If migrating away from Drools: extract all `.drl` files, document rule logic, reimplement as Spring services

**Recommended path:** Drools 8.x with programmatic KIE session management (no `kie-spring`).

#### Actuator & Configuration Changes

- [ ] Review `/actuator/health` — response structure changed; update any health check scripts
- [ ] Spring Boot 3 removes `spring.profiles` in favor of `spring.profiles.active` — verify config
- [ ] Review all `@ConfigurationProperties` classes — stricter binding in Boot 3
- [ ] `spring.main.allow-bean-definition-overriding=true` may indicate bean conflicts — investigate and fix root cause
- [ ] Review EhCache configuration — EhCache 2.x requires migration to EhCache 3.x (JCache compatible):
  ```xml
  <dependency>
      <groupId>org.ehcache</groupId>
      <artifactId>ehcache</artifactId>
      <classifier>jakarta</classifier>
  </dependency>
  ```

#### Milestone 4 — Exit Criteria
- [ ] Application starts successfully on Spring Boot 3.0.x
- [ ] All REST endpoints respond (verified via integration tests)
- [ ] Authentication (JWT login/refresh/validate) works end-to-end
- [ ] Spring Security correctly blocks unauthorized requests
- [ ] Cache operations work (verified via cache-specific tests)
- [ ] At least 80% of existing integration tests pass

---

### Phase 5: Java 21 Compiler Upgrade (Week 12)

**Goal:** Upgrade from Java 17 (if used as intermediate) to Java 21, and then upgrade Spring Boot from 3.0.x to 3.4.x.

#### Tasks

**Upgrade to Spring Boot 3.4.x**:
- [ ] Update parent POM:
  ```xml
  <parent>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-parent</artifactId>
      <version>3.4.3</version>
  </parent>
  ```
- [ ] Update `<java.version>21</java.version>`
- [ ] Run full build and resolve any new deprecations between 3.0 and 3.4

**Virtual Threads (Project Loom)**:
- [ ] Enable virtual threads for Tomcat (zero-code-change performance improvement):
  ```properties
  # application.properties
  spring.threads.virtual.enabled=true
  ```
- [ ] Remove any explicit thread pool executor configurations for web requests (virtual threads handle this)
- [ ] Review any `@Async` executor configurations — can be simplified

**Java 21 Module System**:
- [ ] Verify no `--add-opens` JVM flags are needed (check for reflective access warnings)
- [ ] Update `JAVA_OPTS` in Dockerfile and `docker-compose.yml` if needed

**Optional Java 21 Enhancements** (recommend for new code only, not required):
- [ ] Introduce Java Records for simple DTOs (e.g., response wrappers)
- [ ] Use pattern matching in `instanceof` checks
- [ ] Use `SequencedCollection` where applicable

#### Milestone 5 — Exit Criteria
- [ ] Application builds and runs on Java 21
- [ ] Virtual threads enabled and application starts cleanly
- [ ] Docker image updated: `eclipse-temurin:21-jre-alpine`
- [ ] All tests pass on Java 21

---

### Phase 6: Dependency Modernization (Weeks 13–14)

**Goal:** Upgrade remaining high-risk dependencies to versions compatible with Java 21 / Spring Boot 3.4.

#### AWS SDK Assessment

AWS SDK v1 (1.11.640) is still functional on Java 21 but is in maintenance mode. Options:

- **Option A (Lower effort):** Upgrade to latest AWS SDK v1 (`1.12.x`) — minimal code changes
- **Option B (Recommended for longevity):** Migrate to AWS SDK v2 — full API rewrite of S3/SES integration code

For AWS SDK v2 (Option B):
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>2.30.x</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>ses</artifactId>
    </dependency>
</dependencies>
```

#### Google Cloud Storage

- [ ] Migrate from `com.google.cloud:google-cloud-storage:1.74.0` to `26.x`
- [ ] Review changelog for deprecated methods; the core upload/download API is similar

#### PayPal SDK

- [ ] The `merchantsdk` (NVP/SOAP based) is deprecated
- [ ] Evaluate migration to PayPal Orders REST API:
  - `com.paypal.sdk:paypalhttp` or the newer `com.paypal.sdk:checkout-sdk`
- [ ] This is a significant functional change — allocate dedicated sprint

#### Spring XML Configuration Modernization

- [ ] Consider migrating Spring XML configurations to pure Java `@Configuration` classes:
  - `shopizer-core-context.xml` → `ShopizercoreConfiguration.java`
  - `shopizer-servlet-context.xml` → `ShopizserWebConfiguration.java`
- [ ] Spring XML still works in Spring 6 but is not the recommended approach

#### Milestone 6 — Exit Criteria
- [ ] All third-party integrations (S3, SES, Payment) tested end-to-end
- [ ] No CVEs with CVSS score > 7 in dependency scan (`./mvnw dependency-check:check`)
- [ ] Maven dependency tree has no conflicts (`./mvnw dependency:tree -Dverbose | grep "conflict"`)

---

### Phase 7: Final Validation & Production Readiness (Weeks 15–16)

**Goal:** Full regression testing, performance benchmarking, and production readiness checks.

#### Tasks

- [ ] Run full test suite; target 100% of previously passing tests
- [ ] Perform API contract comparison against baseline snapshot from Phase 0
- [ ] Load test: compare throughput and latency before/after (expect improvement with virtual threads)
- [ ] Security scan: `./mvnw dependency-check:check`
- [ ] Review and update all Docker configurations
- [ ] Update GitHub Actions CI/CD workflow
- [ ] Update CircleCI configuration
- [ ] Update Docker Hub image tags
- [ ] Update `README.md` with new Java/Spring version requirements
- [ ] Update `infra/` Kubernetes configurations if JVM tuning flags are set
- [ ] Conduct a staged rollout: dev → staging → production

#### Milestone 7 (Final) — Exit Criteria
- [ ] 100% of unit tests pass
- [ ] 100% of integration tests pass (or failures explicitly documented as known issues)
- [ ] Application starts cleanly in Docker with `eclipse-temurin:21-jre-alpine`
- [ ] API contract verified — no unintended endpoint changes
- [ ] No critical CVEs
- [ ] CI/CD pipeline green on the new stack
- [ ] Changelog / release notes prepared

---

## 5. Breaking Changes Reference

### Quick-Reference: What Code Will Break

```
EVERY FILE with:                    → Action Required
  import javax.persistence.*        → import jakarta.persistence.*
  import javax.validation.*         → import jakarta.validation.*
  import javax.servlet.*            → import jakarta.servlet.*
  import javax.inject.*             → import jakarta.inject.*
  import javax.annotation.*         → import jakarta.annotation.*
  import javax.mail.*               → import jakarta.mail.*
  import javax.transaction.*        → import jakarta.transaction.*

Security config files:
  extends WebSecurityConfigurerAdapter  → Remove extends, use @Bean SecurityFilterChain
  .antMatchers(...)                     → .requestMatchers(...)
  .authorizeRequests()                  → .authorizeHttpRequests()

JWT files:
  Jwts.builder() + signWith(key, alg)   → Rewwrite with new SecretKey API
  Jwts.parser().setSigningKey(key)      → Jwts.parser().verifyWith(key).build()

Swagger files:
  @EnableSwagger2                        → Remove
  @Api(...)                              → @Tag(...)
  @ApiOperation(...)                     → @Operation(...)
  @ApiParam(...)                         → @Parameter(...)
  @ApiModel(...)                         → @Schema(...)
  @ApiModelProperty(...)                 → @Schema(...)
  new Docket(...)                        → Remove; configure via springdoc properties

application.properties:
  spring.mvc.pathmatch.use-suffix-pattern     → Remove
  spring.jpa.properties.hibernate.use-new-id-generator-mappings → Remove
  spring.jpa.properties.hibernate.dialect=..MySQL8Dialect → Remove (auto-detected)
```

---

## 6. Test Plan

### 6.1 Test Strategy

The test strategy follows a **shift-left** approach: validate correctness at every phase before proceeding to the next. No phase is complete until its milestone tests pass.

### 6.2 Unit Tests

**Framework:** JUnit 5 (already used via `junit-vintage-engine`; migrate to native JUnit 5)

**Migration Tasks:**
- [ ] Remove `junit-vintage-engine` dependency — fully migrate to JUnit 5 API:
  ```xml
  <!-- Remove this -->
  <dependency>
      <groupId>org.junit.vintage</groupId>
      <artifactId>junit-vintage-engine</artifactId>
      <scope>test</scope>
  </dependency>
  ```
- [ ] Replace `@RunWith(SpringRunner.class)` → `@ExtendWith(SpringExtension.class)`
- [ ] Replace `@Before`/`@After` → `@BeforeEach`/`@AfterEach`
- [ ] Replace `@BeforeClass`/`@AfterClass` → `@BeforeAll`/`@AfterAll`
- [ ] Replace `@Ignore` → `@Disabled`
- [ ] Replace `Assert.*` → `Assertions.*` (or use AssertJ)

**Modules with Unit Tests:**
- `sm-core`: `OrderTest.java`, `ProductTest.java`, others
- `sm-shop`: Controller/service unit tests

**Coverage Targets (maintain current thresholds):**

| Metric | Current Threshold | Phase 7 Target |
|--------|-------------------|----------------|
| Line coverage | 30% | 30% (maintain) |
| Branch coverage | 37% | 37% (maintain) |

### 6.3 Integration Tests

**Location:** `sm-shop/src/test/java/com/salesmanager/test/shop/integration/`

**Test Matrix — API Endpoints to Verify:**

| Area | Test File | Endpoints Covered |
|------|-----------|-------------------|
| Orders | `OrderApiIntegrationTest.java` | Order creation, retrieval, status update |
| Products | `ProductManagementAPIIntegrationTest.java` | CRUD operations, image upload |
| Users | `UserApiIntegrationTest.java` | Registration, login, JWT token |
| Authentication | (existing or new) | Login, token refresh, unauthorized access |
| Catalog | (existing or new) | Category, product listing |
| Shopping Cart | (existing or new) | Add/remove items, cart totals |
| Search | (existing or new) | Product search via Elasticsearch |

**Integration Test Execution:**
```bash
# Run integration tests against H2 (fast, CI-safe)
./mvnw -B test -pl sm-shop -am -Dtest='*IntegrationTest' -DfailIfNoTests=false

# Run integration tests against MySQL (production-representative)
./mvnw -B test -pl sm-shop -am -Dtest='*IntegrationTest' -Dspring.profiles.active=mysql -DfailIfNoTests=false
```

### 6.4 API Contract Testing

**Goal:** Ensure no API responses or request formats have changed unintentionally.

- [ ] Export OpenAPI spec at Phase 0 (baseline): `GET /v2/api-docs` → `api-baseline.json`
- [ ] Export OpenAPI spec after Phase 3 (SpringDoc): `GET /v3/api-docs` → `api-post-migration.json`
- [ ] Compare using `openapi-diff` or manual review:
  ```bash
  # Install openapi-diff
  npx openapi-diff api-baseline.json api-post-migration.json
  ```
- [ ] Any breaking changes (removed endpoints, changed request/response schemas) must be **intentional and documented**

### 6.5 Security Testing

- [ ] Verify unauthorized requests return `401 Unauthorized` (not 403 or 200)
- [ ] Verify endpoints requiring ADMIN role return `403 Forbidden` for non-admin tokens
- [ ] Verify JWT token generation produces valid tokens (issue, parse, validate cycle)
- [ ] Verify JWT tokens expire correctly
- [ ] Verify CORS headers are correct on all endpoints
- [ ] Verify XSS protection (AntiSamy) still active on user inputs
- [ ] Run OWASP dependency check:
  ```bash
  ./mvnw dependency-check:check -DfailBuildOnCVSS=7
  ```

### 6.6 Database Integration Testing

- [ ] Verify all JPA entities save and load correctly against MySQL 8.0
- [ ] Verify ID generation strategy produces expected IDs (critical for Hibernate 6)
- [ ] Run schema migration to confirm no data loss with existing database
- [ ] Verify all named queries and native queries execute correctly
- [ ] Test cascade operations (create/update/delete relationships)

**Critical Test: Hibernate ID Generation**
```sql
-- Before migration: verify current ID sequence state
SELECT AUTO_INCREMENT FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'SALESMANAGER';

-- After migration: verify IDs continue from correct sequence
-- Do NOT start at 1 if data already exists
```

### 6.7 Performance Testing

**Baseline (Java 11, Spring Boot 2.5.12):**
- [ ] Record response time for 10 most common API calls under load
- [ ] Record JVM heap usage under sustained load
- [ ] Record startup time

**Post-migration (Java 21, Spring Boot 3.4.x, Virtual Threads):**
- [ ] Repeat same tests
- [ ] Expected improvements:
  - 20–40% throughput increase with virtual threads under I/O-bound operations
  - Reduced thread pool configuration overhead
  - Faster startup (Spring Boot 3.x improvements)

**Load Test Tool:** Use Apache JMeter or Gatling (or `k6`):
```bash
# Example k6 test
k6 run --vus 50 --duration 60s load-test.js
```

### 6.8 Smoke Test Checklist

After each phase, run this minimal smoke test before committing to the next phase:

```bash
# 1. Start application
./mvnw spring-boot:run -pl sm-shop

# 2. Health check
curl -f http://localhost:8080/actuator/health

# 3. Login and get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin@shopizer.com","password":"password"}'

# 4. Protected endpoint
curl http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer <token>"

# 5. Swagger UI loads
curl -f http://localhost:8080/swagger-ui/index.html
```

### 6.9 Regression Test Gate

No migration phase proceeds until:

| Check | Tool | Pass Criteria |
|-------|------|---------------|
| Compilation | `mvn compile` | Zero errors |
| Unit tests | `mvn test` (excluding IT) | 100% pass |
| Integration tests | `mvn test *IntegrationTest` | 100% pass |
| Security scan | `mvn dependency-check:check` | No CVSS ≥ 7 |
| Smoke test | curl scripts above | All return expected responses |

---

## 7. Rollback Strategy

### Rollback Triggers

Immediately roll back if any of the following occur in production:

- Application fails to start
- Authentication completely broken (users cannot log in)
- Data corruption observed (IDs duplicated, records missing)
- Error rate > 5% over 10 minutes post-deployment
- Payment processing failures

### Rollback Plan

1. **Branch strategy:** All migration work happens on `java21-migration` branch; `3.2.7` is never touched until the final merge
2. **Docker:** Previous image `eclipse-temurin:11-jre` tagged images remain in Docker Hub; revert the image tag to roll back
3. **Git:** `git revert` or revert to previous release tag
4. **Database:** Hibernate 6 does NOT auto-migrate schema. The database schema is backward compatible. No DDL rollback needed unless explicitly changed.
5. **Kubernetes:** Use rolling update strategy with a minimum of 1 instance of old version running until health checks pass on new

### Blue-Green Deployment (Recommended)

For production cutover, use blue-green deployment:
1. Deploy Java 21 version to "green" environment
2. Run smoke tests against green
3. Switch load balancer to green
4. Keep blue (Java 11) running for 30 minutes as hot standby
5. Decommission blue after 30 minutes with zero incidents

---

## 8. CI/CD & Infrastructure Updates

### GitHub Actions (`.github/workflows/ci.yml`)

```yaml
# Update Java setup step
- name: Set up Java 21
  uses: actions/setup-java@v4
  with:
    java-version: "21"
    distribution: temurin
    cache: maven
```

### CircleCI (`.circleci/config.yml`)

```yaml
# Update Docker executor image
- image: cimg/openjdk:21.0
  # Or update shopizerecomm/ci:java11 → shopizerecomm/ci:java21
```

### Dockerfile (`sm-shop/Dockerfile`)

```dockerfile
# Update base image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /opt/app
EXPOSE 8080

# Virtual threads don't require special JVM flags, but tune GC if needed:
# ZGC is excellent for low-latency on Java 21
ENV JAVA_OPTS="-XX:+UseZGC -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar shopizer.jar"]
```

### `docker-compose.yml`

No changes needed to service definitions; the `shopizer` service will pick up the new Dockerfile.

### Maven Wrapper

Update to Maven 3.9.9:
```bash
./mvnw wrapper:wrapper -Dmaven=3.9.9
```

---

## 9. Performance & New Features Opportunities

### Virtual Threads (Zero-Config Performance)

Enabling virtual threads with `spring.threads.virtual.enabled=true` on Java 21 is the single highest-impact, lowest-effort improvement available. It allows the embedded Tomcat to handle thousands of concurrent requests with minimal thread overhead — ideal for the I/O-bound nature of e-commerce APIs.

### ZGC Garbage Collector

Java 21's ZGC provides sub-millisecond GC pause times, significantly improving API response consistency under load. Recommended for production:
```
JAVA_OPTS=-XX:+UseZGC
```

### Records for DTOs

Java 21 records reduce boilerplate in model classes:
```java
// Instead of POJO with getters/setters
public record ProductResponse(String id, String name, BigDecimal price) {}
```
Applicable in `sm-shop-model` module for new endpoints.

### Native Image (Future Consideration)

Spring Boot 3.x supports GraalVM native images. For Shopizer, this is a **future enhancement** — the Drools and Infinispan dependencies have limited native image support. Consider after all dependencies are modernized.

---

## 10. Additional Considerations

### Security Hardening (Take Advantage of Migration)

While migrating Spring Security, also:
- [ ] Review and tighten CORS origins — avoid wildcard `*` in production
- [ ] Enable HSTS headers
- [ ] Review CSP headers
- [ ] Ensure session creation policy is `STATELESS` for JWT-based auth
- [ ] Set `server.error.include-stacktrace=never` in production properties

### Deprecation of `spring.main.allow-bean-definition-overriding=true`

This property in `application.properties` suggests bean definition conflicts. Rather than relying on this flag (which is `false` by default in Spring Boot 3), identify and fix the conflicting beans. Spring Boot 3 will warn prominently when this causes issues.

### Elasticsearch / OpenSearch Version

The project uses Elasticsearch 7.5.2. Spring Boot 3.x ships with Elasticsearch 8.x client. This requires:
- [ ] Upgrade Elasticsearch/OpenSearch to 8.x or latest OpenSearch 2.x
- [ ] Review query DSL changes (Elasticsearch 8 uses a new typed client)
- [ ] Update `shopizer-search-version` to a compatible Spring Boot 3 starter

### Long-Term Maintainability

| Recommendation | Rationale |
|----------------|-----------|
| Replace Spring XML configs with `@Configuration` | Spring 7 may deprecate XML config; Java config is more maintainable |
| Migrate from AWS SDK v1 to v2 | v1 enters maintenance mode; v2 is async-capable and compatible with virtual threads |
| Evaluate Drools replacement | Drools 9+ integration with Spring is complex; consider lightweight rule evaluation |
| Add Testcontainers for integration tests | Replace H2 with real MySQL in tests for higher fidelity |
| Adopt OpenTelemetry via Spring Boot Actuator | Spring Boot 3.x has first-class OpenTelemetry support |

### Testcontainers for Higher-Fidelity Tests

Replace H2 in-memory database with actual MySQL container for integration tests:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
```

```java
@SpringBootTest
@Testcontainers
class OrderApiIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
    }
}
```

---

## Appendix A: Dependency Version Summary

```xml
<!-- Root pom.xml — Target versions after migration -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.3</version>
</parent>

<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>

    <!-- Updated -->
    <org.mapstruct.version>1.6.3</org.mapstruct.version>
    <drools.version>9.44.0.Final</drools.version>
    <infinispan.version>15.1.x.Final</infinispan.version>  <!-- or replace with Caffeine -->
    <guava.version>33.4.0-jre</guava.version>
    <commons-lang.version>3.17.0</commons-lang.version>
    <commons-io.version>2.18.0</commons-io.version>
    <commons-collections4.version>4.5.0</commons-collections4.version>
    <commons-validator.version>1.9.0</commons-validator.version>
    <mysql-jdbc-version>9.1.0</mysql-jdbc-version>
    <geoip2.version>4.2.0</geoip2.version>
    <jwt.version>0.12.6</jwt.version>

    <!-- Removed -->
    <!-- swagger.version — replaced by springdoc -->
    <!-- javax.* versions — replaced by jakarta.* -->
    <!-- commons-fileupload — removed -->

    <!-- Added -->
    <springdoc.version>2.8.3</springdoc.version>
</properties>
```

---

## Appendix B: Useful Commands

```bash
# Check for javax.* usage (should be 0 after Phase 3)
grep -r "import javax\." --include="*.java" sm-core/src/main sm-core-model/src/main sm-shop/src/main sm-shop-model/src/main

# Check for WebSecurityConfigurerAdapter (should be 0 after Phase 4)
grep -r "WebSecurityConfigurerAdapter" --include="*.java" .

# Check for antMatchers (should be 0 after Phase 4)
grep -r "antMatchers\|authorizeRequests\(\)" --include="*.java" .

# Check for Springfox annotations (should be 0 after Phase 3)
grep -r "springfox\|@EnableSwagger2\|@ApiOperation\|@Api(" --include="*.java" .

# View dependency tree for conflicts
./mvnw dependency:tree -Dverbose 2>&1 | grep -E "conflict|omitted"

# Check for CVEs
./mvnw dependency-check:check -DfailBuildOnCVSS=7

# Estimate javax.* change scope
grep -r "import javax\." --include="*.java" . | wc -l
```

---

*Document maintained by the Shopizer Engineering Team.
For questions about this migration plan, refer to the Spring Boot Migration Guide:
https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide*
