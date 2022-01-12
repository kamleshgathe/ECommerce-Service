/**
 * Copyright © 2022, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.constants;

public final class ChatRoomConstants {
    public static final int MAX_REMOTE_USERNAME_LENGTH = 64;
    public static final String SPECIAL_CHARECTER = "[^A-Za-z0-9]";
    public static final int BOUND = 999999;

    public static final String FILTER_BY_USER = "user";
    public static final String MATTERMOST_POSTS = "/posts";
    public static final String MATTERMOST_USERS = "/users";
    public static final String MATTERMOST_CHANNELS = "/channels";

    public static final String PASSTHROUGH_PREFIX = "/chat/passthrough";

    public static final String INVALID_ROOM_FORMATTED_MSG = "Invalid chat room id %s";
    public static  final String PATH_PREFIX = "Situation Room";
    public static final String PATH_DELIMITER = "/";

    public static final String PERCENT_SIGN = "%";
    public static final String DOMAIN_OBJECT_ID = "objectId";
    public static final String QUOTATION_MARK = "\"";

    public static final String FIRST_NAME =  "firstName";
    public static final String LAST_NAME =  "lastName";
    public static final String USER_NAME =  "userName";
    public static final String SPACE =  " ";
    public static final String EMPTY = "";
    public static final String INVITATION = "You are invited to join ";
    public static final String USER = " by ";
    public static final String INVITE_REQUEST = " invited you to a Situation Room";
    public static final String BIGTEXT = "bigtext";
    public static final boolean TRUE = true;

    private ChatRoomConstants() {
        //private constructor to make to avoid mistaken initialization
    }
}
