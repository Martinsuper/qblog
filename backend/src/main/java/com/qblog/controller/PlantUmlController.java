package com.qblog.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * PlantUML 图表渲染：文章里的 ```plantuml 代码块由前端编码为标准 PlantUML
 * 字符串（raw deflate + 自定义 6 位 base64），此处本地渲染 SVG 返回。
 * 匿名可读（等价 /uploads/** 的展示资源），只能渲染、不能上传。
 */
@Slf4j
@RestController
@RequestMapping("/plantuml")
public class PlantUmlController {

    /** URL 路径中的编码上限（Tomcat 单段 URI 8KB 内安全） */
    private static final int MAX_CODE_LENGTH = 4000;
    /** 解压后源码上限，防恶意超长图形拖垮内存 */
    private static final int MAX_SOURCE_LENGTH = 120_000;
    /** 语言文本里危险的前处理器：禁止读取本地/远程文件 */
    private static final String[] DENIED_DIRECTIVES = {"!include", "!includeurl", "!includedef", "%include"};

    private final PlantUmlRenderService renderService;

    @Autowired
    public PlantUmlController(PlantUmlRenderService renderService) {
        this.renderService = renderService;
    }

    @GetMapping(value = "/svg/{code}", produces = "image/svg+xml")
    public ResponseEntity<byte[]> svg(@PathVariable String code) {
        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(code, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return badRequest("编码无法解码");
        }
        if (decodedPath.length() > MAX_CODE_LENGTH) {
            return badRequest("图表编码过长");
        }
        String source;
        try {
            source = decodePlantUrl(decodedPath);
        } catch (IllegalArgumentException e) {
            return badRequest("图表编码不合法：" + e.getMessage());
        }
        for (String directive : DENIED_DIRECTIVES) {
            if (source.contains(directive)) {
                return badRequest("图表不允许使用 " + directive + " 指令");
            }
        }
        byte[] svg = renderService.render(source);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic().immutable())
                .body(svg);
    }

    /** PlantUML 标准字符串：raw deflate 解压 + 自定义 6 位字符表 */
    public static String decodePlantUrl(String code) {
        int[] source = new int[code.length() * 3 / 4 + 4];
        int length = decode64(code, source);
        byte[] compressed = new byte[length];
        for (int i = 0; i < length; i++) {
            int value = source[i];
            if (value < 0 || value > 255) throw new IllegalArgumentException("字节值超出范围");
            compressed[i] = (byte) value;
        }
        Inflater inflater = new Inflater(true);
        inflater.setInput(compressed);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(8192, MAX_SOURCE_LENGTH));
            byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                int read = inflater.inflate(buffer);
                if (read == 0 && inflater.needsInput()) throw new IllegalArgumentException("压缩流不完整");
                if (out.size() + read > MAX_SOURCE_LENGTH) throw new IllegalArgumentException("图表源码过大");
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8");
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("压缩流损坏");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            inflater.end();
        }
    }

    private static int decode64(String code, int[] out) {
        int written = 0;
        for (int i = 0; i < code.length(); i += 4) {
            int b1 = charValue(code.charAt(i));
            int b2 = i + 1 < code.length() ? charValue(code.charAt(i + 1)) : 0;
            int b3 = i + 2 < code.length() ? charValue(code.charAt(i + 2)) : 0;
            int b4 = i + 3 < code.length() ? charValue(code.charAt(i + 3)) : 0;
            out[written++] = (b1 << 2 | (b2 >> 4)) & 0xFF;
            out[written++] = ((b2 & 15) << 4 | b3 >> 2) & 0xFF;
            out[written++] = ((b3 & 3) << 6 | b4) & 0xFF;
        }
        return written;
    }

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";

    private static int charValue(char c) {
        int index = ALPHABET.indexOf(c);
        if (index < 0) throw new IllegalArgumentException("非法字符 '" + c + "'");
        return index;
    }

    private static ResponseEntity<byte[]> badRequest(String message) {
        String safe = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        byte[] errorSvg = ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"520\" height=\"72\">" +
                "<rect width=\"100%\" height=\"100%\" fill=\"#fef0f0\" stroke=\"#fde2e2\"/>" +
                "<text x=\"50%\" y=\"50%\" text-anchor=\"middle\" dominant-baseline=\"middle\" " +
                "fill=\"#f56c6c\" font-family=\"-apple-system,sans-serif\" font-size=\"13\">" +
                "PlantUML 渲染失败：" + safe +
                "</text></svg>").getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorSvg);
    }
}
