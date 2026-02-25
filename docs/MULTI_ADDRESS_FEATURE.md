# Customer Multi-Address Feature

## Overview

This document describes the technical design and implementation plan for adding multi-address support to Shopizer. Today, each customer record embeds exactly one billing address and one shipping (delivery) address directly in the `CUSTOMER` table. This feature introduces a standalone `CUSTOMER_ADDRESS` table that allows a customer to have any number of billing and shipping addresses, each with a label and a default flag.

Existing API contracts (`billing` / `delivery` fields on `PersistableCustomer` and `ReadableCustomer`) are fully preserved throughout the migration window.

---

## Table of Contents

1. [Current State](#1-current-state)
2. [Data Model](#2-data-model)
3. [Database Migration](#3-database-migration)
4. [Repository Layer](#4-repository-layer)
5. [Service Layer](#5-service-layer)
6. [DTOs](#6-dtos)
7. [Facade Layer](#7-facade-layer)
8. [REST API](#8-rest-api)
9. [Backward Compatibility](#9-backward-compatibility)
10. [Migration Path](#10-migration-path)
11. [Implementation Sequence](#11-implementation-sequence)
12. [Out of Scope](#12-out-of-scope)

---

## 1. Current State

### Data Model

Addresses are embedded inside the `Customer` entity using JPA `@Embeddable`:

```
Customer
├── @Embedded Billing billing        → BILLING_FIRST_NAME, BILLING_STREET_ADDRESS, …
└── @Embedded Delivery delivery      → DELIVERY_FIRST_NAME, DELIVERY_STREET_ADDRESS, …
```

All columns live in the single `CUSTOMER` table. There is no `CUSTOMER_ADDRESS` table and no `CustomerAddressRepository`.

### Affected Files

| Layer | File |
|-------|------|
| Entity | `sm-core-model/.../model/customer/Customer.java` |
| Embeddables | `sm-core-model/.../model/common/Billing.java`, `Delivery.java` |
| DTOs | `sm-shop-model/.../model/customer/CustomerEntity.java` (base), `PersistableCustomer.java`, `ReadableCustomer.java`, `address/Address.java` |
| API | `sm-shop/.../api/v1/customer/CustomerApi.java` |
| Facade | `sm-shop/.../facade/CustomerFacade.java`, `CustomerFacadeImpl.java` |
| Populators | `sm-shop/.../populator/customer/PersistableCustomerBillingAddressPopulator.java`, `PersistableCustomerShippingAddressPopulator.java`, `ReadableCustomerPopulator.java` |
| Mapper | `sm-shop/.../mapper/customer/ReadableCustomerMapper.java` |
| Repository | `sm-core/.../repositories/customer/CustomerRepository.java` |

### Limitations

| Limitation | Impact |
|------------|--------|
| One billing + one shipping address per customer | Customers with multiple delivery locations must create separate accounts |
| No address labels | Cannot distinguish "Home" from "Office" from "Warehouse" |
| No address type for BOTH | Cannot mark an address usable for both billing and shipping |
| No default selection per type | Checkout has no way to surface a preferred address |
| Embedded schema | Address columns cannot be queried independently |

---

## 2. Data Model

### 2.1 New Enum — `CustomerAddressType`

**File to create:** `sm-core-model/src/main/java/com/salesmanager/core/model/customer/CustomerAddressType.java`

```java
package com.salesmanager.core.model.customer;

public enum CustomerAddressType {
    BILLING,
    SHIPPING,
    BOTH
}
```

### 2.2 New Entity — `CustomerAddress`

**File to create:** `sm-core-model/src/main/java/com/salesmanager/core/model/customer/CustomerAddress.java`

**Table:** `CUSTOMER_ADDRESS`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `CUSTOMER_ADDRESS_ID` | BIGINT | PK, NOT NULL | `SM_SEQUENCER` key: `CUST_ADDR_SEQ_NEXT_VAL` |
| `CUSTOMER_ID` | BIGINT | FK → `CUSTOMER`, NOT NULL | Cascade delete |
| `ADDRESS_TYPE` | VARCHAR(10) | NOT NULL | `BILLING` / `SHIPPING` / `BOTH` |
| `DEFAULT_ADDRESS` | BOOLEAN | NOT NULL, DEFAULT FALSE | At most one default per type per customer |
| `ADDRESS_LABEL` | VARCHAR(100) | NULLABLE | e.g. "Home", "Office", "Warehouse" |
| `FIRST_NAME` | VARCHAR(64) | NOT NULL | |
| `LAST_NAME` | VARCHAR(64) | NOT NULL | |
| `COMPANY` | VARCHAR(100) | NULLABLE | |
| `STREET_ADDRESS` | VARCHAR(256) | NULLABLE | |
| `CITY` | VARCHAR(100) | NULLABLE | |
| `STATE_PROVINCE` | VARCHAR(100) | NULLABLE | Free-text, used when no zone code applies |
| `POSTAL_CODE` | VARCHAR(20) | NULLABLE | |
| `TELEPHONE` | VARCHAR(32) | NULLABLE | |
| `LATITUDE` | VARCHAR(100) | NULLABLE | |
| `LONGITUDE` | VARCHAR(100) | NULLABLE | |
| `ADDR_COUNTRY_ID` | INT | FK → `COUNTRY`, NOT NULL | |
| `ADDR_ZONE_ID` | INT | FK → `ZONE`, NULLABLE | |
| `DATE_CREATED` | TIMESTAMP | NULLABLE | Audit |
| `DATE_MODIFIED` | TIMESTAMP | NULLABLE | Audit |
| `UPDT_ID` | VARCHAR(60) | NULLABLE | Audit |

**JPA mapping skeleton:**

```java
@Entity
@Table(name = "CUSTOMER_ADDRESS")
public class CustomerAddress extends SalesManagerEntity<Long, CustomerAddress> implements Auditable {

    @Id
    @Column(name = "CUSTOMER_ADDRESS_ID", unique = true, nullable = false)
    @TableGenerator(
        name = "TABLE_GEN",
        table = "SM_SEQUENCER",
        pkColumnName = "SEQ_NAME",
        valueColumnName = "SEQ_COUNT",
        pkColumnValue = "CUST_ADDR_SEQ_NEXT_VAL"
    )
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "TABLE_GEN")
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "ADDRESS_TYPE", length = 10, nullable = false)
    private CustomerAddressType addressType;

    @Column(name = "DEFAULT_ADDRESS", nullable = false)
    private boolean defaultAddress = false;

    @Column(name = "ADDRESS_LABEL", length = 100)
    private String addressLabel;

    @Column(name = "FIRST_NAME", length = 64, nullable = false)
    private String firstName;

    @Column(name = "LAST_NAME", length = 64, nullable = false)
    private String lastName;

    @Column(name = "COMPANY", length = 100)
    private String company;

    @Column(name = "STREET_ADDRESS", length = 256)
    private String address;

    @Column(name = "CITY", length = 100)
    private String city;

    @Column(name = "STATE_PROVINCE", length = 100)
    private String stateProvince;

    @Column(name = "POSTAL_CODE", length = 20)
    private String postalCode;

    @Column(name = "TELEPHONE", length = 32)
    private String telephone;

    @Column(name = "LATITUDE", length = 100)
    private String latitude;

    @Column(name = "LONGITUDE", length = 100)
    private String longitude;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Country.class)
    @JoinColumn(name = "ADDR_COUNTRY_ID", nullable = false)
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Zone.class)
    @JoinColumn(name = "ADDR_ZONE_ID")
    private Zone zone;

    @JsonIgnore
    @Embedded
    private AuditSection auditSection = new AuditSection();

    // getters / setters omitted for brevity
}
```

### 2.3 Change to `Customer` Entity

**File to modify:** `sm-core-model/src/main/java/com/salesmanager/core/model/customer/Customer.java`

Add the following collection alongside the existing `attributes` collection:

```java
@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL,
           mappedBy = "customer", orphanRemoval = true)
private List<CustomerAddress> addresses = new ArrayList<>();
```

The existing `billing` and `delivery` embedded fields are **not removed**.

---

## 3. Database Migration

### 3.1 Phase 1 — Create New Table (Non-Destructive DDL)

Run against production **before** deploying new application code. No existing column is touched.

```sql
-- Register sequencer
INSERT INTO SM_SEQUENCER (SEQ_NAME, SEQ_COUNT)
VALUES ('CUST_ADDR_SEQ_NEXT_VAL', 1)
ON DUPLICATE KEY UPDATE SEQ_NAME = SEQ_NAME;

-- Create address table
CREATE TABLE CUSTOMER_ADDRESS (
    CUSTOMER_ADDRESS_ID  BIGINT       NOT NULL,
    CUSTOMER_ID          BIGINT       NOT NULL,
    ADDRESS_TYPE         VARCHAR(10)  NOT NULL,
    DEFAULT_ADDRESS      BOOLEAN      NOT NULL DEFAULT FALSE,
    ADDRESS_LABEL        VARCHAR(100),
    FIRST_NAME           VARCHAR(64)  NOT NULL,
    LAST_NAME            VARCHAR(64)  NOT NULL,
    COMPANY              VARCHAR(100),
    STREET_ADDRESS       VARCHAR(256),
    CITY                 VARCHAR(100),
    STATE_PROVINCE       VARCHAR(100),
    POSTAL_CODE          VARCHAR(20),
    TELEPHONE            VARCHAR(32),
    LATITUDE             VARCHAR(100),
    LONGITUDE            VARCHAR(100),
    ADDR_COUNTRY_ID      INT,
    ADDR_ZONE_ID         INT,
    DATE_CREATED         TIMESTAMP,
    DATE_MODIFIED        TIMESTAMP,
    UPDT_ID              VARCHAR(60),
    CONSTRAINT PK_CUSTOMER_ADDRESS   PRIMARY KEY (CUSTOMER_ADDRESS_ID),
    CONSTRAINT FK_CUST_ADDR_CUSTOMER FOREIGN KEY (CUSTOMER_ID)
        REFERENCES CUSTOMER(CUSTOMER_ID) ON DELETE CASCADE,
    CONSTRAINT FK_CUST_ADDR_COUNTRY  FOREIGN KEY (ADDR_COUNTRY_ID)
        REFERENCES COUNTRY(COUNTRY_ID),
    CONSTRAINT FK_CUST_ADDR_ZONE     FOREIGN KEY (ADDR_ZONE_ID)
        REFERENCES ZONE(ZONE_ID)
);

CREATE INDEX IDX_CUST_ADDR_CUSTOMER_ID ON CUSTOMER_ADDRESS(CUSTOMER_ID);
CREATE INDEX IDX_CUST_ADDR_TYPE        ON CUSTOMER_ADDRESS(CUSTOMER_ID, ADDRESS_TYPE);
```

### 3.2 Phase 2 — Data Migration

Because the project uses `SM_SEQUENCER` table-based ID generation (not native database sequences), row IDs cannot safely be assigned in raw SQL. The migration is performed by a Java runner (see [Section 10.4](#104-run-the-data-migration-utility)) that persists entities through JPA, allowing the standard ID generator to operate correctly.

The migration logic per customer is:

1. If `BILLING_FIRST_NAME` is non-null → create a `CustomerAddress` with `addressType = BILLING`, `defaultAddress = true`, `addressLabel = "Default Billing"`.
2. If `DELIVERY_FIRST_NAME` is non-null and delivery ≠ billing → create a `CustomerAddress` with `addressType = SHIPPING`, `defaultAddress = true`, `addressLabel = "Default Shipping"`.
3. If delivery fields are identical to billing fields → create a single row with `addressType = BOTH`, `defaultAddress = true`.
4. If the customer already has rows in `CUSTOMER_ADDRESS` → skip (idempotent).

### 3.3 Phase 3 — Embedded Column Deprecation (Future Release)

After the migration window closes and the embedded columns are confirmed redundant, the `BILLING_*` and `DELIVERY_*` columns are dropped from `CUSTOMER`. This is **out of scope** for this release.

---

## 4. Repository Layer

**File to create:** `sm-core/src/main/java/com/salesmanager/core/business/repositories/customer/CustomerAddressRepository.java`

Follow the exact query style used in `CustomerAttributeRepository` and `CustomerRepository` (JPQL with explicit `join fetch` for Country and Zone to avoid N+1 queries).

```java
package com.salesmanager.core.business.repositories.customer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.salesmanager.core.model.customer.CustomerAddress;
import com.salesmanager.core.model.customer.CustomerAddressType;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    @Query("select a from CustomerAddress a "
         + "join fetch a.customer ac "
         + "left join fetch a.country "
         + "left join fetch a.zone "
         + "where ac.id = ?1 "
         + "order by a.defaultAddress desc, a.id asc")
    List<CustomerAddress> findByCustomerId(Long customerId);

    @Query("select a from CustomerAddress a "
         + "join fetch a.customer ac "
         + "left join fetch a.country "
         + "left join fetch a.zone "
         + "where ac.id = ?1 "
         + "and (a.addressType = ?2 or a.addressType = 'BOTH') "
         + "order by a.defaultAddress desc, a.id asc")
    List<CustomerAddress> findByCustomerIdAndType(Long customerId, CustomerAddressType type);

    @Query("select a from CustomerAddress a "
         + "join fetch a.customer ac "
         + "left join fetch a.country "
         + "left join fetch a.zone "
         + "where a.id = ?1 and ac.id = ?2")
    Optional<CustomerAddress> findByIdAndCustomerId(Long addressId, Long customerId);

    @Query("select a from CustomerAddress a "
         + "join fetch a.customer ac "
         + "left join fetch a.country "
         + "left join fetch a.zone "
         + "where ac.id = ?1 "
         + "and (a.addressType = ?2 or a.addressType = 'BOTH') "
         + "and a.defaultAddress = true")
    Optional<CustomerAddress> findDefaultByCustomerIdAndType(Long customerId, CustomerAddressType type);

    @Query("select a from CustomerAddress a "
         + "join fetch a.customer ac "
         + "join fetch ac.merchantStore ms "
         + "left join fetch a.country "
         + "left join fetch a.zone "
         + "where ac.id = ?1 and ms.code = ?2")
    List<CustomerAddress> findByCustomerIdAndStoreCode(Long customerId, String storeCode);
}
```

---

## 5. Service Layer

### 5.1 Interface

**File to create:** `sm-core/src/main/java/com/salesmanager/core/business/services/customer/CustomerAddressService.java`

```java
package com.salesmanager.core.business.services.customer;

import java.util.List;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.common.generic.SalesManagerEntityService;
import com.salesmanager.core.model.customer.CustomerAddress;
import com.salesmanager.core.model.customer.CustomerAddressType;
import com.salesmanager.core.model.merchant.MerchantStore;

public interface CustomerAddressService extends SalesManagerEntityService<Long, CustomerAddress> {

    void saveOrUpdate(CustomerAddress address) throws ServiceException;

    List<CustomerAddress> getByCustomerId(Long customerId);

    List<CustomerAddress> getByCustomerIdAndType(Long customerId, CustomerAddressType type);

    /** Returns null if not found (mirrors CustomerService.getByNick() pattern). */
    CustomerAddress getByIdAndCustomerId(Long addressId, Long customerId);

    /**
     * Sets the given address as default for its type.
     * Clears the existing default for the same type in a single transaction.
     */
    void setDefaultAddress(Long addressId, Long customerId) throws ServiceException;

    /**
     * Deletes an address.
     * Throws ServiceException if the address is the last one of its type for the customer.
     * Auto-promotes the oldest remaining address of the same type to default
     * if the deleted address was the default.
     */
    void deleteAddress(Long addressId, Long customerId) throws ServiceException;

    List<CustomerAddress> getByCustomerIdAndStore(Long customerId, MerchantStore store);
}
```

### 5.2 Implementation

**File to create:** `sm-core/src/main/java/com/salesmanager/core/business/services/customer/CustomerAddressServiceImpl.java`

- **Extends:** `SalesManagerEntityServiceImpl<Long, CustomerAddress>`
- **Implements:** `CustomerAddressService`
- **Spring:** `@Service("customerAddressService")`
- **Inject:** `CustomerAddressRepository`

Key behaviours:

**`setDefaultAddress`** — `@Transactional`:
1. Load target address; throw if absent or not owned by customer.
2. Load all addresses for the customer with the same effective type.
3. Set `defaultAddress = false` on every address in that list and save.
4. Set `defaultAddress = true` on target and save.

**`deleteAddress`** — `@Transactional`:
1. Load target; verify customer ownership.
2. Count remaining addresses of the same effective type.
3. If count == 1, throw `ServiceException("Cannot delete the only BILLING/SHIPPING address")`.
4. If deleted address was default, set `defaultAddress = true` on the address with the lowest `id` among the remaining addresses.
5. Delete.

---

## 6. DTOs

### 6.1 `PersistableCustomerAddress`

**File to create:** `sm-shop-model/src/main/java/com/salesmanager/shop/model/customer/address/PersistableCustomerAddress.java`

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | Long | No | Present on update, absent on create |
| `addressType` | String | Yes | `"BILLING"`, `"SHIPPING"`, or `"BOTH"` |
| `defaultAddress` | boolean | No | Default: false |
| `addressLabel` | String | No | e.g. `"Home"` |
| `firstName` | String | Yes | |
| `lastName` | String | Yes | |
| `company` | String | No | |
| `address` | String | No | Street address |
| `city` | String | No | |
| `stateProvince` | String | No | Free-text, used when no zone code applies |
| `zone` | String | No | 2-letter code e.g. `"ON"` |
| `country` | String | Yes | 2-letter ISO code e.g. `"CA"` |
| `postalCode` | String | No | |
| `phone` | String | No | |
| `latitude` | String | No | |
| `longitude` | String | No | |

### 6.2 `ReadableCustomerAddress`

**File to create:** `sm-shop-model/src/main/java/com/salesmanager/shop/model/customer/address/ReadableCustomerAddress.java`

Same fields as `PersistableCustomerAddress` plus `id` always present. Used only for API responses.

### 6.3 Changes to Existing DTOs

**`ReadableCustomer`** — add:
```java
private List<ReadableCustomerAddress> addresses = new ArrayList<>();
```

**`PersistableCustomer`** — add:
```java
private List<PersistableCustomerAddress> addresses;
```

Both existing `billing` and `delivery` fields remain unchanged on `CustomerEntity`.

---

## 7. Facade Layer

### 7.1 New Interface — `CustomerAddressFacade`

**File to create:** `sm-shop/src/main/java/com/salesmanager/shop/store/controller/customer/facade/CustomerAddressFacade.java`

A dedicated facade keeps this concern separate from the already-large `CustomerFacade`.

```java
package com.salesmanager.shop.store.controller.customer.facade;

import java.util.List;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.model.customer.address.PersistableCustomerAddress;
import com.salesmanager.shop.model.customer.address.ReadableCustomerAddress;

public interface CustomerAddressFacade {

    // --- Admin variants (resolved by customer DB id) ---

    ReadableCustomerAddress createAddress(Long customerId, PersistableCustomerAddress address,
                                          MerchantStore store, Language language);

    ReadableCustomerAddress updateAddress(Long customerId, Long addressId,
                                          PersistableCustomerAddress address,
                                          MerchantStore store, Language language);

    void deleteAddress(Long customerId, Long addressId, MerchantStore store);

    List<ReadableCustomerAddress> getAddresses(Long customerId, MerchantStore store, Language language);

    ReadableCustomerAddress getAddress(Long customerId, Long addressId,
                                        MerchantStore store, Language language);

    void setDefaultAddress(Long customerId, Long addressId, MerchantStore store);

    // --- Authenticated customer variants (resolved by userName from JWT principal) ---

    ReadableCustomerAddress createAddress(String userName, PersistableCustomerAddress address,
                                          MerchantStore store, Language language);

    ReadableCustomerAddress updateAddress(String userName, Long addressId,
                                          PersistableCustomerAddress address,
                                          MerchantStore store, Language language);

    void deleteAddress(String userName, Long addressId, MerchantStore store);

    List<ReadableCustomerAddress> getAddresses(String userName, MerchantStore store, Language language);

    void setDefaultAddress(String userName, Long addressId, MerchantStore store);
}
```

### 7.2 `CustomerAddressFacadeImpl`

**File to create:** `sm-shop/src/main/java/com/salesmanager/shop/store/controller/customer/facade/CustomerAddressFacadeImpl.java`

- **Spring:** `@Service("customerAddressFacade")`
- **Inject:** `CustomerAddressService`, `CustomerService`, `CountryService`, `ZoneService`

**Ownership guard** (applied in every method, mirrors `CustomerFacadeImpl.getCustomerByNickAndStoreId`):

```java
// admin variant
Customer customer = customerService.getById(customerId);
if (customer == null || !customer.getMerchantStore().getCode().equals(store.getCode())) {
    throw new ResourceNotFoundException("Customer [" + customerId + "] not found");
}

// authenticated customer variant
Customer customer = customerService.getByNick(userName, store.getId());
if (customer == null) {
    throw new ResourceNotFoundException("Customer [" + userName + "] not found");
}
```

**Read-through for legacy customers** (in `getAddresses`):

```java
List<CustomerAddress> rows = customerAddressService.getByCustomerIdAndStore(customerId, store);
if (rows.isEmpty()) {
    // Customer pre-dates new table — synthesize from embedded fields
    return synthesizeFromLegacyEmbedded(customer);
}
return rows.stream().map(this::toReadable).collect(toList());
```

### 7.3 New Populators

Follow the identical pattern of `PersistableCustomerBillingAddressPopulator` and `CustomerBillingAddressPopulator`.

| File | Converts |
|------|---------|
| `sm-shop/.../populator/customer/PersistableCustomerAddressPopulator.java` | `PersistableCustomerAddress` → `CustomerAddress` |
| `sm-shop/.../populator/customer/ReadableCustomerAddressPopulator.java` | `CustomerAddress` → `ReadableCustomerAddress` |

Both extend `AbstractDataPopulator<Source, Target>`.

### 7.4 Changes to `CustomerFacadeImpl`

**File to modify:** `sm-shop/src/main/java/com/salesmanager/shop/store/controller/customer/facade/CustomerFacadeImpl.java`

1. **`create(PersistableCustomer)`** — after the customer is saved, if `customer.getAddresses()` is non-null and non-empty, persist each entry via `CustomerAddressService`.
2. **`update(PersistableCustomer)`** — same as above.
3. **`updateAddress(Long, MerchantStore, Address, Language)`** — after updating the embedded column, find the corresponding default `CustomerAddress` row by type and sync it. If no row exists, insert one (write-through for legacy records).

### 7.5 Changes to `ReadableCustomerMapper`

**File to modify:** `sm-shop/src/main/java/com/salesmanager/shop/mapper/customer/ReadableCustomerMapper.java`

In `merge()`, after populating `billing` and `delivery`, also populate the new list:

```java
List<CustomerAddress> addressEntities =
    customerAddressService.getByCustomerIdAndStore(source.getId(), store);
if (!addressEntities.isEmpty()) {
    target.setAddresses(
        addressEntities.stream()
                       .map(readableCustomerAddressPopulator::populate)
                       .collect(toList())
    );
}
```

---

## 8. REST API

### 8.1 New Controller

**File to create:** `sm-shop/src/main/java/com/salesmanager/shop/store/api/v1/customer/CustomerAddressApi.java`

Follow the annotation style of `CustomerApi.java`: `@RestController`, `@RequestMapping("/api/v1")`, `@Api(tags = {...})`, `@ApiImplicitParams` for store/language resolution, `@ApiIgnore` on injected parameters.

### 8.2 Admin Endpoints — `/api/v1/private/customer/{id}/addresses`

Require admin or staff authentication (enforced by existing Spring Security config).

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| `POST` | `/api/v1/private/customer/{id}/addresses` | `PersistableCustomerAddress` | `ReadableCustomerAddress` | 200 |
| `GET` | `/api/v1/private/customer/{id}/addresses` | — | `List<ReadableCustomerAddress>` | 200 |
| `GET` | `/api/v1/private/customer/{id}/addresses/{addressId}` | — | `ReadableCustomerAddress` | 200 / 404 |
| `PUT` | `/api/v1/private/customer/{id}/addresses/{addressId}` | `PersistableCustomerAddress` | `ReadableCustomerAddress` | 200 |
| `DELETE` | `/api/v1/private/customer/{id}/addresses/{addressId}` | — | — | 204 / 409 |
| `PATCH` | `/api/v1/private/customer/{id}/addresses/{addressId}/default` | — | — | 200 |

Optional query parameter on GET list: `?type=BILLING` or `?type=SHIPPING`.

### 8.3 Authenticated Customer Endpoints — `/api/v1/auth/customer/addresses`

Customer identity resolved from JWT principal — no `{id}` path variable accepted.

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| `POST` | `/api/v1/auth/customer/addresses` | `PersistableCustomerAddress` | `ReadableCustomerAddress` | 200 |
| `GET` | `/api/v1/auth/customer/addresses` | — | `List<ReadableCustomerAddress>` | 200 |
| `GET` | `/api/v1/auth/customer/addresses/{addressId}` | — | `ReadableCustomerAddress` | 200 / 404 |
| `PUT` | `/api/v1/auth/customer/addresses/{addressId}` | `PersistableCustomerAddress` | `ReadableCustomerAddress` | 200 |
| `DELETE` | `/api/v1/auth/customer/addresses/{addressId}` | — | — | 204 / 409 |
| `PATCH` | `/api/v1/auth/customer/addresses/{addressId}/default` | — | — | 200 |

### 8.4 Request / Response Examples

**POST `/api/v1/auth/customer/addresses`**

Request:
```json
{
  "addressType": "SHIPPING",
  "defaultAddress": false,
  "addressLabel": "Warehouse East",
  "firstName": "Jane",
  "lastName": "Doe",
  "company": "Acme Corp",
  "address": "100 Industrial Blvd",
  "city": "Detroit",
  "zone": "MI",
  "country": "US",
  "postalCode": "48201",
  "phone": "313-555-0100"
}
```

Response:
```json
{
  "id": 42,
  "addressType": "SHIPPING",
  "defaultAddress": false,
  "addressLabel": "Warehouse East",
  "firstName": "Jane",
  "lastName": "Doe",
  "company": "Acme Corp",
  "address": "100 Industrial Blvd",
  "city": "Detroit",
  "stateProvince": null,
  "zone": "MI",
  "country": "US",
  "postalCode": "48201",
  "phone": "313-555-0100",
  "latitude": null,
  "longitude": null
}
```

**GET `/api/v1/auth/customer/addresses`**

Response:
```json
[
  {
    "id": 1,
    "addressType": "BILLING",
    "defaultAddress": true,
    "addressLabel": "Default Billing",
    "firstName": "John",
    "lastName": "Doe",
    "address": "123 Main St",
    "city": "Toronto",
    "zone": "ON",
    "country": "CA",
    "postalCode": "M5V 3A7"
  },
  {
    "id": 42,
    "addressType": "SHIPPING",
    "defaultAddress": false,
    "addressLabel": "Warehouse East",
    "firstName": "Jane",
    "lastName": "Doe",
    "address": "100 Industrial Blvd",
    "city": "Detroit",
    "zone": "MI",
    "country": "US",
    "postalCode": "48201"
  }
]
```

**Error responses:**

| Scenario | HTTP Status | Body |
|----------|-------------|------|
| Address not found or not owned by customer | 404 | `{"message": "Address [42] not found"}` |
| Deleting the last address of a type | 409 | `{"message": "Cannot delete the only SHIPPING address"}` |
| Invalid country/zone code | 400 | Standard Spring validation error |

---

## 9. Backward Compatibility

### 9.1 Existing `billing` / `delivery` fields — No Breaking Change

`ReadableCustomer` and `PersistableCustomer` retain their `billing` and `delivery` fields. All existing API consumers continue to work without modification.

The existing endpoints are unchanged:
- `PATCH /api/v1/private/customer/{id}/address`
- `PATCH /api/v1/auth/customer/address`

### 9.2 Write-Through (Old Endpoints → New Table)

When `updateAddress()` is called via the old endpoints, `CustomerFacadeImpl` is extended to also sync the change into `CUSTOMER_ADDRESS`:

1. Find the default `CustomerAddress` row matching the type for this customer.
2. If found → update it with the new field values.
3. If not found → insert a new default row (handles customers who pre-date the migration).

This keeps both stores in sync during the migration window.

### 9.3 Read-Through (New Endpoints for Legacy Customers)

If `getAddresses()` is called on a customer with no `CUSTOMER_ADDRESS` rows, the facade synthesizes `ReadableCustomerAddress` objects from the embedded `billing` and `delivery` fields on the fly. This makes the new endpoints immediately usable for all customers without waiting for the data migration to complete.

---

## 10. Migration Path

### 10.1 Step 1 — Deploy Schema DDL

Run the `CREATE TABLE CUSTOMER_ADDRESS` DDL (Section 3.1) against the database. No application code change is deployed yet. The operation is purely additive and requires no downtime.

### 10.2 Step 2 — Deploy Application Code

Deploy the new application version with all code changes. At this point:
- New address endpoints are live.
- `ReadableCustomer` includes an `addresses` list (empty for all existing customers).
- Write-through is active for all `PATCH /auth/customer/address` calls.
- Read-through synthesizes addresses from embedded fields for customers with no `CUSTOMER_ADDRESS` rows.

### 10.3 Step 3 — Enable the Migration Runner

A `CustomerAddressMigrationRunner implements ApplicationRunner` is added to `sm-shop`. It is disabled by default:

```properties
# application.properties
shopizer.migration.customer-address.enabled=false
```

Set to `true` and restart to run the one-time migration. The runner pages through all customers (500 per page), checks whether `CUSTOMER_ADDRESS` rows already exist, and creates them from embedded fields if they do not. The process is idempotent and can be re-run safely.

### 10.4 Step 4 — Validate

After the migration runner completes:

```sql
-- Row count check
SELECT COUNT(*) FROM CUSTOMER_ADDRESS;
SELECT COUNT(*) FROM CUSTOMER WHERE BILLING_FIRST_NAME IS NOT NULL;

-- Spot-check
SELECT c.CUSTOMER_ID, c.BILLING_FIRST_NAME, ca.FIRST_NAME, ca.ADDRESS_TYPE
FROM CUSTOMER c
JOIN CUSTOMER_ADDRESS ca ON c.CUSTOMER_ID = ca.CUSTOMER_ID
LIMIT 20;
```

### 10.5 Step 5 — Remove Write-Through (Future Release)

Once the migration window closes, the write-through logic is removed. The embedded column DDL drop (`ALTER TABLE CUSTOMER DROP COLUMN BILLING_*`) is a separate, future release.

---

## 11. Implementation Sequence

Work proceeds module by module to respect the Maven dependency graph (`sm-core-model` → `sm-core` → `sm-shop-model` → `sm-shop`).

```
Phase 1 — sm-core-model
  CREATE  CustomerAddressType.java
  CREATE  CustomerAddress.java
  MODIFY  Customer.java               (add addresses @OneToMany)

Phase 2 — sm-core
  CREATE  CustomerAddressRepository.java
  CREATE  CustomerAddressService.java
  CREATE  CustomerAddressServiceImpl.java

Phase 3 — sm-shop-model
  CREATE  address/PersistableCustomerAddress.java
  CREATE  address/ReadableCustomerAddress.java
  MODIFY  ReadableCustomer.java       (add addresses List)
  MODIFY  PersistableCustomer.java    (add addresses List)

Phase 4 — sm-shop (populator/mapper)
  CREATE  PersistableCustomerAddressPopulator.java
  CREATE  ReadableCustomerAddressPopulator.java
  MODIFY  ReadableCustomerMapper.java (populate addresses list)

Phase 5 — sm-shop (facade)
  CREATE  CustomerAddressFacade.java
  CREATE  CustomerAddressFacadeImpl.java
  MODIFY  CustomerFacadeImpl.java     (write-through + addresses list on create/update)

Phase 6 — sm-shop (API)
  CREATE  CustomerAddressApi.java

Phase 7 — sm-shop (migration)
  CREATE  CustomerAddressMigrationRunner.java
```

---

## 12. Out of Scope

The following are explicitly excluded from this release:

1. **Dropping `BILLING_*` / `DELIVERY_*` columns** from the `CUSTOMER` table.
2. **Removing `billing` / `delivery` fields** from `CustomerEntity`, `PersistableCustomer`, or `ReadableCustomer`.
3. **Order address handling.** Orders snapshot addresses at checkout. `OrderBilling` and `OrderDelivery` are unaffected.
4. **External address validation** (USPS, Google Maps, etc.). Fields are stored as provided.
5. **Cross-store address sharing.** An address belongs to a customer which belongs to one store.
6. **Frontend / UI integration.** This plan covers API and backend only.
7. **Checkout flow pre-population** from the new address list. The checkout flow currently reads `customer.getDelivery()` directly; wiring it to prefer `CUSTOMER_ADDRESS` rows is a separate feature.
8. **Address geocoding.** `latitude` / `longitude` are stored as-is; no geocoding is triggered.
9. **Pagination on address list endpoints.** Customers are not expected to accumulate hundreds of addresses.
10. **Address deduplication.** No duplicate detection is applied.
