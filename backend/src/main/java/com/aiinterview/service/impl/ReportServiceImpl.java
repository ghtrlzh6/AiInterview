package com.aiinterview.service.impl;

import cn.hutool.core.util.IdUtil;
import com.aiinterview.common.BusinessException;
import com.aiinterview.common.PageResult;
import com.aiinterview.entity.DimensionScore;
import com.aiinterview.entity.EvaluationReport;
import com.aiinterview.entity.Position;
import com.aiinterview.entity.Question;
import com.aiinterview.entity.ChatMessage;
import com.aiinterview.mapper.*;
import com.aiinterview.service.ReportService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final float PAGE_MARGIN = 48;
    private static final float LINE_HEIGHT = 17;
    private static final float TITLE_FONT_SIZE = 20;
    private static final float SECTION_FONT_SIZE = 14;
    private static final float BODY_FONT_SIZE = 11;

    private final EvaluationReportMapper reportMapper;
    private final DimensionScoreMapper dimensionScoreMapper;
    private final QuestionMapper questionMapper;
    private final PositionMapper positionMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Value("${app.share-base-url:http://localhost/share}")
    private String shareBaseUrl;

    @Override
    public Map<String, Object> getReport(Long userId, Long reportId) {
        EvaluationReport report = requireOwned(userId, reportId);
        return toDetailMap(report, false);
    }

    @Override
    public PageResult<Map<String, Object>> listReports(Long userId, int page, int size, String positionCode) {
        LambdaQueryWrapper<EvaluationReport> wrapper = new LambdaQueryWrapper<EvaluationReport>()
                .eq(EvaluationReport::getUserId, userId)
                .eq(EvaluationReport::getReportStatus, "COMPLETED")
                .orderByDesc(EvaluationReport::getCreatedAt);
        if (StringUtils.hasText(positionCode)) {
            wrapper.eq(EvaluationReport::getPositionCode, positionCode);
        }
        Page<EvaluationReport> p = reportMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = p.getRecords().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("reportId", r.getId());
            m.put("sessionId", r.getSessionId());
            m.put("positionCode", r.getPositionCode());
            Position pos = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                    .eq(Position::getCode, r.getPositionCode()));
            m.put("positionName", pos != null ? pos.getName() : r.getPositionCode());
            m.put("overallScore", r.getOverallScore());
            m.put("reportStatus", r.getReportStatus());
            m.put("createdAt", r.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), page, size, list);
    }

    @Override
    public byte[] downloadReportPdf(Long userId, Long reportId) {
        EvaluationReport report = requireOwned(userId, reportId);
        if (!"COMPLETED".equals(report.getReportStatus())) {
            throw new BusinessException("报告尚未生成完成，暂不能下载");
        }

        Map<String, Object> detail = toDetailMap(report, false);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(document);
            writer.writeTitle("AI 模拟面试评估报告");
            writer.writeText("岗位：" + value(detail.get("positionName")));
            writer.writeText("综合得分：" + value(detail.get("overallScore")));
            writer.writeText("生成时间：" + value(detail.get("createdAt")));
            writer.addGap(8);

            writer.writeSection("维度得分");
            @SuppressWarnings("unchecked")
            Map<String, Object> scores = (Map<String, Object>) detail.getOrDefault("scores", Map.of());
            writer.writeText("技术：" + value(scores.get("tech"))
                    + "    表达：" + value(scores.get("expression"))
                    + "    逻辑：" + value(scores.get("logic"))
                    + "    深度：" + value(scores.get("depth"))
                    + "    自信：" + value(scores.get("confidence")));

            writer.writeSection("综合评估");
            writer.writeParagraph(value(detail.get("summary")));

            writer.writeSection("亮点");
            writer.writeList(stringList(detail.get("highlights")));

            writer.writeSection("待改进");
            writer.writeList(stringList(detail.get("weaknesses")));

            writer.writeSection("改进建议");
            writer.writeList(stringList(detail.get("suggestions")));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questionScores =
                    (List<Map<String, Object>>) detail.getOrDefault("questionScores", List.of());
            if (!questionScores.isEmpty()) {
                writer.writeSection("逐题点评");
                for (Map<String, Object> item : questionScores) {
                    writer.writeText("第 " + value(item.get("questionOrder")) + " 题：" + value(item.get("questionTitle")));
                    writer.writeText("得分：技术 " + value(item.get("techScore"))
                            + " / 逻辑 " + value(item.get("logicScore"))
                            + " / 深度 " + value(item.get("depthScore")));
                    writer.writeParagraph("我的回答：" + value(item.get("userAnswer")));
                    writer.writeParagraph("参考答案：" + value(item.get("referenceAnswer")));
                    writer.writeParagraph("AI点评：" + value(item.get("comment")));
                    writer.addGap(6);
                }
            }

            writer.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("报告 PDF 生成失败");
        }
    }

    @Override
    public Map<String, Object> share(Long userId, Long reportId) {
        EvaluationReport report = requireOwned(userId, reportId);
        if (!StringUtils.hasText(report.getShareToken())) {
            report.setShareToken(IdUtil.simpleUUID());
            reportMapper.updateById(report);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("shareUrl", shareBaseUrl + "/" + report.getShareToken());
        m.put("shareToken", report.getShareToken());
        return m;
    }

    @Override
    public Map<String, Object> getByShareToken(String token) {
        EvaluationReport report = reportMapper.selectOne(new LambdaQueryWrapper<EvaluationReport>()
                .eq(EvaluationReport::getShareToken, token));
        if (report == null) {
            throw BusinessException.notFound("分享链接无效");
        }
        Map<String, Object> detail = toDetailMap(report, true);
        detail.remove("confidenceScore");
        return detail;
    }

    private Map<String, Object> toDetailMap(EvaluationReport report, boolean shared) {
        Map<String, Object> m = new HashMap<>();
        m.put("reportId", report.getId());
        m.put("sessionId", report.getSessionId());
        m.put("positionCode", report.getPositionCode());
        Position pos = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getCode, report.getPositionCode()));
        m.put("positionName", pos != null ? pos.getName() : report.getPositionCode());
        m.put("reportStatus", report.getReportStatus());
        m.put("overallScore", report.getOverallScore());
        Map<String, Object> scores = new HashMap<>();
        scores.put("tech", report.getTechScore());
        scores.put("expression", report.getExpressionScore());
        scores.put("logic", report.getLogicScore());
        scores.put("depth", report.getDepthScore());
        if (!shared) scores.put("confidence", report.getConfidenceScore());
        m.put("scores", scores);
        m.put("summary", report.getSummary());
        m.put("highlights", report.getHighlights());
        m.put("weaknesses", report.getWeaknesses());
        m.put("suggestions", report.getSuggestions());
        m.put("createdAt", report.getCreatedAt());
        List<DimensionScore> dsList = dimensionScoreMapper.selectList(new LambdaQueryWrapper<DimensionScore>()
                .eq(DimensionScore::getReportId, report.getId())
                .orderByAsc(DimensionScore::getQuestionOrder));
        List<Map<String, Object>> qScores = new ArrayList<>();
        for (DimensionScore ds : dsList) {
            Question q = questionMapper.selectById(ds.getQuestionId());
            // ChatMessage userAnswer =
            // chatMessageMapper.selectOne(
            //         new LambdaQueryWrapper<ChatMessage>()
            //                 .eq(ChatMessage::getSessionId, ds.getSessionId())
            //                 .eq(ChatMessage::getQuestionId, ds.getQuestionId())
            //                 .eq(ChatMessage::getRole, "USER")
            //                 .orderByDesc(ChatMessage::getCreatedAt)
            //                 .last("LIMIT 1")
            // );
            List<ChatMessage> answers =
                    chatMessageMapper.selectList(
                            new LambdaQueryWrapper<ChatMessage>()
                                    .eq(ChatMessage::getSessionId, ds.getSessionId())
                                    .eq(ChatMessage::getQuestionId, ds.getQuestionId())
                                    .eq(ChatMessage::getRole, "USER")
                                    .orderByAsc(ChatMessage::getCreatedAt)
                    );

            String userAnswer = answers.stream()
                    .map(ChatMessage::getContent)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n\n"));

            Map<String, Object> qs = new HashMap<>();
            qs.put("questionOrder", ds.getQuestionOrder());
            qs.put("questionTitle", q != null ? q.getTitle() : "");
            // qs.put(
            //         "userAnswer",
            //         userAnswer != null
            //                 ? userAnswer.getContent()
            //                 : ""
            // );
            qs.put("userAnswer", userAnswer);
            qs.put("referenceAnswer", q.getAnswerReference());
            // qs.put(
            //         "referenceAnswer",
            //         q != null
            //                 ? q.getAnswerReference()
            //                 : ""
            // );
            qs.put("techScore", ds.getTechScore());
            qs.put("logicScore", ds.getLogicScore());
            qs.put("depthScore", ds.getDepthScore());
            qs.put("comment", ds.getComment());
            qScores.add(qs);
        }
        m.put("questionScores", qScores);
        return m;
    }

    private EvaluationReport requireOwned(Long userId, Long reportId) {
        EvaluationReport report = reportMapper.selectById(reportId);
        if (report == null || !report.getUserId().equals(userId)) {
            throw BusinessException.notFound("报告不存在");
        }
        return report;
    }

    private static String value(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "-" : text;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }
        return collection.stream()
                .map(ReportServiceImpl::value)
                .filter(text -> !"-".equals(text))
                .collect(Collectors.toList());
    }

    private static class PdfWriter {
        private final PDDocument document;
        private final PDFont font;
        private PDPage page;
        private PDPageContentStream content;
        private float y;

        PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            this.font = loadChineseFont(document);
            newPage();
        }

        void writeTitle(String text) throws IOException {
            writeLines(wrap(text, TITLE_FONT_SIZE, contentWidth()), TITLE_FONT_SIZE, 22);
            addGap(8);
        }

        void writeSection(String text) throws IOException {
            addGap(10);
            writeLines(wrap(text, SECTION_FONT_SIZE, contentWidth()), SECTION_FONT_SIZE, 20);
            addGap(2);
        }

        void writeText(String text) throws IOException {
            writeLines(wrap(text, BODY_FONT_SIZE, contentWidth()), BODY_FONT_SIZE, LINE_HEIGHT);
        }

        void writeParagraph(String text) throws IOException {
            writeText(text);
            addGap(4);
        }

        void writeList(List<String> items) throws IOException {
            if (items.isEmpty()) {
                writeText("-");
                return;
            }
            for (String item : items) {
                writeText("- " + item);
            }
        }

        void addGap(float gap) throws IOException {
            ensureSpace(gap);
            y -= gap;
        }

        void close() throws IOException {
            if (content != null) {
                content.close();
            }
        }

        private void writeLines(List<String> lines, float fontSize, float lineHeight) throws IOException {
            for (String line : lines) {
                ensureSpace(lineHeight);
                content.beginText();
                content.setFont(font, fontSize);
                content.newLineAtOffset(PAGE_MARGIN, y);
                content.showText(line);
                content.endText();
                y -= lineHeight;
            }
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed >= PAGE_MARGIN) {
                return;
            }
            content.close();
            newPage();
        }

        private void newPage() throws IOException {
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - PAGE_MARGIN;
        }

        private float contentWidth() {
            return page.getMediaBox().getWidth() - PAGE_MARGIN * 2;
        }

        private List<String> wrap(String text, float fontSize, float maxWidth) throws IOException {
            String normalized = value(text)
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
                    .replaceAll("[#*_`>\\[\\]]", "");
            List<String> result = new ArrayList<>();
            for (String paragraph : normalized.split("\n")) {
                if (paragraph.isBlank()) {
                    result.add("");
                    continue;
                }
                StringBuilder line = new StringBuilder();
                for (int offset = 0; offset < paragraph.length(); ) {
                    int cp = paragraph.codePointAt(offset);
                    String next = new String(Character.toChars(cp));
                    String candidate = line + next;
                    if (!line.isEmpty() && font.getStringWidth(candidate) / 1000 * fontSize > maxWidth) {
                        result.add(line.toString());
                        line = new StringBuilder(next);
                    } else {
                        line.append(next);
                    }
                    offset += Character.charCount(cp);
                }
                if (!line.isEmpty()) {
                    result.add(line.toString());
                }
            }
            return result.isEmpty() ? List.of("-") : result;
        }

        private static PDFont loadChineseFont(PDDocument document) throws IOException {
            String[] candidates = {
                    "C:/Windows/Fonts/msyh.ttc",
                    "C:/Windows/Fonts/simhei.ttf",
                    "C:/Windows/Fonts/simsun.ttc",
                    "/System/Library/Fonts/PingFang.ttc",
                    "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                    "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                    "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"
            };
            for (String path : candidates) {
                File file = new File(path);
                if (file.exists() && file.isFile()) {
                    try {
                        return PDType0Font.load(document, file);
                    } catch (IOException ignored) {
                        /* Try the next common CJK font. */
                    }
                }
            }
            throw new BusinessException("未找到可用中文字体，无法生成 PDF");
        }
    }
}
