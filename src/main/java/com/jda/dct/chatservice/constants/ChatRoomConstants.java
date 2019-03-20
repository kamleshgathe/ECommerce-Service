/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */

package com.jda.dct.chatservice.constants;

public final class ChatRoomConstants {
    public static final int MAX_REMOTE_USERNAME_LENGTH = 30;
    public static final String FILTER_BY_USER = "user";
    public static final String MATTERMOST_POSTS = "/posts";
    public static final String MATTERMOST_USERS = "/users";
    public static final String MATTERMOST_CHANNELS = "/channels";

    public static final String PASSTHROUGH_PREFIX = "/chat/passthrough";

    private ChatRoomConstants() {
        //private constructor to make to avoid mistaken initialization
    }
}