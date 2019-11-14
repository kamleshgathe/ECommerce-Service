/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.repository;

import com.jda.dct.domain.ChatRoom;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SituationRoomRepository extends CrudRepository<ChatRoom,String> {
    ChatRoom findByRoomName(String roomName);

    @Query(value = "Select * from chat_room cr where json_contains(cr.domain_object_ids, ?1)",
            nativeQuery = true)
    List<ChatRoom> getChannelByObjectId(String objectId);

    @Query(value = "select cr.* from chat_room cr "
            + " join chat_room_participant crp on crp.room_id = cr.id "
            + " where crp.user_name = :currentUser "
            + " and ( upper(cr.room_name) like :searchTerm "
            + " or upper(cr.description) like :searchTerm "
            + " or upper(cr.situation_type) like :searchTerm "
            + " or upper(cr.entity_type) like :searchTerm "
            + " or json_search( upper(cr.resolution) , 'one', :searchTerm ) is not null "
            + " or json_search( upper(cr.domain_object_ids) , 'one', :searchTerm ) is not null ) "
            + " order by cr.lmd desc ", nativeQuery = true)
    List<ChatRoom> getChatRoomsBySearch(String searchTerm, String currentUser);

    @Query(value = "select cr.* from chat_room cr "
            + " join chat_room_participant crp on crp.room_id = cr.id "
            + " where crp.user_name = :currentUser "
            + " and json_contains(cr.domain_object_ids, :objectId ) "
            + " and ( upper(cr.room_name) like :searchTerm "
            + " or upper(cr.description) like :searchTerm "
            + " or upper(cr.situation_type) like :searchTerm "
            + " or upper(cr.entity_type) like :searchTerm "
            + " or json_search( upper(cr.resolution) , 'one', :searchTerm ) is not null ) "
            + " order by cr.lmd desc ", nativeQuery = true)
    List<ChatRoom> getChatRoomsBySearchInObjectId(String searchTerm, String currentUser, String objectId);

    @Query(value = "Select * from chat_room cr "
            + " join chat_room_participant crp on crp.room_id = cr.id"
            + " where json_contains(cr.domain_object_ids, :objectId ) "
            + " and crp.user_name = :currentUser ", nativeQuery = true)
    List<ChatRoom> getChannelByObjectIdAndUser(String objectId, String currentUser);

}
