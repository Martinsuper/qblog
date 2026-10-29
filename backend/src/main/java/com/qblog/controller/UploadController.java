package com.qblog.controller;

import com.qblog.common.Result;
import com.qblog.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传控制器
 * 管理端专用（SecurityConfig anyRequest 即要求 ADMIN）；文件名随机化，
 * 通过 WebMvcConfig 的 /uploads/** 静态映射对外提供访问
 */
@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;

    @Value("${upload.directory:uploads}")
    private String uploadDirectory;

    @Operation(summary = "上传图片（需管理员）")
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file,
                                                   HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(400, "图片大小不能超过 5MB");
        }

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "只支持 JPG/PNG/GIF/WebP 格式的图片");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException(400, "图片类型不合法");
        }

        try {
            Path dir = Paths.get(uploadDirectory).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            // 随机文件名，避免路径穿越与覆盖
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(dir)) {
                throw new BusinessException(400, "非法的目标路径");
            }

            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // URL 带上 context-path（默认 /api），前端可直接用于 <img src>
            String url = request.getContextPath() + "/uploads/" + filename;
            log.info("Image uploaded: {} ({} bytes) -> {}", originalName, file.getSize(), filename);
            return Result.success(Map.of("url", url));
        } catch (IOException e) {
            log.error("Failed to store uploaded image {}", originalName, e);
            throw new BusinessException(500, "图片保存失败，请稍后重试");
        }
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
