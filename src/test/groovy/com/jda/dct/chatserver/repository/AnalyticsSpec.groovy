/*
 * Copyright © 2022, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.repository

import com.jda.dct.chatservice.repository.Analytics

import spock.lang.Specification
class AnalyticsSpec extends Specification {

    def "test analytics class"() {
        given: "Analytics constructor without arguments"
        def  analytics = new Analytics()
        when: "set report name"
        analytics.setReportName("test")
        then: "should get report name"
        analytics.getReportName() == "test"
    }

    def "test analytics class with arguments"() {
        given: "Analytics constructor with arguments"
        def  analytics = new Analytics("test")
        when: "set report name"
        def reportName = analytics.getReportName()
        then: "should get report name"
        reportName == "test"
    }
}
