/**
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.enums;

public enum EmailTemplateEnum {
    OPEN_SITUATION_ROOM("openRoom"),
    RESOLVED_SITUATION_ROOM("resolvedRoom");
    private String templateName;

    EmailTemplateEnum(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }
}
