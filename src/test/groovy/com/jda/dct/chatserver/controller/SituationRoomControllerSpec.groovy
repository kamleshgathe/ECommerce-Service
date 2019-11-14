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
import com.jda.dct.chatservice.exception.InvalidChatRequest
import com.jda.dct.chatservice.service.SituationRoomService
import com.jda.dct.domain.Attachment
import com.jda.dct.domain.ChatRoomStatus
import com.jda.dct.ignitecaches.springimpl.Tenants
import com.jda.luminate.common.base.ResponseDataWrapper
import com.jda.luminate.ingest.util.InputStreamWrapper
import com.jda.luminate.security.contexts.AuthContext
import groovy.json.JsonSlurper
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Specification

class SituationRoomControllerSpec extends Specification {

    def "test throw exception if service is null"() {
        when: "Create the purchase order"
        new ChatRoomController(null,null)
        then:
        thrown InvalidChatRequest
    }

    def "test no exception"() {
        given:
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        when: "Create the purchase order"

        new ChatRoomController(service,authContext)
        then:
        noExceptionThrown()
    }

    def "should return token"() {
        given:
        def token = new TokenDto();
        token.teamId = "abcd"
        token.token = "token1"
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        service.getSessionToken() >> token;
        authContext.getCurrentTid() >> "tid1"

        when: "Calling get token of a user"
        def ChatRoomController controller = new ChatRoomController(service,authContext);
        ResponseEntity<TokenDto> tokenResponse = controller.getAccessToken();

        then:
        tokenResponse.statusCode.value() == 200
        tokenResponse.body.token == "token1"
        tokenResponse.body.teamId == "abcd"
        Tenants.getCurrent() == "tid1"
    }

    def "test create channel"() {
        given:
        def exepectedResponse = Maps.newHashMap();
        exepectedResponse.put("id", "1111");
        exepectedResponse.put("created_at", new Date());
        exepectedResponse.put("name", "channels_1")
        exepectedResponse.put("team_id", "2222");
        ChatRoomCreateDto situationRoomDto = new ChatRoomCreateDto();
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        service.createChannel(_ as ChatRoomCreateDto) >> exepectedResponse;
        authContext.getCurrentTid() >> "tid1"

        when: "Calling get token of a user"
        def controller = new ChatRoomController(service,authContext);
        ResponseEntity<Map<String, Object>> responseEntity = controller.addNewChannel(Mock(ChatRoomCreateDto))
        then:
        responseEntity.getStatusCode().value() == 200
        responseEntity.getBody() == exepectedResponse
        Tenants.getCurrent() == "tid1"
    }

    def "test delete channel"() {
        given:
        def exepectedResponse = Maps.newHashMap();
        exepectedResponse.put("status", "OK");
        exepectedResponse.put("deletedRoomId", "a5kr3xy6af8gipmw5r47cfzoir")
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        service.removeChannel("a5kr3xy6af8gipmw5r47cfzoir") >> exepectedResponse;
        authContext.getCurrentTid() >> "tid1"

        when: "Calling delete channel"
        def controller = new ChatRoomController(service,authContext);
        ResponseEntity<Map<String, Object>> responseEntity = controller.deleteChannel("a5kr3xy6af8gipmw5r47cfzoir")
        then:
        responseEntity.getStatusCode().value() == 200
        responseEntity.getBody() == exepectedResponse
        Tenants.getCurrent() == "tid1"
    }

    def "test add user to existing"() {
        given:
        def users = Lists.newArrayList("user1");
        def request = new AddUserToRoomDto();
        request.users == users;
        def exepectedResponse = Maps.newHashMap();
        exepectedResponse.put("Status", "success");
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        authContext.getCurrentTid() >> "tid1"
        service.inviteUsers("abcd", request) >> exepectedResponse;

        when: "Add user to existing channel"
        def controller = new ChatRoomController(service,authContext);
        ResponseEntity<Map<String, Object>> responseEntity = controller.inviteUsers("abcd", request)
        then:
        responseEntity.getStatusCode().value() == 200
        Tenants.getCurrent() == "tid1"
    }

    def "test get chat context"() {
        given:
        def entity = new ArrayList();
        entity.add("json1");
        def context = Mock(ChatContext);
        context.getEntity() >> entity;
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        service.getChannelContext("abcd") >> context;
        authContext.getCurrentTid() >> "tid1"

        when: "Add user to existing channel"
        def controller = new ChatRoomController(service,authContext);
        ResponseEntity<ChatContext> responseEntity = controller.getChatRoomContext("abcd")
        then:
        responseEntity.getStatusCode().value() == 200
        ((List) ((ChatContext) responseEntity.getBody()).getEntity())[0] == "json1"
        Tenants.getCurrent() == "tid1"
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

        def service = mockedChatService()
        def authContext = mockedAuthContext()
        service.postMessage(_ as Map) >> expectedObj;
        authContext.getCurrentTid() >> "tid1"
        when:
        def controller = new ChatRoomController(service,authContext);
        ResponseEntity<Map<String, Object>> responseEntity = controller.postMessageToChannel(postObject)

        then:
        responseEntity.statusCode.value() == 200
        responseEntity.getBody() == expectedObj
        Tenants.getCurrent() == "tid1"
    }

    def "resolve room should succeed"() {
        given: "Initialize response context"
        ChatContext expected = Mock(ChatContext)
        expected.getCreatedBy() >> "user1"
        expected.getResolvedAt() >> 123456789
        expected.getResolution() >> Lists.newArrayList("resolved1")
        expected.getResolutionRemark() >> "thanks"
        expected.getRoomStatus() >> ChatRoomStatus.RESOLVED.name()
        expected.getId() >> "room1"

        ResolveRoomDto dto = new ResolveRoomDto();
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        service.resolve(_ as String, _ as ResolveRoomDto) >> expected;
        authContext.getCurrentTid() >> "tid1"

        when: "Resolve room"
        def controller = new ChatRoomController(service,authContext);
        ResponseEntity<ChatContext> response = controller.resolve("room1", dto);
        ChatContext actual = response.getBody();
        then: "Match expectation"
        response.statusCode == HttpStatus.OK
        actual.getResolution() == expected.getResolution()
        actual.resolvedBy == expected.resolvedBy
        actual.resolutionRemark == expected.resolutionRemark
        actual.getId() == actual.getId();
        Tenants.getCurrent() == "tid1"
    }

    def "read resolve room should succeed"() {
        given: "Initialize response context"
        def exepectedResponse = Maps.newHashMap();
        exepectedResponse.put("readResolved", "ok");

        def service = mockedChatService()
        def authContext = mockedAuthContext()
        service.readResolvedChannel() >> exepectedResponse
        authContext.getCurrentTid() >> "tid1"

        when: "Read Resolve room"
        def controller = new ChatRoomController(service,authContext);
        ResponseEntity<Map<String, Object>> responseEntity = controller.readResolvedChannel()
        then: "Match expectation"
        responseEntity.getStatusCode().value() == 200
        responseEntity.getBody() == exepectedResponse
        Tenants.getCurrent() == "tid1"
    }

    def "Deatche user from room should succeed"() {
        given: "Initialize response context"

        Map status = Maps.newHashMap();
        status.put("Status", "Success")
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        authContext.getCurrentTid() >> "tid1"
        service.removeParticipant(_ as String, _ as String) >> status;

        when: "Resolve room"
        def controller = new ChatRoomController(service,authContext);
        ResponseEntity<Map> response = controller.removeUser("room1", "user2");
        then: "Match expectation"
        response.statusCode == HttpStatus.OK
        response.body.get("Status") == "Success"
        Tenants.getCurrent() == "tid1"
    }

    def "test get channels"() {
        given: "Initialize response context"
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        def mockChannels = Mock(List)
        service.getChannels(_,_,_) >> mockChannels
        mockChannels.size() >> 5
        authContext.getCurrentTid() >> "tid1"
        when: "Get rooms"
        def controller = new ChatRoomController(service,authContext);
        ResponseEntity<List> response = controller.getChannels(null, null, null)
        then: "Match expectation"
        response.statusCode == HttpStatus.OK
        response.body.size() == 5
        Tenants.getCurrent() == "tid1"
    }

    def "test join room"() {
        given: "Initialize"
        def map = Maps.newHashMap()
        map.put("Status","Success")
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        authContext.getCurrentTid() >> "tid1"
        service.acceptInvitation("room1") >> map
        when: "join rooms"
        def controller = new ChatRoomController(service,authContext);
        ResponseEntity<Map> response = controller.join("room1")
        then: "Match expectation"
        response.statusCode == HttpStatus.OK
        response.body.get("Status") == "Success"
    }

    def "test unread count"() {
        given: "Initialize"
        def unreadResponse = Mock(List)
        unreadResponse.size() >> 5
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        authContext.getCurrentTid() >> "tid1"
        service.getUnreadCount() >> unreadResponse
        when: "unread counts"
        def controller = new ChatRoomController(service,authContext);
        ResponseEntity<List> response = controller.getUserUnreadCount()
        then: "Match expectation"
        response.statusCode == HttpStatus.OK
        response.body.size() == 5
        Tenants.getCurrent() == "tid1"
    }

    def "upload File"() {
        given: "Initialize"
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        authContext.getCurrentTid() >> "tid1"
        def file = new MockMultipartFile("data", "filename.txt",
                "text/plain", "some data".getBytes())
        when: "upload"
        def controller = new ChatRoomController(service,authContext);
        ResponseEntity<ResponseDataWrapper<Attachment>> response  = controller.upload("id", file, "comment")
        then: "Get 'invalid field value message'"
        response.getStatusCode().value() == 200
    }

    def "download file"(){
        given: "Initializing variables"
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        authContext.getCurrentTid() >> "tid1"
        def inputStream = new ByteArrayInputStream("testData".getBytes())
        def entityId = "shipment-123"
        def documentId = "docId124"
        def fileName = "excel.csv"
        def inputStreamWrapper = new InputStreamWrapper(inputStream, fileName)
        service.getDocument(entityId, documentId) >> inputStreamWrapper
        when: "invoking controller download attachment API"
        def controller = new ChatRoomController(service,authContext);
        ResponseEntity<InputStreamResource>  res =  controller.downloadAttachment(entityId, documentId)
        then: "should succeed"
        res.getStatusCode().value() == 200
        res.getBody().getInputStream() != null

    }

    def "Delete Attachment"(){
        given: "Initializing variables"
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        authContext.getCurrentTid() >> "tid1"
        def id = "shipment-123"
        def attachmentId = "docId124"
        when: "invoking Delete attachment API"
        def controller = new ChatRoomController(service,authContext)
        ResponseEntity<ResponseDataWrapper> resp = controller.deleteAttachment(id, attachmentId)
        then: "should succeed"
        resp.getStatusCode().value() == 200

    }

    def mockedChatService() {
        return Mock(SituationRoomService)
    }

    def mockedAuthContext() {
        return Mock(AuthContext)
    }

    def "test search channels"() {
        given: "Initialize response context"
        def service = mockedChatService()
        def authContext = mockedAuthContext()
        def mockChannels = Mock(List)
        service.searchChannels(_) >> mockChannels
        mockChannels.size() >> 5
        authContext.getCurrentTid() >> "tid1"
        when: "Get rooms"
        def controller = new ChatRoomController(service,authContext)
        ResponseEntity<List> response = controller.searchChannels(null)
        then: "Match expectation"
        response.statusCode == HttpStatus.OK
        response.body.size() == 5
        Tenants.getCurrent() == "tid1"
    }
}
