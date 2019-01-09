/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.service;

import com.jda.dct.chatservice.dto.upstream.SituationRoomDto;
import com.jda.dct.chatservice.dto.upstream.TokenDto;

import java.util.Map;

public interface SituationRoomService {
    TokenDto getSessionToken();

    Map<String,Object> postMessage(Map<String,Object> chat);

    Map<String,Object> createChannel(SituationRoomDto request);
}
