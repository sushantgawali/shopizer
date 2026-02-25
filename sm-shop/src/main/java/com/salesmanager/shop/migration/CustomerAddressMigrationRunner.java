package com.salesmanager.shop.migration;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.repositories.customer.CustomerRepository;
import com.salesmanager.core.business.services.customer.CustomerAddressService;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.customer.CustomerAddress;
import com.salesmanager.core.model.customer.CustomerAddressType;

/**
 * One-time migration runner that copies legacy embedded billing/delivery address
 * data from the CUSTOMER table into the CUSTOMER_ADDRESS table.
 *
 * Disabled by default. Enable by setting:
 *   shopizer.migration.customer-address.enabled=true
 *
 * The runner is idempotent: customers who already have CUSTOMER_ADDRESS rows are skipped.
 */
@Component
public class CustomerAddressMigrationRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerAddressMigrationRunner.class);
    private static final int PAGE_SIZE = 500;

    @Value("${shopizer.migration.customer-address.enabled:false}")
    private boolean enabled;

    private final CustomerRepository customerRepository;
    private final CustomerAddressService customerAddressService;

    public CustomerAddressMigrationRunner(CustomerRepository customerRepository,
                                           CustomerAddressService customerAddressService) {
        this.customerRepository = customerRepository;
        this.customerAddressService = customerAddressService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        LOG.info("Starting CustomerAddress migration...");
        int page = 0;
        long migrated = 0;
        long skipped = 0;

        Page<Customer> customerPage;
        do {
            customerPage = customerRepository.findAll(PageRequest.of(page, PAGE_SIZE));
            for (Customer customer : customerPage.getContent()) {
                try {
                    List<CustomerAddress> existing =
                            customerAddressService.getByCustomerIdAndStore(customer.getId(),
                                    customer.getMerchantStore());
                    if (!existing.isEmpty()) {
                        skipped++;
                        continue;
                    }
                    migrateCustomer(customer);
                    migrated++;
                } catch (Exception e) {
                    LOG.error("Failed to migrate addresses for customer [{}]: {}",
                            customer.getId(), e.getMessage());
                }
            }
            if (migrated % 1000 == 0 && migrated > 0) {
                LOG.info("Migration progress: {} migrated, {} skipped", migrated, skipped);
            }
            page++;
        } while (customerPage.hasNext());

        LOG.info("CustomerAddress migration complete: {} migrated, {} skipped", migrated, skipped);
    }

    private void migrateCustomer(Customer customer) throws ServiceException {
        boolean hasBilling = customer.getBilling() != null
                && StringUtils.isNotBlank(customer.getBilling().getFirstName());
        boolean hasDelivery = customer.getDelivery() != null
                && StringUtils.isNotBlank(customer.getDelivery().getFirstName());

        if (!hasBilling && !hasDelivery) {
            return;
        }

        // Check if billing and delivery are identical — if so, create a single BOTH address
        if (hasBilling && hasDelivery && isSameAddress(customer)) {
            CustomerAddress both = fromBilling(customer);
            both.setAddressType(CustomerAddressType.BOTH);
            both.setAddressLabel("Default Address");
            customerAddressService.saveOrUpdate(both);
            return;
        }

        if (hasBilling) {
            CustomerAddress billingAddr = fromBilling(customer);
            customerAddressService.saveOrUpdate(billingAddr);
        }

        if (hasDelivery) {
            CustomerAddress shippingAddr = fromDelivery(customer);
            customerAddressService.saveOrUpdate(shippingAddr);
        }
    }

    private CustomerAddress fromBilling(Customer customer) {
        CustomerAddress addr = new CustomerAddress();
        addr.setCustomer(customer);
        addr.setAddressType(CustomerAddressType.BILLING);
        addr.setDefaultAddress(true);
        addr.setAddressLabel("Default Billing");
        addr.setFirstName(customer.getBilling().getFirstName());
        addr.setLastName(customer.getBilling().getLastName());
        addr.setCompany(customer.getBilling().getCompany());
        addr.setAddress(customer.getBilling().getAddress());
        addr.setCity(customer.getBilling().getCity());
        addr.setStateProvince(customer.getBilling().getState());
        addr.setPostalCode(customer.getBilling().getPostalCode());
        addr.setTelephone(customer.getBilling().getTelephone());
        addr.setCountry(customer.getBilling().getCountry());
        addr.setZone(customer.getBilling().getZone());
        addr.setLatitude(customer.getBilling().getLatitude());
        addr.setLongitude(customer.getBilling().getLongitude());
        return addr;
    }

    private CustomerAddress fromDelivery(Customer customer) {
        CustomerAddress addr = new CustomerAddress();
        addr.setCustomer(customer);
        addr.setAddressType(CustomerAddressType.SHIPPING);
        addr.setDefaultAddress(true);
        addr.setAddressLabel("Default Shipping");
        addr.setFirstName(customer.getDelivery().getFirstName());
        addr.setLastName(customer.getDelivery().getLastName());
        addr.setCompany(customer.getDelivery().getCompany());
        addr.setAddress(customer.getDelivery().getAddress());
        addr.setCity(customer.getDelivery().getCity());
        addr.setStateProvince(customer.getDelivery().getState());
        addr.setPostalCode(customer.getDelivery().getPostalCode());
        addr.setTelephone(customer.getDelivery().getTelephone());
        addr.setCountry(customer.getDelivery().getCountry());
        addr.setZone(customer.getDelivery().getZone());
        return addr;
    }

    private boolean isSameAddress(Customer customer) {
        String bAddr = StringUtils.trimToEmpty(customer.getBilling().getAddress());
        String dAddr = StringUtils.trimToEmpty(customer.getDelivery().getAddress());
        String bCity = StringUtils.trimToEmpty(customer.getBilling().getCity());
        String dCity = StringUtils.trimToEmpty(customer.getDelivery().getCity());
        String bPostal = StringUtils.trimToEmpty(customer.getBilling().getPostalCode());
        String dPostal = StringUtils.trimToEmpty(customer.getDelivery().getPostalCode());
        return bAddr.equalsIgnoreCase(dAddr)
                && bCity.equalsIgnoreCase(dCity)
                && bPostal.equalsIgnoreCase(dPostal);
    }
}
