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

    /** Returns null if not found. */
    CustomerAddress getByIdAndCustomerId(Long addressId, Long customerId);

    /**
     * Sets the given address as the default for its type.
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
