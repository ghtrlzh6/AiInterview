package com.aiinterview.service.impl;

import com.aiinterview.common.BusinessException;
import com.aiinterview.entity.ResumeProject;
import com.aiinterview.entity.UserResume;
import com.aiinterview.mapper.ResumeProjectMapper;
import com.aiinterview.mapper.UserResumeMapper;
import com.aiinterview.service.ResumeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
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
            Path dir = Paths.get(uploadDir, "resumes", String.valueOf(userId));
            Files.createDirectories(dir);
            String filename = System.currentTimeMillis() + "_" + originalName;
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
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
            mockExtractProjects(resume, text);
        } catch (Exception e) {
            log.warn("Resume parse failed", e);
            resume.setParseStatus("FAILED");
            resume.setRemark(e.getMessage());
            resumeMapper.updateById(resume);
        }
    }

    private void mockExtractProjects(UserResume resume, String text) {
        ResumeProject p = new ResumeProject();
        p.setResumeId(resume.getId());
        p.setProjectName("示例项目");
        p.setSummaryMd(truncate(text, 300));
        p.setTechStackTokens(List.of("Java", "Spring Boot"));
        p.setSortOrder(1);
        projectMapper.insert(p);
    }

    @Override
    public Map<String, Object> getStatus(Long userId, Long resumeId) {
        UserResume resume = requireOwned(userId, resumeId);
        Map<String, Object> m = new HashMap<>();
        m.put("resumeId", resume.getId());
        m.put("parseStatus", resume.getParseStatus());
        m.put("fileName", resume.getFileName());
        m.put("remark", resume.getRemark());
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
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("projectName", p.getProjectName());
                    m.put("summaryMd", p.getSummaryMd());
                    m.put("techStackTokens", p.getTechStackTokens());
                    return m;
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
