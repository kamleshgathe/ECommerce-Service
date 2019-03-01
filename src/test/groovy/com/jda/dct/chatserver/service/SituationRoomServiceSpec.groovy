/*
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.service

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.jda.dct.chatservice.domainreader.EntityReaderFactory
import com.jda.dct.chatservice.dto.upstream.AddUserToRoomDto
import com.jda.dct.chatservice.dto.upstream.ChatContext
import com.jda.dct.chatservice.dto.upstream.ChatRoomCreateDto
import com.jda.dct.chatservice.dto.upstream.ResolveRoomDto
import com.jda.dct.chatservice.dto.upstream.TokenDto
import com.jda.dct.chatservice.repository.ChatRoomParticipantRepository
import com.jda.dct.chatservice.repository.ProxyTokenMappingRepository
import com.jda.dct.chatservice.repository.SituationRoomRepository
import com.jda.dct.chatservice.service.SituationRoomServiceImpl
import com.jda.dct.chatservice.service.UniqueRoomNameGenerator
import com.jda.dct.chatservice.utils.ChatRoomUtil
import com.jda.dct.contexts.AuthContext
import com.jda.dct.domain.ChatRoom
import com.jda.dct.domain.ChatRoomParticipant
import com.jda.dct.domain.ChatRoomParticipantStatus
import com.jda.dct.domain.ChatRoomResolution
import com.jda.dct.domain.ChatRoomStatus
import com.jda.dct.domain.ProxyTokenMapping
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
    UniqueRoomNameGenerator generator;

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

    def "test post message should failed if room is already resolved"() {
        given: "Intialize mocks"
        mock()
        ChatRoom mockedRoom = Mock(ChatRoom);
        mockedRoom.getStatus() >> ChatRoomStatus.RESOLVED;
        roomRepository.findById(_ as String) >> Optional.of(mockedRoom);
        Map<String, Object> chat = new HashMap<>();
        chat.put("channel_id", "1")
        when: "Calling post message"
        initNewSituationRoomService();
        service.postMessage(chat)

        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test post message should failed if room is in resolved state"() {
        given: "Intialize mocks"
        mock()
        Set<ChatRoomParticipant> participants = Sets.newHashSet();
        def mockChatRoom = mockedChatRoom("room1",getDummySnapshot(), participants, "user1",ChatRoomStatus.RESOLVED)
        mockChatRoom.getChats() >> ChatRoomUtil.objectToByteArray(new ArrayList())
        addChatParticipant(mockChatRoom, "user1", ChatRoomParticipantStatus.JOINED)

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
        service.postMessage(chat)

        then: "Expect exception"
        thrown(IllegalArgumentException)
    }

    def "test post message should failed if user is not part of room"() {
        given: "Intialize mocks"
        mock()
        Set<ChatRoomParticipant> participants = Sets.newHashSet();
        def mockChatRoom = mockedChatRoom("room1",getDummySnapshot(), participants, "user1",ChatRoomStatus.OPEN)
        mockChatRoom.getChats() >> ChatRoomUtil.objectToByteArray(new ArrayList())
        addChatParticipant(mockChatRoom, "user1", ChatRoomParticipantStatus.JOINED)

        def tokenMapping = new ProxyTokenMapping()
        tokenMapping.setAppUserId("user2")
        tokenMapping.setRemoteUserId("abcd")
        tokenMapping.setProxyToken("token1")
        authContext.getCurrentUser() >> "user2"
        roomRepository.findById(_ as String) >> Optional.of(mockChatRoom);
        tokenRepository.findByAppUserId("user2") >> tokenMapping;
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
        service.postMessage(chat)

        then: "Expect exception"
        thrown(IllegalArgumentException)
    }



    def "test post message should pass"() {
        given: "Intialize mocks"
        mock()
        Set<ChatRoomParticipant> participants = Sets.newHashSet();
        def mockChatRoom = mockedChatRoom("room1",getDummySnapshot(), participants, "user1",ChatRoomStatus.OPEN)
        mockChatRoom.getChats() >> ChatRoomUtil.objectToByteArray(new ArrayList())
        addChatParticipant(mockChatRoom, "user1", ChatRoomParticipantStatus.JOINED)

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

    def "test create channel should succeed"() {
        given:
        mock()
        def channel = new ChatRoomCreateDto();
        def user = "1"
        def ptm1 = proxyTokenMapping("1", "remote_user1", "token1");
        def ptm2 = proxyTokenMapping("2", "remote_user2", "token1");
        def participants = Sets.newHashSet();
        def room = Mock(ChatRoom);
        room.getCreatedBy() >> user
        room.getParticipants() >> participants
        addChatParticipant(room, "1", ChatRoomParticipantStatus.PENDING)
        addChatParticipant(room, "2", ChatRoomParticipantStatus.PENDING)

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
        1 * generator.next() >> "abcdhahsmss"
    }

    def "test create room name with space should succeed"() {
        given:
        mock()
        def channel = new ChatRoomCreateDto();
        def user = "1"
        def ptm1 = proxyTokenMapping("1", "remote_user1", "token1");
        def ptm2 = proxyTokenMapping("2", "remote_user2", "token1");
        def participants = Sets.newHashSet();
        def room = Mock(ChatRoom);
        room.getCreatedBy() >> user
        room.getParticipants() >> participants
        addChatParticipant(room, "1", ChatRoomParticipantStatus.PENDING)
        addChatParticipant(room, "2", ChatRoomParticipantStatus.PENDING)

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
        channel.setName("name with space")
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
        mock()
        authContext.getCurrentUser()>>"user";
        def request = new AddUserToRoomDto();
        when: "Calling create channel"
        initNewSituationRoomService()
        service.inviteUsers(null, request)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test add users to existing channel expect exception if request is null"() {
        given: "Setup request"
        mock()
        authContext.getCurrentUser()>>"user";
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
        mock()
        authContext.getCurrentUser()>>"user";
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
        mock()
        authContext.getCurrentUser()>>"user";
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

    def "test resolve room expect exception if room already resolved"() {
        given: "Setup request"
        mock()
        def users = Lists.newArrayList("user1");
        def request = new AddUserToRoomDto();
        request.setUsers(users);
        ChatRoom mockdRoom = Mock(ChatRoom);
        mockdRoom.getStatus() >> ChatRoomStatus.RESOLVED
        roomRepository.findById("abcd") >> Optional.of(mockdRoom)
        when: "Calling create channel"
        initNewSituationRoomService()
        service.inviteUsers("abcd", request)
        then: "Should get exception"
        thrown(IllegalArgumentException)
    }

    def "test add user to existing channel and should add user in remote"() {
        given: "Setup request"

        def user1 = "user1"
        def user2 = "user2"
        def users = Lists.newArrayList(user2);
        def request = new AddUserToRoomDto();
        request.setUsers(users);
        mock()
        byte[] snapshot = getDummySnapshot()
        Set<ChatRoomParticipant> participants = Sets.newHashSet();
        def mockChatRoom = mockedChatRoom("abcd", snapshot, participants, user1, ChatRoomStatus.OPEN)

        addChatParticipant(mockChatRoom, user1, ChatRoomParticipantStatus.JOINED)


        _ * authContext.getCurrentUser() >> user1
        1 * tokenRepository.findByAppUserId(user2) >> null;
        tokenRepository.save(_ as ProxyTokenMapping) >> Mock(ProxyTokenMapping)
        roomRepository.findById("abcd") >> Optional.of(mockChatRoom)
        tokenRepository.findByAppUserId(user1) >> proxyTokenMappingWithoutToken();
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
        def participant = Mock(ChatRoomParticipant)
        participant.getUserName() >> "user1";
        def mockChatRoom = mockedChatRoom("abcd", byte[], Lists.newArrayList(participant), "user1", ChatRoomStatus.OPEN)
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

    def "test use all channels should succeed"() {
        given:
        mock()
        def user = "1"
        def participants1 = Sets.newHashSet()
        def participants2 = Sets.newHashSet()
        def participants3 = Sets.newHashSet()

        def participantAllEntries = Lists.newArrayList();

        byte[] snapshot = getDummySnapshot();

        def openRoom1 = mockedChatRoom("room1", snapshot, participants1, user, ChatRoomStatus.OPEN)
        def openRoom2 = mockedChatRoom("room2", snapshot, participants2, user, ChatRoomStatus.OPEN)
        def resolvedRoom = mockedChatRoom("room3", snapshot, participants3, user, ChatRoomStatus.RESOLVED)
        participantAllEntries.add(addChatParticipant(openRoom1, "1", ChatRoomParticipantStatus.JOINED))
        participantAllEntries.add(addChatParticipant(openRoom2, "1", ChatRoomParticipantStatus.PENDING))
        participantAllEntries.add(addChatParticipant(resolvedRoom, "1", ChatRoomParticipantStatus.JOINED))
        authContext.getCurrentUser() >> "1"

        when:
        initNewSituationRoomService()
        List<ChatContext> channels = service.getChannels(null, null);
        then:
        1 * participantRepository.findByUserName(_ as String) >> participantAllEntries
        channels.size() == 3
        channels.stream().filter({
            ChatContext context ->
                context.getRoomStatus() == ChatRoomStatus.OPEN.name()
        }).count() == 2

        channels.stream().filter({
            ChatContext context ->
                context.getRoomStatus() == ChatRoomStatus.RESOLVED.name()
        }).count() == 1
    }

    def "test use all channels should succeed when by room status"() {
        given:
        mock()
        def user = "1"
        def participants1 = Sets.newHashSet()

        def participantAllEntries = Lists.newArrayList();
        def openRoom1 = mockedChatRoom("room1", getDummySnapshot(), participants1, user, ChatRoomStatus.RESOLVED)
        participantAllEntries.add(addChatParticipant(openRoom1, "1", ChatRoomParticipantStatus.JOINED))
        authContext.getCurrentUser() >> "1"

        when:
        initNewSituationRoomService()
        List<ChatContext> channels = service.getChannels("room", "RESOLVED");
        then:
        1 * participantRepository.findByUserNameAndRoomStatus("1", ChatRoomStatus.RESOLVED) >> participantAllEntries
        channels.size() == 1
        channels.stream().filter({
            ChatContext context ->
                context.getRoomStatus() == ChatRoomStatus.RESOLVED.name()
        }).count() == 1

    }

    def "test use all channels should succeed when by user status"() {
        given:
        mock()
        def user = "1"
        def participants1 = Sets.newHashSet()
        def participantAllEntries = Lists.newArrayList();
        def openRoom1 = mockedChatRoom("room1", getDummySnapshot(), participants1, user, ChatRoomStatus.OPEN)

        participantAllEntries.add(addChatParticipant(openRoom1, "1", ChatRoomParticipantStatus.PENDING))
        authContext.getCurrentUser() >> "1"

        when:
        initNewSituationRoomService()
        List<ChatContext> channels = service.getChannels("user", "PENDING");
        then:
        1 * participantRepository.findByUserNameAndStatus("1", ChatRoomParticipantStatus.PENDING) >> participantAllEntries
        channels.size() == 1
        channels.stream().filter({
            ChatContext context ->
                context.getRoomStatus() == ChatRoomStatus.OPEN.name()
        }).count() == 1

        channels.stream().filter({
            ChatContext context ->
                context.getYourStatus() == ChatRoomParticipantStatus.PENDING
        }).count() == 1
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
        byte[] bytes = getDummySnapshot()
        def mockRoom = mockedChatRoom("1", bytes, Lists.newArrayList(), "1", ChatRoomStatus.OPEN)
        roomRepository.findById("1") >> Optional.of(mockRoom);
        when: "Calling get channel context"
        initNewSituationRoomService()
        Object actual = service.getChannelContext("1")
        then: "Should get context"
        ((List) ((ChatContext) actual).getEntity()).get(0) == "json1";
    }


    @Unroll
    def "resolve room should fail if inputs are not valid"() {
        given: "Initialize inputs"
        mock()
        ResolveRoomDto request = new ResolveRoomDto()
        request.resolutionTypes = resolutionType
        request.resolution = resolution;
        request.remark = remark;

        when: "Calling resolve room"
        initNewSituationRoomService()
        service.resolve(roomId, request)
        then:
        thrown(IllegalArgumentException.class)
        where: "Expect IllegalArgumentException"
        roomId | resolutionType                  | resolution    | remark
        null   | Lists.newArrayList("type1")     | "resolution1" | "thanks"
        "1"    | null                            | "resolution1" | "thanks"
        "1"    | Lists.newArrayList("type1")     | null          | "thanks"
        "1"    | Lists.newArrayList("type1")     | "resolution1" | null
        "1"    | Lists.newArrayList(" ")         | "resolution1" | null
        "1"    | Lists.newArrayList("type1", "") | "resolution1" | "thanks"
        "1"    | Lists.newArrayList()            | "resolution1" | "thanks"

    }

    def "resolve room should fail if room does not exists"() {
        given: "Initialize inputs"
        mock()
        authContext.getCurrentUser() >> "user1"
        ResolveRoomDto request = new ResolveRoomDto()
        request.resolutionTypes = Lists.newArrayList("type1")
        request.resolution = "resolution1";
        request.remark = "thanks";
        roomRepository.findById("1") >> Optional.empty()
        when: "Calling resolve room"
        initNewSituationRoomService()
        service.resolve("1", request)
        then: "Expect IllegalArgumentException"
        thrown(IllegalArgumentException.class)
    }

    def "resolve room should fail if room already resolved"() {
        given: "Initialize inputs"
        mock()
        authContext.getCurrentUser() >> "user1"
        ResolveRoomDto request = new ResolveRoomDto()
        request.resolutionTypes = Lists.newArrayList("type1")
        request.resolution = "resolution1";
        request.remark = "thanks";
        ChatRoom mockedRoom = Mock(ChatRoom);
        mockedRoom.getStatus() >> ChatRoomStatus.RESOLVED
        mockedRoom.getResolution() >> buildResolution(request, "user1")
        roomRepository.findById("1") >> Optional.of(mockedRoom)
        when: "Calling resolve room"
        initNewSituationRoomService()
        service.resolve("1", request)
        then: "Expect IllegalArgumentException"
        thrown(IllegalArgumentException.class)
    }

    def "resolve room should failed caller is not part of room"() {
        given: "Initialize inputs"
        mock()
        authContext.getCurrentUser() >> "user2"
        ResolveRoomDto request = new ResolveRoomDto()
        request.resolutionTypes = Lists.newArrayList("type1")
        request.resolution = "resolution1";
        request.remark = "thanks";

        byte[] snapshot = getDummySnapshot()
        Set<ChatRoomParticipant> participants = Sets.newHashSet();
        def mockRoom = mockedChatRoom("room1", snapshot, participants, "user1", ChatRoomStatus.OPEN)

        addChatParticipant(mockRoom, "user1", ChatRoomParticipantStatus.JOINED)

        mockRoom.getResolution() >> buildResolution(request, "user1")

        roomRepository.findById("1") >> Optional.of(mockRoom)
        when: "Calling resolve room"
        initNewSituationRoomService()
        service.resolve("1", request)
        then: "Save room should get called"
        thrown(IllegalArgumentException)
    }


    def "resolve room should failed caller is not yet joined"() {
        given: "Initialize inputs"
        mock()
        authContext.getCurrentUser() >> "user1"
        ResolveRoomDto request = new ResolveRoomDto()
        request.resolutionTypes = Lists.newArrayList("type1")
        request.resolution = "resolution1";
        request.remark = "thanks";

        byte[] snapshot = getDummySnapshot()
        Set<ChatRoomParticipant> participants = Sets.newHashSet();
        def mockRoom = mockedChatRoom("room1", snapshot, participants, "user1", ChatRoomStatus.OPEN)

        addChatParticipant(mockRoom, "user1", ChatRoomParticipantStatus.PENDING)
        addChatParticipant(mockRoom, "user2", ChatRoomParticipantStatus.JOINED)

        mockRoom.getResolution() >> buildResolution(request, "user1")

        roomRepository.findById("1") >> Optional.of(mockRoom)
        when: "Calling resolve room"
        initNewSituationRoomService()
        service.resolve("1", request)
        then: "Save room should get called"
        thrown(IllegalArgumentException)
    }


    def "resolve room should succeed"() {
        given: "Initialize inputs"
        mock()
        authContext.getCurrentUser() >> "user1"
        ResolveRoomDto request = new ResolveRoomDto()
        request.resolutionTypes = Lists.newArrayList("type1")
        request.resolution = "resolution1";
        request.remark = "thanks";

        byte[] snapshot = getDummySnapshot()
        Set<ChatRoomParticipant> participants = Sets.newHashSet();
        def mockRoom = mockedChatRoom("room1", snapshot, participants, "user1", ChatRoomStatus.OPEN)

        addChatParticipant(mockRoom, "user1", ChatRoomParticipantStatus.JOINED)
        addChatParticipant(mockRoom, "user2", ChatRoomParticipantStatus.JOINED)

        mockRoom.getResolution() >> buildResolution(request, "user1")

        roomRepository.findById("1") >> Optional.of(mockRoom)
        when: "Calling resolve room"
        initNewSituationRoomService()
        service.resolve("1", request)
        then: "Save room should get called"
        1 * roomRepository.save(_ as ChatRoom) >> {
            ChatRoom newStateRoom ->
                newStateRoom.getResolution().types == request.resolutionTypes
                newStateRoom.getResolution().getResolution() == request.resolution
                newStateRoom.getResolution().remark == request.remark
                newStateRoom.getResolution().resolvedBy == authContext.getCurrentUser()
                newStateRoom.status == ChatRoomStatus.RESOLVED
                return newStateRoom
        }
    }

    def "test resolution info should come in context"() {
        given: "Initialize inputs"
        mock()
        authContext.getCurrentUser() >> "user1"
        ResolveRoomDto resolutionRequestDto = new ResolveRoomDto()
        resolutionRequestDto.resolutionTypes = Lists.newArrayList("type1")
        resolutionRequestDto.resolution = "resolution1";
        resolutionRequestDto.remark = "thanks";

        def entity = new ArrayList();
        entity.add("json1");
        String jsonString = ChatRoomUtil.objectToJson(entity);
        byte[] bytes = ChatRoomUtil.objectToByteArray(jsonString);

        def mockRoom = mockedChatRoom("1", bytes, Lists.newArrayList(), "user1", ChatRoomStatus.RESOLVED)
        mockRoom.getResolution() >> buildResolution(resolutionRequestDto, "user1")
        roomRepository.findById(_) >> Optional.of(mockRoom)
        when: "Calling resolve room"
        initNewSituationRoomService()
        ChatContext context = service.getChannelContext("1")
        then: "Save room should get called"
        context.getResolution() == resolutionRequestDto.getResolution()
        context.getResolutionTypes() == resolutionRequestDto.getResolutionTypes()
        context.getResolutionRemark() == resolutionRequestDto.getRemark()
        context.getResolvedBy() == "user1"
        new Date(context.getResolvedAt()) != null
    }

    def "test unread should return nothing on remote call exception"() {
        given: "Initialize"
        mock()
        def user = "user1";
        authContext.getCurrentUser() >> user
        def token = proxyTokenMappingWithToken("abcd")
        tokenRepository.findByAppUserId(user) >> token

        def participants = Sets.newHashSet()
        def room = mockedChatRoom("room1", getDummySnapshot(), participants, user, ChatRoomStatus.OPEN)
        ChatRoomParticipant participant = addChatParticipant(room, user, ChatRoomParticipantStatus.JOINED)
        participantRepository.findByUserNameAndStatus(user, ChatRoomParticipantStatus.JOINED) >> Lists.newArrayList(participant)

        restTemplate.exchange(*_) >> {
            throw new RuntimeException("error")
        }

        when: "Calling unread"
        initNewSituationRoomService()
        List response = service.getUnreadCount();
        then: "Shoud match"
        response != null
        response.isEmpty()
    }

    def "test unread should return nothing on remote error"() {
        given: "Initialize"
        mock()
        def user = "user1";
        authContext.getCurrentUser() >> user
        def token = proxyTokenMappingWithToken("abcd")
        tokenRepository.findByAppUserId(user) >> token

        def participants = Sets.newHashSet()
        def room = mockedChatRoom("room1",  getDummySnapshot(), participants, user, ChatRoomStatus.OPEN)
        ChatRoomParticipant participant = addChatParticipant(room, user, ChatRoomParticipantStatus.JOINED)

        participantRepository.findByUserNameAndStatus(user, ChatRoomParticipantStatus.JOINED) >> Lists.newArrayList(participant)

        restTemplate.exchange(*_) >> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"status\":\"error\"}")

        when: "Calling unread"
        initNewSituationRoomService()
        List response = service.getUnreadCount();
        then: "Shoud match"
        response != null
        response.isEmpty()
    }


    def "test unread should succeed"() {
        given: "Initialize"
        mock()
        def user = "user1";
        authContext.getCurrentUser() >> user
        def token = proxyTokenMappingWithToken("abcd")
        tokenRepository.findByAppUserId(user) >> token

        def participants = Sets.newHashSet()
        def room = mockedChatRoom("room1", getDummySnapshot(), participants, user, ChatRoomStatus.OPEN)
        ChatRoomParticipant participant = addChatParticipant(room, user, ChatRoomParticipantStatus.JOINED)

        participantRepository.findByUserNameAndStatus(user, ChatRoomParticipantStatus.JOINED) >> Lists.newArrayList(participant)
        def room1UnreadResponse = Maps.newHashMap();
        room1UnreadResponse.put("team_id", "lct")
        room1UnreadResponse.put("channel_id", "1234")
        room1UnreadResponse.put("msg_count", 5)
        room1UnreadResponse.put("mention_count", 0)

        restTemplate.exchange(*_) >> ResponseEntity.status(HttpStatus.OK).body(room1UnreadResponse)

        when: "Calling unread"
        initNewSituationRoomService()
        List response = service.getUnreadCount();
        then: "Shoud match"
        response != null
        response.size() == 1
        response[0].get("team_id") == "lct"
        response[0].get("channel_id") == "1234"
        response[0].get("msg_count") == 5
    }

    def "test channel creation adds participants with unique ID"() {
        given:
        mock()
        def channel = new ChatRoomCreateDto();
        def user = "1"
        def ptm1 = proxyTokenMapping("1", "remote_user1", "token1");
        def ptm2 = proxyTokenMapping("2", "remote_user2", "token1");
        def participants = Sets.newHashSet();
        def room = Mock(ChatRoom);
        room.getCreatedBy() >> user
        room.getParticipants() >> participants
        participants.add(addChatParticipant(room, "1", ChatRoomParticipantStatus.PENDING))
        participants.add(addChatParticipant(room, "2", ChatRoomParticipantStatus.PENDING))

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
        then: "Should succeed and participant ID should be unique"
        1 * roomRepository.save({
            ChatRoom chatroom ->
                chatroom.getParticipants().getAt(0).getId() == "1" + "-" + chatroom.getId()
        })
    }

    def "test channel creation status should be OPEN"() {
        given:
        mock()
        def channel = new ChatRoomCreateDto();
        def user = "1"
        def ptm = proxyTokenMapping("1", "remote_user1", "token1");
        def participants = Sets.newHashSet();
        def room = Mock(ChatRoom);
        room.getCreatedBy() >> user
        room.getParticipants() >> participants
        participants.add(addChatParticipant(room, "1", ChatRoomParticipantStatus.PENDING))

        authContext.getCurrentUser() >> user

        tokenRepository.findByAppUserId("1") >> ptm

        roomRepository.findById(_ as String) >> Optional.of(room)
        tokenRepository.save(_) >> ptm

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
        then: "Should succeed and participant ID should be unique"
        1 * roomRepository.save({
            ChatRoom chatroom ->
                chatroom.status == ChatRoomStatus.OPEN
        })
    }


    def initNewSituationRoomService() {
        service = new SituationRoomServiceImpl(authContext,
                roomRepository,
                tokenRepository,
                participantRepository,
                generator,
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
        generator = Mock(UniqueRoomNameGenerator)
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

    def addChatParticipant(def chatRoom, def userName, def status) {
        ChatRoomParticipant p = new ChatRoomParticipant()
        p.setUserName(userName);
        p.setRoom(chatRoom)
        p.status = status;
        chatRoom.getParticipants().add(p)
        return p
    }



    def mockedChatRoom(def roomId, def snapshot, def participants, def createdBy, def roomStatus) {
        def room = Mock(ChatRoom)
        room.getId() >> roomId;
        room.getContexts() >> snapshot
        room.getCreationDate() >> new Date()
        room.getLastPostAt() >> new Date()
        room.getLmd() >> new Date()
        room.getParticipants() >> participants
        room.getCreatedBy() >> createdBy
        room.getStatus() >> roomStatus;
        room.getRoomName() >> "test1"
        return room;
    }

    def byte[] getDummySnapshot() {
        def entity = new ArrayList();
        entity.add("json1");
        String jsonString = ChatRoomUtil.objectToJson(entity);
        byte[] bytes = ChatRoomUtil.objectToByteArray(jsonString);
        bytes
    }

    def buildResolution(ResolveRoomDto request, String resolveBy) {
        ChatRoomResolution resolution = new ChatRoomResolution()
        resolution.setResolvedBy(resolveBy)
        resolution.setDate(new Date())
        resolution.setTypes(request.getResolutionTypes())
        resolution.setResolution(request.getResolution())
        resolution.setRemark(request.getRemark());
        return resolution;
    }
}
