/*
 * Copyright © 2022, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.domainreader

import com.jda.dct.chatservice.domainreader.EntityReaderFactory
import com.jda.dct.domain.Site
import com.jda.dct.domain.stateful.Shipment
import com.jda.dct.chatservice.repository.Analytics
import com.jda.dct.persist.ignite.dao.DctDaoBase
import com.jda.dct.domain.Node
import com.jda.luminate.security.contexts.AuthContext
import spock.lang.Specification

class EntityReaderFactorySpec extends Specification {

    def "test constructor should throw exception if shipment repo is null"() {
        when: "Initialize constructor"
        buildEntityReaderFactory(null, null, null, null, null, null, Mock(AuthContext));
        then: "Should expect exception"
        thrown(IllegalArgumentException)
    }

    def "test constructor should throw exception sales order repo is null"() {
        when: "Initialize constructor"
        buildEntityReaderFactory(Mock(DctDaoBase), null, null, null, null,null, Mock(AuthContext));
        then: "Should expect exception"
        thrown(IllegalArgumentException)
    }

    def "test constructor should throw exception purchase order repo is null"() {
        when: "Initialize constructor"
        buildEntityReaderFactory(Mock(DctDaoBase), Mock(DctDaoBase), null, null, null,null, Mock(AuthContext));
        then: "Should expect exception"
        thrown(IllegalArgumentException)
    }

    def "test constructor should throw exception if node repo is null"() {
        when: "Initialize constructor"
        buildEntityReaderFactory(Mock(DctDaoBase), Mock(DctDaoBase), Mock(DctDaoBase), Mock(DctDaoBase), null,null, Mock(AuthContext));
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
                Mock(Analytics),
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
                Mock(Analytics),
                Mock(AuthContext))

        when: "Called with invalid type"
        entityReaderFactory.getEntity("abcd", "id1")
        then: "Should expect exception"
        thrown(IllegalArgumentException)
    }

    def "test when type is analytics"() {
        given: "Initialize reader factory"
        def entityReaderFactory = buildEntityReaderFactory(Mock(DctDaoBase),
                Mock(DctDaoBase),
                Mock(DctDaoBase),
                Mock(DctDaoBase),
                Mock(DctDaoBase),
                Mock(Analytics),
                Mock(AuthContext))

        when: "Called with invalid type"
        def obj = entityReaderFactory.getEntity("analytics", "id1")
        then: "Should expect exception"
        obj != null
    }

    def "test get domain entity"() {
        given: "Initialize reader factory"
        def authContext = Mock(AuthContext)
        def dctShipmentDaoBase = Mock(DctDaoBase)
        def dctSalesOrderDaoBase = Mock(DctDaoBase)
        def dctPurchaseOrderDaoBase = Mock(DctDaoBase)
        def dctDeliveryDaoBase = Mock(DctDaoBase)
        def dctNodeDaoBase = Mock(DctDaoBase)
        def analytics = Mock(Analytics)
        def shipment = Mock(Shipment)
        shipment.getShipmentId() >> "shipment1"
        dctShipmentDaoBase.getById(_, _, _) >> shipment

        def entityReaderFactory = buildEntityReaderFactory(dctShipmentDaoBase,
                dctSalesOrderDaoBase,
                dctPurchaseOrderDaoBase,
                dctDeliveryDaoBase,
                dctNodeDaoBase,
                analytics,
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
                                                 node, analytics,
                                                 authContext) {
        def builder = new EntityReaderFactory.EntityReaderFactoryBuilder();
        builder.shipmentRepo(shipment)
                .purchaseOrderRepo(purchaseOrder)
                .salesOrderRepo(salesOrder)
                .deliveryRepo(delivery)
                .nodeRepo(node)
                .analyticsRepo(analytics)
                .authContext(authContext)
                .build();
    }

    def "test generic node entity"() {
        given: "Initialize reader factory"
        def authContext = Mock(AuthContext)
        def dctShipmentDaoBase = Mock(DctDaoBase)
        def dctSalesOrderDaoBase = Mock(DctDaoBase)
        def dctPurchaseOrderDaoBase = Mock(DctDaoBase)
        def dctDeliveryDaoBase = Mock(DctDaoBase)
        def dctNodeDaoBase = Mock(DctDaoBase)
        def analytics = Mock(Analytics)
        dctNodeDaoBase.getById(_, _, _) >> Mock(Node)

        def entityReaderFactory = buildEntityReaderFactory(dctShipmentDaoBase,
                dctSalesOrderDaoBase,
                dctPurchaseOrderDaoBase,
                dctDeliveryDaoBase,
                dctNodeDaoBase,
                analytics,
                authContext)

        when: "Called with node as type"
        Object obj = entityReaderFactory.getEntity("node", "id1")
        then:
        obj instanceof Node
    }

    def siteNameAndDescriptionInEntity() {
        given: "Initialize reader factory and set the required data for site name and description"
        def authContext = Mock(AuthContext)
        def dctShipmentDaoBase = Mock(DctDaoBase)
        def dctSalesOrderDaoBase = Mock(DctDaoBase)
        def dctPurchaseOrderDaoBase = Mock(DctDaoBase)
        def dctDeliveryDaoBase = Mock(DctDaoBase)
        def dctNodeDaoBase = Mock(DctDaoBase)
        def analytics = Mock(Analytics)
        def node = nodeData()
        dctShipmentDaoBase.getById(_, _, _) >> node

        def entityReaderFactory = buildEntityReaderFactory(dctShipmentDaoBase,
                dctSalesOrderDaoBase,
                dctPurchaseOrderDaoBase,
                dctDeliveryDaoBase,
                dctNodeDaoBase,
                analytics,
                authContext)

        when: "Called with shipment as type"
        Object obj = entityReaderFactory.getEntity("shipment", "id1")

        then: "Should have siteName and SiteDescription"
        assert  obj != null
        "Acme Warehouse Germany" == obj.refObjects.get("shipToSites")[(0)].siteDescription
        "AC-WH01" == obj.refObjects.get("shipToSites")[(0)].siteName
    }

    private Node nodeData() {
        Node node = new Node()
        node.setId("a4b7a825f67930965747445709011120-Node-176d7d88b1953abe7eeeff3b38bf8398")
        node.setNodeType("capacity")
        node.setRefObjects(refObjects())

        return node
    }

    private refObjects() {
        Map<String, String> map = [:]
        Set<Site> set = [] as Set
        set.add(siteDescription())
        map.put("shipToSites", set)

        return map
    }

    private siteDescription() {
        Site site = new Site()
        site.setId("a4b7a825f67930965747445709011120-Site-07eea86e6147ec8e4d133ea3e93488b3")
        site.setSiteName("AC-WH01")
        site.setSiteDescription("Acme Warehouse Germany")

        return site
    }

}
