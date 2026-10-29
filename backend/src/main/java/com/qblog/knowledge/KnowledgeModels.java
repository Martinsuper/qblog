package com.qblog.knowledge;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/** Shared REST/MCP contract. NoteWrite is a full replacement, not a PATCH. */
public final class KnowledgeModels {
    private KnowledgeModels() {}
    @Data public static class NoteQuery {
        private long current = 1;
        private long size = 20;
        private String search;
        private String query;
        private Long folderId;
        private String tag;
        private String source;
        private String sort = "updated";
        private Boolean favorite;
        private Boolean inbox;
    }
    @Data public static class NoteWrite {
        private String title;
        private String content;
        private String summary;
        private Long folderId;
        private List<String> tags;
        private Boolean favorite;
        private String source;
        private String sourceTool;
        private String externalId;
        private Long version;
    }
    @Data public static class NoteView {
        private Long id;
        private String title;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String content;
        private String summary;
        private Long folderId;
        private List<String> tags;
        private Boolean favorite;
        private String source;
        private String sourceTool;
        private String externalId;
        private Long version;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private String indexStatus = "not_configured";
    }
    public record PageResult<T>(List<T> records, long total, long current, long size) {}
    @Data public static class FolderWrite {
        private String name;
        private Long parentId;
        private Integer sort = 0;
    }
    @Data public static class FolderView {
        private Long id;
        private String name;
        private Long parentId;
        private int sort;
        /** Direct active-note count, not a recursive count. */
        private long count;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private List<FolderView> children = new java.util.ArrayList<>();
    }
}
