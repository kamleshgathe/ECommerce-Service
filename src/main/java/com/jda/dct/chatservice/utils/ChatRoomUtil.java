/**
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

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
            throw new IllegalArgumentException("Exception occurred when converting from object to byte");
        }
    }

    /**
     * Converts byte array to list of chat object.
     *
     * @param bytes Bytes of chats.
     * @return List of chat objects.
     */
    public static Object byteArrayToObject(byte[] bytes) {
        try {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                return ois.readObject();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
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
            throw new IllegalArgumentException("Unable to create snapshot object");
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
            throw new IllegalArgumentException("Unable to restore snapshot as object");
        }
    }
}
