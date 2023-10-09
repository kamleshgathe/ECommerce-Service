
package com.chartercommunications.ecommerceservice.service;

import com.chartercommunications.ecommerceservice.dto.Customer;
import com.chartercommunications.ecommerceservice.dto.Order;

public interface CustomerService {
     Double getRewardPoints(Customer customer);
     void updateRewardPoints(Customer customer, Order order);
}
