package com.salesmanager.core.business.repositories.customer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.salesmanager.core.model.customer.CustomerAddress;
import com.salesmanager.core.model.customer.CustomerAddressType;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    @Query("select a from CustomerAddress a "
         + "join fetch a.customer ac "
         + "left join fetch a.country "
         + "left join fetch a.zone "
         + "where ac.id = ?1 "
         + "order by a.defaultAddress desc, a.id asc")
    List<CustomerAddress> findByCustomerId(Long customerId);

    @Query("select a from CustomerAddress a "
         + "join fetch a.customer ac "
         + "left join fetch a.country "
         + "left join fetch a.zone "
         + "where ac.id = ?1 "
         + "and (a.addressType = ?2 or a.addressType = 'BOTH') "
         + "order by a.defaultAddress desc, a.id asc")
    List<CustomerAddress> findByCustomerIdAndType(Long customerId, CustomerAddressType type);

    @Query("select a from CustomerAddress a "
         + "join fetch a.customer ac "
         + "left join fetch a.country "
         + "left join fetch a.zone "
         + "where a.id = ?1 and ac.id = ?2")
    Optional<CustomerAddress> findByIdAndCustomerId(Long addressId, Long customerId);

    @Query("select a from CustomerAddress a "
         + "join fetch a.customer ac "
         + "left join fetch a.country "
         + "left join fetch a.zone "
         + "where ac.id = ?1 "
         + "and (a.addressType = ?2 or a.addressType = 'BOTH') "
         + "and a.defaultAddress = true")
    Optional<CustomerAddress> findDefaultByCustomerIdAndType(Long customerId, CustomerAddressType type);

    @Query("select a from CustomerAddress a "
         + "join fetch a.customer ac "
         + "join fetch ac.merchantStore ms "
         + "left join fetch a.country "
         + "left join fetch a.zone "
         + "where ac.id = ?1 and ms.code = ?2")
    List<CustomerAddress> findByCustomerIdAndStoreCode(Long customerId, String storeCode);
}
