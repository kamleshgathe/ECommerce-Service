/**
 * Copyright © 2020, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.service;

import java.security.SecureRandom;
import java.util.Random;

import org.springframework.stereotype.Component;


/**
 * UniqueIdentifierGenerator will provide random string.
 */
@Component
public class UniqueRoomNameGenerator {

    private static final int DEFAULT_NAME_LENGTH = 22;
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private Random rng = new SecureRandom();

    private char randomChar() {
        return ALPHABET.charAt(rng.nextInt(ALPHABET.length()));
    }

    /**
     * This function provide random string of default length defined in DEFAULT_NAME_LENGTH.
     *
     * @return random string.
     */
    public String next() {
        StringBuilder sb = new StringBuilder();
        int length = DEFAULT_NAME_LENGTH;
        while (length > 0) {
            length--;
            sb.append(randomChar());
        }
        return sb.toString();
    }

}
