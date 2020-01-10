/**
 * Copyright © 2020, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.service;

import static com.jda.dct.chatservice.constants.ChatRoomConstants.DOMAIN_OBJECT_ID;
import static com.jda.dct.chatservice.constants.ChatRoomConstants.FILTER_BY_USER;
import static com.jda.dct.chatservice.constants.ChatRoomConstants.FIRST_NAME;
import static com.jda.dct.chatservice.constants.ChatRoomConstants.LAST_NAME;
import static com.jda.dct.chatservice.constants.ChatRoomConstants.MATTERMOST_CHANNELS;
import static com.jda.dct.chatservice.constants.ChatRoomConstants.MATTERMOST_POSTS;
import static com.jda.dct.chatservice.constants.ChatRoomConstants.MATTERMOST_USERS;
import static com.jda.dct.chatservice.constants.ChatRoomConstants.MAX_REMOTE_USERNAME_LENGTH;
import static com.jda.dct.chatservice.constants.ChatRoomConstants.PATH_DELIMITER;
import static com.jda.dct.chatservice.constants.ChatRoomConstants.PATH_PREFIX;
import static com.jda.dct.chatservice.constants.ChatRoomConstants.PERCENT_SIGN;
import static com.jda.dct.chatservice.constants.ChatRoomConstants.QUOTATION_MARK;
import static com.jda.dct.chatservice.constants.ChatRoomConstants.SPACE;
import static com.jda.dct.chatservice.constants.ChatRoomConstants.USER_NAME;
import static com.jda.dct.chatservice.utils.ChatRoomUtil.buildUrlString;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jda.dct.app.constants.AttachmentConstants;
import com.jda.dct.app.exception.AttachmentException;
import com.jda.dct.chatservice.domainreader.EntityReaderFactory;
import com.jda.dct.chatservice.dto.downstream.AddParticipantDto;
import com.jda.dct.chatservice.dto.downstream.CreateChannelDto;
import com.jda.dct.chatservice.dto.downstream.RemoteUserDto;
import com.jda.dct.chatservice.dto.downstream.RoleDto;
import com.jda.dct.chatservice.dto.downstream.TeamDto;
import com.jda.dct.chatservice.dto.upstream.AddUserToRoomDto;
import com.jda.dct.chatservice.dto.upstream.ChatContext;
import com.jda.dct.chatservice.dto.upstream.ChatRoomCreateDto;
import com.jda.dct.chatservice.dto.upstream.ResolveRoomDto;
import com.jda.dct.chatservice.dto.upstream.TokenDto;
import com.jda.dct.chatservice.exception.ChatException;
import com.jda.dct.chatservice.repository.ChatRoomParticipantRepository;
import com.jda.dct.chatservice.repository.ProxyTokenMappingRepository;
import com.jda.dct.chatservice.repository.SituationRoomRepository;
import com.jda.dct.chatservice.utils.AssertUtil;
import com.jda.dct.chatservice.utils.ChatRoomUtil;
import com.jda.dct.domain.Attachment;
import com.jda.dct.domain.ChatRoom;
import com.jda.dct.domain.ChatRoomParticipant;
import com.jda.dct.domain.ChatRoomParticipantStatus;
import com.jda.dct.domain.ChatRoomResolution;
import com.jda.dct.domain.ChatRoomStatus;
import com.jda.dct.domain.ProxyTokenMapping;
import com.jda.dct.domain.exceptions.DctIoException;
import com.jda.dct.domain.util.StringUtil;
import com.jda.dct.foundation.process.access.DctServiceRestTemplate;
import com.jda.dct.ignitecaches.springimpl.Tenants;
import com.jda.dct.search.SearchConstants;
import com.jda.luminate.ingest.rest.services.attachments.AttachmentValidator;
import com.jda.luminate.ingest.util.InputStreamWrapper;
import com.jda.luminate.io.documentstore.DocumentStoreService;
import com.jda.luminate.io.documentstore.exception.DocumentException;
import com.jda.luminate.security.contexts.AuthContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.apache.tika.mime.MimeTypeException;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;


/**
 * Situation room service implementation.
 */

@Service
public class SituationRoomServiceImpl implements SituationRoomService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SituationRoomServiceImpl.class);
    private static final String CHANNEL_ID = "channel_id";

    @Value("${dct.situationRoom.mattermost.host}")
    private String mattermostUrl;

    @Value("${dct.situationRoom.token}")
    private String adminAccessToken;

    @Value("${dct.situationRoom.mattermost.teamId}")
    private String channelTeamId;

    private final AuthContext authContext;
    private final SituationRoomRepository roomRepository;
    private final ProxyTokenMappingRepository tokenRepository;
    private final AttachmentValidator attachmentValidator;
    private final ChatRoomParticipantRepository participantRepository;
    private final DocumentStoreService documentStoreService;
    private final EntityReaderFactory entityReaderFactory;
    private final UniqueRoomNameGenerator generator;
    private DctServiceRestTemplate dctService;

    @Value("${tenant.umsUri}")
    private String umsUri;

    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Mattermost chat service constructor.
     *
     * @param authContext     Auth context.
     * @param roomRepository  Repository for situation room.
     * @param tokenRepository Proxy token repository.
     */
    public SituationRoomServiceImpl(@Autowired AuthContext authContext,
                                    @Autowired SituationRoomRepository roomRepository,
                                    @Autowired ProxyTokenMappingRepository tokenRepository,
                                    @Autowired ChatRoomParticipantRepository participantRepository,
                                    @Autowired UniqueRoomNameGenerator generator,
                                    @Autowired EntityReaderFactory entityReaderFactory,
                                    @Autowired AttachmentValidator attachmentValidator,
                                    @Autowired DocumentStoreService documentStoreService,
                                    @Autowired DctServiceRestTemplate dctService) {
        this.authContext = authContext;
        this.roomRepository = roomRepository;
        this.tokenRepository = tokenRepository;
        this.participantRepository = participantRepository;
        this.generator = generator;
        this.entityReaderFactory = entityReaderFactory;
        this.attachmentValidator = attachmentValidator;
        this.documentStoreService = documentStoreService;
        this.dctService = dctService;
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
        AssertUtil.notNull(authContext.getCurrentUser(), "Current user can't be null");
        setupUser(authContext.getCurrentUser(), channelTeamId);
        ProxyTokenMapping token = getUserTokenMapping(authContext.getCurrentUser());
        AssertUtil.notNull(token, "User should be present but missing in situation room");
        TokenDto tokenDto = new TokenDto();
        tokenDto.setToken(token.getProxyToken());
        tokenDto.setTeamId(channelTeamId);
        LOGGER.info("Returning token for user {}", authContext.getCurrentUser());
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
        String currentUser = authContext.getCurrentUser();
        validatePostMessageRequest(currentUser, chat);
        Map<String, Object> chatCopy = new HashMap<>(chat);
        LOGGER.info("User {} posting message to channel {}", currentUser,
                getRoomIdFromPostMessage(chatCopy));
        ProxyTokenMapping proxyTokenMapping = getUserTokenMapping(authContext.getCurrentUser());
        HttpHeaders headers = getHttpHeader(proxyTokenMapping.getProxyToken());
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(chatCopy, headers);
        HttpEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(getMessagePath()),
                HttpMethod.POST,
                requestEntity,
                Map.class);
        LOGGER.info("Message posted successfully into channel {} by user {}",
                getRoomIdFromPostMessage(chatCopy),
                authContext.getCurrentUser());
        archiveMessage(chatCopy);
        return response.getBody();
    }

    /**
     * This API is used to create channel in remote system. If the participants does not exists in remote
     * system, first it setup user then assigned them to channel.
     *
     * @param request ChatRoomCreateDto, it contains required information for creating new channel.
     * @return Map object, containing remote system returned information.
     */
    @Override
    @Transactional
    public Map<String, Object> createChannel(ChatRoomCreateDto request) {
        validateChannelCreationRequest(request);
        LOGGER.info("Going to create new channel {} requested by {} with details {}", request.getName(),
                authContext.getCurrentUser(), request);
        Tenants.setCurrent(authContext.getCurrentTid());
        HttpEntity<Map> response = createRemoteServerChatRoom(request);
        String roomId = roomId(response.getBody());
        createChatRoomInApp(request, roomId);
        setupParticipantsIfNotBefore(request.getParticipants(), channelTeamId);
        addParticipantsToRoom(authContext.getCurrentUser(), roomId);
        return response.getBody();
    }

    /**
     * This API is used to delete channel in remote system.
     *
     * @param roomId to be deleted
     * @return Map object, containing remote system returned information.
     */
    @Override
    @Transactional
    public Map<String, Object> removeChannel(String roomId) {
        String currentUser = authContext.getCurrentUser();
        LOGGER.info("Delete Situation Room {} has been called by user {}", roomId, currentUser);

        validateRemoveRoomRequest(roomId, currentUser);
        removeRoomInApp(roomId);
        Map<String, Object> response = removeRoomInRemote(roomId, currentUser);
        LOGGER.info("Room {} removed by {} successfully", roomId, currentUser);
        response.put("deletedRoomId", roomId);
        return response;
    }

    private void removeRoomInApp(String roomId) {
        LOGGER.debug("Going to Delete chat room having Room Id {} meta information into system", roomId);
        roomRepository.deleteById(roomId);
        LOGGER.debug("Chat room having room Id {} deleted successfully", roomId);
    }

    private Map<String, Object> removeRoomInRemote(String roomId, String currentUser) {
        ProxyTokenMapping proxyTokenMapping = getUserTokenMapping(currentUser);
        HttpHeaders headers = getHttpHeader(proxyTokenMapping.getProxyToken());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Map<String, Object>> requestEntity
                = new HttpEntity<>(null, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(removeRoomPath(roomId)),
                HttpMethod.DELETE,
                requestEntity,
                Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            LOGGER.error("Error {} while deleting channel {} for user {}",
                    response.getBody(),
                    roomId,
                    currentUser);
            throw new ResourceAccessException(response.getBody() != null
                    ? response.getBody().toString() :
                    "Remote system unknown exception.");
        }

        LOGGER.info("Situation Room having room Id {} deleted from remote successfully", roomId);

        return response.getBody();
    }

    private String removeRoomPath(String roomId) {
        return getChannelPath() + "/" + roomId;
    }

    private void validateRemoveRoomRequest(String roomId, String currentUser) {
        roomIdInputValidation(roomId);
        Optional<ChatRoom> room = getChatRoomById(roomId);

        if (!room.isPresent()) {
            throw new ChatException(ChatException.ErrorCode.ROOM_NOT_EXISTS);
        }
        AssertUtil.isTrue(room.get().getCreatedBy().equals(currentUser), "Room can only be removed by Creator");
    }

    /**
     * This API allow to add request to an existing channel.
     *
     * @param channel channel id.
     * @param request list request in AddUserToRoomDto object.
     * @return Map containing status
     */
    @Override
    @Transactional
    public Map<String, Object> inviteUsers(String channel, AddUserToRoomDto request) {
        validateInviteUsersInputs(channel, authContext.getCurrentUser(), request);
        setupParticipantsIfNotBefore(request.getUsers(), channelTeamId);
        updateParticipantOfRooms(request.getUsers(), channel);
        Map<String, Object> status = new HashMap<>();
        status.put("Status", "Success");
        return status;
    }

    /**
     * This API will return channel context.
     *
     * @param channelId Channel Id
     * @return ChatContext Channel context object
     */
    @Override
    public ChatContext getChannelContext(String channelId) {
        AssertUtil.isTrue(!StringUtils.isEmpty(channelId), "Channel id can't be null or empty");
        LOGGER.info("Going to fetch chat room {} context request by user {}", channelId, authContext.getCurrentUser());
        Optional<ChatRoom> chatRoom = getChatRoomById(channelId);
        if (!chatRoom.isPresent()) {
            LOGGER.error("Chat room {} does not exists", channelId);
            Object[] args = new Object[1];
            args[0] = channelId;
            throw new ChatException(ChatException.ErrorCode.CHANNEL_NOT_EXISTS, channelId);
        }
        LOGGER.info("Returning chat room {} context", channelId);
        return toChatContext(chatRoom.get(), authContext.getCurrentUser());
    }

    @Override
    public List<ChatContext> getChannels(String by, String type, String requestQueryParam) {
        String currentUser = authContext.getCurrentUser();
        List<ChatRoomParticipant> participants;
        if (!StringUtils.isEmpty(requestQueryParam)) {
            LOGGER.info("Fetching chat rooms by objectIds for user {}", currentUser);
            List<String> objectIds = parseSearchQueryParam(requestQueryParam);
            return getRoomsByObjectIds(objectIds, currentUser);
        } else if (StringUtils.isEmpty(type)) {
            LOGGER.info("Fetching all chat rooms for user {}", currentUser);
            participants = getUserAllRooms(currentUser);
        } else if (StringUtils.isEmpty(by) || FILTER_BY_USER.equals(by.trim())) {
            LOGGER.info("Fetching {} chat rooms for user {}", type, currentUser);
            participants = getRoomsByParticipantStatus(type, currentUser);
        } else {
            LOGGER.info("Fetching {} chat rooms for user {}", type, currentUser);
            participants = getUserAllRoomsOfType(type, currentUser);
        }
        return participants
                .stream()
                .map(ChatRoomParticipant::getRoom)
                .map(room -> toChatContext(room, currentUser))
                .collect(Collectors.toList());
    }

    private List<String> parseSearchQueryParam(String param) {
        if (StringUtils.isEmpty(param)) {
            return Lists.emptyList();
        }
        List<String> objectIds = Arrays.asList(param.trim().split(SearchConstants.COMMA));
        objectIds.replaceAll(String::trim);
        return  objectIds;
    }

    private List<ChatContext> getRoomsByObjectIds(List<String> objectIds, String currentUser) {
        Set<ChatRoom> distinctChatRooms = Sets.newHashSet();
        objectIds.forEach(objectId -> {
            List<ChatRoom> chatRooms = roomRepository.getChannelByObjectId("\"" + objectId + "\"");
            distinctChatRooms.addAll(chatRooms);
        });

        return distinctChatRooms
                .stream()
                .map(room -> toChatContext(room, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> acceptInvitation(String roomId) {
        String currentUser = authContext.getCurrentUser();
        AssertUtil.isTrue(!StringUtils.isEmpty(roomId), "Room id can't be null");
        Map<String, Object> response = addParticipantsToRoom(currentUser, roomId);
        LOGGER.info("User {} added successfully to remote room {}", currentUser, roomId);
        return response;
    }

    @Override
    public List<Map<String, Object>> getUnreadCount() {
        String currentUser = authContext.getCurrentUser();
        List<Map<String, Object>> response = new ArrayList<>();
        LOGGER.info("User {} called for unread message count", currentUser);
        ProxyTokenMapping proxyTokenMapping = getUserTokenMapping(currentUser);
        if (proxyTokenMapping == null) {
            LOGGER.error("the user {} does not have token", currentUser);
            return response;
        }
        String remoteUserId = proxyTokenMapping.getRemoteUserId();
        List<ChatRoomParticipant> userRooms =
                getRoomsByParticipantStatus(ChatRoomParticipantStatus.JOINED.name(), currentUser);
        LOGGER.info("Fetching total {} number of room unread count for user {} as remote user is {}",
                userRooms.size(),
                currentUser,
                remoteUserId);
        HttpHeaders headers = getHttpHeader(proxyTokenMapping.getProxyToken());
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        for (ChatRoomParticipant userRoom : userRooms) {
            String roomId = userRoom.getRoom().getId();
            try {
                ResponseEntity<Map> remoteResponse = restTemplate.exchange(
                        getRemoteActionUrl(getChannelUnreadCountPath(remoteUserId, roomId)),
                        HttpMethod.GET,
                        new HttpEntity<Map<String, Object>>(headers),
                        Map.class);
                if (remoteResponse.getStatusCode().is2xxSuccessful()) {
                    response.add(remoteResponse.getBody());
                } else {
                    LOGGER.error("Error {} while fetching unread count for channel {} for user {}",
                            remoteResponse.getBody(),
                            roomId,
                            currentUser);
                }

            } catch (Exception t) {
                LOGGER.error("Unable to fetch unread count for channel {} for user {}", roomId, currentUser, t);
            }
        }
        return response;
    }

    @Override
    public Map<String, Object> readResolvedChannel() {
        String currentUser = authContext.getCurrentUser();
        LOGGER.info("Participant {} is going to save all resolved Rooms read status", currentUser);

        List<ChatRoomParticipant> participants = getUserAllRooms(currentUser);
        participants.forEach(participant -> {
            if (participant.getRoom().getStatus() == ChatRoomStatus.RESOLVED
                    && !participant.isResolutionRead()) {
                participant.setResolutionRead(true);
            }
        });
        saveParticipants(participants);
        LOGGER.info("participant {} has saved all resolved room read status", currentUser);

        Map<String, Object> response = new HashMap<>();
        response.put("readResolved", "ok");
        return response;
    }

    private void saveParticipants(List<ChatRoomParticipant> participants) {
        LOGGER.debug("Going to persist all participants added or modified");
        participantRepository.saveAll(participants);
        LOGGER.debug("Added or modified Participants persisted successfully");
    }

    @Override
    @Transactional
    public ChatContext resolve(String roomId, ResolveRoomDto request) {
        String currentUser = authContext.getCurrentUser();
        LOGGER.info("User {} is resolving room {} with details {}", currentUser, roomId, request);
        validateResolveRoomInputs(roomId, currentUser, request);
        Optional<ChatRoom> record = getChatRoomById(roomId);

        if (!record.isPresent()) {
            throw new ChatException(ChatException.ErrorCode.INVALID_ROOM);
        }
        ChatRoom room = record.get();

        room.setStatus(ChatRoomStatus.RESOLVED);
        room.setResolution(buildResolution(request, currentUser));
        room.setLmd(room.getResolution().getDate());
        ChatRoom resolvedRoom = saveChatRoom(room);
        saveResolveRoomReadStatus(resolvedRoom, currentUser);
        saveResolutionInRemote(resolvedRoom);

        LOGGER.info("Room {} status has changed to resolved by user {}", roomId, currentUser);
        return toChatContext(resolvedRoom, currentUser);
    }

    private void saveResolveRoomReadStatus(ChatRoom chatRoom, String currentUser) {
        LOGGER.debug("Going to persist resolve read status for all participants of chat room {} into system",
                chatRoom.getRoomName());
        List<ChatRoomParticipant> currentUserRooms = getUserAllRooms(currentUser);
        currentUserRooms.forEach(currentUserRoom -> {
            if (currentUserRoom.getRoom().getId().equals(chatRoom.getId())) {
                currentUserRoom.setResolutionRead(true);
                saveChatRoomParticipant(currentUserRoom);
            }
        });
        LOGGER.debug("Resolve read status for all participants of Chat room {} persisted successfully",
                chatRoom.getRoomName());
    }

    private void saveResolutionInRemote(ChatRoom resolvedRoom) {
        String currentUser = authContext.getCurrentUser();
        String roomId = resolvedRoom.getId();
        LOGGER.info("Going to save Attachment metaData for room {} by user {}", roomId, currentUser);

        Map<String, Object> propsData = new HashMap<>();
        propsData.put("room_status", resolvedRoom.getStatus());
        propsData.put("resolved_by", resolvedRoom.getResolution().getResolvedUser());
        propsData.put("username", currentUser);

        Map<String, Object> attachmentDetail = new HashMap<>();
        attachmentDetail.put(CHANNEL_ID, roomId);
        attachmentDetail.put("message", null);
        attachmentDetail.put("props", propsData);
        postMessageInRemote(attachmentDetail);
        LOGGER.info("Attachment metaData saved in Remote successfully for room {} by user {}", roomId, currentUser);
    }

    private Map<String, Object> postMessageInRemote(Map<String, Object> chat) {
        String currentUser = authContext.getCurrentUser();
        validatePostResolveMessageRequest(chat);
        Map<String, Object> chatCopy = new HashMap<>(chat);
        String roomId = getRoomIdFromPostMessage(chatCopy);
        LOGGER.info("User {} posting Resolve message to channel {}", currentUser,
                roomId);
        ProxyTokenMapping proxyTokenMapping = getUserTokenMapping(currentUser);
        HttpHeaders headers = getHttpHeader(proxyTokenMapping.getProxyToken());
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(chatCopy, headers);
        HttpEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(getMessagePath()),
                HttpMethod.POST,
                requestEntity,
                Map.class);
        LOGGER.info("Message posted successfully into channel {} by user {}",
                roomId,
                authContext.getCurrentUser());
        return response.getBody();
    }

    private void validatePostResolveMessageRequest(Map<String, Object> request) {
        LOGGER.debug("Validating post message request");
        AssertUtil.notNull(request, "Post message can't be null");
        AssertUtil.notEmpty(request, "Post message can't be empty");
        AssertUtil.notNull(getRoomIdFromPostMessage(request),
                "Channel can't be null");
        AssertUtil.isTrue(getRoomIdFromPostMessage(request).trim().length() > 0,
                "Channel can't be empty");
    }

    private void saveChatRoomParticipant(ChatRoomParticipant participant) {
        LOGGER.debug("Going to persist participant added or modified");
        participantRepository.save(participant);
        LOGGER.debug("Added or modified Participant persisted successfully");
    }

    @Override
    public List<Attachment> upload(String roomId, MultipartFile file, String comment) {
        LOGGER.debug("Uploading document for SR  {} ", roomId);
        String path = PATH_PREFIX + PATH_DELIMITER + roomId;
        String fileName = file.getOriginalFilename();
        Set<String> fileNames = new HashSet<>();
        Attachment attachments;
        List<Attachment> attachmentsList;
        List<Attachment> response = new ArrayList<>();
        comment = comment == null ? "" : comment;
        validateRoomState(roomId, authContext.getCurrentUser(), "This room has been Resolved."
                + "File can't be Uploaded.");
        validateFile(file);
        try {
            Optional<ChatRoom> record = getChatRoomById(roomId);
            ChatRoom room = record.get();
            attachmentsList = room.getAttachments();
            InputStream inputStream = file.getInputStream();
            if (attachmentsList != null) {
                Iterator<Attachment> iter = attachmentsList.iterator();
                while (iter.hasNext()) {
                    LinkedHashMap<String, Object> temp = ((LinkedHashMap<String, Object>) (Object) iter.next());
                    fileNames.add(temp.get("attachmentName").toString());
                }
            }
            fileName = StringUtil.incrementFileName(fileName, fileNames);
            documentStoreService.getDocumentStore().store(path, fileName, inputStream);
            attachments = curateResponse(path, fileName, comment);
            response.add(attachments);
            attachmentsList = attachmentsList == null ? new ArrayList<>() : attachmentsList;
            attachmentsList.add(attachments);
            room.setAttachments(attachmentsList);
            saveChatRoom(room);
            savePostInRemote(roomId, attachments, comment);
            LOGGER.info("File Uploaded successfully for room {}", roomId);
        } catch (DocumentException ex) {
            LOGGER.error("failed to upload file {} for user {}", fileName, authContext.getCurrentUser(), ex);
            throw new AttachmentException(AttachmentException.ErrorCode.INVALID_SIGNATURE, null, fileName);
        } catch (IOException | MimeTypeException e) {
            LOGGER.error("failed to upload file {} for user {}", fileName, authContext.getCurrentUser(), e);
            throw new AttachmentException(AttachmentException.ErrorCode.INTERNAL_SERVER_ERROR, null, e.getMessage());
        }
        return response;
    }

    private void savePostInRemote(String roomId, Attachment attachment, String comment) {
        String currentUser = authContext.getCurrentUser();
        LOGGER.info("Going to save Attachment metaData for room {} by user {}", roomId, currentUser);
        Map<String, Object> attachmentDetail = new HashMap<>();
        attachmentDetail.put(CHANNEL_ID, roomId);
        attachmentDetail.put("file_ids", new String[]{attachment.getId()});
        attachmentDetail.put("message", null);
        Map<String, Object> propsData = new HashMap<>();
        propsData.put("attachmentName", attachment.getAttachmentName());
        propsData.put("createdBy", currentUser);
        propsData.put("creationDate", attachment.getCreationDate());
        propsData.put("id", attachment.getId());
        propsData.put("userName", attachment.getUserName());
        propsData.put("comment", comment);
        attachmentDetail.put("props", propsData);
        postMessage(attachmentDetail);
        LOGGER.info("Attachment metaData saved in Remote successfully for room {} by user {}", roomId, currentUser);
    }

    @Override
    public InputStreamWrapper getDocument(String roomId, String documentId) throws IOException {
        LOGGER.debug("Downloading document for SR  {} ", roomId);
        List<Attachment> attachmentsList;
        String filePath;
        String fileName;
        LinkedHashMap<String, String> attachmentMetaData = new LinkedHashMap<>();
        Optional<ChatRoom> record = getChatRoomById(roomId);
        if (!record.isPresent()) {
            throw new ChatException(ChatException.ErrorCode.INVALID_CHAT_ROOM, roomId);
        }
        ChatRoom room = record.get();
        attachmentsList = room.getAttachments();
        if (attachmentsList == null) {
            throw new AttachmentException(AttachmentException.ErrorCode.ATTACHMENTS_NOT_FOUND, null, documentId);
        }
        HashMap<String,Object> res = retrieve(attachmentsList, documentId);
        if (res.containsKey("fileName") && res.containsKey("filePath")
                && !res.get("filePath").toString().isEmpty() && !res.get("fileName").toString().isEmpty()) {
            filePath = res.get("filePath").toString();
            fileName = res.get("fileName").toString();
            String path = filePath + PATH_DELIMITER + fileName;
            Optional<InputStream> documentStream = documentStoreService.getDocumentStore().retrieve(path);
            if (documentStream.isPresent()) {
                return new InputStreamWrapper(documentStream.get(), fileName);
            } else {
                throw new AttachmentException(AttachmentException.ErrorCode.NULL_VALUE, null);
            }
        } else {
            throw new AttachmentException(AttachmentException.ErrorCode.INVALID_ATTACHMENT, null);
        }
    }

    @Override
    public void deleteAttachment(String roomId, String documentId) throws IOException {
        LOGGER.debug("Deleting document for SR  {} ", roomId);
        List<Attachment> attachmentsList;
        String filePath;
        String fileName;
        Optional<ChatRoom> record = getChatRoomById(roomId);
        HashMap<String,Object> res;
        int finalCount;
        validateRoomState(roomId, authContext.getCurrentUser(), "This room has been Resolved."
                + "File can't be deleted.");
        ChatRoom room = record.get();
        attachmentsList = room.getAttachments();
        if (attachmentsList == null) {
            throw new AttachmentException(AttachmentException.ErrorCode.ATTACHMENTS_NOT_FOUND, null, documentId);
        }
        try {
            res = retrieve(attachmentsList, documentId);
            filePath = res.get("filePath").toString();
            fileName = res.get("fileName").toString();
            finalCount = (int) res.get("finalCount");

            res.get("finalCount");
            if (fileName != null) {
                String path = filePath
                        + AttachmentConstants.PATH_DELIMITER + fileName;
                documentStoreService.getDocumentStore().delete(path);
                attachmentsList.remove(finalCount);
                room.setAttachments(attachmentsList);
                saveChatRoom(room);
                deleteAttachmentInRemote(roomId, documentId);
            }
        } catch (NullPointerException | DctIoException t) {
            throw new AttachmentException(AttachmentException.ErrorCode.INVALID_ATTACHMENT, null);
        }
    }

    private void deleteAttachmentInRemote(String roomId, String documentId) {
        Map<String, Object> channelPostResponse = getChannelPosts(roomId);
        Map<String, Object> channelPosts = (Map<String, Object>) channelPostResponse.get("posts");
        if (!CollectionUtils.isEmpty(channelPosts)) {
            String postId = channelPosts
                    .entrySet()
                    .stream()
                    .filter(x -> {
                        Map<String, Object> postVal = (Map<String, Object>) x.getValue();
                        List<String> fileIds = (List<String>) postVal.get("file_ids");
                        return !CollectionUtils.isEmpty(fileIds)
                                && documentId.equalsIgnoreCase(fileIds.get(0));
                    })
                    .map(Map.Entry::getKey)
                    .findAny()
                    .orElse(null);
            if (!StringUtils.isEmpty(postId)) {
                deletePostMessage(postId);
            }
        }
    }

    private void deletePostMessage(String postId) {
        String currentUser = authContext.getCurrentUser();
        ProxyTokenMapping tokenMapping = getUserTokenMapping(currentUser);
        HttpHeaders headers = getHttpHeader(tokenMapping.getProxyToken());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Map<String, Object>> requestEntity
                = new HttpEntity<>(null, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(removePostsPath(postId)),
                HttpMethod.DELETE,
                requestEntity,
                Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            LOGGER.error("Error {} while deleting post {}",
                    response.getBody(),
                    postId);
            throw new ResourceAccessException(response.getBody() != null
                    ? response.getBody().toString() :
                    "Remote system unknown exception.");
        }
    }

    private String removePostsPath(String postId) {
        return MATTERMOST_POSTS + "/" + postId;
    }

    private Map<String, Object> getChannelPosts(String roomId) {
        String currentUser = authContext.getCurrentUser();
        ProxyTokenMapping proxyTokenMapping = getUserTokenMapping(currentUser);
        HttpHeaders headers = getHttpHeader(proxyTokenMapping.getProxyToken());
        ResponseEntity<Map> remoteResponse = restTemplate.exchange(
                getRemoteActionUrl(getChannelPostsPath(roomId)),
                HttpMethod.GET,
                new HttpEntity<Map<String, Object>>(headers),
                Map.class);

        if (!remoteResponse.getStatusCode().is2xxSuccessful()) {
            throw new ChatException(ChatException.ErrorCode.UNABLE_TO_GET_CHANNEL_POSTS, roomId);
        }
        return remoteResponse.getBody();
    }

    private String getChannelPostsPath(String roomId) {
        return MATTERMOST_CHANNELS + "/" + roomId + "/posts";
    }

    private void validateFile(MultipartFile file) {
        attachmentValidator.checkForNull(file);
        attachmentValidator.checkValidExtension(file.getOriginalFilename());
        attachmentValidator.checkFileSize(file);
    }

    private Attachment curateResponse(String path, String fileName, String comment) {
        String user;
        Optional<String> userInfo;
        String userName;
        userInfo = this.userData();
        user = authContext.getCurrentUser();
        userName = userName(user, userInfo);
        Attachment attachments = new Attachment();
        String documentId = StringUtil.getUuid();
        Map<String, String> attachmentMetaData = new HashMap<>();
        attachmentMetaData.put(AttachmentConstants.METADATA_FILE_PATH, path);
        attachmentMetaData.put(AttachmentConstants.COMMENT, comment);
        attachments.setId(documentId);
        attachments.setAttachmentName(fileName);
        attachments.setCreatedBy(authContext.getCurrentUser());
        attachments.setUserName(userName);
        attachments.setCreationDate(new Date());
        attachments.setAttachmentMetaData(attachmentMetaData);
        return attachments;
    }

    /**
     * This API is used to get the User Name from user object.
     * *
     */
    public String userName(String user, Optional<String> userInfo) {
        String userName = user;
        if (userInfo != null && user != null) {
            JsonParser parser = new JsonParser();
            JsonArray val = (JsonArray) parser.parse(userInfo.get());
            for (JsonElement userJson : val) {
                JsonObject users = userJson.getAsJsonObject();
                if (users.has(USER_NAME) && !StringUtils.isEmpty(users.get(USER_NAME))
                        && users.get(USER_NAME).getAsString().trim().contentEquals(user.trim())) {
                    userName = parseFromJsonElement(users, userName);
                    break;
                }
            }
        }
        return userName;
    }

    private String parseFromJsonElement(JsonObject users, String userName) {
        if (users.has(FIRST_NAME) && !StringUtils.isEmpty(users.get(FIRST_NAME))
                && users.has(LAST_NAME) && !StringUtils.isEmpty(users.get(LAST_NAME))) {
            userName = (users.get(FIRST_NAME).getAsString().trim() + SPACE
                    + users.get(LAST_NAME).getAsString().trim());
        } else if (users.has(FIRST_NAME) && !StringUtils.isEmpty(users.get(FIRST_NAME))) {
            userName = users.get(FIRST_NAME).getAsString().trim();
        } else if (users.has(LAST_NAME) && !StringUtils.isEmpty(users.get(LAST_NAME))) {
            userName = users.get(LAST_NAME).getAsString().trim();
        }
        return userName;
    }

    private HashMap<String, Object> retrieve(List<Attachment> attachments, String documentId) {
        LOGGER.debug("Retrieve method is called  for SR  {} ", documentId);
        HashMap<String, Object> response = new HashMap<>();
        int count = 0;
        int finalCount;
        String filePath;
        String fileName;
        Set<String> fileNames = new HashSet<>();
        Iterator<Attachment> iter = attachments.iterator();
        LinkedHashMap<String, String> attachmentMetaData;
        while (iter.hasNext()) {
            LinkedHashMap<String, Object> temp = ((LinkedHashMap<String, Object>) (Object) iter.next());
            fileNames.add(temp.get("attachmentName").toString());
            if (temp.get("id").toString().contentEquals(documentId)) {
                attachmentMetaData = ((LinkedHashMap<String, String>) (Object) temp.get("attachmentMetaData"));
                filePath = attachmentMetaData.get("filePath");
                fileName = temp.get("attachmentName").toString();
                finalCount = count;
                response.put("filePath", filePath);
                response.put("fileName", fileName);
                response.put("finalCount", finalCount);
            }
            count++;
        }
        response.put("fileNames", fileNames);
        return response;

    }

    @Override
    @Transactional
    public Map<String, Object> removeParticipant(String roomId, String targetUser) {
        String currentUser = authContext.getCurrentUser();
        LOGGER.info("Remove participant {} from room {} has been called by user {}", targetUser, roomId, currentUser);
        validateRemoveUserRequest(roomId, currentUser, targetUser);
        ChatRoomParticipant participant = removeParticipantInApp(roomId, targetUser);
        if (ChatRoomParticipantStatus.JOINED.equals(participant.getStatus())) {
            removeParticipantInRemote(roomId, targetUser, currentUser);
        }
        Map<String, Object> status = new HashMap<>();
        status.put("Status", "Success");
        LOGGER.info("User {} removed participant {} from room {} successfully", currentUser, targetUser, roomId);
        return status;
    }

    private void validateChannelCreationRequest(ChatRoomCreateDto request) {
        AssertUtil.notNull(request, "Room creation input can't be null");
        AssertUtil.notEmpty(request.getObjectIds(), "Reference domain object can't be null or empty");
        AssertUtil.notEmpty(request.getParticipants(), "Participants can't be null");
        AssertUtil.isTrue(!StringUtils.isEmpty(request.getEntityType()), "Entity type can't be null or empty");
        AssertUtil.isTrue(!StringUtils.isEmpty(request.getName()), "Room name can't be null or empty");
        AssertUtil.isTrue(!StringUtils.isEmpty(request.getPurpose()), "Purpose can't be null or empty");
        AssertUtil.isTrue(!StringUtils.isEmpty(request.getSituationType()),
                "Situation type can't be null or empty");
    }

    private void validatePostMessageRequest(String currentUser, Map<String, Object> request) {
        LOGGER.debug("Validating post message request");
        AssertUtil.notNull(request, "Post message can't be null");
        AssertUtil.notEmpty(request, "Post message can't be empty");
        AssertUtil.notNull(getRoomIdFromPostMessage(request),
                "Channel can't be null");
        AssertUtil.isTrue(getRoomIdFromPostMessage(request).trim().length() > 0,
                "Channel can't be empty");
        String roomId = getRoomIdFromPostMessage(request);
        validateRoomState(roomId, currentUser, "This room has been Resolved/Deleted."
                + "Messages cannot be posted to this room.");
        Optional<ChatRoom> room = getChatRoomById(roomId);
        if (!room.isPresent()) {
            throw new ChatException(ChatException.ErrorCode.ROOM_NOT_EXISTS);
        }
        boolean present = room.get()
                .getParticipants()
                .stream()
                .anyMatch(p -> p.getUserName().equals(currentUser)
                        && p.getStatus().equals(ChatRoomParticipantStatus.PENDING));
        AssertUtil.isTrue(!present,
                String.format("You are not authorize to resolve room %s", room.get().getRoomName()));

    }

    private void validateInviteUsersInputs(String roomId, String currentUser, AddUserToRoomDto request) {
        LOGGER.debug("Validating invite user request");
        roomIdInputValidation(roomId);
        AssertUtil.notNull(request, "Request can't be null");
        AssertUtil.notEmpty(request.getUsers(), "Users can't be empty");
        validateRoomState(roomId, currentUser, "New invitation can't be sent for resolved room");
    }

    private void validateResolveRoomInputs(String roomId, String currentUser, ResolveRoomDto request) {
        LOGGER.debug("Validating resolve room request");
        roomIdInputValidation(roomId);
        AssertUtil.notNull(request.getResolution(),
                "Resolution can't be null");
        AssertUtil.notEmpty(request.getResolution(), "Resolution can't be empty");
        request.getResolution()
                .forEach(resolution -> AssertUtil.isTrue(!StringUtils.isEmpty(resolution), "Invalid resolution type"));
        validateRoomState(roomId, currentUser, "Room is already resolved");
        Optional<ChatRoom> room = getChatRoomById(roomId);
        if (!room.isPresent()) {
            throw new ChatException(ChatException.ErrorCode.ROOM_NOT_EXISTS);
        }
        boolean present = room.get()
                .getParticipants()
                .stream()
                .anyMatch(p -> p.getUserName().equals(currentUser)
                        && p.getStatus().equals(ChatRoomParticipantStatus.PENDING));
        AssertUtil.isTrue(!present,
                String.format("You are not authorize to resolve room %s", room.get().getRoomName()));
    }

    private void validateRemoveUserRequest(String roomId, String currentUser, String targetUser) {
        LOGGER.debug("Validating resolve room request");
        roomIdInputValidation(roomId);
        validateRoomState(roomId, currentUser, "Room is already resolved,user can't be removed");
        Optional<ChatRoom> room = getChatRoomById(roomId);

        if (!room.isPresent()) {
            throw new ChatException(ChatException.ErrorCode.ROOM_NOT_EXISTS);
        }
        boolean present = room.get()
                .getParticipants()
                .stream()
                .anyMatch(p -> p.getUserName().equals(currentUser)
                        && p.getStatus().equals(ChatRoomParticipantStatus.PENDING));
        AssertUtil.isTrue(!present, String.format("You are not authorize to remove room %s", room.get().getRoomName()));
        AssertUtil.isTrue(!room.get().getCreatedBy().equals(targetUser), "Creator of can't be remove");
    }

    private void roomIdInputValidation(String roomId) {
        AssertUtil.isTrue(!StringUtils.isEmpty(roomId), "Room Id can't be null or empty");
    }

    private void validateRoomState(String roomId,
                                   String currentUser,
                                   String invalidStatusMsg) {
        Optional<ChatRoom> room = getChatRoomById(roomId);
        if (!room.isPresent()) {
            throw new ChatException(ChatException.ErrorCode.INVALID_CHAT_ROOM, roomId);
        }
        AssertUtil.isTrue(room.get().getStatus() != ChatRoomStatus.RESOLVED,
            invalidStatusMsg);
        boolean present = room.get().getParticipants().stream().anyMatch(p -> p.getUserName()
            .equalsIgnoreCase(currentUser));
        AssertUtil.isTrue(present, String.format("You are not part of %s room", room.get().getRoomName()));
    }

    private HttpEntity<Map> createRemoteServerChatRoom(ChatRoomCreateDto request) {
        LOGGER.info("Creating new situation room {} by user {} requested, with details {}", request.getName(),
                authContext.getCurrentUser(), request);
        ProxyTokenMapping tokenMapping = getUserTokenMapping(authContext.getCurrentUser());
        HttpHeaders headers = getHttpHeader(tokenMapping.getProxyToken());
        HttpEntity<CreateChannelDto> requestEntity
                = new HttpEntity<>(buildRemoteChannelCreationRequest(request), headers);
        HttpEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(getChannelPath()),
                HttpMethod.POST,
                requestEntity,
                Map.class);

        LOGGER.info("Channel {} creation done in remote system", request.getName());

        return response;
    }

    private void createChatRoomInApp(ChatRoomCreateDto request, String roomId) {
        ChatRoom chatRoom = buildChatRoom(roomId, request);
        LOGGER.debug("Going to create chat room {} meta information into system", chatRoom.getRoomName());
        saveChatRoom(chatRoom);
        LOGGER.debug("Chat room {} meta information persisted successfully", chatRoom.getRoomName());
    }

    private ChatRoomParticipant removeParticipantInApp(String roomId, String targetUser) {
        Optional<ChatRoom> roomRecord = getChatRoomById(roomId);
        if (!roomRecord.isPresent()) {
            throw new ChatException(ChatException.ErrorCode.INVALID_CHAT_ROOM, roomId);
        }
        ChatRoom room = roomRecord.get();
        Set<ChatRoomParticipant> participants = room.getParticipants();
        Optional<ChatRoomParticipant> targetParticipant = participants
            .stream()
            .filter(p -> p.getUserName().equalsIgnoreCase(targetUser)).findAny();

        if (!targetParticipant.isPresent()) {
            throw new ChatException(ChatException.ErrorCode.PARTICIPANT_NOT_BELONG);
        }
        room.getParticipants().remove(targetParticipant.get());
        room.setLmd(new Date());
        saveChatRoom(room);
        return targetParticipant.get();
    }

    private void removeParticipantInRemote(String roomId, String targetUser, String currentUser) {
        ProxyTokenMapping callerTokenMapping = getUserTokenMapping(currentUser);
        ProxyTokenMapping targetUserTokenMapping = getUserTokenMapping(targetUser);
        HttpHeaders headers = getHttpHeader(callerTokenMapping.getProxyToken());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<CreateChannelDto> requestEntity
                = new HttpEntity<>(null, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(removeParticipantPath(roomId, targetUserTokenMapping.getRemoteUserId())),
                HttpMethod.DELETE,
                requestEntity,
                Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            LOGGER.error("Error {} while fetching unread count for channel {} for user {}",
                    response.getBody(),
                    roomId,
                    currentUser);
            throw new ResourceAccessException(response.getBody() != null
                    ? response.getBody().toString() :
                    "Remote system unknown exception.");
        }
    }

    private Optional<ChatRoom> getChatRoomById(String id) {
        LOGGER.debug("Fetching chat room by id {}", id);
        return roomRepository.findById(id);

    }


    private List<ChatRoomParticipant> getUserAllRoomsOfType(String type, String currentUser) {
        List<ChatRoomParticipant> participants;
        participants = participantRepository.findByUserNameAndRoomStatusOrderByRoomLmdDesc(currentUser,
                ChatRoomStatus.valueOf(type));
        return participants;
    }

    /**
     * Method to get all Chat Room participants for current user.
     *
     * @return Returns a list of Chat Room Participants.
     */
    @Cacheable(value = "channels", key = "#currentUser")
    public List<ChatRoomParticipant> getUserAllRooms(String currentUser) {
        List<ChatRoomParticipant> participants;
        participants = participantRepository.findByUserNameOrderByRoomLmdDesc(currentUser);
        return participants;
    }

    private List<ChatRoomParticipant> getRoomsByParticipantStatus(String type, String currentUser) {
        List<ChatRoomParticipant> participants;
        participants = participantRepository.findByUserNameAndStatusOrderByRoomLmdDesc(currentUser,
                ChatRoomParticipantStatus.valueOf(type));
        return participants;
    }

    private void setupParticipantsIfNotBefore(List<String> users, String teamId) {
        users.forEach(user -> setupUser(user, teamId));
    }

    private ProxyTokenMapping setupUser(String appUserId, String teamId) {
        LOGGER.info("Checking whether user {} exist in system or not", appUserId);
        ProxyTokenMapping proxyTokenMapping = getUserTokenMapping(appUserId);
        if (proxyTokenMapping == null) {
            LOGGER.warn("User {} does not exists into system", appUserId);
            Map<String, Object> newUserResponse = crateUser(appUserId);
            setupRoles(remoteUserId(newUserResponse));
            proxyTokenMapping = addUserTokenMapping(appUserId, remoteUserId(newUserResponse));
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
            throw new ChatException(ChatException.ErrorCode.UNABLE_TO_JOIN_ROOM);
        }
        LOGGER.info("User {} added to team {} successfully", remoteUserId, teamId);
    }

    private Map<String, Object> addParticipantsToRoom(String user, String roomId) {
        Optional<ChatRoom> roomRecord = getChatRoomById(roomId);
        if (!roomRecord.isPresent()) {
            throw new ChatException(ChatException.ErrorCode.INVALID_CHAT_ROOM, roomId);
        }
        ChatRoom room = roomRecord.get();

        Optional<ChatRoomParticipant> participantRecord = room.getParticipants()
            .stream().filter(p -> p.getUserName().equalsIgnoreCase(user)).findFirst();

        if (!participantRecord.isPresent()) {
            throw new ChatException(ChatException.ErrorCode.USER_NOT_INVITED, user, roomId);
        }
        ChatRoomParticipant participant = participantRecord.get();
        if (participant.getStatus() == ChatRoomParticipantStatus.JOINED) {
            LOGGER.info("User {} already joined to room {} not doing anything", user, roomId);
            Map<String, Object> response = new HashMap<>();
            response.put("channel_id", roomId);
            response.put("user_id", user);
            return response;
        }
        Date date = new Date();
        participant.setJoinedAt(date);
        participant.setStatus(ChatRoomParticipantStatus.JOINED);
        room.setLmd(date);
        room.setTotalMessageCount(room.getTotalMessageCount() + 1);
        String creatorToken = getUserTokenMapping(room.getCreatedBy()).getProxyToken();
        String remoteUserId = getUserTokenMapping(user).getRemoteUserId();
        LOGGER.info("Going to add user {} for room {} as remote user id {} into remote system",
                user, roomId, remoteUserId);
        Map response = joinRoom(creatorToken, remoteUserId, roomId);
        saveChatRoom(room);
        LOGGER.info("Room {} last modified updated successfully for new joined {}", roomId, user);
        return (Map<String, Object>) response;
    }

    private Map joinRoom(String callerToken, String remoteUserId, String roomId) {
        HttpHeaders headers = getHttpHeader(callerToken);
        HttpEntity<AddParticipantDto> requestEntity
                = new HttpEntity<>(
                buildChannelMemberRequest(remoteUserId, ""), headers);
        HttpEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(getAddParticipantPath(roomId)),
                HttpMethod.POST,
                requestEntity,
                Map.class);
        if (!((ResponseEntity<Map>) response).getStatusCode().is2xxSuccessful()) {
            throw new ChatException(ChatException.ErrorCode.UNABLE_TO_JOIN_ROOM);
        }
        return response.getBody();
    }

    private ProxyTokenMapping setupAccessToken(ProxyTokenMapping mapping) {
        LOGGER.info("Generating access token in remote system for user {} ", mapping.getAppUserId());
        HttpHeaders headers = getHttpHeader(adminAccessToken);
        Map<String, String> request = teamRequest();
        HttpEntity<RemoteUserDto> requestEntity = new HttpEntity(request, headers);
        HttpEntity<Map> response = restTemplate.exchange(
                getRemoteActionUrl(getTokenPath(mapping.getRemoteUserId())),
                HttpMethod.POST,
                requestEntity,
                Map.class);
        if (!((ResponseEntity<Map>) response).getStatusCode().is2xxSuccessful()) {
            throw new ChatException(ChatException.ErrorCode.UNABLE_TO_UPDATE_ROLE);
        }
        LOGGER.info("Access token generated successfully for user {}", mapping.getAppUserId());
        mapping.setProxyToken(token(response.getBody()));
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
        ResponseEntity<Map> response = restTemplate.exchange(
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
            throw new ChatException(ChatException.ErrorCode.UNABLE_TO_UPDATE_ROLE);
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
        Date currentDate = new Date();
        proxyTokenMapping.setCreationDate(currentDate);
        proxyTokenMapping.setLmd(currentDate);
        ProxyTokenMapping mapping = saveProxyTokenMapping(proxyTokenMapping);
        LOGGER.info("Remote user id {} mapping for appuser {} finish successfully", mapping.getRemoteUserId(), appUser);
        return mapping;
    }

    private ProxyTokenMapping getUserTokenMapping(String user) {
        return tokenRepository.findByAppUserId(user);
    }

    private ProxyTokenMapping saveProxyTokenMapping(ProxyTokenMapping proxyTokenMapping) {
        return tokenRepository.save(proxyTokenMapping);
    }

    private void archiveMessage(Map<String, Object> chat) {
        String roomId = getRoomIdFromPostMessage(chat);
        LOGGER.debug("Going to archive conversion for room {}", roomId);
        Optional<ChatRoom> record = getChatRoomById(getRoomIdFromPostMessage(chat));
        if (!record.isPresent()) {
            throw new ChatException(ChatException.ErrorCode.INVALID_CHAT_ROOM, roomId);
        }
        ChatRoom room = record.get();
        List<Object> chats = (List<Object>) ChatRoomUtil.byteArrayToObject(room.getChats());
        chats.add(chat);
        room.setTotalMessageCount(room.getTotalMessageCount() + 1);
        room.setChats(ChatRoomUtil.objectToByteArray(chats));
        Date date = new Date();
        room.setLastPostAt(date);
        room.setLmd(date);
        saveChatRoom(room);
        LOGGER.debug("Chat archived successfully for room {}", getRoomIdFromPostMessage(chat));
    }

    private void updateParticipantOfRooms(List<String> users, String roomId) {
        LOGGER.debug("Going to add new participants for room {}", roomId);
        Optional<ChatRoom> record = getChatRoomById(roomId);
        if (!record.isPresent()) {
            throw new ChatException(ChatException.ErrorCode.INVALID_CHAT_ROOM, roomId);
        }
        ChatRoom room = record.get();
        Set<ChatRoomParticipant> existingUsers = room.getParticipants();
        existingUsers.addAll(buildParticipants(room, users));
        room.setParticipants(existingUsers);
        saveChatRoom(room);
        LOGGER.debug("Participants updated successfully {}", room);
    }

    private ChatRoom saveChatRoom(ChatRoom chatRoom) {
        LOGGER.debug("Going to persist chat room {} meta information into system", chatRoom.getRoomName());
        ChatRoom savedChatRoot = roomRepository.save(chatRoom);
        LOGGER.debug("Chat room {} meta information persisted successfully", chatRoom.getRoomName());
        return savedChatRoot;
    }

    private ChatContext toChatContext(ChatRoom room, String caller) {
        ChatContext context = new ChatContext();
        context.setId(room.getId());
        context.setName(room.getRoomName());
        context.setTeamId(room.getTeamId());
        context.setCreatedBy(room.getCreatedBy());
        context.setEntityType(room.getEntityType());
        context.setRoomStatus(room.getStatus().name());
        context.setAttachments(room.getAttachments());
        if (room.getResolution() != null) {
            ChatRoomResolution resolution = room.getResolution();
            context.setResolution(resolution.getResolution());
            context.setResolutionRemark(resolution.getRemark());
            context.setResolvedBy(resolution.getResolvedBy());
            context.setResolvedAt(resolution.getDate().getTime());
            context.setResolvedUser(resolution.getResolvedUser());
        }

        List<String> channelUsers = new ArrayList<>();
        room.getParticipants().forEach(participant -> {
            if (participant.getUserName().equalsIgnoreCase(caller)) {
                context.setYourStatus(participant.getStatus());
                context.setResolveReadStatus(participant.isResolutionRead());
            }
            channelUsers.add(participant.getUserName());
        });
        context.setTotalMessageCount(room.getTotalMessageCount());
        context.setParticipants(channelUsers);
        context.setPurpose(room.getDescription());
        context.setSituationType(room.getSituationType());
        context.setEntity(ChatRoomUtil.jsonToObject(
                (String) ChatRoomUtil.byteArrayToObject(room.getContexts())));
        context.setCreatedAt(room.getCreationDate().getTime());
        context.setUpdatedAt(room.getLmd().getTime());
        context.setLastPostAt(room.getLastPostAt().getTime());

        context.setUserName(room.getUserName());
        context.setDeletedAt(0);
        context.setExpiredAt(0);
        context.setExtraUpdateAt(0);
        return context;
    }

    private ChatRoom buildChatRoom(String id, ChatRoomCreateDto request) {
        ChatRoom room = new ChatRoom();
        Optional<String> userInfo;
        userInfo = userData();
        String userName = userName(authContext.getCurrentUser(), userInfo);
        room.setId(id);
        room.setRoomName(request.getName());
        room.setEntityType(request.getEntityType());
        room.setCreatedBy(authContext.getCurrentUser());
        room.setDescription(request.getPurpose());
        room.setTeamId(channelTeamId);
        room.setStatus(ChatRoomStatus.OPEN);
        room.setResolution(null);
        room.setSituationType(request.getSituationType());
        room.setParticipants(buildParticipantsIncludingCreator(room, request.getParticipants()));
        Date time = new Date();
        room.setCreationDate(time);
        room.setLmd(time);
        room.setLastPostAt(time);
        room.setTotalMessageCount(1);
        room.setTid(authContext.getCurrentTid());
        room.setDomainObjectIds(request.getObjectIds());
        room.setChats(ChatRoomUtil.objectToByteArray(Lists.newArrayList()));
        room.setUserName(userName);
        List<Object> chatEntities = new ArrayList<>();
        for (String entityId : request.getObjectIds()) {
            Object entity = entityReaderFactory.getEntity(request.getEntityType(), entityId);
            chatEntities.add(entity);
        }
        room.setContexts(ChatRoomUtil.objectToByteArray(ChatRoomUtil.objectToJson(chatEntities)));
        return room;
    }

    private Set<ChatRoomParticipant> buildParticipantsIncludingCreator(ChatRoom room, List<String> joinees) {

        ChatRoomParticipant invitee = buildParticipant(room, authContext.getCurrentUser());
        invitee.setJoinedAt(new Date());
        invitee.setStatus(ChatRoomParticipantStatus.JOINED);
        Set<ChatRoomParticipant> participants = buildParticipants(room, joinees);
        if (participants.contains(invitee)) {
            participants.remove(invitee);
            participants.add(invitee);
        }
        return participants;
    }

    private Set<ChatRoomParticipant> buildParticipants(ChatRoom room, List<String> joinees) {
        Set<ChatRoomParticipant> participants = new HashSet<>();
        participants.add(buildParticipant(room, authContext.getCurrentUser()));
        joinees.forEach(user -> participants.add(buildParticipant(room, user)));
        return participants;
    }

    private ChatRoomParticipant buildParticipant(ChatRoom room, String userName) {
        ChatRoomParticipant participant = new ChatRoomParticipant();
        participant.setId(toParticipantId(userName, room.getId()));
        participant.setRoom(room);
        participant.setUserName(userName);
        participant.setInvitedAt(new Date());
        participant.setStatus(ChatRoomParticipantStatus.PENDING);
        return participant;
    }

    private CreateChannelDto buildRemoteChannelCreationRequest(ChatRoomCreateDto request) {
        CreateChannelDto dto = new CreateChannelDto();
        dto.setTeamId(channelTeamId);
        dto.setName(generator.next());
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

    private Map<String, String> teamRequest() {
        Map<String, String> request = new HashMap<>();
        request.put("description", "situation room");
        return request;
    }

    private RoleDto buildRoles() {
        return new RoleDto("team_user channel_admin "
                + "channel_user system_user_access_token");
    }

    private ChatRoomResolution buildResolution(ResolveRoomDto request, String resolveBy) {
        Optional<String> userInfo;
        String userName;
        ChatRoomResolution resolution = new ChatRoomResolution();
        userInfo = this.userData();
        userName = userName(resolveBy,userInfo);
        resolution.setDate(new Date());
        resolution.setRemark(request.getRemark());
        resolution.setResolvedBy(resolveBy);
        resolution.setResolvedUser(userName);
        resolution.setResolution(request.getResolution());
        return resolution;
    }

    private String getRemoteActionUrl(String cxtPath) {
        return buildUrlString(mattermostUrl, cxtPath).toString();
    }

    private static String getUsersPath() {
        return MATTERMOST_USERS;
    }

    private static String getMessagePath() {
        return MATTERMOST_POSTS;
    }

    private static String getChannelPath() {
        return MATTERMOST_CHANNELS;
    }

    private static String getAddParticipantPath(String roomId) {
        return getChannelPath() + "/" + roomId + "/members";
    }

    private static String removeParticipantPath(String room, String user) {
        return getAddParticipantPath(room) + "/" + user;
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

    private static String getChannelUnreadCountPath(String userId, String channelId) {
        return getUsersPath() + "/" + userId + "/" + getChannelPath() + "/" + channelId + "/unread";
    }

    private static String roomId(Map<String, Object> input) {
        return (String) input.get("id");
    }

    private static String remoteUserId(Map<String, Object> input) {
        return (String) input.get("id");
    }

    private String token(Map body) {
        return (String) body.get("token");
    }

    private String getRoomIdFromPostMessage(Map<String, Object> request) {
        return (String) request.get("channel_id");
    }

    private String toParticipantId(String userName, String roomId) {
        return userName + "-" + roomId;
    }

    private HttpHeaders getHttpHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, HttpHeaders.ACCEPT);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }

    @VisibleForTesting
    protected void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @VisibleForTesting
    protected void setChannelTeamId(String teamId) {
        this.channelTeamId = teamId;
    }

    @VisibleForTesting
    protected void setMattermostUrl(String mattermostUrl) {
        this.mattermostUrl = mattermostUrl;
    }

    @VisibleForTesting
    protected void setuserUrl(String umsUri) {
        this.umsUri = umsUri;
    }

    /**
     * Method is return the chat room details based on given search string.
     * Allow Search within SR
     * The search will search only on a fixed set of fields like -
     *          SR Names,
     *          SR descriptions,
     *          transaction object ID,
     *          Issue type,
     *          Page type,
     *          resolution,
     * we will search across open and resolved rooms 
     *
     * @param requestParams     -- input value contain search text and object id
     * @return List    -- response will be search result in form of chat context object list
     */
    @Override
    public List<ChatContext> searchChannels(Map<String, String> requestParams) {
        String currentUser = authContext.getCurrentUser();
        LOGGER.debug("User {} is searching in SR with search string {} ", currentUser, requestParams);

        List<ChatContext> searchResultChannels;
        List<ChatRoom> tempRooms;
        String searchQueryValue = null;
        String domainObjectId = null;

        if (!CollectionUtils.isEmpty(requestParams)) {
            searchQueryValue = requestParams.get(SearchConstants.SEARCH_TEXT);
            domainObjectId = requestParams.get(DOMAIN_OBJECT_ID);
        }

        if (!StringUtils.isEmpty(searchQueryValue)) {
            String searchTerm = PERCENT_SIGN + searchQueryValue.toUpperCase() + PERCENT_SIGN;
            if (StringUtils.isEmpty(domainObjectId)) {
                tempRooms = roomRepository.getChatRoomsBySearch(searchTerm, currentUser);
            } else {
                domainObjectId = QUOTATION_MARK + domainObjectId + QUOTATION_MARK;
                tempRooms = roomRepository.getChatRoomsBySearchInObjectId(searchTerm, currentUser, domainObjectId);
            }
        } else {
            if (StringUtils.isEmpty(domainObjectId)) {
                List<ChatRoomParticipant> currentUserRooms = getUserAllRooms(currentUser);
                tempRooms = currentUserRooms.stream().map(ChatRoomParticipant::getRoom).collect(Collectors.toList());
            } else {
                domainObjectId = QUOTATION_MARK + domainObjectId + QUOTATION_MARK;
                tempRooms = roomRepository.getChannelByObjectIdAndUser(domainObjectId, currentUser);
            }
        }

        if (tempRooms != null) {
            searchResultChannels = tempRooms.stream().map(room -> toChatContext(room, currentUser))
                    .collect(Collectors.toList());
        } else {
            searchResultChannels = new ArrayList<>();
        }

        return searchResultChannels;
    }

    /**
     * This API is used to get the User's data object.
     * *
     */
    @Cacheable(value = "usersInfo")
    public Optional<String> userData() {
        Optional<String> userInfo;
        userInfo = dctService.restTemplateForTenantService(umsUri);
        return userInfo;
    }

}
