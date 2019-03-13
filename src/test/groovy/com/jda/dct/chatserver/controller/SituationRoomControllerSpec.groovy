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
import com.jda.dct.chatservice.dto.upstream.*
import com.jda.dct.chatservice.service.SituationRoomService
import com.jda.dct.domain.ChatRoomStatus
import groovy.json.JsonSlurper
import org.springframework.http.HttpStatus
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
        exepectedResponse.put("id", "1111");
        exepectedResponse.put("created_at", new Date());
        exepectedResponse.put("name", "channels_1")
        exepectedResponse.put("team_id", "2222");
        ChatRoomCreateDto situationRoomDto = new ChatRoomCreateDto();
        def service = Mock(SituationRoomService);
        service.createChannel(_ as ChatRoomCreateDto) >> exepectedResponse;

        when: "Calling get token of a user"
        def controller = new ChatRoomController(service);
        ResponseEntity<Map<String, Object>> responseEntity = controller.addNewChannel(Mock(ChatRoomCreateDto))
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
        exepectedResponse.put("Status", "success");
        def service = Mock(SituationRoomService);
        service.inviteUsers("abcd", request) >> exepectedResponse;

        when: "Add user to existing channel"
        def controller = new ChatRoomController(service);
        ResponseEntity<Map<String, Object>> responseEntity = controller.inviteUsers("abcd", request)
        then:
        responseEntity.getStatusCode().value() == 200
    }

    def "test get chat context"() {
        given:
        def entity = new ArrayList();
        entity.add("json1");
        def context = Mock(ChatContext);
        context.getEntity() >> entity;
        def service = Mock(SituationRoomService);
        service.getChannelContext("abcd") >> context;

        when: "Add user to existing channel"
        def controller = new ChatRoomController(service);
        ResponseEntity<ChatContext> responseEntity = controller.getChatRoomContext("abcd")
        then:
        responseEntity.getStatusCode().value() == 200
        ((List) ((ChatContext) responseEntity.getBody()).getEntity())[0] == "json1"

    }

    def "test post message"() {
        given:
        def jsonSlurper = new JsonSlurper()
        Map<String, Object> postObject = jsonSlurper.parseText('{"channel_id": "9k3ebut7ij85db74igboy97a1o",' +
                '"message": "Sending just like","root_id": "","file_ids":null,"props": ""}')

        Map<String, Object> expectedObj = jsonSlurper.parseText('{"id": "kwysn85zubfpzdno4nzjx7k9ao",' +
                '"create_at": 1547061027479,"update_at": 1547061027479,"edit_at": 0, "delete_at": 0,' +
                '"is_pinned": false,"user_id": "jtjktxa6n7d7ig1f4a9p9ih9gc","channel_id": "9k3ebut7ij85db74igboy97a1o",' +
                '"root_id": "","parent_id": "","original_id": "","message": "Sending just like",' +
                '"type": "","props": {}, "hashtags": "","pending_post_id": ""}')

        def service = Mock(SituationRoomService);
        service.postMessage(_ as Map) >> expectedObj;

        when:
        def controller = new ChatRoomController(service);
        ResponseEntity<Map<String, Object>> responseEntity = controller.postMessageToChannel(postObject)

        then:
        responseEntity.statusCode.value() == 200
        responseEntity.getBody() == expectedObj
    }

    def "resolve room should succeed"() {
        given: "Initialize response context"
        ChatContext expected = Mock(ChatContext)
        expected.getCreatedBy() >> "user1"
        expected.getResolvedAt() >> 123456789
        expected.getResolution() >> "resolved1"
        expected.getResolutionRemark() >> "thanks"
        expected.getRoomStatus() >> ChatRoomStatus.RESOLVED.name()
        expected.getResolutionTypes() >> Lists.newArrayList("shipment started")
        expected.getId() >> "room1"

        ResolveRoomDto dto = new ResolveRoomDto();
        def service = Mock(SituationRoomService);
        service.resolve(_ as String, _ as ResolveRoomDto) >> expected;

        when: "Resolve room"
        def controller = new ChatRoomController(service);
        ResponseEntity<ChatContext> response = controller.resolve("room1", dto);
        ChatContext actual = response.getBody();
        then: "Match expectation"
        response.statusCode == HttpStatus.OK
        actual.resolution == expected.resolution
        actual.resolvedBy == expected.resolvedBy
        actual.getResolutionTypes() == expected.getResolutionTypes()
        actual.resolutionRemark == expected.resolutionRemark
        actual.getId() == actual.getId();
    }

    def "Deatche user from room should succeed"() {
        given: "Initialize response context"

        Map status = Maps.newHashMap();
        status.put("Status", "Success")
        def service = Mock(SituationRoomService);
        service.removeParticipant(_ as String, _ as String) >> status;

        when: "Resolve room"
        def controller = new ChatRoomController(service);
        ResponseEntity<Map> response = controller.removeUser("room1", "user2");
        then: "Match expectation"
        response.statusCode == HttpStatus.OK
        response.body.get("Status") == "Success"
    }

    def "test get channels"() {
        given: "Initialize response context"
        def service = Mock(SituationRoomService);
        def mockChannels = Mock(List)
        service.getChannels(_,_) >> mockChannels
        mockChannels.size() >> 5

        when: "Get rooms"
        def controller = new ChatRoomController(service);
        ResponseEntity<List> response = controller.getChannels(null, null)
        then: "Match expectation"
        response.statusCode == HttpStatus.OK
        response.body.size() == 5
    }

    def "test join room"() {
        given: "Initialize"
        def map = Maps.newHashMap()
        map.put("Status","Success")
        def service = Mock(SituationRoomService);
        service.acceptInvitation("room1") >> map
        when: "join rooms"
        def controller = new ChatRoomController(service);
        ResponseEntity<Map> response = controller.join("room1")
        then: "Match expectation"
        response.statusCode == HttpStatus.OK
        response.body.get("Status") == "Success"
    }

    def "test unread count"() {
        given: "Initialize"
        def unreadResponse = Mock(List)
        unreadResponse.size() >> 5
        def service = Mock(SituationRoomService);
        service.getUnreadCount() >> unreadResponse
        when: "unread counts"
        def controller = new ChatRoomController(service);
        ResponseEntity<List> response = controller.getUserUnreadCount()
        then: "Match expectation"
        response.statusCode == HttpStatus.OK
        response.body.size() == 5
    }
}
