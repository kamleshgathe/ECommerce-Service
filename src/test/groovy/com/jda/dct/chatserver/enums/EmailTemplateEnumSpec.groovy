/*
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.enums

import com.jda.dct.chatservice.enums.EmailTemplateEnum
import spock.lang.Specification

class EmailTemplateEnumSpec extends Specification {
    def "test getTemplateName"() {
        given:
        EmailTemplateEnum templateEnum = EmailTemplateEnum.OPEN_SITUATION_ROOM
        when:
        String name = templateEnum.templateName
        then:
        name == 'openRoom'
    }
}
