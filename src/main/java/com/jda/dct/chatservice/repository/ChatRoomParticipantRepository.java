/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */

package com.jda.dct.chatservice.repository;

import com.jda.dct.domain.ChatRoomParticipant;
import com.jda.dct.domain.ChatRoomParticipantStatus;
import com.jda.dct.domain.ChatRoomStatus;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRoomParticipantRepository extends CrudRepository<ChatRoomParticipant, String> {
    List<ChatRoomParticipant> findByUserName(String userName);

    List<ChatRoomParticipant> findByUserNameAndStatus(String userName, ChatRoomParticipantStatus status);

    List<ChatRoomParticipant> findByUserNameAndRoomStatus(String userName, ChatRoomStatus status);
}
