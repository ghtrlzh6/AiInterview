package com.aiinterview.service;

import java.util.List;
import java.util.Map;

public interface SystemConfigService {

    void reload();

    String get(String key);

    String get(String key, String defaultValue);

    void set(String key, String value);

    void setBatch(Map<String, String> updates);

    List<String> keysByPrefix(String prefix);

    boolean isMaskedValue(String value);
}
