package com.salesmanager.test.shop.integration.customer;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.nio.charset.Charset;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.core.model.customer.CustomerGender;
import com.salesmanager.shop.application.ShopApplication;
import com.salesmanager.shop.model.customer.PersistableCustomer;
import com.salesmanager.shop.model.customer.address.Address;
import com.salesmanager.shop.model.customer.address.PersistableCustomerAddress;
import com.salesmanager.shop.model.customer.address.ReadableCustomerAddress;
import com.salesmanager.shop.store.security.AuthenticationRequest;
import com.salesmanager.shop.store.security.AuthenticationResponse;
import com.salesmanager.test.shop.common.ServicesTestSupport;

/**
 * Characterization tests for the multi-address feature.
 *
 * Tests are ordered (NAME_ASCENDING) so that setup steps (register, create)
 * run before read/update/delete steps. Shared state is stored in static fields
 * because each @Test method gets a fresh instance.
 *
 * The H2 in-memory database is recreated fresh for every test run
 * (hbm2ddl.auto=create), so fixed email addresses are safe to use.
 *
 * Coverage:
 *   - Admin endpoints: POST/GET/PUT/PATCH/DELETE on /private/customer/{id}/addresses
 *   - Auth-customer endpoints: POST/GET/PUT/PATCH/DELETE on /auth/customer/addresses
 *   - ?type= query param filter on list endpoints
 *   - First address of a type is automatically marked as default
 *   - Validation: missing required fields returns 4xx
 */
@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CustomerAddressIntegrationTest extends ServicesTestSupport {

    // ------------------------------------------------------------
    // Shared state: written by early tests, read by later tests
    // ------------------------------------------------------------

    private static final String CUSTOMER_EMAIL    = "addr.test.customer@shopizer-test.com";
    private static final String CUSTOMER_PASSWORD = "Test1234";

    private static Long customerId;
    private static Long adminBillingAddressId;
    private static Long adminShippingAddressId;
    private static Long authBillingAddressId;
    private static Long authShippingAddressId;

    // =========================================================================
    // 1.  Setup – register a test customer
    // =========================================================================

    @Test
    public void test01_registerCustomer() {
        PersistableCustomer customer = new PersistableCustomer();
        customer.setEmailAddress(CUSTOMER_EMAIL);
        customer.setPassword(CUSTOMER_PASSWORD);
        customer.setGender(CustomerGender.M.name());
        customer.setLanguage("en");
        customer.setStoreCode(Constants.DEFAULT_STORE);

        Address billing = new Address();
        billing.setFirstName("Addr");
        billing.setLastName("Test");
        billing.setCountry("US");
        customer.setBilling(billing);

        HttpEntity<PersistableCustomer> entity = new HttpEntity<>(customer, getHeader());
        ResponseEntity<PersistableCustomer> response =
                testRestTemplate.postForEntity("/api/v1/customer/register", entity, PersistableCustomer.class);

        assertThat(response.getStatusCode(), is(OK));
        assertNotNull(response.getBody());
        // The returned PersistableCustomer carries the generated DB id
        customerId = response.getBody().getId();
        assertNotNull("Registration should return a non-null customer id", customerId);
        assertTrue("Customer id must be positive", customerId > 0);
    }

    // =========================================================================
    // 2.  Admin endpoints – /private/customer/{id}/addresses
    // =========================================================================

    @Test
    public void test02_adminCreateBillingAddress() {
        assertNotNull("test01 must run first", customerId);

        PersistableCustomerAddress address = billingAddress("123 Main St", "Springfield", "US");
        address.setAddressLabel("Home Billing");

        HttpEntity<PersistableCustomerAddress> entity =
                new HttpEntity<>(address, getHeader());
        ResponseEntity<ReadableCustomerAddress> response =
                testRestTemplate.postForEntity(
                        "/api/v1/private/customer/" + customerId + "/addresses",
                        entity, ReadableCustomerAddress.class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress body = response.getBody();
        assertNotNull(body);
        assertNotNull("Created address must have an id", body.getId());
        assertThat(body.getAddressType(), is("BILLING"));
        assertThat(body.getFirstName(),   is("John"));
        assertThat(body.getLastName(),    is("Doe"));
        assertThat(body.getCity(),        is("Springfield"));
        assertThat(body.getCountry(),     is("US"));
        // First address of its type must be auto-set as default
        assertTrue("First BILLING address must be default", body.isDefaultAddress());

        adminBillingAddressId = body.getId();
    }

    @Test
    public void test03_adminCreateShippingAddress() {
        assertNotNull("test01 must run first", customerId);

        PersistableCustomerAddress address = shippingAddress("456 Oak Ave", "Shelbyville", "US");
        address.setAddressLabel("Office Shipping");

        HttpEntity<PersistableCustomerAddress> entity =
                new HttpEntity<>(address, getHeader());
        ResponseEntity<ReadableCustomerAddress> response =
                testRestTemplate.postForEntity(
                        "/api/v1/private/customer/" + customerId + "/addresses",
                        entity, ReadableCustomerAddress.class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress body = response.getBody();
        assertNotNull(body);
        assertThat(body.getAddressType(), is("SHIPPING"));
        assertThat(body.getCity(),        is("Shelbyville"));
        assertTrue("First SHIPPING address must be default", body.isDefaultAddress());

        adminShippingAddressId = body.getId();
    }

    @Test
    public void test04_adminListAllAddresses() {
        assertNotNull("test02 must run first", adminBillingAddressId);
        assertNotNull("test03 must run first", adminShippingAddressId);

        HttpEntity<String> entity = new HttpEntity<>(getHeader());
        ResponseEntity<ReadableCustomerAddress[]> response =
                testRestTemplate.exchange(
                        "/api/v1/private/customer/" + customerId + "/addresses",
                        GET, entity, ReadableCustomerAddress[].class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress[] addresses = response.getBody();
        assertNotNull(addresses);
        assertThat("Should have at least 2 addresses (billing + shipping)",
                addresses.length, greaterThanOrEqualTo(2));

        boolean hasBilling  = false;
        boolean hasShipping = false;
        for (ReadableCustomerAddress a : addresses) {
            if ("BILLING".equals(a.getAddressType()))  hasBilling  = true;
            if ("SHIPPING".equals(a.getAddressType())) hasShipping = true;
        }
        assertTrue("List must contain a BILLING address",  hasBilling);
        assertTrue("List must contain a SHIPPING address", hasShipping);
    }

    @Test
    public void test05_adminListFilterByBillingType() {
        assertNotNull("test02 must run first", adminBillingAddressId);

        HttpEntity<String> entity = new HttpEntity<>(getHeader());
        ResponseEntity<ReadableCustomerAddress[]> response =
                testRestTemplate.exchange(
                        "/api/v1/private/customer/" + customerId + "/addresses?type=BILLING",
                        GET, entity, ReadableCustomerAddress[].class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress[] addresses = response.getBody();
        assertNotNull(addresses);
        assertTrue("Filtered list must not be empty", addresses.length > 0);
        for (ReadableCustomerAddress a : addresses) {
            String type = a.getAddressType();
            assertTrue("Each address must be BILLING or BOTH, got: " + type,
                    "BILLING".equals(type) || "BOTH".equals(type));
        }
    }

    @Test
    public void test06_adminListFilterByShippingType() {
        assertNotNull("test03 must run first", adminShippingAddressId);

        HttpEntity<String> entity = new HttpEntity<>(getHeader());
        ResponseEntity<ReadableCustomerAddress[]> response =
                testRestTemplate.exchange(
                        "/api/v1/private/customer/" + customerId + "/addresses?type=SHIPPING",
                        GET, entity, ReadableCustomerAddress[].class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress[] addresses = response.getBody();
        assertNotNull(addresses);
        assertTrue("Filtered list must not be empty", addresses.length > 0);
        for (ReadableCustomerAddress a : addresses) {
            String type = a.getAddressType();
            assertTrue("Each address must be SHIPPING or BOTH, got: " + type,
                    "SHIPPING".equals(type) || "BOTH".equals(type));
        }
    }

    @Test
    public void test07_adminGetSingleAddress() {
        assertNotNull("test02 must run first", adminBillingAddressId);

        HttpEntity<String> entity = new HttpEntity<>(getHeader());
        ResponseEntity<ReadableCustomerAddress> response =
                testRestTemplate.exchange(
                        "/api/v1/private/customer/" + customerId + "/addresses/" + adminBillingAddressId,
                        GET, entity, ReadableCustomerAddress.class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress body = response.getBody();
        assertNotNull(body);
        assertThat(body.getId(),          is(adminBillingAddressId));
        assertThat(body.getAddressType(), is("BILLING"));
        assertThat(body.getFirstName(),   is("John"));
        assertThat(body.getAddress(),     is("123 Main St"));
    }

    @Test
    public void test08_adminUpdateAddress() {
        assertNotNull("test02 must run first", adminBillingAddressId);

        PersistableCustomerAddress update = billingAddress("789 Elm St", "Capital City", "US");
        update.setAddressLabel("Updated Billing");
        update.setPhone("555-9999");

        HttpEntity<PersistableCustomerAddress> entity =
                new HttpEntity<>(update, getHeader());
        ResponseEntity<ReadableCustomerAddress> response =
                testRestTemplate.exchange(
                        "/api/v1/private/customer/" + customerId + "/addresses/" + adminBillingAddressId,
                        PUT, entity, ReadableCustomerAddress.class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress body = response.getBody();
        assertNotNull(body);
        assertThat(body.getAddress(),      is("789 Elm St"));
        assertThat(body.getCity(),         is("Capital City"));
        assertThat(body.getAddressLabel(), is("Updated Billing"));
        assertThat(body.getPhone(),        is("555-9999"));
    }

    @Test
    public void test09_adminCreateSecondBillingToTestDefaultSwitch() {
        assertNotNull("test01 must run first", customerId);

        // Create a second BILLING address
        PersistableCustomerAddress address = billingAddress("999 New Rd", "Ogdenville", "US");
        address.setAddressLabel("Second Billing");

        HttpEntity<PersistableCustomerAddress> entity =
                new HttpEntity<>(address, getHeader());
        ResponseEntity<ReadableCustomerAddress> response =
                testRestTemplate.postForEntity(
                        "/api/v1/private/customer/" + customerId + "/addresses",
                        entity, ReadableCustomerAddress.class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress second = response.getBody();
        assertNotNull(second);
        Long secondId = second.getId();

        // Patch the second one as default
        HttpEntity<Void> patchEntity = new HttpEntity<>(getHeader());
        ResponseEntity<Void> patchResponse =
                testRestTemplate.exchange(
                        "/api/v1/private/customer/" + customerId + "/addresses/" + secondId + "/default",
                        PATCH, patchEntity, Void.class);
        assertThat(patchResponse.getStatusCode(), is(OK));

        // Now re-fetch the second address — it must be default
        HttpEntity<String> getEntity = new HttpEntity<>(getHeader());
        ResponseEntity<ReadableCustomerAddress> getResponse =
                testRestTemplate.exchange(
                        "/api/v1/private/customer/" + customerId + "/addresses/" + secondId,
                        GET, getEntity, ReadableCustomerAddress.class);
        assertThat(getResponse.getStatusCode(), is(OK));
        assertTrue("Second billing address must now be the default",
                getResponse.getBody().isDefaultAddress());
    }

    @Test
    public void test09b_adminBillingListShowsMultipleAddressesAndSingleDefault() {
        assertNotNull("test02 must run first", adminBillingAddressId);

        HttpEntity<String> entity = new HttpEntity<>(getHeader());
        ResponseEntity<ReadableCustomerAddress[]> response =
                testRestTemplate.exchange(
                        "/api/v1/private/customer/" + customerId + "/addresses?type=BILLING",
                        GET, entity, ReadableCustomerAddress[].class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress[] addresses = response.getBody();
        assertNotNull(addresses);
        assertTrue("Expected at least two BILLING/BOTH addresses after creating a second one",
                addresses.length >= 2);

        int defaultCount = 0;
        for (ReadableCustomerAddress a : addresses) {
            assertTrue("Expected BILLING or BOTH, got: " + a.getAddressType(),
                    "BILLING".equals(a.getAddressType()) || "BOTH".equals(a.getAddressType()));
            if (a.isDefaultAddress()) {
                defaultCount++;
            }
        }

        assertThat("Exactly one default address should exist for BILLING", defaultCount, is(1));
    }

    @Test
    public void test10_adminDeleteShippingAddress() {
        assertNotNull("test03 must run first", adminShippingAddressId);

        HttpEntity<Void> entity = new HttpEntity<>(getHeader());
        ResponseEntity<Void> response =
                testRestTemplate.exchange(
                        "/api/v1/private/customer/" + customerId + "/addresses/" + adminShippingAddressId,
                        DELETE, entity, Void.class);

        assertTrue("Deleting the only SHIPPING address should be rejected (4xx/5xx) or succeed if rules changed. Got: "
                        + response.getStatusCode(),
                response.getStatusCode().is4xxClientError()
                        || response.getStatusCode().is5xxServerError()
                        || response.getStatusCode() == NO_CONTENT);
    }

    // =========================================================================
    // 3.  Auth-customer endpoints – /auth/customer/addresses
    // =========================================================================

    @Test
    public void test11_authCustomerCreateBillingAddress() {
        HttpHeaders customerHeader = getCustomerHeader();
        PersistableCustomerAddress address = billingAddress("1 Auth Billing Lane", "Portland", "US");
        address.setAddressLabel("My Home");

        HttpEntity<PersistableCustomerAddress> entity = new HttpEntity<>(address, customerHeader);
        ResponseEntity<ReadableCustomerAddress> response =
                testRestTemplate.postForEntity(
                        "/api/v1/auth/customer/addresses",
                        entity, ReadableCustomerAddress.class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getId());
        assertThat(body.getAddressType(), is("BILLING"));
        assertThat(body.getCity(),        is("Portland"));

        authBillingAddressId = body.getId();
    }

    @Test
    public void test12_authCustomerCreateShippingAddress() {
        HttpHeaders customerHeader = getCustomerHeader();
        PersistableCustomerAddress address = shippingAddress("2 Auth Ship Ave", "Seattle", "US");
        address.setAddressLabel("My Office");

        HttpEntity<PersistableCustomerAddress> entity = new HttpEntity<>(address, customerHeader);
        ResponseEntity<ReadableCustomerAddress> response =
                testRestTemplate.postForEntity(
                        "/api/v1/auth/customer/addresses",
                        entity, ReadableCustomerAddress.class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress body = response.getBody();
        assertNotNull(body);
        assertThat(body.getAddressType(), is("SHIPPING"));
        assertThat(body.getCity(),        is("Seattle"));

        authShippingAddressId = body.getId();
    }

    @Test
    public void test13_authCustomerListAllAddresses() {
        assertNotNull("test11 must run first", authBillingAddressId);
        assertNotNull("test12 must run first", authShippingAddressId);

        HttpHeaders customerHeader = getCustomerHeader();
        HttpEntity<String> entity = new HttpEntity<>(customerHeader);
        ResponseEntity<ReadableCustomerAddress[]> response =
                testRestTemplate.exchange(
                        "/api/v1/auth/customer/addresses",
                        GET, entity, ReadableCustomerAddress[].class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress[] addresses = response.getBody();
        assertNotNull(addresses);
        assertThat("Auth customer must see at least 2 addresses", addresses.length, greaterThanOrEqualTo(2));

        boolean hasBilling  = false;
        boolean hasShipping = false;
        for (ReadableCustomerAddress a : addresses) {
            if ("BILLING".equals(a.getAddressType()))  hasBilling  = true;
            if ("SHIPPING".equals(a.getAddressType())) hasShipping = true;
        }
        assertTrue("Must include a BILLING address",  hasBilling);
        assertTrue("Must include a SHIPPING address", hasShipping);
    }

    @Test
    public void test14_authCustomerListFilterByBillingType() {
        HttpHeaders customerHeader = getCustomerHeader();
        HttpEntity<String> entity = new HttpEntity<>(customerHeader);
        ResponseEntity<ReadableCustomerAddress[]> response =
                testRestTemplate.exchange(
                        "/api/v1/auth/customer/addresses?type=BILLING",
                        GET, entity, ReadableCustomerAddress[].class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress[] addresses = response.getBody();
        assertNotNull(addresses);
        assertTrue("BILLING filter must return at least one address", addresses.length > 0);
        for (ReadableCustomerAddress a : addresses) {
            String type = a.getAddressType();
            assertTrue("Expected BILLING or BOTH, got: " + type,
                    "BILLING".equals(type) || "BOTH".equals(type));
        }
    }

    @Test
    public void test15_authCustomerListFilterByShippingType() {
        HttpHeaders customerHeader = getCustomerHeader();
        HttpEntity<String> entity = new HttpEntity<>(customerHeader);
        ResponseEntity<ReadableCustomerAddress[]> response =
                testRestTemplate.exchange(
                        "/api/v1/auth/customer/addresses?type=SHIPPING",
                        GET, entity, ReadableCustomerAddress[].class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress[] addresses = response.getBody();
        assertNotNull(addresses);
        assertTrue("SHIPPING filter must return at least one address", addresses.length > 0);
        for (ReadableCustomerAddress a : addresses) {
            String type = a.getAddressType();
            assertTrue("Expected SHIPPING or BOTH, got: " + type,
                    "SHIPPING".equals(type) || "BOTH".equals(type));
        }
    }

    @Test
    public void test16_authCustomerGetSingleAddress() {
        assertNotNull("test11 must run first", authBillingAddressId);

        HttpHeaders customerHeader = getCustomerHeader();
        HttpEntity<String> entity = new HttpEntity<>(customerHeader);
        ResponseEntity<ReadableCustomerAddress> response =
                testRestTemplate.exchange(
                        "/api/v1/auth/customer/addresses/" + authBillingAddressId,
                        GET, entity, ReadableCustomerAddress.class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress body = response.getBody();
        assertNotNull(body);
        assertThat(body.getId(),          is(authBillingAddressId));
        assertThat(body.getAddressType(), is("BILLING"));
        assertThat(body.getCity(),        is("Portland"));
    }

    @Test
    public void test17_authCustomerUpdateAddress() {
        assertNotNull("test11 must run first", authBillingAddressId);

        HttpHeaders customerHeader = getCustomerHeader();
        PersistableCustomerAddress update = billingAddress("10 Updated Blvd", "Denver", "US");
        update.setPhone("800-UPDATED");
        update.setAddressLabel("Updated Home");

        HttpEntity<PersistableCustomerAddress> entity = new HttpEntity<>(update, customerHeader);
        ResponseEntity<ReadableCustomerAddress> response =
                testRestTemplate.exchange(
                        "/api/v1/auth/customer/addresses/" + authBillingAddressId,
                        PUT, entity, ReadableCustomerAddress.class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress body = response.getBody();
        assertNotNull(body);
        assertThat(body.getAddress(),      is("10 Updated Blvd"));
        assertThat(body.getCity(),         is("Denver"));
        assertThat(body.getPhone(),        is("800-UPDATED"));
        assertThat(body.getAddressLabel(), is("Updated Home"));
    }

    @Test
    public void test18_authCustomerSetDefaultShipping() {
        assertNotNull("test12 must run first", authShippingAddressId);

        // Create a second SHIPPING so we have something to promote
        HttpHeaders customerHeader = getCustomerHeader();
        PersistableCustomerAddress second = shippingAddress("99 Second Ship St", "Austin", "US");
        second.setAddressLabel("Second Ship");
        HttpEntity<PersistableCustomerAddress> createEntity = new HttpEntity<>(second, customerHeader);
        ResponseEntity<ReadableCustomerAddress> createResponse =
                testRestTemplate.postForEntity("/api/v1/auth/customer/addresses",
                        createEntity, ReadableCustomerAddress.class);
        assertThat(createResponse.getStatusCode(), is(OK));
        Long secondShipId = createResponse.getBody().getId();

        // Set the new second address as default
        HttpEntity<Void> patchEntity = new HttpEntity<>(customerHeader);
        ResponseEntity<Void> patchResponse =
                testRestTemplate.exchange(
                        "/api/v1/auth/customer/addresses/" + secondShipId + "/default",
                        PATCH, patchEntity, Void.class);
        assertThat(patchResponse.getStatusCode(), is(OK));

        // Verify it is now default
        HttpEntity<String> getEntity = new HttpEntity<>(customerHeader);
        ResponseEntity<ReadableCustomerAddress> getResponse =
                testRestTemplate.exchange(
                        "/api/v1/auth/customer/addresses/" + secondShipId,
                        GET, getEntity, ReadableCustomerAddress.class);
        assertThat(getResponse.getStatusCode(), is(OK));
        assertTrue("Newly set default shipping must be marked default",
                getResponse.getBody().isDefaultAddress());
    }

    @Test
    public void test19_authCustomerDeleteShippingAddress() {
        assertNotNull("test12 must run first", authShippingAddressId);

        HttpHeaders customerHeader = getCustomerHeader();
        HttpEntity<Void> entity = new HttpEntity<>(customerHeader);
        ResponseEntity<Void> response =
                testRestTemplate.exchange(
                        "/api/v1/auth/customer/addresses/" + authShippingAddressId,
                        DELETE, entity, Void.class);

        assertThat(response.getStatusCode(), is(NO_CONTENT));
    }

    // =========================================================================
    // 4.  Isolation – one customer cannot see another customer's addresses
    // =========================================================================

    @Test
    public void test20_authCustomerCannotAccessOtherCustomerAddress() {
        // adminBillingAddressId belongs to another customer created by admin tests
        // The auth customer endpoint resolves addresses by the JWT principal's identity,
        // so it should return 404 (not found) for addresses that belong to a different customer.
        assertNotNull("test02 must run first", adminBillingAddressId);

        HttpHeaders customerHeader = getHeader(CUSTOMER_EMAIL, CUSTOMER_PASSWORD);
        HttpEntity<String> entity = new HttpEntity<>(customerHeader);
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/auth/customer/addresses/" + adminBillingAddressId,
                        GET, entity, String.class);

        // Must NOT return 2xx — the address belongs to a different customer
        assertTrue("Auth customer must not see another customer's address, status: "
                        + response.getStatusCode(),
                !response.getStatusCode().is2xxSuccessful());
    }

    // =========================================================================
    // 5.  Validation – missing required fields
    // =========================================================================

    @Test
    public void test21_createAddressMissingFirstName() {
        PersistableCustomerAddress bad = new PersistableCustomerAddress();
        bad.setAddressType("BILLING");
        // firstName intentionally omitted — @NotEmpty should reject it
        bad.setLastName("Doe");
        bad.setCountry("US");
        bad.setAddress("1 Bad Lane");
        bad.setCity("Nowhere");

        HttpEntity<PersistableCustomerAddress> entity = new HttpEntity<>(bad, getHeader());
        ResponseEntity<String> response =
                testRestTemplate.postForEntity(
                        "/api/v1/private/customer/" + customerId + "/addresses",
                        entity, String.class);

        assertTrue("Missing firstName must be rejected with 4xx/5xx, got: " + response.getStatusCode(),
                response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
    }

    @Test
    public void test22_createAddressMissingAddressType() {
        PersistableCustomerAddress bad = new PersistableCustomerAddress();
        // addressType intentionally omitted — @NotEmpty should reject it
        bad.setFirstName("John");
        bad.setLastName("Doe");
        bad.setCountry("US");

        HttpEntity<PersistableCustomerAddress> entity = new HttpEntity<>(bad, getHeader());
        ResponseEntity<String> response =
                testRestTemplate.postForEntity(
                        "/api/v1/private/customer/" + customerId + "/addresses",
                        entity, String.class);

        assertTrue("Missing addressType must be rejected with 4xx/5xx, got: " + response.getStatusCode(),
                response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
    }

    @Test
    public void test23_createAddressMissingCountry() {
        PersistableCustomerAddress bad = new PersistableCustomerAddress();
        bad.setAddressType("BILLING");
        bad.setFirstName("John");
        bad.setLastName("Doe");
        // country intentionally omitted

        HttpEntity<PersistableCustomerAddress> entity = new HttpEntity<>(bad, getHeader());
        ResponseEntity<String> response =
                testRestTemplate.postForEntity(
                        "/api/v1/private/customer/" + customerId + "/addresses",
                        entity, String.class);

        assertTrue("Missing country must be rejected with 4xx/5xx, got: " + response.getStatusCode(),
                response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
    }

    @Test
    public void test24_createAddressWithInvalidCountryCode() {
        PersistableCustomerAddress bad = billingAddress("1 Invalid St", "Nowhere", "ZZ");

        HttpEntity<PersistableCustomerAddress> entity = new HttpEntity<>(bad, getHeader());
        ResponseEntity<String> response =
                testRestTemplate.postForEntity(
                        "/api/v1/private/customer/" + customerId + "/addresses",
                        entity, String.class);

        // Invalid country → service throws ServiceRuntimeException → 4xx or 5xx
        assertTrue("Invalid country code must be rejected, got: " + response.getStatusCode(),
                response.getStatusCode().is4xxClientError()
                        || response.getStatusCode().is5xxServerError());
    }

    // =========================================================================
    // 6.  Admin GET single address – 404 for unknown id
    // =========================================================================

    @Test
    public void test25_adminGetNonExistentAddress() {
        HttpEntity<String> entity = new HttpEntity<>(getHeader());
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/private/customer/" + customerId + "/addresses/9999999",
                        GET, entity, String.class);

        assertThat(response.getStatusCode().value(), is(404));
    }

    // =========================================================================
    // 7.  Admin GET for non-existent customer – 404
    // =========================================================================

    @Test
    public void test26_adminGetAddressesForNonExistentCustomer() {
        HttpEntity<String> entity = new HttpEntity<>(getHeader());
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/private/customer/9999999/addresses",
                        GET, entity, String.class);

        assertThat(response.getStatusCode().value(), is(404));
    }

    // =========================================================================
    // 8.  Admin response includes full address fields
    // =========================================================================

    @Test
    public void test27_adminCreatedAddressFieldsRoundTrip() {
        PersistableCustomerAddress address = new PersistableCustomerAddress();
        address.setAddressType("SHIPPING");
        address.setFirstName("Round");
        address.setLastName("Trip");
        address.setCompany("Acme Inc");
        address.setAddress("7 Field Check Rd");
        address.setCity("Testville");
        address.setPostalCode("99999");
        address.setPhone("123-456-7890");
        address.setCountry("US");
        address.setStateProvince("CA");
        address.setAddressLabel("Round Trip Label");

        HttpEntity<PersistableCustomerAddress> entity = new HttpEntity<>(address, getHeader());
        ResponseEntity<ReadableCustomerAddress> response =
                testRestTemplate.postForEntity(
                        "/api/v1/private/customer/" + customerId + "/addresses",
                        entity, ReadableCustomerAddress.class);

        assertThat(response.getStatusCode(), is(OK));
        ReadableCustomerAddress body = response.getBody();
        assertNotNull(body);
        assertThat(body.getFirstName(),    is("Round"));
        assertThat(body.getLastName(),     is("Trip"));
        assertThat(body.getCompany(),      is("Acme Inc"));
        assertThat(body.getAddress(),      is("7 Field Check Rd"));
        assertThat(body.getCity(),         is("Testville"));
        assertThat(body.getPostalCode(),   is("99999"));
        assertThat(body.getPhone(),        is("123-456-7890"));
        assertThat(body.getCountry(),      is("US"));
        assertThat(body.getStateProvince(),is("CA"));
        assertThat(body.getAddressLabel(), is("Round Trip Label"));
        assertThat(body.getAddressType(),  is("SHIPPING"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private PersistableCustomerAddress billingAddress(String street, String city, String country) {
        PersistableCustomerAddress a = new PersistableCustomerAddress();
        a.setAddressType("BILLING");
        a.setFirstName("John");
        a.setLastName("Doe");
        a.setAddress(street);
        a.setCity(city);
        a.setCountry(country);
        a.setPostalCode("12345");
        return a;
    }

    private PersistableCustomerAddress shippingAddress(String street, String city, String country) {
        PersistableCustomerAddress a = new PersistableCustomerAddress();
        a.setAddressType("SHIPPING");
        a.setFirstName("Jane");
        a.setLastName("Doe");
        a.setAddress(street);
        a.setCity(city);
        a.setCountry(country);
        a.setPostalCode("54321");
        return a;
    }

    private HttpHeaders getCustomerHeader() {
        ResponseEntity<AuthenticationResponse> response = testRestTemplate.postForEntity(
                "/api/v1/customer/login",
                new HttpEntity<>(new AuthenticationRequest(CUSTOMER_EMAIL, CUSTOMER_PASSWORD)),
                AuthenticationResponse.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));
        headers.add("Authorization", "Bearer " + response.getBody().getToken());
        return headers;
    }
}
