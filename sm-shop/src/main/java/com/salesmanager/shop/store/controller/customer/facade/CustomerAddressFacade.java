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
