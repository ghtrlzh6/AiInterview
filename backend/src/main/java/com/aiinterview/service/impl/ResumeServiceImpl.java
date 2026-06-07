package com.aiinterview.service.impl;

import com.aiinterview.common.BusinessException;
import com.aiinterview.entity.ResumeProject;
import com.aiinterview.entity.User;
import com.aiinterview.entity.UserResume;
import com.aiinterview.mapper.ResumeProjectMapper;
import com.aiinterview.mapper.UserMapper;
import com.aiinterview.mapper.UserResumeMapper;
import com.aiinterview.service.ResumeService;
import com.aiinterview.util.FileUploadUtil;
import com.aiinterview.util.ResumeSectionParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final UserResumeMapper resumeMapper;
    private final ResumeProjectMapper projectMapper;
    private final UserMapper userMapper;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public Map<String, Object> upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 PDF 文件");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException("仅支持 PDF 格式");
        }
        try {
            Path dir = Paths.get(uploadDir, "resumes", String.valueOf(userId)).toAbsolutePath().normalize();
            String filename = System.currentTimeMillis() + "_" + originalName;
            Path target = dir.resolve(filename);
            FileUploadUtil.saveMultipart(file, target);
            UserResume resume = new UserResume();
            resume.setUserId(userId);
            resume.setFileUrl("/uploads/resumes/" + userId + "/" + filename);
            resume.setFileName(originalName);
            resume.setParseStatus("PENDING");
            resumeMapper.insert(resume);
            parseAsync(resume.getId(), target);
            Map<String, Object> data = new HashMap<>();
            data.put("resumeId", resume.getId());
            data.put("parseStatus", resume.getParseStatus());
            return data;
        } catch (IOException e) {
            throw new BusinessException("文件上传失败");
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

    private void extractProjects(UserResume resume, String projectText) {
        if (!StringUtils.hasText(projectText)) {
            return;
        }
        List<String> sections = splitProjectSections(projectText);
        int order = 1;
        for (String section : sections) {
            String trimmed = section.trim();
            if (trimmed.length() < 20) {
                continue;
            }
            ResumeProject p = new ResumeProject();
            p.setResumeId(resume.getId());
            p.setProjectName(extractProjectName(trimmed, order));
            p.setSummaryMd(truncate(trimmed, 500));
            p.setTechStackTokens(extractTechStack(trimmed));
            p.setSortOrder(order++);
            projectMapper.insert(p);
            if (order > 5) {
                break;
            }
        }
        if (order == 1) {
            ResumeProject p = new ResumeProject();
            p.setResumeId(resume.getId());
            p.setProjectName("简历摘要");
            p.setSummaryMd(truncate(projectText.trim(), 500));
            p.setTechStackTokens(extractTechStack(projectText));
            p.setSortOrder(1);
            projectMapper.insert(p);
        }
    }

    private List<String> splitProjectSections(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.replace('\r', '\n');
        String[] byHeading = normalized.split("(?=(?i)(项目经历|实习经历|工作经历|个人项目|研发经历))");
        if (byHeading.length > 1) {
            return Arrays.stream(byHeading)
                    .skip(1)
                    .map(s -> s.split("(?=(?i)(教育经历|技能|自我评价|获奖|证书))")[0])
                    .filter(s -> s.trim().length() >= 20)
                    .collect(Collectors.toList());
        }
        return Arrays.stream(normalized.split("\\n{2,}"))
                .filter(s -> s.contains("项目") || s.matches("(?s).*\\d{4}.*"))
                .limit(5)
                .collect(Collectors.toList());
    }

    private String extractProjectName(String section, int order) {
        String[] lines = section.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() >= 2 && trimmed.length() <= 40 && !trimmed.matches("(?i).*负责.*")) {
                return trimmed.replaceAll("^[\\d.、\\-•\\s]+", "");
            }
        }
        return "项目 " + order;
    }

    private List<String> extractTechStack(String text) {
        List<String> keywords = List.of(
                "Java", "Spring Boot", "Spring", "MySQL", "Redis", "Vue", "React",
                "Python", "Docker", "Kubernetes", "Kafka", "MyBatis", "TypeScript");
        List<String> found = new ArrayList<>();
        for (String kw : keywords) {
            if (text.contains(kw)) {
                found.add(kw);
            }
        }
        return found.isEmpty() ? List.of("待补充") : found;
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
            throw BusinessException.notFound("简历不存在");
        }
        return resume;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
