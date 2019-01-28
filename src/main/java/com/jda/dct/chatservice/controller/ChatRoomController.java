/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */

package com.jda.dct.chatservice.controller;

import com.jda.dct.chatservice.dto.upstream.AddUserToRoomDto;
import com.jda.dct.chatservice.dto.upstream.ChatContext;
import com.jda.dct.chatservice.dto.upstream.ChatRoomCreateDto;
import com.jda.dct.chatservice.dto.upstream.TokenDto;
import com.jda.dct.chatservice.service.SituationRoomService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/chat")
public class ChatRoomController {

    private SituationRoomService service;

    public ChatRoomController(@Autowired SituationRoomService service) {
        Assert.notNull(service, "Situation service can't be null");
        this.service = service;
    }

    @RequestMapping(value = "/token",
        method = RequestMethod.GET,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenDto> getAccessToken() {
        return ResponseEntity.ok(service.getSessionToken());
    }

    @RequestMapping(value = "/posts",
        method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> postMessageToChannel(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(service.postMessage(request));
    }

    @RequestMapping(value = "/channels",
        method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> addNewChannel(@RequestBody ChatRoomCreateDto request) {

        return ResponseEntity.ok(service.createChannel(request));
    }

    @RequestMapping(value = "/channels/{channel_id}/members",
        method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> addUsersToChannels(@PathVariable("channel_id") String channelId,
                                                                  @RequestBody AddUserToRoomDto request) {
        return ResponseEntity.ok(service.addUserToChannel(channelId, request));
    }


    @RequestMapping(value = "/channels/{channel_id}/context",
        method = RequestMethod.GET,
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatContext> getChatRoomContext(@PathVariable("channel_id") String channelId) {
        return ResponseEntity.ok(service.getChannelContext(channelId));
    }

}
