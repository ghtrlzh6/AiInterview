package com.aiinterview.controller;

import com.aiinterview.common.PageResult;
import com.aiinterview.common.Result;
import com.aiinterview.service.ReportService;
import com.aiinterview.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Tag(name = "报告")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "报告详情")
    @GetMapping("/{reportId}")
    public Result<Map<String, Object>> detail(@PathVariable Long reportId) {
        return Result.success(reportService.getReport(SecurityUtils.currentUserId(), reportId));
    }

    @Operation(summary = "报告列表")
    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String positionCode) {
        return Result.success(reportService.listReports(SecurityUtils.currentUserId(), page, size, positionCode));
    }

    @Operation(summary = "下载报告 PDF")
    @GetMapping("/{reportId}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long reportId) {
        byte[] pdf = reportService.downloadReportPdf(SecurityUtils.currentUserId(), reportId);
        String filename = "interview-report-" + reportId + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(pdf);
    }

    @Operation(summary = "生成分享链接")
    @PostMapping("/{reportId}/share")
    public Result<Map<String, Object>> share(@PathVariable Long reportId) {
        return Result.success(reportService.share(SecurityUtils.currentUserId(), reportId));
    }

    @Operation(summary = "分享访问")
    @GetMapping("/share/{shareToken}")
    public Result<Map<String, Object>> shareView(@PathVariable String shareToken) {
        return Result.success(reportService.getByShareToken(shareToken));
    }
}
