/**
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.exception;

import com.jda.dct.domain.exceptions.DctException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Wrapper of Chat server illegal argument exception with bad request status.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class InvalidChatRequest extends DctException {

    public InvalidChatRequest(String message) {
        super(HttpStatus.BAD_REQUEST.toString(), "chat", message, null, "");
    }
}
