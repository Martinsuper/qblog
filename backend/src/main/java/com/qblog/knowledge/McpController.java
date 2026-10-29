package com.qblog.knowledge;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.qblog.knowledge.KnowledgeModels.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;

/** Streamable HTTP, JSON-only, stateless MCP. No session identifier is issued or accepted. */
@RestController
@RequiredArgsConstructor
public class McpController {
    public static final String PROTOCOL = "2025-03-26";
    public static final int MAX_BODY = 1024 * 1024;
    private final ObjectMapper json;
    private final KnowledgeService notes;

    @PostMapping(value="/mcp", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> rpc(HttpServletRequest request, Authentication authentication) throws IOException {
        if (request.getHeader("Mcp-Session-Id") != null) return ResponseEntity.badRequest().body(error(null,-32600,"Sessions are not supported"));
        String version = request.getHeader("MCP-Protocol-Version");
        if (version != null && !PROTOCOL.equals(version)) return ResponseEntity.badRequest().body(error(null,-32600,"Unsupported protocol version"));
        if (request.getContentLengthLong() > MAX_BODY) return ResponseEntity.status(413).body(error(null,-32600,"Request too large"));
        byte[] bytes = request.getInputStream().readNBytes(MAX_BODY + 1);
        if (bytes.length > MAX_BODY) return ResponseEntity.status(413).body(error(null,-32600,"Request too large"));
        JsonNode body;
        try { body = json.readTree(bytes); } catch (Exception e) { return ResponseEntity.ok(error(null,-32700,"Parse error")); }
        if (body == null || !body.isObject() || !"2.0".equals(body.path("jsonrpc").asText()) || !body.path("method").isTextual()
                || (body.has("id") && !(body.get("id").isTextual() || body.get("id").isIntegralNumber())))
            return ResponseEntity.ok(error(null,-32600,"Invalid Request"));
        JsonNode id = body.get("id");
        String method = body.path("method").asText();
        if (id == null) {
            if ("notifications/initialized".equals(method)) return ResponseEntity.accepted().build();
            // Notifications never produce JSON-RPC responses.
            return ResponseEntity.accepted().build();
        }
        if (body.has("params") && !body.get("params").isObject()) return ResponseEntity.ok(error(id,-32602,"Invalid params"));
        JsonNode params = body.path("params");
        Object result;
        switch (method) {
            case "initialize" -> {
                if (!params.path("protocolVersion").isTextual() || !params.path("capabilities").isObject() || !params.path("clientInfo").isObject())
                    return ResponseEntity.ok(error(id,-32602,"Invalid params"));
                result = Map.of("protocolVersion",PROTOCOL,"capabilities",Map.of("tools",Map.of("listChanged",false)),
                        "serverInfo",Map.of("name","QBlog private knowledge","version","1.0.0"),
                        "instructions","SQL text search; no vector retrieval. Stateless JSON transport; no sessions.");
            }
            case "ping" -> result = Map.of();
            case "tools/list" -> result = Map.of("tools",toolDefinitions());
            case "tools/call" -> {
                if (!params.path("name").isTextual() || !params.path("arguments").isObject()) return ResponseEntity.ok(error(id,-32602,"Invalid params"));
                String name = params.path("name").asText();
                if (!Set.of("notes_search","notes_get","notes_create","notes_update").contains(name)) return ResponseEntity.ok(error(id,-32602,"Unknown tool"));
                try {
                    var principal = (KnowledgeTokenService.Principal)authentication.getPrincipal();
                    Object data = call(name,params.get("arguments"),principal);
                    result = Map.of("content",List.of(Map.of("type","text","text",json.writeValueAsString(data))),"isError",false);
                } catch (Exception denied) {
                    // Identical errors for nonexistent/out-of-scope records, external IDs and directories.
                    result = Map.of("content",List.of(Map.of("type","text","text","Tool request denied or invalid")),"isError",true);
                }
            }
            default -> { return ResponseEntity.ok(error(id,-32601,"Method not found")); }
        }
        ObjectNode response = json.createObjectNode(); response.put("jsonrpc","2.0"); response.set("id",id); response.set("result",json.valueToTree(result));
        return ResponseEntity.ok().header("MCP-Protocol-Version",PROTOCOL).body(response);
    }
    private Object call(String name, JsonNode args, KnowledgeTokenService.Principal token) {
        var directory = token.directoryScope();
        var scope = new KnowledgeScope(directory.directoryIds(),directory.all(),directory.inbox());
        token.require(name.equals("notes_search") || name.equals("notes_get") ? "READ" : "WRITE");
        switch (name) {
            case "notes_search": {
                fields(args,Set.of("search","folderId","inbox","current","size","tag"));
                NoteQuery query = json.convertValue(args,NoteQuery.class);
                if (query.getCurrent()<1 || query.getSize()<1 || query.getSize()>100 || (query.getSearch()!=null && query.getSearch().length()>500)) throw new IllegalArgumentException();
                if (query.getFolderId()!=null) scope.requireFolder(query.getFolderId());
                if (Boolean.TRUE.equals(query.getInbox())) scope.requireFolder(null);
                return notes.list(query,scope);
            }
            case "notes_get": {
                fields(args,Set.of("id")); return notes.get(id(args),scope);
            }
            case "notes_create": {
                fields(args,Set.of("title","content","summary","folderId","tags","favorite","externalId","version"));
                NoteWrite write = json.convertValue(args,NoteWrite.class); validateWrite(write);
                scope.requireFolder(write.getFolderId()); write.setSource("mcp");
                write.setSourceTool("token:"+token.id());
                if (write.getExternalId()==null) write.setExternalId(UUID.randomUUID().toString());
                if (write.getExternalId().isBlank() || write.getExternalId().length()>200) throw new IllegalArgumentException();
                return notes.upsert(write,scope);
            }
            case "notes_update": {
                fields(args,Set.of("id","title","content","summary","folderId","tags","favorite","version"));
                Long id = id(args); ObjectNode data = ((ObjectNode)args).deepCopy(); data.remove("id");
                NoteWrite write = json.convertValue(data,NoteWrite.class); validateWrite(write);
                // Both old and new directory checked before any mutation, also checked by service transaction.
                NoteView old = notes.get(id,scope); scope.requireFolder(old.getFolderId()); scope.requireFolder(write.getFolderId());
                if (write.getVersion()==null) throw new IllegalArgumentException();
                write.setSource(old.getSource()); write.setSourceTool(old.getSourceTool()); write.setExternalId(old.getExternalId());
                return notes.update(id,write,scope);
            }
            default: throw new IllegalArgumentException();
        }
    }
    private void validateWrite(NoteWrite write) {
        if (write.getTitle()==null || write.getTitle().isBlank() || write.getTitle().length()>200 || write.getContent()==null || write.getContent().length()>900000) throw new IllegalArgumentException();
    }
    private Long id(JsonNode args) { if (!args.path("id").isIntegralNumber() || !args.path("id").canConvertToLong() || args.path("id").asLong()<=0) throw new IllegalArgumentException(); return args.path("id").asLong(); }
    private void fields(JsonNode args,Set<String> allowed) {
        args.fieldNames().forEachRemaining(k -> {
            if (!allowed.contains(k)) throw new IllegalArgumentException();
            JsonNode value = args.get(k);
            if (Set.of("id","folderId","version","current","size").contains(k)) {
                if (value.isNull() && "folderId".equals(k)) return;
                if (!value.isIntegralNumber() || !value.canConvertToLong() || value.asLong() < ("version".equals(k) ? 0 : 1)) throw new IllegalArgumentException();
            } else if (Set.of("favorite","inbox").contains(k)) {
                if (!value.isBoolean()) throw new IllegalArgumentException();
            } else if ("tags".equals(k)) {
                if (!value.isArray() || value.size() > 50) throw new IllegalArgumentException();
                value.forEach(tag -> { if (!tag.isTextual() || tag.asText().isBlank() || tag.asText().length()>50) throw new IllegalArgumentException(); });
            } else if (!value.isTextual()) throw new IllegalArgumentException();
        });
    }
    private ObjectNode error(JsonNode id,int code,String message) {
        ObjectNode result=json.createObjectNode(); result.put("jsonrpc","2.0"); result.set("id",id==null?json.nullNode():id);
        result.set("error",json.valueToTree(Map.of("code",code,"message",message))); return result;
    }
    private List<Map<String,Object>> toolDefinitions() {
        Map<String,Object> text=Map.of("type","string"), number=Map.of("type","integer","minimum",1), folder=Map.of("type",List.of("integer","null"),"minimum",1);
        Map<String,Object> write=new LinkedHashMap<>(); write.put("title",text); write.put("content",text); write.put("summary",text); write.put("folderId",folder);
        write.put("tags",Map.of("type","array","items",text)); write.put("favorite",Map.of("type","boolean")); write.put("version",Map.of("type","integer","minimum",0));
        var create=new LinkedHashMap<>(write); create.put("externalId",text);
        var update=new LinkedHashMap<>(write); update.put("id",number);
        return List.of(tool("notes_search","SQL text search within authorized folders (not vector search)",Map.of("search",text,"folderId",folder,"inbox",Map.of("type","boolean"),"tag",text,"current",number,"size",Map.of("type","integer","minimum",1,"maximum",100)),List.of()),
                tool("notes_get","Read an authorized note",Map.of("id",number),List.of("id")),
                tool("notes_create","Create or idempotently upsert by this token's externalId",create,List.of("title","content")),
                tool("notes_update","Replace a note; current version required; folderId omitted means inbox",update,List.of("id","title","content","version")));
    }
    private Map<String,Object> tool(String name,String description,Map<String,Object> properties,List<String> required) {
        return Map.of("name",name,"description",description,"inputSchema",Map.of("type","object","properties",properties,"required",required,"additionalProperties",false));
    }
}
