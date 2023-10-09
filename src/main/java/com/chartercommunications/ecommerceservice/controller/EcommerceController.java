
package com.chartercommunications.ecommerceservice.controller;

import com.chartercommunications.ecommerceservice.dto.Customer;
import com.chartercommunications.ecommerceservice.dto.Order;
import com.chartercommunications.ecommerceservice.service.CustomerService;
import com.chartercommunications.ecommerceservice.service.OrderService;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/Ecommerce")
public class EcommerceController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    @GetMapping(value = { "", "/rewardPoints" })
    public @NotNull Double getRewardPoints(Customer customer) {
        return customerService.getRewardPoints(customer);
    }

    @PostMapping (value = { "", "/rewardPoints" })
    public @NotNull void updateRewardPoints(Customer customer, Order order) {
         customerService.updateRewardPoints(customer,order);
    }


    // orderService constructor injection

    @GetMapping(value = { "", "/orders" })
    public @NotNull Iterable<Order> getOrders() {
        return orderService.getAllOrders();
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {

        // create order logic
        this.orderService.create(order);

        MultiValueMap<String, String> headers = null;
        return new ResponseEntity<>(order, headers, HttpStatus.CREATED);
    }

}
