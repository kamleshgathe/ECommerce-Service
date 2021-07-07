/**
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.dto.upstream;

import java.util.List;

public class EmailParticipantsDto {
    private String roomName;
    private String inviteeFullname;
    private String creatorFullname;
    private List<ParticipantProfileDto> receivers;

    public EmailParticipantsDto(String roomName, String inviteeFullname, String creatorFullname, List<ParticipantProfileDto> receivers) {
        this.roomName = roomName;
        this.inviteeFullname = inviteeFullname;
        this.creatorFullname = creatorFullname;
        this.receivers = receivers;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getInviteeFullname() {
        return inviteeFullname;
    }

    public String getCreatorFullname() {
        return creatorFullname;
    }

    public List<ParticipantProfileDto> getReceivers() {
        return receivers;
    }
}
