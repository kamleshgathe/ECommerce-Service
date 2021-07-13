/*
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.service.impl

import com.jda.dct.chatservice.dto.upstream.EmailParticipantsDto
import com.jda.dct.chatservice.dto.upstream.ParticipantProfileDto
import com.jda.dct.chatservice.enums.EmailTemplateEnum
import com.jda.dct.chatservice.service.impl.EmailServiceImpl
import com.jda.dct.email.BaseEmailNotifier
import spock.lang.Shared
import spock.lang.Specification

class EmailServiceImplSpec extends Specification {
    @Shared
    EmailServiceImpl emailService
    @Shared
    BaseEmailNotifier baseEmailNotifier

    def setupSpec() {
        baseEmailNotifier = Mock(BaseEmailNotifier){
            sendEmail(_)>> true
        }
        emailService = new EmailServiceImpl()
        emailService.baseEmailNotifier = baseEmailNotifier
    }

    def "test sendSituationRoomEmailNotification"() {
        given:
        List<ParticipantProfileDto> receivers = new ArrayList<>()
        ParticipantProfileDto profile1 = new ParticipantProfileDto("id1", "receiver1 full name")
        ParticipantProfileDto profile2 = new ParticipantProfileDto("id2", "receiver2 full name")
        receivers.add(profile1)
        receivers.add(profile2)
        EmailParticipantsDto emailParticipants = new EmailParticipantsDto(roomName, "invitteeName", "creatorName", receivers)
        EmailTemplateEnum emailTemplateEnum = templateEnum
        when:
        boolean result = emailService.sendSituationRoomEmailNotification(emailParticipants, emailTemplateEnum)
        then:
            result == true

        where:
        templateEnum                                | roomName
        EmailTemplateEnum.OPEN_SITUATION_ROOM       | 'testRoomName1'
        EmailTemplateEnum.RESOLVED_SITUATION_ROOM   | 'testRoomName2'
    }
}
