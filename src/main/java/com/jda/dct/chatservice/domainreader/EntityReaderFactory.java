/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */

package com.jda.dct.chatservice.domainreader;

import com.jda.dct.contexts.AuthContext;
import com.jda.dct.domain.Node;
import com.jda.dct.domain.stateful.Delivery;
import com.jda.dct.domain.stateful.PurchaseOrder;
import com.jda.dct.domain.stateful.SalesOrder;
import com.jda.dct.domain.stateful.Shipment;
import com.jda.dct.persist.ignite.dao.DctDaoBase;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.Assert;

/**
 * Domain entity reader factory. This factory provided methods for reading domain entity object
 * for chat service.
 */
public class EntityReaderFactory {

    private static final String TYPE_SHIPMENT = "shipment";
    private static final String TYPE_PURCHASE_ORDER = "purchase_order";
    private static final String TYPE_SALES_ORDER = "sales_order";
    private static final String TYPE_DELIVERY = "delivery";
    private static final String TYPE_INVENTORY = "inventory";

    private AuthContext authContext;

    private final Map<String, DctDaoBase> repoMap;

    /**
     * Constructor for domain entity reader.
     */
    private EntityReaderFactory() {
        repoMap = new HashMap<>();
    }

    /**
     * Method returns domain entity object for given business entity and entity id. If type is
     * not supported type by chat server it will return IllegalArgumentException.
     *
     * @param type domain entity type.
     * @param id   entity id.
     * @return Domain entity object.
     **/
    public Object getEntity(String type, String id) {
        Assert.isTrue(Strings.isNotEmpty(type), "Entity type can't be null or empty");
        Assert.isTrue(Strings.isNotEmpty(id), "Entity id can't be null or empty");
        if (!repoMap.containsKey(type)) {
            throw new IllegalArgumentException(String.format("Invalid entity type %s", type));
        }

        return repoMap.get(type).getById(authContext.getCurrentTid(), id);
    }

    /**
     * EntityReaderFactoryBuilder is builder for EntityReaderFactory which hides the complexity of
     * EntityReaderFactory  creation.
     */
    public static class EntityReaderFactoryBuilder {
        private DctDaoBase<Shipment> shipmentRepo;
        private DctDaoBase<SalesOrder> salesOrderRepo;
        private DctDaoBase<PurchaseOrder> purchaseOrderRepo;
        private DctDaoBase<Delivery> deliveryRepo;
        private DctDaoBase<Node> inventoryRepo;

        private AuthContext authContext;

        public EntityReaderFactoryBuilder shipmentRepo(final DctDaoBase<Shipment> shipmentRepo) {
            this.shipmentRepo = shipmentRepo;
            return this;
        }

        public EntityReaderFactoryBuilder salesOrderRepo(final DctDaoBase<SalesOrder> salesOrderRepo) {
            this.salesOrderRepo = salesOrderRepo;
            return this;
        }

        public EntityReaderFactoryBuilder purchaseOrderRepo(final DctDaoBase<PurchaseOrder> purchaseOrderRepo) {
            this.purchaseOrderRepo = purchaseOrderRepo;
            return this;
        }

        public EntityReaderFactoryBuilder deliveryRepo(final DctDaoBase<Delivery> deliveryRepo) {
            this.deliveryRepo = deliveryRepo;
            return this;
        }

        public EntityReaderFactoryBuilder inventoryRepo(final DctDaoBase<Node> inventoryRepo) {
            this.inventoryRepo = inventoryRepo;
            return this;
        }

        public EntityReaderFactoryBuilder authContext(final AuthContext authContext) {
            this.authContext = authContext;
            return this;
        }

        /**
         * Builder for EntityReaderFactory.
         * @return return EntityReaderFactory.
         */
        public EntityReaderFactory build() {
            Assert.notNull(shipmentRepo, "Shipment repository can't be null");
            Assert.notNull(salesOrderRepo, "SalesOrder repository can't be null");
            Assert.notNull(purchaseOrderRepo, "PurchaseOrder repository can't be null");
            Assert.notNull(deliveryRepo, "Delivery repository can't be null");
            Assert.notNull(inventoryRepo, "Inventory repository can't be null");
            Assert.notNull(authContext, "AuthContext can't be null");

            EntityReaderFactory readerFactory = new EntityReaderFactory();
            readerFactory.repoMap.put(TYPE_SHIPMENT, shipmentRepo);
            readerFactory.repoMap.put(TYPE_PURCHASE_ORDER, purchaseOrderRepo);
            readerFactory.repoMap.put(TYPE_SALES_ORDER, salesOrderRepo);
            readerFactory.repoMap.put(TYPE_DELIVERY, deliveryRepo);
            readerFactory.repoMap.put(TYPE_INVENTORY, inventoryRepo);
            readerFactory.authContext = authContext;
            return readerFactory;
        }
    }
}
