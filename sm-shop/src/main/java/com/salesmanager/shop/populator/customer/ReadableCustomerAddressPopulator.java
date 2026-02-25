package com.salesmanager.shop.populator.customer;

import com.salesmanager.core.business.exception.ConversionException;
import com.salesmanager.core.business.utils.AbstractDataPopulator;
import com.salesmanager.core.model.customer.CustomerAddress;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.model.customer.address.ReadableCustomerAddress;

public class ReadableCustomerAddressPopulator
        extends AbstractDataPopulator<CustomerAddress, ReadableCustomerAddress> {

    @Override
    public ReadableCustomerAddress populate(CustomerAddress source, ReadableCustomerAddress target,
            MerchantStore store, Language language) throws ConversionException {

        target.setId(source.getId());
        target.setAddressType(source.getAddressType() != null ? source.getAddressType().name() : null);
        target.setDefaultAddress(source.isDefaultAddress());
        target.setAddressLabel(source.getAddressLabel());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setCompany(source.getCompany());
        target.setAddress(source.getAddress());
        target.setCity(source.getCity());
        target.setStateProvince(source.getStateProvince());
        target.setPostalCode(source.getPostalCode());
        target.setPhone(source.getTelephone());
        target.setLatitude(source.getLatitude());
        target.setLongitude(source.getLongitude());

        if (source.getCountry() != null) {
            target.setCountry(source.getCountry().getIsoCode());
        }
        if (source.getZone() != null) {
            target.setZone(source.getZone().getCode());
        }

        return target;
    }

    @Override
    protected ReadableCustomerAddress createTarget() {
        return new ReadableCustomerAddress();
    }
}
