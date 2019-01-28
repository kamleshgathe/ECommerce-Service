/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.dto.upstream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatContext {
    private String name;
    private String situationType;
    private String entityType;
    private String purpose;
    private String roomType = "P";
    private List<String> participants;
    private Object entity;

    @JsonCreator
    public ChatContext() {
    }

    @JsonProperty(value = "name", required = true)
    public String getName() {
        return name;
    }

    @JsonProperty(value = "purpose", required = true)
    public String getPurpose() {
        return purpose;
    }

    @JsonProperty(value = "type")
    public String getType() {
        return roomType;
    }

    @JsonProperty(value = "situation_type")
    public String getSituationType() {
        return situationType;
    }

    @JsonProperty(value = "entity_type", required = true)
    public String getEntityType() {
        return entityType;
    }

    @JsonProperty(value = "participants")
    public List<String> getParticipants() {
        return participants;
    }

    @JsonProperty(value = "entity")
    public Object getEntity() {
        return entity;
    }

    public void setName(String name) {
        this.name = name;
    }


    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }


    public void setType(String type) {
        this.roomType = type;
    }

    public void setSituationType(String situationType) {
        this.situationType = situationType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public void setEntity(Object entity) {
        this.entity = entity;
    }


    @JsonIgnore
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ChatRoomCreateDto{");
        sb.append(", name='").append(name).append('\'');
        sb.append(", situationType='").append(situationType).append('\'');
        sb.append(", entityType='").append(entityType).append('\'');
        sb.append(", purpose='").append(purpose).append('\'');
        sb.append(", roomType='").append(roomType).append('\'');
        sb.append(", participants=").append(participants);
        sb.append('}');
        return sb.toString();
    }
}
