package com.salesmanager.shop.store.controller.customer.facade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.salesmanager.core.business.exception.ConversionException;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.customer.CustomerAddressService;
import com.salesmanager.core.business.services.customer.CustomerService;
import com.salesmanager.core.business.services.reference.country.CountryService;
import com.salesmanager.core.business.services.reference.zone.ZoneService;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.customer.CustomerAddress;
import com.salesmanager.core.model.customer.CustomerAddressType;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.country.Country;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.reference.zone.Zone;
import com.salesmanager.shop.model.customer.address.PersistableCustomerAddress;
import com.salesmanager.shop.model.customer.address.ReadableCustomerAddress;
import com.salesmanager.shop.populator.customer.PersistableCustomerAddressPopulator;
import com.salesmanager.shop.populator.customer.ReadableCustomerAddressPopulator;
import com.salesmanager.shop.store.api.exception.ResourceNotFoundException;
import com.salesmanager.shop.store.api.exception.ServiceRuntimeException;

@Service("customerAddressFacade")
public class CustomerAddressFacadeImpl implements CustomerAddressFacade {

    @Inject
    private CustomerAddressService customerAddressService;

    @Inject
    private CustomerService customerService;

    @Inject
    private CountryService countryService;

    @Inject
    private ZoneService zoneService;

    // -------------------------------------------------------------------------
    // Admin variants
    // -------------------------------------------------------------------------

    @Override
    public ReadableCustomerAddress createAddress(Long customerId, PersistableCustomerAddress address,
            MerchantStore store, Language language) {
        Customer customer = resolveCustomerById(customerId, store);
        return persist(address, null, customer, store, language);
    }

    @Override
    public ReadableCustomerAddress updateAddress(Long customerId, Long addressId,
            PersistableCustomerAddress address, MerchantStore store, Language language) {
        Customer customer = resolveCustomerById(customerId, store);
        CustomerAddress existing = resolveAddress(addressId, customer.getId());
        return persist(address, existing, customer, store, language);
    }

    @Override
    public void deleteAddress(Long customerId, Long addressId, MerchantStore store) {
        Customer customer = resolveCustomerById(customerId, store);
        try {
            customerAddressService.deleteAddress(addressId, customer.getId());
        } catch (ServiceException e) {
            throw new ServiceRuntimeException(e.getMessage());
        }
    }

    @Override
    public List<ReadableCustomerAddress> getAddresses(Long customerId, MerchantStore store, Language language) {
        Customer customer = resolveCustomerById(customerId, store);
        return getAddressesForCustomer(customer, store, language);
    }

    @Override
    public ReadableCustomerAddress getAddress(Long customerId, Long addressId,
            MerchantStore store, Language language) {
        Customer customer = resolveCustomerById(customerId, store);
        CustomerAddress entity = resolveAddress(addressId, customer.getId());
        return toReadable(entity, store, language);
    }

    @Override
    public void setDefaultAddress(Long customerId, Long addressId, MerchantStore store) {
        Customer customer = resolveCustomerById(customerId, store);
        try {
            customerAddressService.setDefaultAddress(addressId, customer.getId());
        } catch (ServiceException e) {
            throw new ServiceRuntimeException(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Authenticated customer variants
    // -------------------------------------------------------------------------

    @Override
    public ReadableCustomerAddress createAddress(String userName, PersistableCustomerAddress address,
            MerchantStore store, Language language) {
        Customer customer = resolveCustomerByNick(userName, store);
        return persist(address, null, customer, store, language);
    }

    @Override
    public ReadableCustomerAddress updateAddress(String userName, Long addressId,
            PersistableCustomerAddress address, MerchantStore store, Language language) {
        Customer customer = resolveCustomerByNick(userName, store);
        CustomerAddress existing = resolveAddress(addressId, customer.getId());
        return persist(address, existing, customer, store, language);
    }

    @Override
    public void deleteAddress(String userName, Long addressId, MerchantStore store) {
        Customer customer = resolveCustomerByNick(userName, store);
        try {
            customerAddressService.deleteAddress(addressId, customer.getId());
        } catch (ServiceException e) {
            throw new ServiceRuntimeException(e.getMessage());
        }
    }

    @Override
    public List<ReadableCustomerAddress> getAddresses(String userName, MerchantStore store, Language language) {
        Customer customer = resolveCustomerByNick(userName, store);
        return getAddressesForCustomer(customer, store, language);
    }

    @Override
    public void setDefaultAddress(String userName, Long addressId, MerchantStore store) {
        Customer customer = resolveCustomerByNick(userName, store);
        try {
            customerAddressService.setDefaultAddress(addressId, customer.getId());
        } catch (ServiceException e) {
            throw new ServiceRuntimeException(e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private Customer resolveCustomerById(Long customerId, MerchantStore store) {
        Customer customer = customerService.getById(customerId);
        if (customer == null || !customer.getMerchantStore().getCode().equals(store.getCode())) {
            throw new ResourceNotFoundException("Customer [" + customerId + "] not found");
        }
        return customer;
    }

    private Customer resolveCustomerByNick(String userName, MerchantStore store) {
        Customer customer = customerService.getByNick(userName, store.getId());
        if (customer == null) {
            throw new ResourceNotFoundException("Customer [" + userName + "] not found");
        }
        return customer;
    }

    private CustomerAddress resolveAddress(Long addressId, Long customerId) {
        CustomerAddress address = customerAddressService.getByIdAndCustomerId(addressId, customerId);
        if (address == null) {
            throw new ResourceNotFoundException("Address [" + addressId + "] not found");
        }
        return address;
    }

    private List<ReadableCustomerAddress> getAddressesForCustomer(Customer customer,
            MerchantStore store, Language language) {
        List<CustomerAddress> entities = customerAddressService.getByCustomerIdAndStore(customer.getId(), store);
        if (entities.isEmpty()) {
            return synthesizeFromLegacyEmbedded(customer);
        }
        return entities.stream()
                       .map(a -> toReadable(a, store, language))
                       .collect(Collectors.toList());
    }

    private ReadableCustomerAddress persist(PersistableCustomerAddress dto, CustomerAddress existing,
            Customer customer, MerchantStore store, Language language) {
        try {
            PersistableCustomerAddressPopulator populator = new PersistableCustomerAddressPopulator();
            CustomerAddress entity = (existing != null) ? existing : new CustomerAddress();
            entity = populator.populate(dto, entity, store, language);
            entity.setCustomer(customer);

            // Resolve country
            Map<String, Country> countriesMap = countryService.getCountriesMap(language);
            Country country = countriesMap.get(dto.getCountry());
            if (country == null) {
                throw new ServiceRuntimeException("Unsupported country code: " + dto.getCountry());
            }
            entity.setCountry(country);

            // Resolve zone (optional)
            if (StringUtils.isNotBlank(dto.getZone())) {
                Zone zone = zoneService.getByCode(dto.getZone());
                if (zone == null) {
                    throw new ServiceRuntimeException("Unsupported zone code: " + dto.getZone());
                }
                entity.setZone(zone);
                entity.setStateProvince(null);
            } else {
                entity.setZone(null);
            }

            // First address of its type becomes default automatically
            if (existing == null) {
                List<CustomerAddress> sameType = customerAddressService.getByCustomerIdAndType(
                        customer.getId(), entity.getAddressType());
                if (sameType.isEmpty()) {
                    entity.setDefaultAddress(true);
                }
            }

            customerAddressService.saveOrUpdate(entity);
            return toReadable(entity, store, language);

        } catch (ServiceRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceRuntimeException("Error saving customer address: " + e.getMessage());
        }
    }

    private ReadableCustomerAddress toReadable(CustomerAddress entity, MerchantStore store, Language language) {
        try {
            ReadableCustomerAddressPopulator populator = new ReadableCustomerAddressPopulator();
            return populator.populate(entity, store, language);
        } catch (ConversionException e) {
            throw new ServiceRuntimeException("Error converting customer address: " + e.getMessage());
        }
    }

    /**
     * Synthesizes ReadableCustomerAddress objects from the legacy embedded billing/delivery
     * fields for customers who pre-date the CUSTOMER_ADDRESS table.
     */
    private List<ReadableCustomerAddress> synthesizeFromLegacyEmbedded(Customer customer) {
        List<ReadableCustomerAddress> result = new ArrayList<>();

        if (customer.getBilling() != null
                && StringUtils.isNotBlank(customer.getBilling().getFirstName())) {
            ReadableCustomerAddress billing = new ReadableCustomerAddress();
            billing.setAddressType(CustomerAddressType.BILLING.name());
            billing.setDefaultAddress(true);
            billing.setAddressLabel("Default Billing");
            billing.setFirstName(customer.getBilling().getFirstName());
            billing.setLastName(customer.getBilling().getLastName());
            billing.setCompany(customer.getBilling().getCompany());
            billing.setAddress(customer.getBilling().getAddress());
            billing.setCity(customer.getBilling().getCity());
            billing.setStateProvince(customer.getBilling().getState());
            billing.setPostalCode(customer.getBilling().getPostalCode());
            billing.setPhone(customer.getBilling().getTelephone());
            if (customer.getBilling().getCountry() != null) {
                billing.setCountry(customer.getBilling().getCountry().getIsoCode());
            }
            if (customer.getBilling().getZone() != null) {
                billing.setZone(customer.getBilling().getZone().getCode());
            }
            result.add(billing);
        }

        if (customer.getDelivery() != null
                && StringUtils.isNotBlank(customer.getDelivery().getFirstName())) {
            ReadableCustomerAddress shipping = new ReadableCustomerAddress();
            shipping.setAddressType(CustomerAddressType.SHIPPING.name());
            shipping.setDefaultAddress(true);
            shipping.setAddressLabel("Default Shipping");
            shipping.setFirstName(customer.getDelivery().getFirstName());
            shipping.setLastName(customer.getDelivery().getLastName());
            shipping.setCompany(customer.getDelivery().getCompany());
            shipping.setAddress(customer.getDelivery().getAddress());
            shipping.setCity(customer.getDelivery().getCity());
            shipping.setStateProvince(customer.getDelivery().getState());
            shipping.setPostalCode(customer.getDelivery().getPostalCode());
            shipping.setPhone(customer.getDelivery().getTelephone());
            if (customer.getDelivery().getCountry() != null) {
                shipping.setCountry(customer.getDelivery().getCountry().getIsoCode());
            }
            if (customer.getDelivery().getZone() != null) {
                shipping.setZone(customer.getDelivery().getZone().getCode());
            }
            result.add(shipping);
        }

        return result;
    }
}
