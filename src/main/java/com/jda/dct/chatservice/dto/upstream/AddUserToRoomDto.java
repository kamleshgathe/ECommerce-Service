/**
 * Copyright © 2020, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.dto.upstream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AddUserToRoomDto {

    private List<String> users;

    @JsonCreator
    public AddUserToRoomDto() {
        // this constructor will be initialize by jackson
    }


    public List<String> getUsers() {
        return users;
    }

    @JsonProperty(value = "users",required = true)
    public void setUsers(List<String> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AddUserToRoomDto{");
        sb.append("users=").append(users);
        sb.append('}');
        return sb.toString();
    }
}
