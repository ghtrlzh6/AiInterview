package com.aiinterview.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aiinterview.common.BusinessException;
import com.aiinterview.entity.ResumeProject;
import com.aiinterview.entity.User;
import com.aiinterview.entity.UserResume;
import com.aiinterview.mapper.ResumeProjectMapper;
import com.aiinterview.mapper.UserMapper;
import com.aiinterview.mapper.UserResumeMapper;
import com.aiinterview.service.ResumeService;
import com.aiinterview.service.ai.LlmService;
import com.aiinterview.util.FileUploadUtil;
import com.aiinterview.util.ResumeSectionParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final UserResumeMapper resumeMapper;
    private final ResumeProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final LlmService llmService;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public Map<String, Object> upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please upload a PDF file");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new BusinessException("Only PDF files are supported");
        }
        Path target = null;
        try {
            Path dir = Paths.get(uploadDir, "resumes", String.valueOf(userId)).toAbsolutePath().normalize();
            String filename = UUID.randomUUID() + ".pdf";
            target = dir.resolve(filename);
            FileUploadUtil.saveMultipart(file, target);
            Path savedPath = target;

            UserResume resume = new UserResume();
            resume.setUserId(userId);
            resume.setFileUrl("/uploads/resumes/" + userId + "/" + filename);
            resume.setFileName(originalName);
            resume.setParseStatus("PENDING");
            resumeMapper.insert(resume);
            taskExecutor.execute(() -> parseAsync(resume.getId(), savedPath));

            Map<String, Object> data = new HashMap<>();
            data.put("resumeId", resume.getId());
            data.put("parseStatus", resume.getParseStatus());
            return data;
        } catch (Exception e) {
            log.warn("Resume upload failed for user {}", userId, e);
            if (target != null) {
                try {
                    Files.deleteIfExists(target);
                } catch (Exception ignored) {
                    // best-effort cleanup
                }
            }
            throw new BusinessException("Resume upload failed: " + e.getClass().getSimpleName());
        }
    }

    @Async("taskExecutor")
    public void parseAsync(Long resumeId, Path filePath) {
        UserResume resume = resumeMapper.selectById(resumeId);
        if (resume == null) return;
        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            resume.setResumeTextMd(text);
            resume.setParseStatus("SUCCESS");
            resumeMapper.updateById(resume);

            Map<String, String> sections = ResumeSectionParser.extractSections(text);
            syncProfileFromResume(resume.getUserId(), sections);
            extractProjects(resume, sections.getOrDefault(ResumeSectionParser.KEY_PROJECT, text));
        } catch (Exception e) {
            log.warn("Resume parse failed", e);
            resume.setParseStatus("FAILED");
            resume.setRemark(e.getMessage());
            resumeMapper.updateById(resume);
        }
    }

    private void syncProfileFromResume(Long userId, Map<String, String> sections) {
        if (sections.isEmpty()) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        if (sections.containsKey(ResumeSectionParser.KEY_EDUCATION)) {
            user.setEducationExperience(sections.get(ResumeSectionParser.KEY_EDUCATION));
            Map<String, String> hints = new HashMap<>();
            if (StringUtils.hasText(user.getSchool())) {
                hints.put("school", user.getSchool());
            }
            if (StringUtils.hasText(user.getMajor())) {
                hints.put("major", user.getMajor());
            }
            ResumeSectionParser.fillSchoolMajor(hints, sections.get(ResumeSectionParser.KEY_EDUCATION));
            if (hints.containsKey("school") && !StringUtils.hasText(user.getSchool())) {
                user.setSchool(hints.get("school"));
            }
            if (hints.containsKey("major") && !StringUtils.hasText(user.getMajor())) {
                user.setMajor(hints.get("major"));
            }
        }
        if (sections.containsKey(ResumeSectionParser.KEY_PERSONAL_SKILLS)) {
            user.setPersonalSkills(sections.get(ResumeSectionParser.KEY_PERSONAL_SKILLS));
        }
        if (sections.containsKey(ResumeSectionParser.KEY_PROJECT)) {
            user.setProjectExperience(sections.get(ResumeSectionParser.KEY_PROJECT));
        }
        if (sections.containsKey(ResumeSectionParser.KEY_INTERNSHIP)) {
            user.setInternshipExperience(sections.get(ResumeSectionParser.KEY_INTERNSHIP));
        }
        userMapper.updateById(user);
    }

    private void extractProjects(UserResume resume, String text) {
        projectMapper.delete(new LambdaQueryWrapper<ResumeProject>().eq(ResumeProject::getResumeId, resume.getId()));
        List<ResumeProject> projects = llmService.isAvailable()
                ? extractProjectsWithLlm(resume.getId(), text)
                : List.of();
        if (projects.isEmpty()) {
            projects = extractProjectsByRule(resume.getId(), text);
        }
        int order = 1;
        for (ResumeProject project : projects.stream().limit(5).toList()) {
            project.setSortOrder(order++);
            projectMapper.insert(project);
        }
    }

    private List<ResumeProject> extractProjectsWithLlm(Long resumeId, String text) {
        try {
            String prompt = "Extract up to 3 project experiences from this resume text as a JSON array. "
                    + "Fields: projectName, summaryMd, techStackTokens(string array). Return JSON only.\n\n"
                    + truncate(text, 5000);
            String raw = llmService.chat(List.of(new LlmService.ChatMessage("user", prompt)));
            JSONArray arr = JSONUtil.parseArray(extractJsonArray(raw));
            List<ResumeProject> result = new ArrayList<>();
            for (Object item : arr) {
                JSONObject obj = JSONUtil.parseObj(item);
                ResumeProject p = new ResumeProject();
                p.setResumeId(resumeId);
                p.setProjectName(defaultString(obj.getStr("projectName"), "Resume project"));
                p.setSummaryMd(defaultString(obj.getStr("summaryMd"), truncate(text, 300)));
                p.setTechStackTokens(obj.getJSONArray("techStackTokens") == null
                        ? inferTechTokens(text)
                        : obj.getJSONArray("techStackTokens").toList(String.class));
                result.add(p);
            }
            return result;
        } catch (Exception e) {
            log.warn("LLM resume project extraction failed, fallback to rules", e);
            return List.of();
        }
    }

    private List<ResumeProject> extractProjectsByRule(Long resumeId, String text) {
        String normalized = text == null ? "" : text.replace("\r", "\n");
        List<String> blocks = Arrays.stream(normalized.split("\\n\\s*\\n|项目经历|项目经验|Projects?|PROJECTS?"))
                .map(String::trim)
                .filter(s -> s.length() > 30)
                .limit(3)
                .toList();
        if (blocks.isEmpty()) {
            blocks = List.of(normalized);
        }
        List<String> techTokens = inferTechTokens(normalized);
        List<ResumeProject> result = new ArrayList<>();
        int index = 1;
        for (String block : blocks) {
            ResumeProject p = new ResumeProject();
            p.setResumeId(resumeId);
            p.setProjectName(inferProjectName(block, index++));
            p.setSummaryMd(truncate(block, 500));
            p.setTechStackTokens(techTokens);
            result.add(p);
        }
        return result;
    }

    private List<String> inferTechTokens(String text) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        List<String> candidates = List.of("Java", "Spring Boot", "MySQL", "Redis", "Vue", "React",
                "TypeScript", "Python", "Django", "Flask", "C++", "C#", "Unity", "Docker", "Kafka", "MyBatis");
        List<String> tokens = candidates.stream()
                .filter(token -> lower.contains(token.toLowerCase(Locale.ROOT)))
                .limit(8)
                .collect(Collectors.toList());
        return tokens.isEmpty() ? List.of("项目设计", "问题排查") : tokens;
    }

    private String inferProjectName(String block, int index) {
        return Arrays.stream(block.split("\\n"))
                .map(line -> line.replaceAll("^[#\\-*\\d.\\s]+", "").trim())
                .filter(line -> line.length() >= 4 && line.length() <= 40)
                .findFirst()
                .orElse("项目 " + index);
    }

    @Override
    public Map<String, Object> getLatest(Long userId) {
        UserResume resume = resumeMapper.selectOne(new LambdaQueryWrapper<UserResume>()
                .eq(UserResume::getUserId, userId)
                .orderByDesc(UserResume::getCreatedAt)
                .last("LIMIT 1"));
        if (resume == null) {
            return Map.of();
        }
        return toStatusMap(resume);
    }

    @Override
    public Map<String, Object> getStatus(Long userId, Long resumeId) {
        UserResume resume = requireOwned(userId, resumeId);
        return toStatusMap(resume);
    }

    private Map<String, Object> toStatusMap(UserResume resume) {
        Map<String, Object> m = new HashMap<>();
        m.put("resumeId", resume.getId());
        m.put("parseStatus", resume.getParseStatus());
        m.put("fileName", resume.getFileName());
        m.put("fileUrl", resume.getFileUrl());
        m.put("remark", resume.getRemark());
        m.put("createdAt", resume.getCreatedAt());
        if ("SUCCESS".equals(resume.getParseStatus()) && StringUtils.hasText(resume.getResumeTextMd())) {
            m.put("resumeTextPreview", resume.getResumeTextMd());
            m.put("parsedSections", ResumeSectionParser.extractSections(resume.getResumeTextMd()));
        }
        return m;
    }

    @Override
    public List<Map<String, Object>> listProjects(Long userId, Long resumeId) {
        UserResume resume = requireOwned(userId, resumeId);
        if (!"SUCCESS".equals(resume.getParseStatus())) {
            return List.of();
        }
        return projectMapper.selectList(new LambdaQueryWrapper<ResumeProject>()
                        .eq(ResumeProject::getResumeId, resumeId)
                        .orderByAsc(ResumeProject::getSortOrder))
                .stream().map(p -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", p.getId());
                    item.put("projectName", p.getProjectName());
                    item.put("summaryMd", p.getSummaryMd());
                    item.put("techStackTokens", p.getTechStackTokens());
                    return item;
                }).collect(Collectors.toList());
    }

    private UserResume requireOwned(Long userId, Long resumeId) {
        UserResume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw BusinessException.notFound("Resume not found");
        }
        return resume;
    }

    private String extractJsonArray(String raw) {
        int start = raw == null ? -1 : raw.indexOf('[');
        int end = raw == null ? -1 : raw.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return "[]";
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
