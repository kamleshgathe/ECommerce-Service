/*
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.service

import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.jda.dct.chatservice.domainreader.EntityReaderFactory
import com.jda.dct.chatservice.dto.upstream.AddUserToRoomDto
import com.jda.dct.chatservice.dto.upstream.ChatContext
import com.jda.dct.chatservice.dto.upstream.ChatRoomCreateDto
import com.jda.dct.chatservice.dto.upstream.TokenDto
import com.jda.dct.chatservice.repository.ChatRoomParticipantRepository
import com.jda.dct.chatservice.repository.ProxyTokenMappingRepository
import com.jda.dct.chatservice.repository.SituationRoomRepository
import com.jda.dct.chatservice.service.SituationRoomServiceImpl
import com.jda.dct.chatservice.utils.ChatRoomUtil
import com.jda.dct.contexts.AuthContext
import com.jda.dct.domain.ChatRoom
import com.jda.dct.domain.ChatRoomParticipant
import com.jda.dct.domain.ChatRoomParticipantStatus
import com.jda.dct.domain.ProxyTokenMapping
import org.assertj.core.util.Lists
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class SituationRoomServiceSpec extends Specification {

    def CHANNEL_TEAM_ID = "team1"
    AuthContext authContext
    SituationRoomRepository roomRepository;
    ProxyTokenMappingRepository tokenRepository
    ChatRoomParticipantRepository participantRepository;
    RestTemplate restTemplate;
    EntityReaderFactory entityReaderFactory

    @Subject
    SituationRoomServiceImpl service;

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
        then: "should get token and team id"

        tokenDto.teamId == CHANNEL_TEAM_ID
    }

    def "test post message should throw exception if input is null"() {
        given: "Intialize mocks"
        mock()
        Map<String, Object> chat = null;
        when: "Calling post message"
        initNewSituationRoomService();
        service.postMessage(chat)

        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test post message should throw exception if input is empty"() {
        given: "Intialize mocks"
        mock()
        roomRepository.findById(_ as String) >> Optional.empty();
        Map<String, Object> chat = new HashMap<>();
        when: "Calling post message"
        initNewSituationRoomService();
        service.postMessage(chat)

        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test post message should throw exception if channel is invalid"() {
        given: "Intialize mocks"
        mock()
        roomRepository.findById(_ as String) >> chatRoom;
        Map<String, Object> chat = new HashMap<>();
        chat.put("channel_id", channelId)
        when: "Calling post message"
        initNewSituationRoomService();
        service.postMessage(chat)

        then: "Should get exception"
        thrown(exceptionClazz)

        where:
        channelId | chatRoom         | exceptionClazz
        null      | Optional.empty() | IllegalArgumentException
        ""        | Optional.empty() | IllegalArgumentException
        "123"     | Optional.empty() | IllegalArgumentException
    }


    def "test post message should pass"() {
        given: "Intialize mocks"
        mock()
        def mockChatRoom = Mock(ChatRoom)
        mockChatRoom.getChats() >> ChatRoomUtil.objectToByteArray(new ArrayList())
        def tokenMapping = new ProxyTokenMapping()
        tokenMapping.setAppUserId("user1")
        tokenMapping.setRemoteUserId("abcd")
        tokenMapping.setProxyToken("token1")
        authContext.getCurrentUser() >> "user1"
        roomRepository.findById(_ as String) >> Optional.of(mockChatRoom);
        tokenRepository.findByAppUserId("user1") >> tokenMapping;
        restTemplate.exchange(_ as String, _ as HttpMethod, _ as HttpEntity, Map.class, *_) >>
                {
                    Map body = Maps.newHashMap();
                    body.put("id", "111")
                    return mockedResponseEntity(HttpStatus.OK, body)
                }
        Map<String, Object> chat = new HashMap<>();
        chat.put("channel_id", "channel1")
        chat.put("message", "msg1")
        when: "Calling post message"
        initNewSituationRoomService();
        Map<String, Object> response = service.postMessage(chat)

        then: "Response should match as expected"
        1 * roomRepository.save(_ as ChatRoom)
        response.get("id") == "111"
    }

    def "test create channel expect exception if input null"() {
        given:
        def channel = null
        when: "Calling create channel"
        initNewSituationRoomService()
        service.createChannel(channel)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test create channel expect exception if no domian entity"() {
        given:
        def channel = new ChatRoomCreateDto();
        when: "Calling create channel"
        initNewSituationRoomService()
        service.createChannel(channel)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test create channel expect exception if no participant"() {
        given:
        def channel = new ChatRoomCreateDto();
        channel.setObjectIds(Lists.newArrayList("1", "2"))
        when: "Calling create channel"
        initNewSituationRoomService()
        service.createChannel(channel)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test create channel expect exception if team id missing"() {
        given:
        def channel = new ChatRoomCreateDto();
        channel.setObjectIds(Lists.newArrayList("1", "2"))
        channel.setParticipants(Lists.newArrayList("1", "2"))
        when: "Calling create channel"
        initNewSituationRoomService()
        service.createChannel(channel)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test create channel expect exception if entity type missing"() {
        given:
        def channel = new ChatRoomCreateDto();
        channel.setObjectIds(Lists.newArrayList("1", "2"))
        channel.setParticipants(Lists.newArrayList("1", "2"))
        when: "Calling create channel"
        initNewSituationRoomService()
        service.createChannel(channel)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test create channel expect exception if name missing"() {
        given:
        def channel = new ChatRoomCreateDto();
        channel.setObjectIds(Lists.newArrayList("1", "2"))
        channel.setParticipants(Lists.newArrayList("1", "2"))
        channel.setEntityType("shipment")
        when: "Calling create channel"
        initNewSituationRoomService()
        service.createChannel(channel)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test create channel expect exception if purpose missing"() {
        given:
        def channel = new ChatRoomCreateDto();
        channel.setObjectIds(Lists.newArrayList("1", "2"))
        channel.setParticipants(Lists.newArrayList("1", "2"))
        channel.setEntityType("shipment")
        channel.setName("name1")
        when: "Calling create channel"
        initNewSituationRoomService()
        service.createChannel(channel)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test create channel expect exception if situation type missing"() {
        given:
        def channel = new ChatRoomCreateDto();
        channel.setObjectIds(Lists.newArrayList("1", "2"))
        channel.setParticipants(Lists.newArrayList("1", "2"))
        channel.setEntityType("shipment")
        channel.setName("name1")
        channel.setPurpose("situation room for shipment delayed")
        when: "Calling create channel"
        initNewSituationRoomService()
        service.createChannel(channel)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test create channel with user"() {
        given:
        def channel = new ChatRoomCreateDto();
        channel.setObjectIds(Lists.newArrayList("1", "2"))
        channel.setParticipants(Lists.newArrayList("1", "2"))
        channel.setEntityType("shipment")
        channel.setName("name1")
        channel.setPurpose("situation room for shipment delayed")
        when: "Calling create channel"
        initNewSituationRoomService()
        service.createChannel(channel)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test create channel with room name having space"() {
        given:
        def channel = new ChatRoomCreateDto();
        channel.setObjectIds(Lists.newArrayList("1", "2"))
        channel.setParticipants(Lists.newArrayList("1", "2"))
        channel.setEntityType("shipment")
        channel.setName("name1 aa")
        channel.setPurpose("situation room for shipment delayed")
        when: "Calling create channel"
        initNewSituationRoomService()
        service.createChannel(channel)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test create channel with duplicate room name"() {
        given:
        mock()
        def channel = new ChatRoomCreateDto();
        channel.setObjectIds(Lists.newArrayList("1", "2"))
        channel.setParticipants(Lists.newArrayList("1", "2"))
        channel.setEntityType("shipment")
        channel.setName("name1")
        channel.setPurpose("situation room for shipment delayed")
        roomRepository.findByRoomName("name1") >> Mock(ChatRoom)
        when: "Calling create channel"
        initNewSituationRoomService()
        service.createChannel(channel)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test create channel should succeed"() {
        given:
        mock()
        def channel = new ChatRoomCreateDto();
        def user = "1"
        def ptm1 = proxyTokenMapping("1", "remote_user1", "token1");
        def ptm2 = proxyTokenMapping("2", "remote_user2", "token1");
        def room = Mock(ChatRoom);
        room.getCreatedBy() >> user
        def participants = Sets.newHashSet();
        participants.add(chatParticipant(room, "1", ChatRoomParticipantStatus.PENDING))
        participants.add(chatParticipant(room, "2", ChatRoomParticipantStatus.PENDING))
        room.getParticipants() >> participants

        authContext.getCurrentUser() >> user

        tokenRepository.findByAppUserId("1") >> ptm1
        tokenRepository.findByAppUserId("2") >> ptm2

        roomRepository.findById(_ as String) >> Optional.of(room)
        tokenRepository.save({
            ProxyTokenMapping ptm -> ptm.getAppUserId() == "1" ? ptm1 : ptm2
        });
        restTemplate.exchange(_ as String, _ as HttpMethod, _ as HttpEntity, Map.class, *_) >>
                {
                    args ->
                        Map body = Maps.newHashMap();
                        if (args[0].contains("/channels")) {
                            body.put("id", "1")
                        }
                        return mockedResponseEntity(HttpStatus.OK, body)
                }

        channel.setObjectIds(Lists.newArrayList("1", "2"))
        channel.setParticipants(Lists.newArrayList("1", "2"))
        channel.setEntityType("shipment")
        channel.setName("name1")
        channel.setSituationType("shipment_delayed")
        channel.setPurpose("situation room for shipment delayed")
        when: "Calling create channel"
        initNewSituationRoomService()
        service.createChannel(channel)
        then: "Should succeed"
        true
    }

    def "test add users to existing channel expect exception if channel is missing"() {
        given: "Setup request"
        def request = new AddUserToRoomDto();
        when: "Calling create channel"
        initNewSituationRoomService()
        service.inviteUsers(null, request)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test add users to existing channel expect exception if request is null"() {
        given: "Setup request"
        def request = new AddUserToRoomDto();
        request.setUsers(null);
        when: "Calling create channel"
        initNewSituationRoomService()
        service.inviteUsers("abcd", request)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test add users to existing channel expect exception if users in request null"() {
        given: "Setup request"
        def request = new AddUserToRoomDto();
        request.setUsers(null);
        when: "Calling create channel"
        initNewSituationRoomService()
        service.inviteUsers("abcd", request)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test add users to existing channel expect exception if users in request empty"() {
        given: "Setup request"
        def request = new AddUserToRoomDto();
        request.setUsers(Lists.newArrayList());
        when: "Calling create channel"
        initNewSituationRoomService()
        service.inviteUsers("abcd", request)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test add users to existing channel expect exception if channel does not exists"() {
        given: "Setup request"
        mock()
        def users = Lists.newArrayList("user1");
        def request = new AddUserToRoomDto();
        request.setUsers(users);
        roomRepository.findById("abcd") >> Optional.empty()
        when: "Calling create channel"
        initNewSituationRoomService()
        service.inviteUsers("abcd", request)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test add user to existing channel and should add user in remote"() {
        given: "Setup request"


        def user = "newUser"
        def users = Lists.newArrayList(user);
        def request = new AddUserToRoomDto();
        request.setUsers(users);
        mock()
        def mockChatRoom = Mock(ChatRoom);
        mockChatRoom.getParticipants() >> Lists.newArrayList("user1")
        _ * authContext.getCurrentUser() >> user
        1 * tokenRepository.findByAppUserId(user) >> null;
        tokenRepository.save(_ as ProxyTokenMapping) >> Mock(ProxyTokenMapping)
        roomRepository.findById("abcd") >> Optional.of(mockChatRoom)
        //tokenRepository.save(_ as ProxyTokenMapping) >> proxyTokenMappingWithToken("token")
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
        when: "Calling create channel"
        initNewSituationRoomService()
        service.inviteUsers("abcd", request)
        then: "Should succeed"
        1 * roomRepository.save(_)
    }

    def "test add existing user to existing channel and should not add user in remote"() {
        given: "Setup request"


        def user = "user1"
        def users = Lists.newArrayList(user);
        def request = new AddUserToRoomDto();
        request.setUsers(users);
        mock()
        def mockChatRoom = Mock(ChatRoom);
        def participant = Mock(ChatRoomParticipant)
        participant.getUserName() >> "user1";
        mockChatRoom.getParticipants() >> Lists.newArrayList(participant)
        mockChatRoom.getRoomName() >> "test"

        _ * authContext.getCurrentUser() >> user
        1 * tokenRepository.findByAppUserId(user) >> null;
        tokenRepository.save(_ as ProxyTokenMapping) >> proxyTokenMappingWithToken("token")
        roomRepository.findById("abcd") >> Optional.of(mockChatRoom)
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
        when: "Calling create channel"
        initNewSituationRoomService()
        service.inviteUsers("abcd", request)
        then: "Should succeed"
        1 * roomRepository.save({
            ChatRoom room ->
                room.getParticipants().getAt(0).userName == "user1"
        })
    }

    @Unroll
    def "test get chat context should get exception if input is invalid"() {
        given: "Setup request"
        mock()
        roomRepository.findById("1") >> Optional.empty()
        when: "Calling get channel context"
        initNewSituationRoomService()
        service.getChannelContext(channel)
        then: "Should get exception"
        thrown(ex)
        where:
        channel | ex
        null    | IllegalArgumentException
        ""      | IllegalArgumentException
        "1"     | IllegalArgumentException
    }

    def "test get chat context should succeed"() {
        given: "Setup request"
        mock()
        def entity = new ArrayList();
        entity.add("json1");
        String jsonString = ChatRoomUtil.objectToJson(entity);
        byte[] bytes = ChatRoomUtil.objectToByteArray(jsonString);
        def mockRoom = Mock(ChatRoom)
        mockRoom.getContexts() >> bytes
        mockRoom.getCreationDate() >> new Date()
        mockRoom.getLastPostAt() >> new Date()
        mockRoom.getLmd() >> new Date()
        mockRoom.getParticipants() >> new ArrayList<>()
        roomRepository.findById("1") >> Optional.of(mockRoom);
        when: "Calling get channel context"
        initNewSituationRoomService()
        Object actual = service.getChannelContext("1")
        then: "Should get context"
        ((List) ((ChatContext) actual).getEntity()).get(0) == "json1";
    }


    def initNewSituationRoomService() {
        service = new SituationRoomServiceImpl(authContext,
                roomRepository,
                tokenRepository,
                participantRepository,
                entityReaderFactory)
        service.setRestTemplate(restTemplate)
        service.setChannelTeamId(CHANNEL_TEAM_ID)
        service.setMattermostUrl("http://localhost:80/api/v4")
    }

    def mock() {
        authContext = Mock(AuthContext)
        roomRepository = Mock(SituationRoomRepository)
        tokenRepository = Mock(ProxyTokenMappingRepository)
        restTemplate = Mock(RestTemplate)
        participantRepository = Mock(ChatRoomParticipantRepository)
        entityReaderFactory = Mock(EntityReaderFactory)
    }

    def mockedResponseEntity(status, body) {
        def responseEntity = Mock(ResponseEntity)
        responseEntity.getBody() >> body
        responseEntity.getStatusCode() >> status
        return responseEntity;
    }

    def proxyTokenMapping(def appUser, def remoteUser, def token) {
        def mapping = new ProxyTokenMapping()
        mapping.setAppUserId(appUser)
        mapping.setRemoteUserId(remoteUser)
        mapping.setProxyToken(token)
        return mapping;
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

    def chatParticipant(def chatRoom, def userName, def status) {
        ChatRoomParticipant p = new ChatRoomParticipant()
        p.setUserName(userName);
        p.setRoom(chatRoom)
        p.status = ChatRoomParticipantStatus.PENDING;
        return p;
    }
}
