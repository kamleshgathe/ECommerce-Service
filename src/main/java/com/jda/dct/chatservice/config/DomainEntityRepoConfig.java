/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.config;

import com.jda.dct.chatservice.domainreader.EntityReaderFactory;
import com.jda.dct.contexts.AuthContext;
import com.jda.dct.domain.stateful.Shipment;
import com.jda.dct.persist.ignite.dao.ShipmentDaoImpl;
import com.jda.luminate.ingest.rest.services.IngestionApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class DomainEntityRepoConfig {

    /***************************************
     * DAO Definitions.                    *
     ***************************************/
    @Autowired
    private ShipmentDaoImpl shipmentDao;

    @Autowired
    private AuthContext authContext;

    @Bean
    public Class shipmentClass() {
        return Shipment.class;
    }

    @Bean
    public EntityReaderFactory entityReaderFactory() {
        return new EntityReaderFactory(shipmentDao,authContext);
    }

}
