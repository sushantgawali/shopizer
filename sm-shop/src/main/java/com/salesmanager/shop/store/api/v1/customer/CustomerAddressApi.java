package com.salesmanager.shop.store.api.v1.customer;

import java.security.Principal;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.model.customer.address.PersistableCustomerAddress;
import com.salesmanager.shop.model.customer.address.ReadableCustomerAddress;
import com.salesmanager.shop.store.controller.customer.facade.CustomerAddressFacade;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping(value = "/api/v1")
@Api(tags = {"Customer Address management resource (Customer Address Api)"})
@SwaggerDefinition(tags = {@Tag(name = "Customer Address management resource",
        description = "Manage customer shipping and billing addresses")})
public class CustomerAddressApi {

    @Inject
    private CustomerAddressFacade customerAddressFacade;

    // =========================================================================
    // Admin endpoints  —  /private/customer/{id}/addresses
    // =========================================================================

    @PostMapping("/private/customer/{id}/addresses")
    @ApiOperation(httpMethod = "POST", value = "Creates an address for a customer",
            notes = "Requires administration access", produces = "application/json",
            response = ReadableCustomerAddress.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name = "store", dataType = "string", defaultValue = "DEFAULT"),
        @ApiImplicitParam(name = "lang",  dataType = "string", defaultValue = "en")
    })
    public ReadableCustomerAddress createForAdmin(
            @PathVariable Long id,
            @Valid @RequestBody PersistableCustomerAddress address,
            @ApiIgnore MerchantStore merchantStore,
            @ApiIgnore Language language) {
        return customerAddressFacade.createAddress(id, address, merchantStore, language);
    }

    @GetMapping("/private/customer/{id}/addresses")
    @ApiOperation(httpMethod = "GET", value = "Lists addresses for a customer",
            notes = "Requires administration access", produces = "application/json")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "store", dataType = "string", defaultValue = "DEFAULT"),
        @ApiImplicitParam(name = "lang",  dataType = "string", defaultValue = "en")
    })
    public List<ReadableCustomerAddress> listForAdmin(
            @PathVariable Long id,
            @RequestParam(required = false) String type,
            @ApiIgnore MerchantStore merchantStore,
            @ApiIgnore Language language) {
        List<ReadableCustomerAddress> addresses =
                customerAddressFacade.getAddresses(id, merchantStore, language);
        return filterByType(addresses, type);
    }

    @GetMapping("/private/customer/{id}/addresses/{addressId}")
    @ApiOperation(httpMethod = "GET", value = "Gets a specific address for a customer",
            notes = "Requires administration access", produces = "application/json",
            response = ReadableCustomerAddress.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name = "store", dataType = "string", defaultValue = "DEFAULT"),
        @ApiImplicitParam(name = "lang",  dataType = "string", defaultValue = "en")
    })
    public ReadableCustomerAddress getForAdmin(
            @PathVariable Long id,
            @PathVariable Long addressId,
            @ApiIgnore MerchantStore merchantStore,
            @ApiIgnore Language language) {
        return customerAddressFacade.getAddress(id, addressId, merchantStore, language);
    }

    @PutMapping("/private/customer/{id}/addresses/{addressId}")
    @ApiOperation(httpMethod = "PUT", value = "Updates an address for a customer",
            notes = "Requires administration access", produces = "application/json",
            response = ReadableCustomerAddress.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name = "store", dataType = "string", defaultValue = "DEFAULT"),
        @ApiImplicitParam(name = "lang",  dataType = "string", defaultValue = "en")
    })
    public ReadableCustomerAddress updateForAdmin(
            @PathVariable Long id,
            @PathVariable Long addressId,
            @Valid @RequestBody PersistableCustomerAddress address,
            @ApiIgnore MerchantStore merchantStore,
            @ApiIgnore Language language) {
        return customerAddressFacade.updateAddress(id, addressId, address, merchantStore, language);
    }

    @DeleteMapping("/private/customer/{id}/addresses/{addressId}")
    @ApiOperation(httpMethod = "DELETE", value = "Deletes an address for a customer",
            notes = "Requires administration access")
    @ApiImplicitParams({@ApiImplicitParam(name = "store", dataType = "string", defaultValue = "DEFAULT")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteForAdmin(
            @PathVariable Long id,
            @PathVariable Long addressId,
            @ApiIgnore MerchantStore merchantStore) {
        customerAddressFacade.deleteAddress(id, addressId, merchantStore);
    }

    @PatchMapping("/private/customer/{id}/addresses/{addressId}/default")
    @ApiOperation(httpMethod = "PATCH", value = "Sets an address as default for its type",
            notes = "Requires administration access")
    @ApiImplicitParams({@ApiImplicitParam(name = "store", dataType = "string", defaultValue = "DEFAULT")})
    public void setDefaultForAdmin(
            @PathVariable Long id,
            @PathVariable Long addressId,
            @ApiIgnore MerchantStore merchantStore) {
        customerAddressFacade.setDefaultAddress(id, addressId, merchantStore);
    }

    // =========================================================================
    // Authenticated customer endpoints  —  /auth/customer/addresses
    // =========================================================================

    @PostMapping("/auth/customer/addresses")
    @ApiOperation(httpMethod = "POST", value = "Creates an address for the authenticated customer",
            produces = "application/json", response = ReadableCustomerAddress.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name = "store", dataType = "string", defaultValue = "DEFAULT"),
        @ApiImplicitParam(name = "lang",  dataType = "string", defaultValue = "en")
    })
    public ReadableCustomerAddress createForAuthCustomer(
            @Valid @RequestBody PersistableCustomerAddress address,
            @ApiIgnore MerchantStore merchantStore,
            @ApiIgnore Language language,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        return customerAddressFacade.createAddress(principal.getName(), address, merchantStore, language);
    }

    @GetMapping("/auth/customer/addresses")
    @ApiOperation(httpMethod = "GET", value = "Lists addresses for the authenticated customer",
            produces = "application/json")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "store", dataType = "string", defaultValue = "DEFAULT"),
        @ApiImplicitParam(name = "lang",  dataType = "string", defaultValue = "en")
    })
    public List<ReadableCustomerAddress> listForAuthCustomer(
            @RequestParam(required = false) String type,
            @ApiIgnore MerchantStore merchantStore,
            @ApiIgnore Language language,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        List<ReadableCustomerAddress> addresses =
                customerAddressFacade.getAddresses(principal.getName(), merchantStore, language);
        return filterByType(addresses, type);
    }

    @GetMapping("/auth/customer/addresses/{addressId}")
    @ApiOperation(httpMethod = "GET", value = "Gets a specific address for the authenticated customer",
            produces = "application/json", response = ReadableCustomerAddress.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name = "store", dataType = "string", defaultValue = "DEFAULT"),
        @ApiImplicitParam(name = "lang",  dataType = "string", defaultValue = "en")
    })
    public ReadableCustomerAddress getForAuthCustomer(
            @PathVariable Long addressId,
            @ApiIgnore MerchantStore merchantStore,
            @ApiIgnore Language language,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        // Resolve customer first to get the id, then fetch the specific address
        List<ReadableCustomerAddress> addresses =
                customerAddressFacade.getAddresses(principal.getName(), merchantStore, language);
        return addresses.stream()
                .filter(a -> addressId.equals(a.getId()))
                .findFirst()
                .orElseThrow(() -> new com.salesmanager.shop.store.api.exception.ResourceNotFoundException(
                        "Address [" + addressId + "] not found"));
    }

    @PutMapping("/auth/customer/addresses/{addressId}")
    @ApiOperation(httpMethod = "PUT", value = "Updates an address for the authenticated customer",
            produces = "application/json", response = ReadableCustomerAddress.class)
    @ApiImplicitParams({
        @ApiImplicitParam(name = "store", dataType = "string", defaultValue = "DEFAULT"),
        @ApiImplicitParam(name = "lang",  dataType = "string", defaultValue = "en")
    })
    public ReadableCustomerAddress updateForAuthCustomer(
            @PathVariable Long addressId,
            @Valid @RequestBody PersistableCustomerAddress address,
            @ApiIgnore MerchantStore merchantStore,
            @ApiIgnore Language language,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        return customerAddressFacade.updateAddress(principal.getName(), addressId, address,
                merchantStore, language);
    }

    @DeleteMapping("/auth/customer/addresses/{addressId}")
    @ApiOperation(httpMethod = "DELETE", value = "Deletes an address for the authenticated customer")
    @ApiImplicitParams({@ApiImplicitParam(name = "store", dataType = "string", defaultValue = "DEFAULT")})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteForAuthCustomer(
            @PathVariable Long addressId,
            @ApiIgnore MerchantStore merchantStore,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        customerAddressFacade.deleteAddress(principal.getName(), addressId, merchantStore);
    }

    @PatchMapping("/auth/customer/addresses/{addressId}/default")
    @ApiOperation(httpMethod = "PATCH",
            value = "Sets an address as the default for its type for the authenticated customer")
    @ApiImplicitParams({@ApiImplicitParam(name = "store", dataType = "string", defaultValue = "DEFAULT")})
    public void setDefaultForAuthCustomer(
            @PathVariable Long addressId,
            @ApiIgnore MerchantStore merchantStore,
            HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        customerAddressFacade.setDefaultAddress(principal.getName(), addressId, merchantStore);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private List<ReadableCustomerAddress> filterByType(List<ReadableCustomerAddress> addresses, String type) {
        if (type == null || type.isBlank()) {
            return addresses;
        }
        String upperType = type.toUpperCase();
        return addresses.stream()
                .filter(a -> upperType.equals(a.getAddressType()) || "BOTH".equals(a.getAddressType()))
                .collect(java.util.stream.Collectors.toList());
    }
}
