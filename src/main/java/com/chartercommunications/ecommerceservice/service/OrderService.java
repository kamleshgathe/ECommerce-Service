
package com.chartercommunications.ecommerceservice.service;

import com.chartercommunications.ecommerceservice.dto.Order;

public interface OrderService {
    Iterable<Order> getAllOrders();
    Order create(Order order);
    void update(Order order);
}
