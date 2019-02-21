/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.dto.upstream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class ResolveRoomDto {
    private List<String> resolutionTypes;
    private String resolution;
    private String remark;

    @JsonCreator
    public ResolveRoomDto() {
        //this has been kept blank to get initialize by jackson
    }

    public List<String> getResolutionTypes() {
        return resolutionTypes;
    }

    public String getResolution() {
        return resolution;
    }

    public String getRemark() {
        return remark;
    }

    @JsonProperty(value = "resolution_type", required = true)
    @NotNull(message = "Resolution type can't be null")
    @NotEmpty(message = "Resolution type can't be empty")
    public void setResolutionTypes(List<String> resolutionTypes) {
        this.resolutionTypes = resolutionTypes;
    }

    @JsonProperty(value = "resolution", required = true)
    @NotNull(message = "Resolution can't be null or empty")
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    @JsonProperty(value = "resolution_remark", required = true)
    @NotNull(message = "Resolution remark can't be null or empty")
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResolveRoomDto{");
        sb.append("resolutionTypes='").append(resolutionTypes).append('\'');
        sb.append(", resolution='").append(resolution).append('\'');
        sb.append(", remark='").append(remark).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
