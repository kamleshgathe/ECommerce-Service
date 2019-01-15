/*
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.service

import com.google.common.collect.Maps
import com.jda.dct.chatservice.domainreader.EntityReaderFactory
import com.jda.dct.chatservice.dto.upstream.TokenDto
import com.jda.dct.chatservice.repository.ProxyTokenMappingRepository
import com.jda.dct.chatservice.repository.SituationRoomRepository
import com.jda.dct.chatservice.service.SituationRoomServiceImpl
import com.jda.dct.contexts.AuthContext
import com.jda.dct.domain.ProxyTokenMapping
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class SituationRoomServiceSpec extends Specification {

    def CHANNEL_TEAM_ID = "team1"
    def AuthContext authContext
    def SituationRoomRepository roomRepository;
    def ProxyTokenMappingRepository tokenRepository
    def RestTemplate restTemplate;
    def EntityReaderFactory entityReaderFactory

    def SituationRoomServiceImpl service;

    def "test get token expect exception if current user is null"() {
        given:
        mock()
        authContext.getCurrentUser() >> null
        when:
        initNewSituationRoomService();
        service.getSessionToken();
        then:
        thrown IllegalArgumentException
    }

    def "test get token"() {
        given:
        def user = "user1"
        def tokenMapping = Mock(ProxyTokenMapping)
        mock()
        tokenMapping.getProxyToken() >> "token"
        authContext.getCurrentUser() >> user
        tokenRepository.findByAppUserId(user) >> tokenMapping;

        when:
        initNewSituationRoomService();
        TokenDto tokenDto = service.getSessionToken();
        then:
        0 * tokenRepository.save(_ as ProxyTokenMapping)
        tokenDto.teamId == CHANNEL_TEAM_ID
        tokenDto.token == "token"

    }

    def "test get token if user not already present"() {
        given:
        def user = "user1"

        mock()
        _ * authContext.getCurrentUser() >> user
        1 * tokenRepository.findByAppUserId(user) >> null;
        tokenRepository.save(_ as ProxyTokenMapping) >> proxyTokenMappingWithToken("token")
        tokenRepository.findByAppUserId(user) >> proxyTokenMappingWithoutToken();
        restTemplate.exchange(_ as String, _ as HttpMethod, _ as HttpEntity, Map.class, *_) >>
                {
                    args ->
                        Map body = Maps.newHashMap();
                        if (args[0].contains("/users")) {
                            body.put("id", "123")
                        } else if (args[0].contains("/roles")) {
                            body.put("status", "ok")
                        } else {
                            body.put("token", "token")
                        }
                        return mockedResponseEntity(HttpStatus.OK, body)
                }
        when:
        initNewSituationRoomService();
        TokenDto tokenDto = service.getSessionToken();
        then:

        tokenDto.teamId == CHANNEL_TEAM_ID
    }

    def initNewSituationRoomService() {
        service = new SituationRoomServiceImpl(authContext, roomRepository, tokenRepository,entityReaderFactory)
        service.setRestTemplate(restTemplate)
        service.setChannelTeamId(CHANNEL_TEAM_ID)
        service.setMattermostUrl("http://localhost:80/api/v4")
    }

    def mock() {
        authContext = Mock(AuthContext)
        roomRepository = Mock(SituationRoomRepository)
        tokenRepository = Mock(ProxyTokenMappingRepository)
        restTemplate = Mock(RestTemplate)
        entityReaderFactory = Mock(EntityReaderFactory)
    }

    def mockedResponseEntity(status, body) {
        def responseEntity = Mock(ResponseEntity)
        responseEntity.getBody() >> body
        responseEntity.getStatusCode() >> status
        return responseEntity;
    }

    def proxyTokenMappingWithoutToken() {
        def mapping = new ProxyTokenMapping()
        mapping.setAppUserId("111")
        mapping.setRemoteUserId("123")
        return mapping;
    }

    def proxyTokenMappingWithToken(def token) {
        def mapping = proxyTokenMappingWithoutToken();
        mapping.setProxyToken(token);
        return mapping;
    }
}
