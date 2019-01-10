package com.jda.dct.chatserver.controller

import com.google.common.collect.Maps
import com.jda.dct.chatservice.controller.SituationRoomController
import com.jda.dct.chatservice.dto.upstream.SituationRoomDto
import com.jda.dct.chatservice.dto.upstream.TokenDto
import com.jda.dct.chatservice.service.SituationRoomService
import groovy.json.JsonSlurper
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class SituationRoomControllerSpec extends Specification {

    def "test throw exception if service is null"() {
        when: "Create the purchase order"
        new SituationRoomController(null)
        then:
        thrown IllegalArgumentException
    }

    def "test no exception"() {
        given:
        def service = Mock(SituationRoomService);
        when: "Create the purchase order"

        new SituationRoomController(service)
        then:
        noExceptionThrown()
    }

    def "should return token"() {
        given:
        def token = new TokenDto();
        token.teamId = "abcd"
        token.token = "token1"
        def service = Mock(SituationRoomService);
        service.getSessionToken() >> token;

        when: "Calling get token of a user"
        def SituationRoomController controller = new SituationRoomController(service);
        ResponseEntity<TokenDto> tokenResponse = controller.getAccessToken();

        then:
        tokenResponse.statusCode.value() == 200
        tokenResponse.body.token == "token1"
        tokenResponse.body.teamId == "abcd"
    }

    def "test create channel"() {
        given:
        /*SituationRoomDto channelDto = new SituationRoomDto();
        channelDto.setName("name1");
        channelDto.setEntityType("shipment")
        channelDto.setHeader("header1");
        channelDto.setObjectIds(Lists.newArrayList("o1","o2"))
        channelDto.setParticipants(Lists.newArrayList("p1","p2"))
        channelDto.setType("P")
        channelDto.setSituationType("shipment delayed")
        channelDto.setPurpose("purpose")
        channelDto.setTeamId("team1")*/
        def exepectedResponse = Maps.newHashMap();
        exepectedResponse.put("id","1111");
        exepectedResponse.put("created_at",new Date());
        exepectedResponse.put("name","channels_1")
        exepectedResponse.put("team_id","2222");
        SituationRoomDto situationRoomDto = new SituationRoomDto();
        def service = Mock(SituationRoomService);
        service.createChannel(_ as SituationRoomDto) >> exepectedResponse;

        when: "Calling get token of a user"
        def controller = new SituationRoomController(service);
        ResponseEntity<Map<String,Object>> responseEntity =controller.addNewChannel(Mock(SituationRoomDto))
        then:
        responseEntity.getStatusCode().value() == 200
        responseEntity.getBody() == exepectedResponse
    }

    def "test post message" () {
        given:
         def jsonSlurper = new JsonSlurper()
         Map<String,Object> postObject = jsonSlurper.parseText('{"channel_id": "9k3ebut7ij85db74igboy97a1o",' +
                '"message": "Sending just like","root_id": "","file_ids":null,"props": ""}')

        Map<String,Object> expectedObj = jsonSlurper.parseText('{"id": "kwysn85zubfpzdno4nzjx7k9ao",' +
                '"create_at": 1547061027479,"update_at": 1547061027479,"edit_at": 0, "delete_at": 0,' +
                '"is_pinned": false,"user_id": "jtjktxa6n7d7ig1f4a9p9ih9gc","channel_id": "9k3ebut7ij85db74igboy97a1o",' +
                '"root_id": "","parent_id": "","original_id": "","message": "Sending just like",' +
                '"type": "","props": {}, "hashtags": "","pending_post_id": ""}')

        def service = Mock(SituationRoomService);
        service.postMessage(_ as Map) >> expectedObj;

        when:
        def controller = new SituationRoomController(service);
        ResponseEntity<Map<String,Object>> responseEntity =controller.postMessageToChannel(postObject)

        then:
        responseEntity.statusCode.value() == 200
        responseEntity.getBody() == expectedObj
    }
}
