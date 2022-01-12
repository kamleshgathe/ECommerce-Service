/*
 * Copyright © 2022, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.service

import com.jda.dct.chatservice.service.UniqueRoomNameGenerator
import spock.lang.Specification

class UniqueChatRoomGeneratorSpec extends Specification {
    def "test random string should be of valid length"() {
        when: "Getting next random name"
        UniqueRoomNameGenerator generator = new UniqueRoomNameGenerator()
        def str = generator.next()
        then: "Should not be null and of valid length"
        str != null
        str.length() == 22
        !str.contains(" ")
    }
}
