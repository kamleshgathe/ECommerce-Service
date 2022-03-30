/**
 * Copyright © 2022, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.repository;

public class Analytics {
    /**
     * Constructor for Analytics.
     */
    public Analytics(String reportName) {
        super();
        this.reportName = reportName;

    }

    public Analytics() {
        super();
    }


    public String reportName;

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }


}

