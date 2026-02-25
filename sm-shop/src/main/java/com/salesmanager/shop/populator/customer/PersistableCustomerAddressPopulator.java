package com.salesmanager.shop.populator.customer;

import org.apache.commons.lang3.StringUtils;

import com.salesmanager.core.business.exception.ConversionException;
import com.salesmanager.core.business.utils.AbstractDataPopulator;
import com.salesmanager.core.model.customer.CustomerAddress;
import com.salesmanager.core.model.customer.CustomerAddressType;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.model.customer.address.PersistableCustomerAddress;

public class PersistableCustomerAddressPopulator
        extends AbstractDataPopulator<PersistableCustomerAddress, CustomerAddress> {

    @Override
    public CustomerAddress populate(PersistableCustomerAddress source, CustomerAddress target,
            MerchantStore store, Language language) throws ConversionException {

        if (StringUtils.isNotBlank(source.getAddressType())) {
            try {
                target.setAddressType(CustomerAddressType.valueOf(source.getAddressType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ConversionException("Invalid addressType: " + source.getAddressType()
                        + ". Allowed values: BILLING, SHIPPING, BOTH");
            }
        }

        target.setDefaultAddress(source.isDefaultAddress());
        target.setAddressLabel(source.getAddressLabel());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());

        if (StringUtils.isNotBlank(source.getCompany())) {
            target.setCompany(source.getCompany());
        }
        if (StringUtils.isNotBlank(source.getAddress())) {
            target.setAddress(source.getAddress());
        }
        if (StringUtils.isNotBlank(source.getCity())) {
            target.setCity(source.getCity());
        }
        if (StringUtils.isNotBlank(source.getStateProvince())) {
            target.setStateProvince(source.getStateProvince());
        }
        if (StringUtils.isNotBlank(source.getPostalCode())) {
            target.setPostalCode(source.getPostalCode());
        }
        if (StringUtils.isNotBlank(source.getPhone())) {
            target.setTelephone(source.getPhone());
        }
        if (StringUtils.isNotBlank(source.getLatitude())) {
            target.setLatitude(source.getLatitude());
        }
        if (StringUtils.isNotBlank(source.getLongitude())) {
            target.setLongitude(source.getLongitude());
        }

        return target;
    }

    @Override
    protected CustomerAddress createTarget() {
        return new CustomerAddress();
    }
}
