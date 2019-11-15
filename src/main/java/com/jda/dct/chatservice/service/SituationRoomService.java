/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */

package com.jda.dct.chatservice.service;

import com.jda.dct.chatservice.dto.upstream.AddUserToRoomDto;
import com.jda.dct.chatservice.dto.upstream.ChatContext;
import com.jda.dct.chatservice.dto.upstream.ChatRoomCreateDto;
import com.jda.dct.chatservice.dto.upstream.ResolveRoomDto;
import com.jda.dct.chatservice.dto.upstream.TokenDto;
import com.jda.dct.domain.Attachment;
import com.jda.luminate.ingest.util.InputStreamWrapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;


public interface SituationRoomService {
    TokenDto getSessionToken();

    List<ChatContext> getChannels(String by,String type, String requestQueryParam);

    ChatContext getChannelContext(String roomId);

    Map<String, Object> createChannel(ChatRoomCreateDto request);

    Map<String, Object> removeChannel(String roomId);

    Map<String, Object> inviteUsers(String roomId, AddUserToRoomDto users);

    Map<String,Object> removeParticipant(String roomId,String targetUser);

    Map<String, Object> postMessage(Map<String, Object> chat);

    Map<String, Object> acceptInvitation(String roomId);

    List<Map<String,Object>> getUnreadCount();

    ChatContext resolve(String roomId,ResolveRoomDto request);

    Map<String, Object> readResolvedChannel();

    /**
     * Method is return the chat room details based on given search text.
     *
     * @param requestParams         -- input value contain search text and object id
     * @return List    -- response will be search result in form of chat context object list
     */

    List<ChatContext> searchChannels(Map<String, String> requestParams);

    List<Attachment> upload(String roomId, MultipartFile file, String comment);

    InputStreamWrapper getDocument(String roomId, String documentId) throws IOException;

    void deleteAttachment(String roomId, String documentId) throws IOException;
}
