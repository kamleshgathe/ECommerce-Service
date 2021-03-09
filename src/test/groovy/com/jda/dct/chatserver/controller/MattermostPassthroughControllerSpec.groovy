/*
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.controller


import com.jda.dct.chatservice.controller.MattermostPassthroughController
import com.jda.dct.chatservice.service.MattermostPassthroughService
import com.jda.dct.ignitecaches.springimpl.Tenants
import com.jda.luminate.security.contexts.AuthContext
import org.assertj.core.util.Lists
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

class MattermostPassthroughControllerSpec extends Specification {
    def "test constructor exception if service is null"() {
        when: "Creating controller with null service"
        new MattermostPassthroughController(null,null)
        then: "Expect exception"
        thrown(IllegalArgumentException)
    }

    def "test constructor should be initialize if service is not null"() {
        when: "Creating constroller with not null service"
        def controller = new MattermostPassthroughController(Mock(MattermostPassthroughService),null)
        then: "Controller should be initialize"
        controller != null
    }

    def "test passthrouh call"() {
        given: "Initialize"
        def authContext = Mock(AuthContext)
        authContext.getCurrentTid() >> "tid1"
        def service = Mock(MattermostPassthroughService)
        service.passthrough(_ , _ as HttpMethod, _ ) >> responseEntity
        when: "Creating constroller with not null service"
        def controller = new MattermostPassthroughController(service,authContext)
        ResponseEntity actual = controller.passthrough(body, method, httpServletRequest);
        then: "Controller should be initialize"
        actual.statusCode == responseEntity.statusCode
        actual.body == responseEntity.body
        Tenants.getCurrent() == "tid1"
        where:
        body | method         | httpServletRequest       | responseEntity
        null | HttpMethod.GET | buildHttpServetRequest() | buildResponseEntity("abcd", HttpStatus.OK)
        "{\"status\": 123}" | HttpMethod.POST | buildHttpServetRequest() | buildResponseEntity("abcd", HttpStatus.OK)
    }

    def  buildHttpServetRequest() {
        def httpRequest = Mock(HttpServletRequest)
        httpRequest.getHeaderNames() >> Collections.enumeration(Lists.newArrayList(HttpHeaders.AUTHORIZATION))
        httpRequest.getHeaders(HttpHeaders.AUTHORIZATION) >> "Bearer :abcd"
        return httpRequest;
    }

    def  buildResponseEntity(Object body, HttpStatus status) {
        return ResponseEntity.status(status).body(body);
    }

}
