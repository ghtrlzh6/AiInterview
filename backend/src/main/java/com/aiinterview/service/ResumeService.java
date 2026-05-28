package com.aiinterview.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ResumeService {

    Map<String, Object> upload(Long userId, MultipartFile file);

    Map<String, Object> getStatus(Long userId, Long resumeId);

    List<Map<String, Object>> listProjects(Long userId, Long resumeId);
}
