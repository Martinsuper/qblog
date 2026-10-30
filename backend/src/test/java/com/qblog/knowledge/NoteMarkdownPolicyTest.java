package com.qblog.knowledge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class NoteMarkdownPolicyTest {
    private static final String VALID = """
            ## 一、简介
            内容
            ### 1.1 功能
            #### 1.1.1 搜索
            ### 1.2 限制
            ## 二、参考资料
            """;

    @Test void acceptsMatchingHierarchyAndCrLf() {
        assertDoesNotThrow(() -> NoteMarkdownPolicy.validate(VALID));
        assertDoesNotThrow(() -> NoteMarkdownPolicy.validate(VALID.replace("\n", "\r\n")));
    }

    @Test void ignoresCodeBlocksAndQuotes() {
        assertDoesNotThrow(() -> NoteMarkdownPolicy.validate(VALID + """
                ```markdown
                # 示例
                ## 不编号
                ```
                ~~~~markdown
                ```
                # 示例
                ~~~~
                > # 引用
                > ## 不编号

                    # 缩进代码
                """));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "正文", "## 目录", "## 目录\n\n- 1 简介", "# 标题\n\n## 目录"})
    void rejectsMissingStructure(String content) {
        assertThrows(NoteMarkdownPolicy.InvalidFormat.class, () -> NoteMarkdownPolicy.validate(content));
    }

    @ParameterizedTest
    @ValueSource(strings = {"## 简介", "## 1 简介", "## 二、简介", "## 一、 简介", "# 一、简介",
            "##### 1 简介", "### 1.1 简介", "#### 1.1.1 简介"})
    void rejectsMalformedFirstSection(String heading) {
        assertThrows(NoteMarkdownPolicy.InvalidFormat.class,
                () -> NoteMarkdownPolicy.validate(VALID.replace("## 一、简介", heading)));
    }

    @Test void rejectsDuplicatesSkippedNumbersAndWrongParent() {
        for (String bad : new String[]{VALID.replace("1.2 限制", "1.1 限制"),
                VALID.replace("1.2 限制", "1.3 限制"), VALID.replace("1.2 限制", "2.1 限制"),
                VALID.replace("1.1.1 搜索", "1.1.2 搜索"), VALID.replace("二、参考资料", "三、参考资料"),
                VALID.replace("二、参考资料", "一、参考资料")}) {
            assertThrows(NoteMarkdownPolicy.InvalidFormat.class, () -> NoteMarkdownPolicy.validate(bad));
        }
    }

    @Test void acceptsNumberedSectionsWithoutDirectory() {
        assertDoesNotThrow(() -> NoteMarkdownPolicy.validate("## 一、简介\n正文"));
        assertThrows(NoteMarkdownPolicy.InvalidFormat.class,
                () -> NoteMarkdownPolicy.validate("## 目录\n\n- 1 简介\n\n## 1 简介"));
    }

    @Test void recognizesSetextHeadingsRatherThanSilentlyBypassingPolicy() {
        assertThrows(NoteMarkdownPolicy.InvalidFormat.class,
                () -> NoteMarkdownPolicy.validate(VALID + "\n未编号标题\n------\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"安装接入：客户端配置", "配置:鉴权", "如何接入？", "开始使用!", "这是什么", "能做什么功能",
            "怎么做配置", "如何做验证", "这是一个超过二十个字符的章节标题用于验证长度限制"})
    void rejectsVerboseOrConversationalTitles(String title) {
        assertThrows(NoteMarkdownPolicy.InvalidFormat.class,
                () -> NoteMarkdownPolicy.validate("## 一、" + title));
    }

    @Test void titleLengthExcludesNumberAndCountsUnicodeCodePoints() {
        assertDoesNotThrow(() -> NoteMarkdownPolicy.validate("## 一、" + "技".repeat(20)));
        assertDoesNotThrow(() -> NoteMarkdownPolicy.validate("## 一、" + "𠮷".repeat(20)));
        assertThrows(NoteMarkdownPolicy.InvalidFormat.class,
                () -> NoteMarkdownPolicy.validate("## 一、" + "𠮷".repeat(21)));
        assertThrows(NoteMarkdownPolicy.InvalidFormat.class,
                () -> NoteMarkdownPolicy.validate("## 一、安装\n### 1.1 配置：鉴权"));
    }

    @Test void supportsChineseChapterTransitionsAndNumericChildren() {
        assertEquals("十", NoteMarkdownPolicy.chineseNumber(10));
        assertEquals("十一", NoteMarkdownPolicy.chineseNumber(11));
        assertEquals("二十", NoteMarkdownPolicy.chineseNumber(20));
        assertEquals("一百零一", NoteMarkdownPolicy.chineseNumber(101));
        assertEquals("一千零一十", NoteMarkdownPolicy.chineseNumber(1010));
        StringBuilder content = new StringBuilder();
        for (int i = 1; i <= 21; i++) content.append("## ").append(NoteMarkdownPolicy.chineseNumber(i))
                .append("、章节\n### ").append(i).append(".1 内容\n#### ").append(i).append(".1.1 示例\n");
        assertDoesNotThrow(() -> NoteMarkdownPolicy.validate(content.toString()));
        assertThrows(NoteMarkdownPolicy.InvalidFormat.class,
                () -> NoteMarkdownPolicy.validate("## 一、简介\n## 二、安装\n### 1.1 配置"));
    }
}
