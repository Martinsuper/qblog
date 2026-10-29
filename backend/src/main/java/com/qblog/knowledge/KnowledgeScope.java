package com.qblog.knowledge;

import java.util.Set;
import com.qblog.common.exception.BusinessException;

/** Trusted server-built scope. Never deserialize a client's scope as authority. */
public record KnowledgeScope(Set<Long> folderIds, boolean unrestricted, boolean inbox) {
    public KnowledgeScope { folderIds = folderIds == null ? Set.of() : Set.copyOf(folderIds); }
    public KnowledgeScope(Set<Long> folderIds, boolean unrestricted) { this(folderIds, unrestricted, false); }
    public static KnowledgeScope all() { return new KnowledgeScope(Set.of(), true, true); }
    public static KnowledgeScope folders(Set<Long> ids) { return new KnowledgeScope(ids, false, false); }
    public void requireFolder(Long folderId) {
        if (!unrestricted && (folderId == null ? !inbox : !folderIds.contains(folderId)))
            throw new BusinessException(403, "目录不在授权范围内");
    }
}
