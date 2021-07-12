/**
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.service;


import com.jda.dct.chatservice.dto.upstream.EmailParticipantsDto;
import com.jda.dct.chatservice.enums.EmailTemplateEnum;

public interface EmailService {

    boolean sendSituationRoomEmailNotification(EmailParticipantsDto emailParticipants,
                                               EmailTemplateEnum emailTemplateEnum);
}
