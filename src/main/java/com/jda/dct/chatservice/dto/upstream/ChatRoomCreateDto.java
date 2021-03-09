/**
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.dto.upstream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatRoomCreateDto {

    private String name;
    private String situationType;
    private String entityType;
    private String purpose;
    private String header;
    private String roomType = "P";
    private List<String> participants;
    private List<String> objectIds;

    @JsonCreator
    public ChatRoomCreateDto() {
        //this has been kept blank to get initialize by jackson
    }

    public String getName() {
        return name;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getHeader() {
        return header;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getSituationType() {
        return situationType;
    }

    public String getEntityType() {
        return entityType;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public List<String> getObjectIds() {
        return objectIds;
    }


    @JsonProperty(value = "name", required = true)
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty(value = "purpose", required = true)
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    @JsonProperty(value = "header", required = false)
    public void setHeader(String header) {
        this.header = header;
    }

    @JsonProperty(value = "type", required = false)
    public void setType(String type) {
        this.roomType = type;
    }

    @JsonProperty(value = "situation_type", required = true)
    public void setSituationType(String situationType) {
        this.situationType = situationType;
    }

    @JsonProperty(value = "entity_type", required = true)
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    @JsonProperty(value = "participants", required = false)
    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    @JsonProperty(value = "object_ids", required = true)
    public void setObjectIds(List<String> objectIds) {
        this.objectIds = objectIds;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ChatRoomCreateDto{");
        sb.append(", name='").append(name).append('\'');
        sb.append(", situationType='").append(situationType).append('\'');
        sb.append(", entityType='").append(entityType).append('\'');
        sb.append(", purpose='").append(purpose).append('\'');
        sb.append(", header='").append(header).append('\'');
        sb.append(", roomType='").append(roomType).append('\'');
        sb.append(", participants=").append(participants);
        sb.append(", objectIds=").append(objectIds);
        sb.append('}');
        return sb.toString();
    }
}
