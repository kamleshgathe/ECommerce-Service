/*
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.controller

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.jda.dct.chatservice.controller.ChatRoomController
import com.jda.dct.chatservice.dto.upstream.AddUserToRoomDto
import com.jda.dct.chatservice.dto.upstream.ChatContext
import com.jda.dct.chatservice.dto.upstream.ChatRoomCreateDto
import com.jda.dct.chatservice.dto.upstream.TokenDto
import com.jda.dct.chatservice.service.SituationRoomService
import groovy.json.JsonSlurper
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class SituationRoomControllerSpec extends Specification {

    def "test throw exception if service is null"() {
        when: "Create the purchase order"
        new ChatRoomController(null)
        then:
        thrown IllegalArgumentException
    }

    def "test no exception"() {
        given:
        def service = Mock(SituationRoomService);
        when: "Create the purchase order"

        new ChatRoomController(service)
        then:
        noExceptionThrown()
    }

    def "should return token"() {
        given:
        def token = new TokenDto();
        token.teamId = "abcd"
        token.token = "token1"
        def service = Mock(SituationRoomService);
        service.getSessionToken() >> token;

        when: "Calling get token of a user"
        def ChatRoomController controller = new ChatRoomController(service);
        ResponseEntity<TokenDto> tokenResponse = controller.getAccessToken();

        then:
        tokenResponse.statusCode.value() == 200
        tokenResponse.body.token == "token1"
        tokenResponse.body.teamId == "abcd"
    }

    def "test create channel"() {
        given:
        def exepectedResponse = Maps.newHashMap();
        exepectedResponse.put("id","1111");
        exepectedResponse.put("created_at",new Date());
        exepectedResponse.put("name","channels_1")
        exepectedResponse.put("team_id","2222");
        ChatRoomCreateDto situationRoomDto = new ChatRoomCreateDto();
        def service = Mock(SituationRoomService);
        service.createChannel(_ as ChatRoomCreateDto) >> exepectedResponse;

        when: "Calling get token of a user"
        def controller = new ChatRoomController(service);
        ResponseEntity<Map<String,Object>> responseEntity =controller.addNewChannel(Mock(ChatRoomCreateDto))
        then:
        responseEntity.getStatusCode().value() == 200
        responseEntity.getBody() == exepectedResponse
    }

    def "test add user to existing"() {
        given:
        def users = Lists.newArrayList("user1");
        def request = new AddUserToRoomDto();
        request.users == users;
        def exepectedResponse = Maps.newHashMap();
        exepectedResponse.put("Status","success");
        def service = Mock(SituationRoomService);
        service.addUserToChannel("abcd",request) >> exepectedResponse;

        when: "Add user to existing channel"
        def controller = new ChatRoomController(service);
        ResponseEntity<Map<String,Object>> responseEntity =controller.addUsersToChannels("abcd",request)
        then:
        responseEntity.getStatusCode().value() == 200
    }

    def "test get chat context"() {
        given:
        def entity = new ArrayList();
        entity.add("json1");
        def context = Mock(ChatContext);
        context.getEntity()>>entity;
        def service = Mock(SituationRoomService);
        service.getChannelContext("abcd") >> context;

        when: "Add user to existing channel"
        def controller = new ChatRoomController(service);
        ResponseEntity<ChatContext> responseEntity =controller.getChatRoomContext("abcd")
        then:
        responseEntity.getStatusCode().value() == 200
        ((List)((ChatContext)responseEntity.getBody()).getEntity())[0] == "json1"

    }

    def "test post message" () {
        given:
         def jsonSlurper = new JsonSlurper()
         Map<String,Object> postObject = jsonSlurper.parseText('{"channel_id": "9k3ebut7ij85db74igboy97a1o",' +
                '"message": "Sending just like","root_id": "","file_ids":null,"props": ""}')

        Map<String,Object> expectedObj = jsonSlurper.parseText('{"id": "kwysn85zubfpzdno4nzjx7k9ao",' +
                '"create_at": 1547061027479,"update_at": 1547061027479,"edit_at": 0, "delete_at": 0,' +
                '"is_pinned": false,"user_id": "jtjktxa6n7d7ig1f4a9p9ih9gc","channel_id": "9k3ebut7ij85db74igboy97a1o",' +
                '"root_id": "","parent_id": "","original_id": "","message": "Sending just like",' +
                '"type": "","props": {}, "hashtags": "","pending_post_id": ""}')

        def service = Mock(SituationRoomService);
        service.postMessage(_ as Map) >> expectedObj;

        when:
        def controller = new ChatRoomController(service);
        ResponseEntity<Map<String,Object>> responseEntity =controller.postMessageToChannel(postObject)

        then:
        responseEntity.statusCode.value() == 200
        responseEntity.getBody() == expectedObj
    }
}
