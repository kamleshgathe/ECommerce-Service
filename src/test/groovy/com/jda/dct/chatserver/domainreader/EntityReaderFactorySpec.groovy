/*
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.domainreader

import com.jda.dct.chatservice.domainreader.EntityReaderFactory
import com.jda.dct.contexts.AuthContext
import com.jda.dct.domain.stateful.Shipment
import com.jda.dct.persist.ignite.dao.DctDaoBase
import spock.lang.Specification

class EntityReaderFactorySpec extends Specification {

    def "test constructor should throw exception if any is parameter is null"() {
        when: "Initialize constructor"
        new EntityReaderFactory(null, Mock(AuthContext));
        then: "Should expect exception"
        thrown(IllegalArgumentException)
    }

    def "test constructor should throw exception if all parameter is null"() {
        when: "Initialize constructor"
        new EntityReaderFactory(null, null);
        then: "Should expect exception"
        thrown(IllegalArgumentException)
    }

    def "test throw exception if type is not valid"() {
        given: "Initialize reader factory"
        def entityReaderFactory = new EntityReaderFactory(Mock(DctDaoBase), Mock(AuthContext))

        when: "Called with invalid type"
        entityReaderFactory.getEntity("abcd", "id1")
        then: "Should expect exception"
        thrown(IllegalArgumentException)
    }

    def "test get domain entity"() {
        given: "Initialize reader factory"
        def authContext = Mock(AuthContext)
        def dctDaoBase = Mock(DctDaoBase)
        def shipment = Mock(Shipment)
        shipment.getShipmentId() >> "shipment1"
        dctDaoBase.getById(_, _) >> shipment;

        def entityReaderFactory = new EntityReaderFactory(dctDaoBase, authContext)

        when: "Called with shipment as type"
        Object obj = entityReaderFactory.getEntity("shipment", "id1")
        then: "Should expect exception"
        obj.getShipmentId() == "shipment1"
    }

}
