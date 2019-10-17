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
import com.jda.dct.domain.ChatRoomParticipantStatus;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatContext {
    private String id;
    private String name;
    private String displayName;
    private String teamId;
    private String roomType = "P";
    private String header;
    private String purpose;
    private String situationType;
    private String entityType;
    private String createdBy;
    private String roomStatus;
    private List<String> resolution;
    private String resolutionRemark;
    private String resolvedBy;

    private ChatRoomParticipantStatus yourStatus;

    private long totalMessageCount;

    private long createdAt;
    private long updatedAt;
    private long deletedAt;
    private long lastPostAt;
    private long expiredAt;
    private long extraUpdateAt;
    private long resolvedAt;

    private List<String> participants;
    private Object entity;

    @JsonCreator
    public ChatContext() {
        //this has been kept blank to get initialize by jackson
    }

    @JsonProperty(value = "id", required = true)
    public String getId() {
        return id;
    }

    @JsonProperty(value = "name", required = true)
    public String getName() {
        return name;
    }

    @JsonProperty(value = "purpose", required = true)
    public String getPurpose() {
        return purpose;
    }

    @JsonProperty(value = "display_name", required = true)
    public String getDisplayName() {
        return displayName;
    }

    @JsonProperty(value = "type")
    public String getType() {
        return roomType;
    }

    @JsonProperty(value = "team_id")
    public String getTeamId() {
        return teamId;
    }

    @JsonProperty(value = "header")
    public String getHeader() {
        return header;
    }

    @JsonProperty(value = "situation_type")
    public String getSituationType() {
        return situationType;
    }

    @JsonProperty(value = "entity_type", required = true)
    public String getEntityType() {
        return entityType;
    }

    @JsonProperty(value = "room_status", required = true)
    public String getRoomStatus() {
        return roomStatus;
    }

    @JsonProperty(value = "resolution")
    public List<String> getResolution() {
        return resolution;
    }

    @JsonProperty("resolution_remark")
    public String getResolutionRemark() {
        return resolutionRemark;
    }

    @JsonProperty("resolved_by")
    public String getResolvedBy() {
        return resolvedBy;
    }

    @JsonProperty(value = "status")
    public ChatRoomParticipantStatus getYourStatus() {
        return yourStatus;
    }

    @JsonProperty(value = "total_msg_count")
    public long getTotalMessageCount() {
        return totalMessageCount;
    }

    @JsonProperty(value = "creator_id")
    public String getCreatedBy() {
        return createdBy;
    }

    @JsonProperty(value = "created_at")
    public long getCreatedAt() {
        return createdAt;
    }

    @JsonProperty(value = "updated_at")
    public long getUpdatedAt() {
        return updatedAt;
    }

    @JsonProperty(value = "deleted_at")
    public long getDeletedAt() {
        return deletedAt;
    }

    @JsonProperty(value = "last_post_at")
    public long getLastPostAt() {
        return lastPostAt;
    }

    @JsonProperty(value = "expired_at")
    public long getExpiredAt() {
        return expiredAt;
    }

    @JsonProperty(value = "extra_update_at")
    public long getExtraUpdateAt() {
        return extraUpdateAt;
    }

    @JsonProperty(value = "resolved_at")
    public long getResolvedAt() {
        return resolvedAt;
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

    public void setId(String id) {
        this.id = id;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setYourStatus(ChatRoomParticipantStatus yourStatus) {
        this.yourStatus = yourStatus;
    }

    public void setRoomStatus(String roomStatus) {
        this.roomStatus = roomStatus;
    }

    public void setResolution(List<String> resolution) {
        this.resolution = resolution;
    }

    public void setResolutionRemark(String resolutionRemark) {
        this.resolutionRemark = resolutionRemark;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public void setTotalMessageCount(long totalMessageCount) {
        this.totalMessageCount = totalMessageCount;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeletedAt(long deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setLastPostAt(long lastPostAt) {
        this.lastPostAt = lastPostAt;
    }

    public void setExpiredAt(long expiredAt) {
        this.expiredAt = expiredAt;
    }

    public void setExtraUpdateAt(long extraUpdateAt) {
        this.extraUpdateAt = extraUpdateAt;
    }

    public void setResolvedAt(long resolvedAt) {
        this.resolvedAt = resolvedAt;
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
        final StringBuilder sb = new StringBuilder("ChatContext{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", displayName='").append(displayName).append('\'');
        sb.append(", teamId='").append(teamId).append('\'');
        sb.append(", roomType='").append(roomType).append('\'');
        sb.append(", header='").append(header).append('\'');
        sb.append(", purpose='").append(purpose).append('\'');
        sb.append(", situationType='").append(situationType).append('\'');
        sb.append(", entityType='").append(entityType).append('\'');
        sb.append(", createdBy='").append(createdBy).append('\'');
        sb.append(", totalMessageCount=").append(totalMessageCount);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(", deletedAt=").append(deletedAt);
        sb.append(", lastPostAt=").append(lastPostAt);
        sb.append(", expiredAt=").append(expiredAt);
        sb.append(", extraUpdateAt=").append(extraUpdateAt);
        sb.append(", participants=").append(participants);
        sb.append(", entity=").append(entity);
        sb.append('}');
        return sb.toString();
    }
}
