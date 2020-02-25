/**
 * Copyright © 2020, JDA Software Group, Inc. ALL RIGHTS RESERVED.
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
import com.jda.dct.chatservice.utils.AssertUtil;
import com.jda.dct.domain.Attachment;
import com.jda.dct.ignitecaches.springimpl.Tenants;
import com.jda.luminate.common.base.ResponseDataWrapper;
import com.jda.luminate.ingest.util.InputStreamWrapper;
import com.jda.luminate.security.contexts.AuthContext;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotEmpty;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/chat")
public class ChatRoomController {


    private SituationRoomService service;
    private AuthContext authContext;

    /**
     * Chat room controller constructor.
     * @param service Situation room service class.
     * @param authContext Auth context.
     */
    public ChatRoomController(@Autowired SituationRoomService service,
                              @Autowired AuthContext authContext) {
        AssertUtil.notNull(service, "Situation service can't be null");
        this.service = service;
        this.authContext = authContext;
    }

    /**
     * Return the token of user.
     * @return json representation of team and token.
     */
    @GetMapping(value = "/token",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenDto> getAccessToken() {
        Tenants.setCurrent(authContext.getCurrentTid());
        return ResponseEntity.ok(service.getSessionToken());
    }

    /**
     * Returns list of channel for logged in user.
     * @param by user.
     * @param type type of room.
     * @return return the channels in form of json
     */
    @GetMapping(value = "/channels",
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ChatContext>> getChannels(
            @RequestParam(value = "by", required = false) String by,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "objectIds", required = false) String objectIds) {

        Tenants.setCurrent(authContext.getCurrentTid());
        return ResponseEntity.ok(service.getChannels(by, type, objectIds));
    }

    @PostMapping(value = "/posts",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> postMessageToChannel(@RequestBody Map<String, Object> request) {
        Tenants.setCurrent(authContext.getCurrentTid());
        return ResponseEntity.ok(service.postMessage(request));
    }

    /**
     * Add new chat room into system.
     * @param request chat room request.
     * @return created chat room information.
     */
    @PostMapping(value = "/channels",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> addNewChannel(@RequestBody ChatRoomCreateDto request) {

        Tenants.setCurrent(authContext.getCurrentTid());
        return ResponseEntity.ok(service.createChannel(request));
    }

    /**
     * Delete a particular chat Room by room Id.
     * @param channelId chat room Id
     * @return Delete status of chat room
     */
    @DeleteMapping(value = "/channels/{channel_id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> deleteChannel(@PathVariable("channel_id") String channelId) {

        Tenants.setCurrent(authContext.getCurrentTid());
        return ResponseEntity.ok(service.removeChannel(channelId));
    }

    @PostMapping(value = "/channels/{channel_id}/members",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> inviteUsers(@PathVariable("channel_id") String channelId,
                                                           @RequestBody AddUserToRoomDto request) {
        Tenants.setCurrent(authContext.getCurrentTid());
        return ResponseEntity.ok(service.inviteUsers(channelId, request));
    }

    @PostMapping(value = "/channels/{channel_id}/members/{user_id}/delete",
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> removeUser(@PathVariable("channel_id") String channelId,
                                                          @PathVariable("user_id") String userId) {
        Tenants.setCurrent(authContext.getCurrentTid());
        return ResponseEntity.ok(service.removeParticipant(channelId, userId));
    }

    @GetMapping(value = "/channels/{channel_id}/context",
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatContext> getChatRoomContext(@PathVariable("channel_id") String channelId) {
        Tenants.setCurrent(authContext.getCurrentTid());
        return ResponseEntity.ok(service.getChannelContext(channelId));
    }

    @PutMapping(value = "/channels/{channel_id}/join",
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> join(@PathVariable("channel_id") String channelId) {
        Tenants.setCurrent(authContext.getCurrentTid());
        return ResponseEntity.ok(service.acceptInvitation(channelId));
    }

    @GetMapping(value = "/channels/unReadCount",
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getUserUnreadCount() {
        Tenants.setCurrent(authContext.getCurrentTid());
        return ResponseEntity.ok(service.getUnreadCount());
    }

    @PostMapping(value = "/channels/{channel_id}/resolve",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatContext> resolve(@PathVariable("channel_id") String roomId,
                                               @RequestBody ResolveRoomDto resolveRequest) {
        Tenants.setCurrent(authContext.getCurrentTid());
        return ResponseEntity.ok(service.resolve(roomId, resolveRequest));
    }

    @PutMapping(value = "/channels/readResolved",
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> readResolvedChannel() {
        Tenants.setCurrent(authContext.getCurrentTid());
        return ResponseEntity.ok(service.readResolvedChannel());
    }


    /**
     * Method is used to search with in the SR.
     *
     * @param requestParams     -- input value contain search text and object id
     * @return ResponseEntity    -- response will be search result in form of json
     */
    @GetMapping(value = "/channels/search",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ChatContext>> searchChannels(
            @RequestParam Map<String, String> requestParams) {
        Tenants.setCurrent(authContext.getCurrentTid());
        return ResponseEntity.ok(service.searchChannels(requestParams));
    }

    /**
     * This method helps to Upload attachment for a particular ChatRoom .
     */

    @PostMapping(value = "/channels/{channel_id}/uploadAttachment",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDataWrapper<Attachment>> upload(@PathVariable("channel_id") String roomId,
                                           @RequestParam(value = "file", required = false) MultipartFile file,
                                           @RequestParam(value = "comment", required = false) String comment) {
        ResponseDataWrapper<Attachment> responseDataWrapper = new ResponseDataWrapper();
        Tenants.setCurrent(authContext.getCurrentTid());
        responseDataWrapper.setData(service.upload(roomId, file, comment));
        return ResponseEntity.ok().body(responseDataWrapper);
    }

    /**
     * This method helps to download attachment for a particular ChatRoom.
     */
    @GetMapping(value = "/channels/{channel_id}/attachments/{documentId}")
    public ResponseEntity<InputStreamResource> downloadAttachment(@PathVariable("channel_id") String roomId,
                                                                  @PathVariable final String documentId)
            throws IOException {
        Tenants.setCurrent(authContext.getCurrentTid());
        InputStreamWrapper streamWrapper = service.getDocument(roomId, documentId);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Disposition", "attachment; filename=" + streamWrapper.getFileName());
        httpHeaders.add("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok()
                .headers(httpHeaders).body(new InputStreamResource(streamWrapper.getInputStream()));
    }

    /**
     * Delete the attachment of the ChatRoom.
     *
     * @param channelId     ChatRoom id.
     * @param documentId     documentId.
     * @return
     */
    @DeleteMapping(value = "/channels/{channel_id}/attachments/{documentId}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDataWrapper<T>> deleteAttachment(
            @NotEmpty @PathVariable("channel_id") String channelId,
            @NotEmpty @PathVariable String documentId) throws IOException {

        Tenants.setCurrent(authContext.getCurrentTid());
        // Delete attachment and updated entity object and Delete from store.
        service.deleteAttachment(channelId, documentId);
        ResponseDataWrapper<T> responseDataWrapper = new ResponseDataWrapper<>();
        responseDataWrapper.setCount(1);
        return ResponseEntity.ok().body(responseDataWrapper);
    }
}
