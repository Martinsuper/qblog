-- Private, single-admin knowledge workspace. Legacy tables remain untouched.
-- MySQL 8.0. Back up first. DDL auto-commits; data copy below is transactional.
-- Safe to re-run: legacy IDs are preserved and INSERT IGNORE never overwrites edits.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_workspace (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT IGNORE INTO knowledge_workspace(id,name) VALUES(1,'Private administrator workspace');

CREATE TABLE IF NOT EXISTS knowledge_folder (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL DEFAULT 1,
    parent_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_knowledge_folder_parent(workspace_id,parent_id,sort),
    CONSTRAINT fk_knowledge_folder_workspace FOREIGN KEY(workspace_id) REFERENCES knowledge_workspace(id),
    CONSTRAINT fk_knowledge_folder_parent FOREIGN KEY(parent_id) REFERENCES knowledge_folder(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_note (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL DEFAULT 1,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT NOT NULL,
    summary VARCHAR(500) NOT NULL DEFAULT '',
    folder_id BIGINT NULL,
    tags TEXT NOT NULL COMMENT 'JSON array of strings',
    favorite TINYINT(1) NOT NULL DEFAULT 0,
    source VARCHAR(10) NOT NULL DEFAULT 'manual',
    source_tool VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
    external_id VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_knowledge_external(workspace_id,source_tool,external_id),
    KEY idx_knowledge_note_list(workspace_id,deleted,update_time,id),
    KEY idx_knowledge_note_folder(workspace_id,folder_id,deleted),
    KEY idx_knowledge_note_filters(workspace_id,deleted,source,favorite),
    CONSTRAINT fk_knowledge_note_workspace FOREIGN KEY(workspace_id) REFERENCES knowledge_workspace(id),
    CONSTRAINT fk_knowledge_note_folder FOREIGN KEY(folder_id) REFERENCES knowledge_folder(id),
    CONSTRAINT chk_knowledge_tags CHECK(JSON_VALID(tags) AND JSON_TYPE(tags)='ARRAY'),
    CONSTRAINT chk_knowledge_source CHECK(source IN ('manual','mcp')),
    CONSTRAINT chk_knowledge_version CHECK(version>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

START TRANSACTION;
-- Serialize application writes with data migration as well.
SELECT id FROM knowledge_workspace WHERE id=1 FOR UPDATE;
INSERT IGNORE INTO knowledge_folder(id,workspace_id,parent_id,name,sort,create_time,update_time)
SELECT id,1,NULL,name,COALESCE(sort,0),COALESCE(create_time,NOW(6)),COALESCE(update_time,NOW(6))
FROM category;

-- Both published notes AND drafts are private after migration. Already-deleted
-- articles become soft-deleted tombstones, not newly visible notes. Every author's
-- legacy content intentionally belongs to the one shared administrator workspace.
INSERT IGNORE INTO knowledge_note(id,workspace_id,title,content,summary,folder_id,tags,favorite,source,source_tool,external_id,version,deleted,create_time,update_time)
SELECT a.id,1,a.title,COALESCE(a.content,''),COALESCE(a.summary,''),f.id,
       COALESCE((SELECT JSON_ARRAYAGG(t.name) FROM article_tag atg JOIN tag t ON t.id=atg.tag_id WHERE atg.article_id=a.id),'[]'),
       COALESCE(a.top,0)<>0,'manual',NULL,NULL,0,IF(a.status=2,1,0),
       COALESCE(a.create_time,NOW(6)),COALESCE(a.update_time,NOW(6))
FROM article a LEFT JOIN knowledge_folder f ON f.id=a.category_id AND f.workspace_id=1;
COMMIT;
-- No indexed flag: no search index service is configured. REST reports
-- indexStatus='not_configured'. Search is a parameterized title/content scan.
