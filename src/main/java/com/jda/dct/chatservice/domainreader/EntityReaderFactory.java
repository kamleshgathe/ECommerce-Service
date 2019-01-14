/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.domainreader;

import com.jda.dct.domain.stateful.Shipment;
import com.jda.luminate.ingest.rest.services.IngestionApiService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Domain entity reader factory. This factory provided methods for reading domain entity object
 * for chat service.
 */
@Component
public class EntityReaderFactory {

    private static final String TYPE_SHIPMENT = "shipment";

    private IngestionApiService<Shipment> shipmentIngestionApiService;

    /**
     * Constructor for domain entity reader.
     *
     * @param shipmentIngestionApiService ShipmentIngestionApiService instance.
     */
    public EntityReaderFactory(IngestionApiService<Shipment> shipmentIngestionApiService) {
        this.shipmentIngestionApiService = shipmentIngestionApiService;
    }

    /**
     * This method returns domain entity object for given business entity and entity id. If type is
     * not supported type by chat server it will return IllegalArgumentException.
     *
     * @param type domain entity type
     * @param id   entity id
     * @return Domain entity object
     */
    public Object getEntity(String type, String id) {
        Assert.isTrue(Strings.isNotEmpty(type), "Entity type can't be null or empty");
        Assert.isTrue(Strings.isNotEmpty(id), "Entity id can't be null or empty");
        if (TYPE_SHIPMENT.equals(type)) {
            return shipmentIngestionApiService.getObject(id);
        }
        throw new IllegalArgumentException(String.format("Invalid entity type {}", type));
    }
}
