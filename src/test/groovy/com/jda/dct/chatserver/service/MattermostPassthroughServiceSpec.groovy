/*
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.service

import com.jda.luminate.security.contexts.AuthContext
import org.springframework.http.HttpEntity
import org.springframework.web.client.HttpStatusCodeException
import spock.lang.Shared

import static com.jda.dct.chatservice.constants.ChatRoomConstants.PASSTHROUGH_PREFIX

import org.assertj.core.util.Lists
import org.springframework.http.HttpHeaders
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest

import com.jda.dct.chatservice.repository.ProxyTokenMappingRepository
import com.jda.dct.chatservice.service.MattermostPassthroughService
import com.jda.dct.domain.ProxyTokenMapping
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class MattermostPassthroughServiceSpec extends Specification {
    @Shared
    def MATTERMOST_END_POINT = "http://localhost:8000/api/v4"

    AuthContext authContext
    ProxyTokenMappingRepository tokenRepository
    RestTemplate restTemplate;


    def "test passthrough should fail if token does not exists"() {
        given: "Initialize"
        mock()
        tokenRepository.findByAppUserId("1") >> null
        authContext.getCurrentUser() >> "1"

        def service = new MattermostPassthroughService(authContext, tokenRepository)

        when: "Calling passthrough"
        ResponseEntity responseEntity = service.passthrough(null, HttpMethod.GET, buildHttpServetRequest())

        then: "Should get exception"
        responseEntity.statusCode == HttpStatus.BAD_REQUEST
    }

    def "test passthrough remote exception"() {
        given: "Initialize"
        mock()

        def token = Mock(ProxyTokenMapping)
        token.getProxyToken() >> "123"
        tokenRepository.findByAppUserId("1") >> token
        authContext.getCurrentUser() >> "1"
        def service = new MattermostPassthroughService(authContext, tokenRepository)
        service.setRestTemplate(restTemplate)
        service.setMattermostUrl(MATTERMOST_END_POINT)
        def httpRequest = buildHttpServetRequest();
        httpRequest.getRequestURI() >> "/user/me"

        def errorResponse = Mock(HttpStatusCodeException)
        errorResponse.getRawStatusCode() >> 500
        errorResponse.getResponseBodyAsString() >> "error"
        restTemplate.exchange(_,_, _ as HttpEntity, Object.class) >>  {
            throw errorResponse
        };

        when: "Calling passthrough"

        ResponseEntity actual = service.passthrough("abcd", HttpMethod.GET, httpRequest);

        then: "Controller should be initialize"
        actual.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        actual.body == "error"
    }


    @Unroll
    def "test passthrough should succeed"() {
        given: "Initialize"
        mock()

        def token = Mock(ProxyTokenMapping)
        token.getProxyToken() >> "123"
        tokenRepository.findByAppUserId("1") >> token
        authContext.getCurrentUser() >> "1"
        def service = new MattermostPassthroughService(authContext, tokenRepository)
        service.setRestTemplate(restTemplate)
        service.setMattermostUrl(MATTERMOST_END_POINT)
        def httpRequest = buildHttpServetRequest();
        httpRequest.getRequestURI() >> requestUri

        restTemplate.exchange(targetUrl, method, _ as HttpEntity, Object.class) >> responseEntity

        when: "Calling passthrough"

        ResponseEntity actual = service.passthrough(requestBody, method, httpRequest);

        then: "Controller should be initialize"
        actual.statusCode == responseEntity.statusCode
        actual.body == responseEntity.body

        where:
        mattermostCtxPath            | targetUrl                                | requestUri                             | requestBody          | method          | httpServletRequest       | responseEntity
        "/user/me"                   | MATTERMOST_END_POINT + mattermostCtxPath | PASSTHROUGH_PREFIX + mattermostCtxPath | null                 | HttpMethod.GET  | buildHttpServetRequest() | buildResponseEntity("abcd", HttpStatus.OK)
        "/team/users/"               | MATTERMOST_END_POINT + mattermostCtxPath | PASSTHROUGH_PREFIX + mattermostCtxPath | "{\"status\": 123}"  | HttpMethod.POST | buildHttpServetRequest() | buildResponseEntity("abcd", HttpStatus.OK)
        "/channels/xxysksknn/unread" | MATTERMOST_END_POINT + mattermostCtxPath | PASSTHROUGH_PREFIX + mattermostCtxPath | "{\"status\": 1111}" | HttpMethod.POST | buildHttpServetRequest() | buildResponseEntity("abcd", HttpStatus.OK)
    }

    def mock() {
        authContext = Mock(AuthContext)
        tokenRepository = Mock(ProxyTokenMappingRepository)
        restTemplate = Mock(RestTemplate)
    }

    def buildHttpServetRequest() {
        def httpRequest = Mock(HttpServletRequest)
        httpRequest.getHeaderNames() >> Collections.enumeration(Lists.newArrayList(HttpHeaders.AUTHORIZATION))
        httpRequest.getHeaders(HttpHeaders.AUTHORIZATION) >> "Bearer :abcd"
        return httpRequest;
    }

    def buildResponseEntity(Object body, HttpStatus status) {
        return ResponseEntity.status(status).body(body);
    }
}
