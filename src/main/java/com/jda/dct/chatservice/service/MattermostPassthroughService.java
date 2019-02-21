/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */

package com.jda.dct.chatservice.service;

import static com.jda.dct.chatservice.utils.ChatRoomUtil.buildUrlString;

import com.google.common.annotations.VisibleForTesting;
import com.jda.dct.chatservice.repository.ProxyTokenMappingRepository;
import com.jda.dct.contexts.AuthContext;
import com.jda.dct.domain.ProxyTokenMapping;
import java.util.Enumeration;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;


/**
 * Mattermost pass trough service.
 */

@Service
public class MattermostPassthroughService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SituationRoomServiceImpl.class);

    @Value("${dct.situationRoom.mattermost.host}")
    private String mattermostUrl;

    private final AuthContext authContext;
    private final ProxyTokenMappingRepository tokenRepository;

    private RestTemplate restTemplate = new RestTemplate();

    /**
     * Mattermost passthrough service.
     *
     * @param authContext     Auth context.
     * @param tokenRepository Proxy token repository.
     */
    public MattermostPassthroughService(@Autowired AuthContext authContext,
                                        @Autowired ProxyTokenMappingRepository tokenRepository) {
        this.authContext = authContext;
        this.tokenRepository = tokenRepository;
    }

    /**
     * Pass through the call to mattermost server. This call inject user access token for
     * mattermost server.
     *
     * @param body    request body received from client.
     * @param method  HttpMethod.
     * @param request request object.
     * @return ResponseEntity received from mattermost. On exception propagate the same.
     */
    public ResponseEntity passthrough(@RequestBody Object body,
                                      HttpMethod method,
                                      HttpServletRequest request) {
        String requestUrl = request.getRequestURI();
        String queryString = request.getQueryString();
        String currentUser = authContext.getCurrentUser();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("User {} called for {}-{} with query param {}", currentUser,
                method, requestUrl, queryString);
        }

        LOGGER.info("User {} called for {}-{} with query param {}", currentUser,
            method, requestUrl, queryString);

        ProxyTokenMapping proxyTokenMapping = getToken(currentUser);
        if (Objects.isNull(proxyTokenMapping)) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("You are not authorize for situation room");
        }

        HttpHeaders headers = upgradeRequestHeader(proxyTokenMapping.getProxyToken(), request);
        HttpEntity httpEntity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.exchange(getRemoteUrl(requestUrl, queryString),
                method,
                httpEntity,
                Object.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getRawStatusCode())
                .headers(e.getResponseHeaders())
                .body(e.getResponseBodyAsString());
        }

    }

    private String getRemoteUrl(String requestUrl, String queryParams) {
        requestUrl = requestUrl.replaceFirst("/chat/passthrough", "");
        return buildUrlString(mattermostUrl, requestUrl, queryParams).toString();
    }

    private ProxyTokenMapping getToken(String currentUer) {
        return tokenRepository.findByAppUserId(currentUer);
    }

    private HttpHeaders upgradeRequestHeader(String token, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.set(headerName, request.getHeader(headerName));
        }
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }

    @VisibleForTesting
    protected void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @VisibleForTesting
    protected void setMattermostUrl(String mattermostUrl) {
        this.mattermostUrl = mattermostUrl;
    }
}
