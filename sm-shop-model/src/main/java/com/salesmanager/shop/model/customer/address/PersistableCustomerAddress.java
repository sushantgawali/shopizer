package com.salesmanager.shop.model.customer.address;

import java.io.Serializable;

import javax.validation.constraints.NotEmpty;

import io.swagger.annotations.ApiModelProperty;

public class PersistableCustomerAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Present on update (PUT), absent on create (POST). */
    private Long id;

    @ApiModelProperty(notes = "Address type: BILLING, SHIPPING, or BOTH", required = true)
    @NotEmpty
    private String addressType;

    @ApiModelProperty(notes = "True if this is the default address for its type")
    private boolean defaultAddress = false;

    @ApiModelProperty(notes = "Optional label, e.g. Home, Office, Warehouse")
    private String addressLabel;

    @NotEmpty
    private String firstName;

    @NotEmpty
    private String lastName;

    private String company;

    private String address;

    private String city;

    @ApiModelProperty(notes = "Free-text state/province, used when no zone code applies")
    private String stateProvince;

    @ApiModelProperty(notes = "2-letter zone/province code, e.g. ON, CA")
    private String zone;

    @ApiModelProperty(notes = "2-letter ISO country code, e.g. US, CA", required = true)
    @NotEmpty
    private String country;

    private String postalCode;

    private String phone;

    private String latitude;

    private String longitude;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }

    public String getAddressLabel() {
        return addressLabel;
    }

    public void setAddressLabel(String addressLabel) {
        this.addressLabel = addressLabel;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
}
