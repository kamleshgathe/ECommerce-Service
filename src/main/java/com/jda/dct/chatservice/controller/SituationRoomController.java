/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */

package com.jda.dct.chatservice.controller;

import com.jda.dct.chatservice.dto.upstream.SituationRoomDto;
import com.jda.dct.chatservice.dto.upstream.TokenDto;
import com.jda.dct.chatservice.service.SituationRoomService;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/situationRoom")
public class SituationRoomController {

    private SituationRoomService service;

    public SituationRoomController(@Autowired SituationRoomService service) {
        Assert.notNull(service, "Situation service can't be null");
        this.service = service;
    }

    @RequestMapping(value = "/api/v4/token",
            method = RequestMethod.GET,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokenDto> getAccessToken() {
        return ResponseEntity.ok(service.getSessionToken());
    }

    @RequestMapping(value = "/api/v4/posts",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> postMessageToChannel(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(service.postMessage(request));
    }

    @RequestMapping(value = "/api/v4/channels",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> addNewChannel(@RequestBody SituationRoomDto request) {

        return ResponseEntity.ok(service.createChannel(request));
    }


}
