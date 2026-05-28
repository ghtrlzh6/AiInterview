package com.aiinterview.util;

import java.util.Map;

public final class PromptTemplateUtil {

    private PromptTemplateUtil() {
    }

    public static String render(String template, Map<String, String> variables) {
        if (template == null) return "";
        if (variables == null) return template;
        String result = template;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue() != null ? e.getValue() : "");
        }
        return result;
    }
}
