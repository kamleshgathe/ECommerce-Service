/*
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */

package com.jda.dct.chatservice.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jda.dct.chatservice.exception.ChatException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;


@Component
public class ChatRoomUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private ChatRoomUtil() {
    }

    /**
     * Utility to convert List of chats to byte array.
     *
     * @param objects List of chats.
     * @return byte array for chats.
     */

    public static byte[] objectToByteArray(Object objects) {
        try {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(objects);
                oos.flush();
                return bos.toByteArray();
            }
        } catch (IOException e) {
            throw new ChatException(ChatException.ErrorCode.OBJECT_CONVERSION_ERROR);
        }
    }

    /**
     * Converts byte array to list of chat object.
     *
     * @param bytes Bytes of chats.
     * @return List of chat objects.
     */
    public static Object byteArrayToObject(byte[] bytes) {
        Set whiteList = new HashSet<>(Arrays.asList(
                "Object",
                "ChatContext",
                "java.util.ArrayList",
                "java.util.HashMap",
                "java.util.LinkedHashMap"
        ));
        try {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
                try (WhitelistedObjectInputStream ois = new WhitelistedObjectInputStream(bis, whiteList)) {
                    return ois.readObject();
                }
            }
        } catch (Exception e) {
            throw new ChatException(ChatException.ErrorCode.BYTE_CONVERSION_ERROR, e.getMessage());
        }
    }

    /**
     * Convert given object to json string.
     *
     * @param object any object.
     * @return String representing json form or a object
     */
    public static String objectToJson(List object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ChatException(ChatException.ErrorCode.CREATE_SNAPSHOT_ERROR);
        }
    }

    /**
     * Convert json string to object.
     *
     * @param json String representing json.
     * @return Object representing json string.
     */
    public static List jsonToObject(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, List.class);
        } catch (IOException e) {
            throw new ChatException(ChatException.ErrorCode.RESTORE_SNAPSHOT_ERROR);
        }
    }

    /**
     * Construct the URI for given base and context path.
     *
     * @param baseUrl Base url.
     * @param cxtPath context path.
     * @return URI.
     */
    public static URI buildUrlString(String baseUrl, String cxtPath) {
        return buildUrlString(baseUrl, cxtPath, null);
    }

    /**
     * Construct the URI for given base and context path along with query params.
     *
     * @param baseUrl Base url.
     * @param cxtPath context path.
     * @param params  query params.
     * @return URI
     */
    public static URI buildUrlString(String baseUrl, String cxtPath, String params) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path(cxtPath).query(params).build(true).toUri();
    }
}
