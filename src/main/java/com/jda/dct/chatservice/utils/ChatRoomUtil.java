/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */

package com.jda.dct.chatservice.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ChatRoomUtil {

    private ChatRoomUtil() {

    }

    /**
     * Utility to convert List of chats to byte array.
     *
     * @param chats List of chats.
     * @return byte array for chats.
     */

    public static byte[] objectToByteArray(Object chats) {
        byte[] bytes = null;
        try {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(chats);
                oos.flush();
                bytes = bos.toByteArray();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Exception occurred when converting from object to byte");
        }
        return bytes;
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

}
