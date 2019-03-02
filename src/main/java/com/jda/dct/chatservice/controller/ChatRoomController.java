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
import com.jda.dct.chatservice.dto.upstream.ResolveRoomDto;
import com.jda.dct.chatservice.dto.upstream.TokenDto;
import com.jda.dct.chatservice.service.SituationRoomService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/chat")
public class ChatRoomController {

    private SituationRoomService service;

    public ChatRoomController(@Autowired SituationRoomService service) {
        Assert.notNull(service, "Situation service can't be null");
        this.service = service;
    }

    @GetMapping(value = "/token",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenDto> getAccessToken() {
        return ResponseEntity.ok(service.getSessionToken());
    }

    @GetMapping(value = "/channels",
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ChatContext>> getChannels(@RequestParam(value = "by",required = false) String by,
                                                         @RequestParam(value = "type", required = false) String type) {
        return ResponseEntity.ok(service.getChannels(by,type));
    }

    @PostMapping(value = "/posts",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> postMessageToChannel(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(service.postMessage(request));
    }

    @PostMapping(value = "/channels",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> addNewChannel(@RequestBody ChatRoomCreateDto request) {

        return ResponseEntity.ok(service.createChannel(request));
    }

    @PostMapping(value = "/channels/{channel_id}/members",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> inviteUsers(@PathVariable("channel_id") String channelId,
                                                           @RequestBody AddUserToRoomDto request) {
        return ResponseEntity.ok(service.inviteUsers(channelId, request));
    }

    @DeleteMapping(value = "/channels/{channel_id}/members/{user_id}",
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> removeUser(@PathVariable("channel_id") String channelId,
                                                           @PathVariable("user_id") String userId) {
        return ResponseEntity.ok(service.removeParticipant(channelId, userId));
    }

    @GetMapping(value = "/channels/{channel_id}/context",
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatContext> getChatRoomContext(@PathVariable("channel_id") String channelId) {
        return ResponseEntity.ok(service.getChannelContext(channelId));
    }

    @PutMapping(value = "/channels/{channel_id}/join",
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> join(@PathVariable("channel_id") String channelId) {
        return ResponseEntity.ok(service.acceptInvitation(channelId));
    }

    @GetMapping(value = "/channels/unReadCount",
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getUserUnreadCount() {
        return ResponseEntity.ok(service.getUnreadCount());
    }

    @PostMapping(value = "/channels/{channel_id}/resolve",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatContext> resolve(@PathVariable("channel_id") String roomId,
                                               @RequestBody ResolveRoomDto resolveRequest) {
        return ResponseEntity.ok(service.resolve(roomId, resolveRequest));
    }
}
