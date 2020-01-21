/**
 * Copyright © 2020, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.dto.downstream;

public class RoleDto {
    private String roles;

    public RoleDto(String roles) {
        this.roles = roles;
    }

    public String getRoles() {
        return roles;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RoleDto{");
        sb.append("roles='").append(roles).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
