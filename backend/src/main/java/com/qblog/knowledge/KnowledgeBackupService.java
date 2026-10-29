package com.qblog.knowledge;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.sql.Timestamp;
import java.time.Instant;

/** Export-only data archive, NOT a replacement for a tested MySQL dump/restore plan.
 * A repeatable-read snapshot spans bounded keyset batches. No token table is read.
 * Synchronous servlet output keeps both the transaction and authorization context
 * on the request thread. Client disconnect aborts and releases the transaction.
 */
@Service @RequiredArgsConstructor
public class KnowledgeBackupService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    static final int BATCH_SIZE = 10;
    private static final String FOLDER_COLUMNS = "id,workspace_id,parent_id,name,sort,create_time,update_time";
    private static final String NOTE_COLUMNS = "id,workspace_id,title,content,summary,folder_id,tags,favorite,source,source_tool,external_id,version,deleted,create_time,update_time";

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, rollbackFor = IOException.class)
    public void export(OutputStream output) throws IOException {
        try (JsonGenerator g = json.getFactory().createGenerator(output)) {
            g.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
            g.writeStartObject();
            g.writeStringField("format", "qblog-knowledge-export");
            g.writeNumberField("schemaVersion", 1);
            g.writeStringField("exportedAt", Instant.now().toString());
            g.writeBooleanField("exportOnly", true);
            g.writeBooleanField("includesDeletedNotes", true);
            g.writeBooleanField("includesApiTokens", false);
            g.writeStringField("recovery", "No JSON import endpoint. For disaster recovery use a tested MySQL SQL dump including knowledge_workspace, knowledge_folder and knowledge_note. API tokens/secrets are intentionally excluded; recreate integrations after recovery.");
            g.writeObjectFieldStart("workspace");
            g.writeNumberField("id", 1);
            g.writeStringField("name", jdbc.queryForObject("SELECT name FROM knowledge_workspace WHERE id=1", String.class));
            g.writeEndObject();
            long folders = rows(g, "folders", "knowledge_folder", FOLDER_COLUMNS);
            long notes = rows(g, "notes", "knowledge_note", NOTE_COLUMNS);
            g.writeObjectFieldStart("counts"); g.writeNumberField("folders", folders); g.writeNumberField("notes", notes); g.writeEndObject();
            // A client must see complete=true and a valid closing object before treating the export as successful.
            g.writeBooleanField("complete", true);
            g.writeEndObject(); g.flush();
        } catch (UncheckedIOException ex) { throw ex.getCause(); }
    }
    private long rows(JsonGenerator g, String field, String table, String columns) throws IOException {
        g.writeArrayFieldStart(field);
        long cursor = 0, count = 0;
        String[] names = columns.split(",");
        while (true) {
            long[] result = jdbc.query("SELECT " + columns + " FROM " + table + " WHERE workspace_id=1 AND id>? ORDER BY id LIMIT ?", (ResultSetExtractor<long[]>) rs -> {
                long last = 0, found = 0;
                try {
                    while (rs.next()) {
                        g.writeStartObject();
                        for (String column : names) {
                            g.writeFieldName(column);
                            Object value = rs.getObject(column);
                            if (value instanceof Timestamp time) g.writeString(time.toLocalDateTime().toString());
                            else if (column.equals("tags") && value != null) g.writeTree(json.readTree(value.toString()));
                            else g.writeObject(value);
                        }
                        g.writeEndObject(); last = rs.getLong("id"); found++;
                    }
                    g.flush();
                } catch (IOException ex) { throw new UncheckedIOException(ex); }
                return new long[]{last, found};
            }, cursor, BATCH_SIZE);
            if (result == null || result[1] == 0) break;
            cursor = result[0]; count += result[1];
            if (result[1] < BATCH_SIZE) break;
        }
        g.writeEndArray(); return count;
    }
}
