/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */

package com.jda.dct.chatservice.domainreader;

import com.jda.dct.contexts.AuthContext;
import com.jda.dct.domain.stateful.Shipment;
import com.jda.dct.persist.ignite.dao.DctDaoBase;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.Assert;

/**
 * Domain entity reader factory. This factory provided methods for reading domain entity object
 * for chat service.
 */
public class EntityReaderFactory {

    private static final String TYPE_SHIPMENT = "shipment";

    private DctDaoBase<Shipment> shipmentRepo;
    private AuthContext authContext;

    /**
     * Constructor for domain entity reader.
     *
     * @param shipmentRepo ShipmentIngestionApiService instance.
     * @param authContext  Authentication context;
     */
    public EntityReaderFactory(final DctDaoBase<Shipment> shipmentRepo, final AuthContext authContext) {
        Assert.notNull(shipmentRepo, "Shipment ingestion repository can't be null");
        Assert.notNull(authContext, "AuthContext can't be null");
        this.shipmentRepo = shipmentRepo;
        this.authContext = authContext;
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
        if (TYPE_SHIPMENT.equals(type)) {
            return shipmentRepo.getById(authContext.getCurrentTid(), id);
        }
        throw new IllegalArgumentException(String.format("Invalid entity type {}", type));
    }
}
