/**
 * Copyright © 2023, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.chartercommunications.ecommerceservice.service.impl;

import com.chartercommunications.ecommerceservice.dto.Customer;
import com.chartercommunications.ecommerceservice.dto.Order;
import com.chartercommunications.ecommerceservice.service.CustomerService;

public class CustomerServiceImpl implements CustomerService {
    @Override
    public Double getRewardPoints(Customer customer) {
        return customer.getRewardPoints();
    }

    @Override
    public void updateRewardPoints(Customer customer, Order order) {
        Double totalAmount = order.getOrderTotal();
        Double rewardPoints = 0.0;
        if(totalAmount > 50.00){
            rewardPoints = (totalAmount - 50) * 1 + (totalAmount - 100) * 1 ;
            Double totalRewardPoints = customer.getRewardPoints()+rewardPoints;
            customer.setRewardPoints(totalRewardPoints);
        }

    }
}
