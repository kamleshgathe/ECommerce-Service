/**
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.utils;

import com.jda.dct.chatservice.exception.InvalidChatRequest;
import java.util.List;
import java.util.Map;

public class AssertUtil {


    private AssertUtil() {

    }

    /**
     * Assert util for validating request.
     *
     * @param condition boolean condition value.
     * @param message   message to use for exception if there is error.
     * @throws InvalidChatRequest if {@code expression} is {@code false}
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new InvalidChatRequest(message);
        }
    }


    /**
     * Assert util for validating request.
     *
     * @param object  input object.
     * @param message message to use for exception if there is error.
     * @throws InvalidChatRequest if the object is {@code null}
     */

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new InvalidChatRequest(message);
        }
    }

    /**
     * Assert util for validating request.
     *
     * @param list    input list.
     * @param message message to use for exception if there is error.
     * @throws InvalidChatRequest if the map is {@code null} or contains no entries
     */
    public static void notEmpty(List<String> list, String message) {
        if (list == null || list.isEmpty()) {
            throw new InvalidChatRequest(message);
        }
    }

    /**
     * Assert util for validating request.
     *
     * @param map     input map.
     * @param message message to use for exception if there is error.
     */
    public static void notEmpty(Map<String, Object> map, String message) {
        if (map == null || map.isEmpty()) {
            throw new InvalidChatRequest(message);
        }
    }
}
