/**
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.dto.downstream;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AddParticipantDto {
    private String userId;
    private String postRootId;

    @JsonProperty("user_id")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @JsonProperty("post_root_id")
    public String getPostRootId() {
        return postRootId;
    }

    public void setPostRootId(String rootPostId) {
        this.postRootId = rootPostId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AddParticipantDto{");
        sb.append("userId='").append(userId).append('\'');
        sb.append(", postRootId='").append(postRootId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
