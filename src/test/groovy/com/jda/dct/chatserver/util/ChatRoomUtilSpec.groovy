/*
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.util

import com.jda.dct.chatservice.utils.ChatRoomUtil
import org.assertj.core.util.Lists
import org.assertj.core.util.Maps
import spock.lang.Specification

public class ChatRoomUtilSpec extends Specification {

    def "should create byte array"() {
        given: " initialize object"
        List<Map<String, Object>> objects = Lists.newArrayList()
        objects.add(Maps.newHashMap("key1", "value"));
        when: "converting to byte array"
        ChatRoomUtil.objectToByteArray(objects);
        then: "should not throw any exception"
        noExceptionThrown()
    }

    def "test object from byte array"() {
        given: "Initialize byte array"
        List<Map<String, Object>> objects = Lists.newArrayList()
        objects.add(Maps.newHashMap("key1", "value"));
        byte[] bytes = ChatRoomUtil.objectToByteArray(objects);

        when: "Converting to object"
        Object convertedObject = ChatRoomUtil.byteArrayToObject(bytes);

        then: "Object should match"
        objects == convertedObject
    }
}
