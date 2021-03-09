/**
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.dto.downstream;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateChannelDto {
    private String teamId;
    private String name;
    private String displayName;
    private String purpose;
    private String header;
    private String roomType;

    @JsonProperty("team_id")
    public String getTeamId() {
        return teamId;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("display_name")
    public String getDisplayName() {
        return displayName;
    }

    @JsonProperty("purpose")
    public String getPurpose() {
        return purpose;
    }

    @JsonProperty("header")
    public String getHeader() {
        return header;
    }

    @JsonProperty("type")
    public String getRoomType() {
        return roomType;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public void setName(String name) {
        this.name = name;
        setDisplayName(name);
    }

    private void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CreateChannelDto{");
        sb.append("teamId='").append(teamId).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", displayName='").append(displayName).append('\'');
        sb.append(", purpose='").append(purpose).append('\'');
        sb.append(", header='").append(header).append('\'');
        sb.append(", roomType='").append(roomType).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
