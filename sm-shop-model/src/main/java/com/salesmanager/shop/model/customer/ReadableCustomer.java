package com.salesmanager.shop.model.customer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.salesmanager.shop.model.customer.address.ReadableCustomerAddress;
import com.salesmanager.shop.model.customer.attribute.ReadableCustomerAttribute;
import com.salesmanager.shop.model.security.ReadableGroup;


public class ReadableCustomer extends CustomerEntity implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private List<ReadableCustomerAttribute> attributes = new ArrayList<ReadableCustomerAttribute>();
	private List<ReadableGroup> groups = new ArrayList<ReadableGroup>();
	private List<ReadableCustomerAddress> addresses = new ArrayList<ReadableCustomerAddress>();
	
	public void setAttributes(List<ReadableCustomerAttribute> attributes) {
		this.attributes = attributes;
	}
	public List<ReadableCustomerAttribute> getAttributes() {
		return attributes;
	}
	public List<ReadableGroup> getGroups() {
		return groups;
	}
	public void setGroups(List<ReadableGroup> groups) {
		this.groups = groups;
	}

	public List<ReadableCustomerAddress> getAddresses() {
		return addresses;
	}
	public void setAddresses(List<ReadableCustomerAddress> addresses) {
		this.addresses = addresses;
	}

}
