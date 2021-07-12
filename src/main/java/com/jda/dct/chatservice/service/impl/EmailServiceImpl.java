/**
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.service.impl;

import com.jda.dct.chatservice.dto.upstream.EmailParticipantsDto;
import com.jda.dct.chatservice.dto.upstream.ParticipantProfileDto;
import com.jda.dct.chatservice.enums.EmailTemplateEnum;
import com.jda.dct.chatservice.service.EmailService;
import com.jda.dct.email.BaseEmailNotifier;
import com.jda.dct.email.EmailRequest;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final static String OPEN_SITUATION_ROOM_SUBJECT_TEMPLATE = "%s invited you to collaborate";
    private final static String RESOLVED_SITUATION_ROOM_SUBJECT_TEMPLATE = "%s has been handled";

    @Value("${lctUrl:}")
    protected String lctUrl;
    @Autowired
    protected BaseEmailNotifier baseEmailNotifier;

    /**
     * Send open situation room email notification to participant.
     *
     * @param emailParticipants object to store all information which is needed.
     * @param emailTemplateEnum
     * @return
     */
    @Override
    @Async("emailThreadPoolTaskExecutor")
    public boolean sendSituationRoomEmailNotification(EmailParticipantsDto emailParticipants,
                                                      EmailTemplateEnum emailTemplateEnum) {
        String emailSubject = null;
        switch (emailTemplateEnum) {
            case OPEN_SITUATION_ROOM:
                emailSubject = String.format(OPEN_SITUATION_ROOM_SUBJECT_TEMPLATE, emailParticipants.getInviteeFullname());
                break;
            case RESOLVED_SITUATION_ROOM:
                emailSubject = String.format(RESOLVED_SITUATION_ROOM_SUBJECT_TEMPLATE, emailParticipants.getRoomName());
                break;
        }

        return sendEmailNotification(emailParticipants, emailSubject, emailTemplateEnum);
    }

    private boolean sendEmailNotification(EmailParticipantsDto emailParticipants, String emailSubject,
                                          EmailTemplateEnum templateEnum) {
        LOGGER.info("sendEmailNotification with subject: {}, template: {}, lct url: {}",
                emailSubject, templateEnum.getTemplateName(), lctUrl);
        String templateName = templateEnum.getTemplateName();
        for (ParticipantProfileDto profile : emailParticipants.getReceivers()) {
            EmailRequest emailRequest = new EmailRequest(profile.getUserId(), emailSubject, templateName);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("fullName", profile.getFullName());
            parameters.put("inviteeFullName", emailParticipants.getCreatorFullname());
            parameters.put("situationRoomName", emailParticipants.getRoomName());
            parameters.put("creatorFullName", emailParticipants.getCreatorFullname());
            parameters.put("lctUrl", lctUrl);

            baseEmailNotifier.prepareEmailContent(emailRequest, parameters);
            try {
                baseEmailNotifier.sendEmail(emailRequest);
            } catch (Exception e) {
                LOGGER.error("send email failed:" + profile.getUserId(), e);
            }
        }
        return true;
    }
}
