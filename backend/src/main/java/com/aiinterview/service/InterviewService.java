package com.aiinterview.service;

import com.aiinterview.dto.interview.CodingSubmitRequest;
import com.aiinterview.dto.interview.SendMessageRequest;
import com.aiinterview.dto.interview.StartInterviewRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

public interface InterviewService {

    Map<String, Object> start(Long userId, StartInterviewRequest request);

    SseEmitter sendMessage(Long userId, Long sessionId, SendMessageRequest request);

    Map<String, Object> end(Long userId, Long sessionId);

    Map<String, Object> getSession(Long userId, Long sessionId);

    Map<String, Object> getMessages(Long userId, Long sessionId);

    Map<String, Object> codingSubmit(Long userId, Long sessionId, CodingSubmitRequest request);

    Map<String, Object> latestCodingSubmit(Long userId, Long sessionId, Long questionId);
}
