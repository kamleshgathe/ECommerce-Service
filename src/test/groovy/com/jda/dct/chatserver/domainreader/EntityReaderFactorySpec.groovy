/*
 * Copyright © 2022, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.domainreader

import com.jda.dct.chatservice.domainreader.EntityReaderFactory
import com.jda.dct.domain.stateful.Shipment
import com.jda.dct.persist.ignite.dao.DctDaoBase
import com.jda.luminate.security.contexts.AuthContext
import spock.lang.Specification

class EntityReaderFactorySpec extends Specification {

    def "test constructor should throw exception if shipment repo is null"() {
        when: "Initialize constructor"
        buildEntityReaderFactory(null, null, null, null, null, Mock(AuthContext));
        then: "Should expect exception"
        thrown(IllegalArgumentException)
    }

    def "test constructor should throw exception sales order repo is null"() {
        when: "Initialize constructor"
        buildEntityReaderFactory(Mock(DctDaoBase), null, null, null, null, Mock(AuthContext));
        then: "Should expect exception"
        thrown(IllegalArgumentException)
    }

    def "test constructor should throw exception purchase order repo is null"() {
        when: "Initialize constructor"
        buildEntityReaderFactory(Mock(DctDaoBase), Mock(DctDaoBase), null, null, null, Mock(AuthContext));
        then: "Should expect exception"
        thrown(IllegalArgumentException)
    }

    def "test constructor should throw exception if node repo is null"() {
        when: "Initialize constructor"
        buildEntityReaderFactory(Mock(DctDaoBase), Mock(DctDaoBase), Mock(DctDaoBase), Mock(DctDaoBase), null, Mock(AuthContext));
        then: "Should expect exception"
        thrown(IllegalArgumentException)
    }

    def "test constructor should throw exception auth context is null"() {
        when: "Initialize constructor"
        buildEntityReaderFactory(Mock(DctDaoBase),
                Mock(DctDaoBase),
                Mock(DctDaoBase),
                Mock(DctDaoBase),
                Mock(DctDaoBase),
                null);
        then: "Should expect exception"
        thrown(IllegalArgumentException)
    }

    def "test throw exception if type is not valid"() {
        given: "Initialize reader factory"
        def entityReaderFactory = buildEntityReaderFactory(Mock(DctDaoBase),
                Mock(DctDaoBase),
                Mock(DctDaoBase),
                Mock(DctDaoBase),
                Mock(DctDaoBase),
                Mock(AuthContext))

        when: "Called with invalid type"
        entityReaderFactory.getEntity("abcd", "id1")
        then: "Should expect exception"
        thrown(IllegalArgumentException)
    }

    def "test get domain entity"() {
        given: "Initialize reader factory"
        def authContext = Mock(AuthContext)
        def dctShipmentDaoBase = Mock(DctDaoBase)
        def dctSalesOrderDaoBase = Mock(DctDaoBase)
        def dctPurchaseOrderDaoBase = Mock(DctDaoBase)
        def dctDeliveryDaoBase = Mock(DctDaoBase)
        def dctNodeDaoBase = Mock(DctDaoBase)
        def shipment = Mock(Shipment)
        shipment.getShipmentId() >> "shipment1"
        dctShipmentDaoBase.getById(_, _) >> shipment;

        def entityReaderFactory = buildEntityReaderFactory(dctShipmentDaoBase,
                dctSalesOrderDaoBase,
                dctPurchaseOrderDaoBase,
                dctDeliveryDaoBase,
                dctNodeDaoBase,
                authContext)

        when: "Called with shipment as type"
        Object obj = entityReaderFactory.getEntity("shipment", "id1")
        then: "Should expect exception"
        obj.getShipmentId() == "shipment1"
    }

    EntityReaderFactory buildEntityReaderFactory(shipment,
                                                 salesOrder,
                                                 purchaseOrder,
                                                 delivery,
                                                 node,
                                                 authContext) {
        def builder = new EntityReaderFactory.EntityReaderFactoryBuilder();
        builder.shipmentRepo(shipment)
                .purchaseOrderRepo(purchaseOrder)
                .salesOrderRepo(salesOrder)
                .deliveryRepo(delivery)
                .nodeRepo(node)
                .authContext(authContext)
                .build();
    }
}
