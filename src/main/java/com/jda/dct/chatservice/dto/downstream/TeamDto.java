/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.dto.downstream;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TeamDto {


    private String userId;
    private String teamId;

    @JsonProperty("user_id")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @JsonProperty("team_id")
    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TeamDto{");
        sb.append("userId='").append(userId).append('\'');
        sb.append(", teamId='").append(teamId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
