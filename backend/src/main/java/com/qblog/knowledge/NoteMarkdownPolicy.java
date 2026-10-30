package com.qblog.knowledge;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Validates numbered MCP note headings without requiring a table of contents. */
public final class NoteMarkdownPolicy {
    public static final String INSTRUCTIONS = "笔记正文不要添加目录，直接编写编号章节。"
            + "正文禁止一级标题，顶级章节使用 ## 一、标题、## 二、标题（顿号后不加空格）；"
            + "子章节使用 ### 1.1 标题、#### 1.1.1 标题（编号后一个空格）。"
            + "顶级中文序号从一连续递增，子章节以对应顶级章节的阿拉伯数字序号为前缀，各级从1递增，不重复、不跳号、不跳级。"
            + "参考资料和附录也须编号。代码块、引用块中的标题不参与校验。"
            + "所有章节标题（含小标题）须简洁专业：编号后标题文字不超过20个Unicode字符，建议2至12字；"
            + "使用明确的专业名词或动宾短语，同级标题保持平行结构，一标题只表达一个主题。"
            + "禁止使用这是什么、能做什么、怎么做、如何做等口语化开头；不使用冒号拼接说明、问号或感叹号。"
            + "解释、步骤细节和宣传性措辞放入正文，不堆叠修饰语，不为压缩长度删除必要的技术术语。"
            + "创建或更新前必须检查全部章节编号；不合规将拒绝保存。";
    private static final Pattern NUMBERED = Pattern.compile("([1-9][0-9]*(?:\\.[1-9][0-9]*){0,2}) ([^\\s].*)");
    private static final Pattern CHINESE_NUMBERED = Pattern.compile("([一二三四五六七八九十百千零]+)、([^\\s].*)");
    private static final Pattern TITLE_PUNCTUATION = Pattern.compile("[：:？?！!]");

    private NoteMarkdownPolicy() {}

    /** Rejects invalid content without accessing persisted notes or exposing authorization information. */
    public static void validate(String content) {
        Node document = Parser.builder().build().parse(content);
        List<Section> sections = new ArrayList<>();
        int[] counters = new int[3];
        int[] previousDepth = {-1};
        document.accept(new AbstractVisitor() {
            @Override public void visit(BlockQuote quote) { /* Quoted examples are not note sections. */ }
            @Override public void visit(Heading current) {
                int depth = current.getLevel() - 2;
                if (depth < 0 || depth > 2) throw invalid("章节仅允许二至四级标题");
                if (current.getParent() != document) throw invalid("章节标题不能嵌套在列表等容器中");
                if (depth > previousDepth[0] + 1) throw invalid("标题层级不能跳级");
                String title = text(current);
                var match = (depth == 0 ? CHINESE_NUMBERED : NUMBERED).matcher(title);
                if (!match.matches()) throw invalid(depth == 0 ? "顶级章节使用 一、标题 格式，顿号后不加空格" : "子章节使用 1.1 标题 或 1.1.1 标题 格式，编号后一个空格");
                validateTitle(match.group(2));
                counters[depth]++;
                for (int i = depth + 1; i < counters.length; i++) counters[i] = 0;
                StringBuilder expected = new StringBuilder();
                for (int i = 0; i <= depth; i++) {
                    if (i > 0) expected.append('.');
                    expected.append(counters[i]);
                }
                String expectedNumber = depth == 0 ? chineseNumber(counters[0]) : expected.toString();
                if (!expectedNumber.equals(match.group(1)))
                    throw invalid("章节编号不连续或归属错误：期望 " + expectedNumber);
                sections.add(new Section(depth, title));
                previousDepth[0] = depth;
            }
        });
        if (sections.isEmpty()) throw invalid("至少需要一个正文章节");
    }

    /** Formats canonical Chinese chapter numbers from one through nine thousand nine hundred ninety-nine. */
    static String chineseNumber(int number) {
        if (number < 1 || number > 9999) throw invalid("顶级章节数量不得超过9999");
        String digits = "零一二三四五六七八九";
        String[] units = {"", "十", "百", "千"};
        StringBuilder out = new StringBuilder();
        boolean zero = false;
        for (int place = 3, divisor = 1000; place >= 0; place--, divisor /= 10) {
            int digit = number / divisor % 10;
            if (digit == 0) {
                if (!out.isEmpty()) zero = true;
            } else {
                if (zero) out.append('零');
                if (!(digit == 1 && place == 1 && out.isEmpty())) out.append(digits.charAt(digit));
                out.append(units[place]);
                zero = false;
            }
        }
        return out.toString();
    }

    /** Enforces objective brevity rules; semantic professionalism is additionally guided by instructions. */
    private static void validateTitle(String title) {
        if (!title.equals(title.strip())) throw invalid("章节标题不能包含首尾空白");
        if (title.codePointCount(0, title.length()) > 20) throw invalid("章节标题文字不得超过20个字符（不含编号）");
        if (TITLE_PUNCTUATION.matcher(title).find()) throw invalid("章节标题不使用冒号、问号或感叹号，说明放入正文");
        for (String prefix : List.of("这是什么", "能做什么", "怎么做", "如何做"))
            if (title.startsWith(prefix)) throw invalid("章节标题避免口语化开头，使用专业名词或动宾短语");
    }

    /** Extracts visible inline text for checking heading numbering. */
    private static String text(Node node) {
        StringBuilder out = new StringBuilder();
        node.accept(new AbstractVisitor() {
            @Override public void visit(Text value) { out.append(value.getLiteral()); }
            @Override public void visit(Code value) { out.append(value.getLiteral()); }
            @Override public void visit(SoftLineBreak value) { out.append('\n'); }
            @Override public void visit(HardLineBreak value) { out.append('\n'); }
            @Override public void visit(HtmlInline value) { throw invalid("标题不允许内嵌 HTML"); }
        });
        return out.toString();
    }

    private static InvalidFormat invalid(String reason) { return new InvalidFormat("笔记格式不符合规范：" + reason); }
    private record Section(int depth, String title) {}

    /** Safe, content-policy-only failure that may be returned to an MCP caller. */
    public static final class InvalidFormat extends IllegalArgumentException {
        private InvalidFormat(String message) { super(message); }
    }
}
