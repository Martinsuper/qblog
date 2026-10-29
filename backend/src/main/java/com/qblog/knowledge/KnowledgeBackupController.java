package com.qblog.knowledge;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 知识库导出端点（仅管理员）。
 * 同步写响应流：导出服务的只读事务与鉴权上下文都留在请求线程上，
 * 客户端断开会中止导出并释放事务。
 */
@RestController
@RequiredArgsConstructor
public class KnowledgeBackupController {

    private final KnowledgeBackupService backups;

    @GetMapping("/knowledge/backup/export")
    public void export(HttpServletResponse response) throws IOException {
        String filename = "knowledge-export-"
                + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".json";
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        backups.export(response.getOutputStream());
    }
}
