package com.aiinterview.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 按简历常见标题关键词切分 PDF 文本。
 */
public final class ResumeSectionParser {

    public static final String KEY_EDUCATION = "educationExperience";
    public static final String KEY_PERSONAL_SKILLS = "personalSkills";
    public static final String KEY_PROJECT = "projectExperience";
    public static final String KEY_INTERNSHIP = "internshipExperience";

    private record HeadingRule(String sectionKey, List<String> keywords) {
    }

    private static final List<HeadingRule> RULES = List.of(
            new HeadingRule(KEY_EDUCATION, List.of("教育经历", "教育背景", "学习经历", "学历")),
            new HeadingRule(KEY_PERSONAL_SKILLS, List.of("个人能力", "个人技能", "专业技能", "技能特长", "掌握技能", "职业技能", "技术栈")),
            new HeadingRule(KEY_PROJECT, List.of("项目经历", "项目经验", "个人项目", "研发经历", "项目实践")),
            new HeadingRule(KEY_INTERNSHIP, List.of("实习经历", "实习经验", "工作经历", "工作经验", "实践经历"))
    );

    private static final Pattern STOP_HEADING = Pattern.compile(
            "(?i)(?<![\\w\\p{L}])(自我评价|获奖情况|荣誉|证书|兴趣爱好|联系方式|个人简介|基本信息)(?![\\w\\p{L}])");

    private ResumeSectionParser() {
    }

    public static Map<String, String> extractSections(String rawText) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(rawText)) {
            return result;
        }
        String text = rawText.replace('\r', '\n');
        List<MatchPoint> points = findHeadings(text);
        if (points.isEmpty()) {
            return result;
        }
        for (int i = 0; i < points.size(); i++) {
            MatchPoint current = points.get(i);
            int contentStart = current.contentStart();
            int contentEnd = i + 1 < points.size() ? points.get(i + 1).start() : text.length();
            if (contentStart >= contentEnd) {
                continue;
            }
            String body = cleanBody(text.substring(contentStart, contentEnd));
            if (!StringUtils.hasText(body)) {
                continue;
            }
            result.merge(current.sectionKey(), body, (oldVal, newVal) ->
                    oldVal.length() >= newVal.length() ? oldVal : newVal);
        }
        return result;
    }

    public static void fillSchoolMajor(Map<String, String> target, String educationText) {
        if (!StringUtils.hasText(educationText)) {
            return;
        }
        if (!target.containsKey("school")) {
            Matcher schoolMatcher = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9]{2,30}(?:大学|学院|学校))").matcher(educationText);
            if (schoolMatcher.find()) {
                target.put("school", schoolMatcher.group(1).trim());
            }
        }
        if (!target.containsKey("major")) {
            Matcher majorMatcher = Pattern.compile("(?:专业[:：\\s]|主修[:：\\s])([\\u4e00-\\u9fa5A-Za-z0-9（）()\\-\\s]{2,40})").matcher(educationText);
            if (majorMatcher.find()) {
                target.put("major", majorMatcher.group(1).trim());
            } else {
                Matcher fallback = Pattern.compile("[\\u4e00-\\u9fa5]{2,20}专业").matcher(educationText);
                if (fallback.find()) {
                    target.put("major", fallback.group().replace("专业", "").trim());
                }
            }
        }
    }

    private static List<MatchPoint> findHeadings(String text) {
        List<MatchPoint> points = new ArrayList<>();
        for (HeadingRule rule : RULES) {
            for (String keyword : rule.keywords()) {
                Pattern pattern = Pattern.compile("(?i)(?<![\\w\\p{L}])" + Pattern.quote(keyword) + "(?![\\w\\p{L}])");
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    points.add(new MatchPoint(matcher.start(), matcher.end(), rule.sectionKey()));
                }
            }
        }
        points.sort((a, b) -> Integer.compare(a.start(), b.start()));
        List<MatchPoint> deduped = new ArrayList<>();
        int lastStart = -1;
        for (MatchPoint point : points) {
            if (point.start() - lastStart < 3) {
                continue;
            }
            deduped.add(point);
            lastStart = point.start();
        }
        return deduped;
    }

    private static String cleanBody(String section) {
        String cleaned = section.trim();
        cleaned = STOP_HEADING.split(cleaned)[0].trim();
        cleaned = cleaned.replaceAll("(?m)^[\\s\\-•·|]+", "");
        return cleaned.trim();
    }

    private record MatchPoint(int start, int headingEnd, String sectionKey) {
        int contentStart() {
            int lineEnd = headingEnd;
            return lineEnd;
        }
    }
}
