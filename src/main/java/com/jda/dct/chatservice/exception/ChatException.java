/*
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.exception;

import com.jda.dct.domain.exceptions.DctException;

public class ChatException extends DctException {
    private static final long serialVersionUID = 1L;

    public ChatException(final ErrorCode errorCode, final Throwable cause, final Object... messageArguments) {
        super(errorCode.getCode(), "chat", errorCode.getMessage(), cause, messageArguments);
    }

    public ChatException(final ErrorCode errorCode, final Object... messageArguments) {
        this(errorCode, null, messageArguments);
    }

    public enum ErrorCode {
        OBJECT_CONVERSION_ERROR("objectconversion", "Exception occurred when converting from object to byte."),
        BYTE_CONVERSION_ERROR("objectconversion", "Exception occurred when converting from byte to object. {0}"),
        CREATE_SNAPSHOT_ERROR("createsnapshot", "Unable to create snapshot object."),
        RESTORE_SNAPSHOT_ERROR("restoresnapshot", "Unable to restore snapshot as object."),
        CHANNEL_NOT_EXISTS("channelnotexists", "Channel {0} does not exists"),
        INVALID_ROOM("invalidroom", "Invalid room"),
        ROOM_NOT_EXISTS("roomnotexists","Room does not exists,wrong room name for post message"),
        INVALID_CHAT_ROOM("invalidchatroom","The room has either been deleted or doesn't exist."),
        PARTICIPANT_NOT_BELONG("participantnotbelong","Participant does not belong to room"),
        USER_NOT_INVITED("usernotinvited","User {0} not invited to room {1}"),
        UNABLE_TO_JOIN_ROOM("unabletojoin","Unable to joined situation room."),
        UNABLE_TO_UPDATE_ROLE("unabletoupdaterole","Unable to update roles."),
        UNABLE_TO_GET_CHANNEL_POSTS("unabletogetchannelposts", "Unable to get posts for room {0}");

        private final String code;
        private final String message;

        ErrorCode(final String code, final String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
