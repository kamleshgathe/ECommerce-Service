/*
 * Copyright © 2022, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.config

import com.esotericsoftware.kryo.util.IntMap
import com.jda.dct.chatservice.config.DomainEntityRepoConfig
import com.jda.dct.chatservice.domainreader.EntityReaderFactory
import com.jda.dct.chatservice.repository.Analytics
import com.jda.dct.domain.stateful.Delivery
import com.jda.dct.domain.stateful.PurchaseOrder
import com.jda.dct.domain.stateful.SalesOrder
import com.jda.dct.domain.stateful.Shipment
import com.jda.dct.domain.Node
import com.jda.dct.persist.ignite.dao.DctDaoBase

import com.jda.luminate.security.contexts.AuthContext
import org.mockito.internal.stubbing.answers.ThrowsException
import spock.lang.Specification



class DomainEntityRepoConfigSpec extends Specification {
    AuthContext authContext
     DctDaoBase<Shipment> shipmentRepo
    DctDaoBase<SalesOrder> salesOrderRepo;
     DctDaoBase<PurchaseOrder> purchaseOrderRepo;
     DctDaoBase<Delivery> deliveryRepo;
     DctDaoBase<Node> nodeRepo;
     Analytics analytics;


    def "test DomainEntityRepoConfig deliveryClass"() {
        given:
        DomainEntityRepoConfig domainEntityRepoConfig = new DomainEntityRepoConfig()
        when:
        Class deliveryClass = domainEntityRepoConfig.deliveryClass()
        then:
        deliveryClass == Delivery
    }

    def "test DomainEntityRepoConfig salesOrderClass"() {
        given:
        DomainEntityRepoConfig domainEntityRepoConfig = new DomainEntityRepoConfig()
        when:
        Class salesOrderClass = domainEntityRepoConfig.salesOrderClass()
        then:
        salesOrderClass == SalesOrder
    }

    def "test DomainEntityRepoConfig shipmentClass"() {
        given:
        DomainEntityRepoConfig domainEntityRepoConfig = new DomainEntityRepoConfig()
        when:
        Class shipmentClass = domainEntityRepoConfig.shipmentClass()
        then:
        shipmentClass == Shipment
    }

    def "test DomainEntityRepoConfig purchaseOrderClass"() {
        given:
        DomainEntityRepoConfig domainEntityRepoConfig = new DomainEntityRepoConfig()
        when:
        Class purchaseOrderClass = domainEntityRepoConfig.purchaseOrderClass()
        then:
        purchaseOrderClass == PurchaseOrder
    }

    def "test DomainEntityRepoConfig nodeClass"() {
        given:
        DomainEntityRepoConfig domainEntityRepoConfig = new DomainEntityRepoConfig()
        when:
        Class nodeClass = domainEntityRepoConfig.nodeClass()
        then:
        nodeClass == Node
    }

    def "test DomainEntityRepoConfig analyticsClass"() {
        given:
        DomainEntityRepoConfig domainEntityRepoConfig = new DomainEntityRepoConfig()
        when:
        Class analyticsClass = domainEntityRepoConfig.analyticsClass()
        then:
        analyticsClass == Analytics
    }

    def "test entityReaderFactory "(){
        given: "Initialize reader factory"
        authContext = Mock(AuthContext)
        shipmentRepo = Mock(DctDaoBase)
        salesOrderRepo = Mock(DctDaoBase)
        purchaseOrderRepo = Mock(DctDaoBase)
        deliveryRepo = Mock(DctDaoBase)
        nodeRepo = Mock(DctDaoBase)
        analytics = Mock(Analytics)
        DomainEntityRepoConfig domainEntityRepoConfig = new DomainEntityRepoConfig()
        EntityReaderFactory entityReaderFactory1 = new EntityReaderFactory()
        def builder = new EntityReaderFactory.EntityReaderFactoryBuilder()
        builder.authContext(authContext)
                .salesOrderRepo(salesOrderRepo)
                .purchaseOrderRepo(purchaseOrderRepo)
                .shipmentRepo(shipmentRepo)
                .deliveryRepo(deliveryRepo)
                .nodeRepo(nodeRepo)
                .analyticsRepo(analytics)
                .build()
        when:
        EntityReaderFactory entityReaderFactory = domainEntityRepoConfig.entityReaderFactory()

        then:
        thrown(IllegalArgumentException)
    }



}
