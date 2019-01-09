/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.service;

import com.jda.dct.chatservice.dto.downstream.AddParticipantDto;
import com.jda.dct.chatservice.dto.downstream.CreateChannelDto;
import com.jda.dct.chatservice.dto.downstream.RemoteUserDto;
import com.jda.dct.chatservice.dto.downstream.RoleDto;
import com.jda.dct.chatservice.dto.downstream.TeamDto;
import com.jda.dct.chatservice.dto.upstream.SituationRoomDto;
import com.jda.dct.chatservice.dto.upstream.TokenDto;
import com.jda.dct.chatservice.repository.ProxyTokenMappingRepository;
import com.jda.dct.chatservice.repository.SituationRoomRepository;
import com.jda.dct.contexts.AuthContext;
import com.jda.dct.domain.ProxyTokenMapping;
import com.jda.dct.domain.SituationRoom;
import com.jda.dct.domain.SituationRoomStatus;
import com.jda.dct.ignitecaches.springimpl.Tenants;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


/**
 * Situation room service implementation.
 */

@Service
public class SituationRoomServiceImpl implements SituationRoomService {

    private static Logger LOGGER = LoggerFactory.getLogger(SituationRoomServiceImpl.class);

    private static final int MAX_REMOTE_USERNAME_LENGTH = 19;

    @Value("${dct.situationRoom.mattermost.host}")
    private String mattermostUrl;

    @Value("${dct.situationRoom.token}")
    private String adminAccessToken;

    @Value("${dct.situationRoom.mattermost.teamId}")
    private String channelTeamId;

    private AuthContext authContext;
    private SituationRoomRepository situationRoomRepository;
    private ProxyTokenMappingRepository proxyTokenMappingRepository;

    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Mattermost chat service constructor.
     *
     * @param authContext                 Auth context.
     * @param situationRoomRepository     Repository for situation room.
     * @param proxyTokenMappingRepository Proxy token repository.
     */
    public SituationRoomServiceImpl(@Autowired AuthContext authContext,
                                    @Autowired SituationRoomRepository situationRoomRepository,
                                    @Autowired ProxyTokenMappingRepository proxyTokenMappingRepository) {
        this.authContext = authContext;
        this.situationRoomRepository = situationRoomRepository;
        this.proxyTokenMappingRepository = proxyTokenMappingRepository;
    }

    /**
     * This API is used to get the current loggedin user token id from remote system.
     * If user does not exists into the remote system, it will first create and create token
     * and return to the caller.
     *
     * @return TokenDto contain token and team id information.
     */
    @Override
    public TokenDto getSessionToken() {
        LOGGER.info("Get chat session token for situation room has called for user {}", authContext.getCurrentUser());
        Assert.notNull(authContext.getCurrentUser(), "Current user can't be null");
        setupUser(authContext.getCurrentUser(), channelTeamId);
        ProxyTokenMapping token = getUserTokenMapping(authContext.getCurrentUser());
        Assert.notNull(token, "User should be pressent but missing in situation room");
        TokenDto tokenDto = new TokenDto();
        tokenDto.setToken(token.getProxyToken());
        tokenDto.setTeamId(channelTeamId);
        LOGGER.info("Returning token foe user {}", authContext.getCurrentUser());
        return tokenDto;
    }

    /**
     * This API is used to post message to chat channel.
     *
     * @param chat Map object. Chat client send chat service provider specific input.
     * @return Map object, contains response from remote system.
     */
    @Override
    public Map<String, Object> postMessage(Map<String, Object> chat) {
        LOGGER.info("User {} posting message to channel {}", authContext.getCurrentUser(), chat.get("channel_id"));
        HttpHeaders headers = getHttpHeader(adminAccessToken);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(chat, headers);
        HttpEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(getMessagePath()),
                HttpMethod.POST,
                requestEntity,
                Map.class);
        LOGGER.info("Message posted successfully into channel {} by user {}", chat.get("channel_id"),
                authContext.getCurrentUser());
        return response.getBody();
    }

    /**
     * This API is used to create channel in remote system. If the participants does not exists in remote
     * system, first it setup user then assigned them to channel.
     *
     * @param request SituationRoomDto, it contains required information for creating new channel.
     * @return Map object, containing remote system returned information.
     */
    @Override
    public Map<String, Object> createChannel(SituationRoomDto request) {
        validateRequest(request);
        LOGGER.info("Going to create new channel {} requested by {} with details", request.getName(),
                authContext.getCurrentUser(), request);
        HttpEntity<Map> response = createSituationRoom(request);
        String roomId = (String) response.getBody().get("id");
        setupParticipantsIfNotBefore(request);
        addParticipantsToRoom(request.getParticipants(), roomId);

        return response.getBody();
    }


    private void validateRequest(SituationRoomDto request) {
        Assert.notNull(request, "Channel creation input can't be null");
        Assert.notEmpty(request.getObjectIds(), "Reference domain object can't be null or empty");
        Assert.notNull(request.getParticipants(), "Participants can't be null");
    }

    private HttpEntity<Map> createSituationRoom(SituationRoomDto request) {
        LOGGER.info("Creating new situation room {} by user requested, with details", request.getName(),
                authContext.getCurrentUser(), request);
        HttpHeaders headers = getHttpHeader(adminAccessToken);
        HttpEntity<CreateChannelDto> requestEntity
                = new HttpEntity<>(buildRemoteChannelCreationRequest(request), headers);
        HttpEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(getChannelPath()),
                HttpMethod.POST,
                requestEntity,
                Map.class);

        LOGGER.info("Channel {} creation done in remote system", request.getName());
        Tenants.setCurrent(authContext.getCurrentTid());
        SituationRoom situationRoom = buildSituationRoom(request);
        situationRoom.setRoomId((String) response.getBody().get("id"));
        LOGGER.info("Going to persist channel {} meta information into system", request.getName());
        situationRoomRepository.save(situationRoom);
        LOGGER.info("Channel {} meta information persisted successfully", request.getName());
        return response;
    }

    private void setupParticipantsIfNotBefore(SituationRoomDto request) {
        request.getParticipants().forEach(user -> setupUser(user, request.getTeamId()));
    }

    private ProxyTokenMapping setupUser(String appUserId, String teamId) {
        LOGGER.info("Checking whether user {} exist in system or not", appUserId);
        ProxyTokenMapping proxyTokenMapping = getUserTokenMapping(appUserId);
        if (proxyTokenMapping == null) {
            LOGGER.warn("User {} does not exists into system", appUserId);
            Assert.isNull(proxyTokenMapping,
                    String.format("User %s does not exists, going to create", appUserId));

            Map<String, Object> newUserResponse = crateUser(appUserId);
            setupRoles((String) newUserResponse.get("id"));
            proxyTokenMapping = addUserTokenMapping(appUserId, (String) newUserResponse.get("id"));
            proxyTokenMapping = setupAccessToken(proxyTokenMapping);
            joinTeam(proxyTokenMapping.getRemoteUserId(), teamId);
            LOGGER.info("Setup done for user {}", appUserId);
        } else {
            LOGGER.info("User {} already present into system", appUserId);
        }
        return proxyTokenMapping;
    }

    private void joinTeam(String remoteUserId, String teamId) {
        LOGGER.info("Adding user {} to team {}", remoteUserId, teamId);
        HttpHeaders headers = getHttpHeader(adminAccessToken);
        HttpEntity<TeamDto> requestEntity
                = new HttpEntity<>(
                buildJoinTeamRequest(remoteUserId, teamId),
                headers);
        HttpEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(getTeamsPath(teamId)),
                HttpMethod.POST,
                requestEntity,
                Map.class);
        if (!((ResponseEntity<Map>) response).getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Unable to joined situation room");
        }
        LOGGER.info("User {} added to team {} successfully", remoteUserId, teamId);
    }

    private void addParticipantsToRoom(List<String> users, String roomId) {
        users.stream().map(this::getUserTokenMapping).forEach(ptm -> joinRoom(ptm.getRemoteUserId(), roomId));
    }

    private void joinRoom(String remoteUserId, String roomId) {
        HttpHeaders headers = getHttpHeader(adminAccessToken);
        HttpEntity<AddParticipantDto> requestEntity
                = new HttpEntity<>(
                buildChannelMemberRequest(remoteUserId, ""), headers);
        HttpEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(getAddParticipantPath(roomId)),
                HttpMethod.POST,
                requestEntity,
                Map.class);
        if (!((ResponseEntity<Map>) response).getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Unable to joined situation room");
        }
    }

    private ProxyTokenMapping setupAccessToken(ProxyTokenMapping mapping) {
        LOGGER.info("Generating access token in remote system for user {} ", mapping.getAppUserId());
        HttpHeaders headers = getHttpHeader(adminAccessToken);
        Map<String, String> request = new HashMap<>();
        request.put("description", "situation room");
        HttpEntity<RemoteUserDto> requestEntity = new HttpEntity(request, headers);
        HttpEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(getTokenPath(mapping.getRemoteUserId())),
                HttpMethod.POST,
                requestEntity,
                Map.class);
        if (!((ResponseEntity<Map>) response).getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Unable to update roles.");
        }
        LOGGER.info("Access token generated successfully for user {}", mapping.getAppUserId());
        mapping.setProxyToken((String) response.getBody().get("token"));
        LOGGER.info("Updating token mapping for user {}", mapping.getAppUserId());
        ProxyTokenMapping updatedMapping = saveProxyTokenMapping(mapping);
        LOGGER.info("Token mapping for user {} updated successfully", mapping.getAppUserId());
        return updatedMapping;
    }

    private Map<String, Object> crateUser(String appUserId) {
        LOGGER.warn("Creating new user{} into system", appUserId);
        RemoteUserDto newUser = buildNewRemoteUser(appUserId);
        HttpHeaders headers = getHttpHeader(adminAccessToken);
        HttpEntity<RemoteUserDto> requestEntity = new HttpEntity<>(newUser, headers);
        HttpEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(getUsersPath()),
                HttpMethod.POST,
                requestEntity,
                Map.class);
        LOGGER.info("User {} created in system and with id {}", appUserId,
                response.getBody().get("id"));

        return response.getBody();
    }

    private void setupRoles(String userId) {
        LOGGER.info("Assigning roles to user {}", userId);
        HttpHeaders headers = getHttpHeader(adminAccessToken);
        RoleDto role = buildRoles();
        HttpEntity<RoleDto> requestEntity = new HttpEntity<>(role, headers);
        HttpEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(getRolePath(userId)),
                HttpMethod.PUT,
                requestEntity,
                Map.class);
        if (!((ResponseEntity<Map>) response).getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Unable to update roles.");
        }
        LOGGER.info("Roles assigned successfully to user {}", userId);
    }


    private ProxyTokenMapping addUserTokenMapping(String appUser, String remoteUserId) {
        LOGGER.info("Adding remote user id mapping for user {}", appUser);
        ProxyTokenMapping proxyTokenMapping = new ProxyTokenMapping();
        proxyTokenMapping.setAppUserId(appUser);
        proxyTokenMapping.setRemoteUserId(remoteUserId);
        proxyTokenMapping.setProxyToken("not present");
        proxyTokenMapping.setTid(authContext.getCurrentTid());
        ProxyTokenMapping mapping = saveProxyTokenMapping(proxyTokenMapping);
        LOGGER.info("Remote user id {} mapping for appuser {} finish successfully", mapping.getRemoteUserId(), appUser);
        return mapping;
    }

    private ProxyTokenMapping getUserTokenMapping(String user) {
        return proxyTokenMappingRepository.findByAppUserId(user);
    }

    private ProxyTokenMapping saveProxyTokenMapping(ProxyTokenMapping proxyTokenMapping) {
        return proxyTokenMappingRepository.save(proxyTokenMapping);
    }

    private SituationRoom buildSituationRoom(SituationRoomDto request) {
        SituationRoom room = new SituationRoom();
        room.setRoomName(request.getName());
        room.setEntityType(request.getEntityType());
        room.setCreatedBy(authContext.getCurrentTid());
        room.setDescription(request.getPurpose());
        room.setTeamId(request.getTeamId());
        room.setStatus(SituationRoomStatus.NEW);
        room.setResolution("WORKING");
        room.setSituationType(request.getSituationType());
        room.setParticipants(request.getParticipants());
        room.setCreationDate(new Date());
        room.setTid(authContext.getCurrentTid());
        room.setDomainObjectIds(request.getObjectIds());
        return room;
    }

    private CreateChannelDto buildRemoteChannelCreationRequest(SituationRoomDto request) {
        CreateChannelDto dto = new CreateChannelDto();
        dto.setTeamId(request.getTeamId());
        dto.setName(request.getName());
        dto.setHeader(request.getHeader());
        dto.setPurpose(request.getPurpose());
        dto.setRoomType(request.getRoomType());
        return dto;
    }

    private RemoteUserDto buildNewRemoteUser(String username) {
        username = username.replace("@", "");
        username = username.length() < MAX_REMOTE_USERNAME_LENGTH
                ? username : username.substring(0, MAX_REMOTE_USERNAME_LENGTH);
        RemoteUserDto user = new RemoteUserDto();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPassword("dummy1234");
        System.out.println("new user which going to be save ==>" + user.toString());
        return user;
    }

    private TeamDto buildJoinTeamRequest(String userId, String teamId) {
        TeamDto teamDto = new TeamDto();
        teamDto.setUserId(userId);
        teamDto.setTeamId(teamId);
        return teamDto;
    }

    private AddParticipantDto buildChannelMemberRequest(String userId, String postId) {
        AddParticipantDto dto = new AddParticipantDto();
        dto.setPostRootId(postId);
        dto.setUserId(userId);
        return dto;
    }

    private RoleDto buildRoles() {
        return new RoleDto("system_user system_user_access_token");
    }

    private String getRemoteActionUrl(String cxtPath) {
        return UriComponentsBuilder.fromHttpUrl(mattermostUrl)
                .path(cxtPath).toUriString();
    }

    private static String getUsersPath() {
        return "/users";
    }

    private static String getMessagePath() {
        return "/posts";
    }

    private static String getChannelPath() {
        return "/channels";
    }

    private static String getAddParticipantPath(String roomId) {
        return getChannelPath() + "/" + roomId + "/members";
    }

    private static String getTeamsPath(String teamId) {
        return "/teams/" + teamId + "/members";
    }

    private static String getRolePath(String userId) {
        return getUsersPath() + "/" + userId + "/roles";
    }

    private static String getTokenPath(String userId) {
        return getUsersPath() + "/" + userId + "/tokens";
    }

    private HttpHeaders getHttpHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, HttpHeaders.ACCEPT);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }
}
