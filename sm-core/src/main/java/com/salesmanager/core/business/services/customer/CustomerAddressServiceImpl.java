package com.salesmanager.core.business.services.customer;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.repositories.customer.CustomerAddressRepository;
import com.salesmanager.core.business.services.common.generic.SalesManagerEntityServiceImpl;
import com.salesmanager.core.model.customer.CustomerAddress;
import com.salesmanager.core.model.customer.CustomerAddressType;
import com.salesmanager.core.model.merchant.MerchantStore;

@Service("customerAddressService")
public class CustomerAddressServiceImpl
        extends SalesManagerEntityServiceImpl<Long, CustomerAddress>
        implements CustomerAddressService {

    @Inject
    private CustomerAddressRepository customerAddressRepository;

    public CustomerAddressServiceImpl(CustomerAddressRepository customerAddressRepository) {
        super(customerAddressRepository);
        this.customerAddressRepository = customerAddressRepository;
    }

    @Override
    public void saveOrUpdate(CustomerAddress address) throws ServiceException {
        if (address.getId() != null && address.getId() > 0) {
            update(address);
        } else {
            save(address);
        }
    }

    @Override
    public List<CustomerAddress> getByCustomerId(Long customerId) {
        return customerAddressRepository.findByCustomerId(customerId);
    }

    @Override
    public List<CustomerAddress> getByCustomerIdAndType(Long customerId, CustomerAddressType type) {
        return customerAddressRepository.findByCustomerIdAndType(customerId, type);
    }

    @Override
    public CustomerAddress getByIdAndCustomerId(Long addressId, Long customerId) {
        return customerAddressRepository.findByIdAndCustomerId(addressId, customerId).orElse(null);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long addressId, Long customerId) throws ServiceException {
        Optional<CustomerAddress> targetOpt = customerAddressRepository.findByIdAndCustomerId(addressId, customerId);
        if (!targetOpt.isPresent()) {
            throw new ServiceException("Address [" + addressId + "] not found for customer [" + customerId + "]");
        }
        CustomerAddress target = targetOpt.get();

        // Clear current default(s) for the same effective type
        List<CustomerAddress> sameType = customerAddressRepository.findByCustomerIdAndType(customerId, target.getAddressType());
        for (CustomerAddress a : sameType) {
            if (a.isDefaultAddress()) {
                a.setDefaultAddress(false);
                customerAddressRepository.save(a);
            }
        }

        target.setDefaultAddress(true);
        customerAddressRepository.save(target);
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId, Long customerId) throws ServiceException {
        Optional<CustomerAddress> targetOpt = customerAddressRepository.findByIdAndCustomerId(addressId, customerId);
        if (!targetOpt.isPresent()) {
            throw new ServiceException("Address [" + addressId + "] not found for customer [" + customerId + "]");
        }
        CustomerAddress target = targetOpt.get();

        List<CustomerAddress> remaining = customerAddressRepository.findByCustomerIdAndType(customerId, target.getAddressType());
        if (remaining.size() <= 1) {
            throw new ServiceException("Cannot delete the only " + target.getAddressType().name() + " address");
        }

        // If deleting the default, promote the next oldest address
        if (target.isDefaultAddress()) {
            remaining.stream()
                     .filter(a -> !a.getId().equals(addressId))
                     .min((a, b) -> a.getId().compareTo(b.getId()))
                     .ifPresent(a -> {
                         a.setDefaultAddress(true);
                         customerAddressRepository.save(a);
                     });
        }

        customerAddressRepository.delete(target);
    }

    @Override
    public List<CustomerAddress> getByCustomerIdAndStore(Long customerId, MerchantStore store) {
        return customerAddressRepository.findByCustomerIdAndStoreCode(customerId, store.getCode());
    }
}
