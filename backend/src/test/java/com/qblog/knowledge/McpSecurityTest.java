package com.qblog.knowledge;

import com.fasterxml.jackson.databind.*;
import com.qblog.knowledge.KnowledgeModels.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class McpSecurityTest {
    private final ObjectMapper json = new ObjectMapper();
    private final KnowledgeService notes = mock(KnowledgeService.class);
    private final KnowledgeFolderService folders = mock(KnowledgeFolderService.class);
    private final McpController controller = new McpController(json,notes,folders);
    private Object invoke(String name,String arguments,Set<String> rights) throws Exception { return invoke(name,arguments,rights,new KnowledgeTokenService.DirectoryScope(false,false,Set.of(2L))); }
    private Object invoke(String name,String arguments,Set<String> rights,KnowledgeTokenService.DirectoryScope directory) throws Exception {
        // Existing authorization tests must use valid Markdown so policy failures cannot mask scope regressions.
        arguments = arguments.replace("\"content\":\"x\"", "\"content\":" + json.writeValueAsString("## 一、简介\n内容"));
        MockHttpServletRequest request=new MockHttpServletRequest();
        request.setContent(("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\""+name+"\",\"arguments\":"+arguments+"}}").getBytes());
        var p=new KnowledgeTokenService.Principal(8L,1L,rights,directory);
        return controller.rpc(request,new UsernamePasswordAuthenticationToken(p,null,List.of())).getBody();
    }
    private Object invokeAll(String name,String arguments,Set<String> rights) throws Exception { return invoke(name,arguments,rights,new KnowledgeTokenService.DirectoryScope(true,false,Set.of())); }
    private void denied(Object body) { assertTrue(json.valueToTree(body).path("result").path("isError").asBoolean()); }
    @Test void malformedMarkdownIsRejectedBeforeCreateOrUpdate() throws Exception {
        for (String name : List.of("notes_create", "notes_update")) {
            var args = json.createObjectNode().put("title", "test").put("content", "## 未编号").put("folderId", 2);
            if (name.equals("notes_update")) args.put("id", 1).put("version", 0);
            Object result = invoke(name, json.writeValueAsString(args), Set.of("WRITE"));
            denied(result);
            assertTrue(json.valueToTree(result).path("result").path("content").get(0).path("text").asText().contains("笔记格式不符合规范"));
        }
        verifyNoInteractions(notes);
    }
    @Test void validMarkdownCanUpdateAndRetainsMetadata() throws Exception {
        NoteView old = new NoteView(); old.setFolderId(2L); old.setSource("mcp"); old.setSourceTool("token:8"); old.setExternalId("e");
        when(notes.get(eq(1L), any())).thenReturn(old);
        when(notes.update(eq(1L), any(), any())).thenReturn(old);
        Object body = invoke("notes_update", "{\"id\":1,\"title\":\"x\",\"content\":\"x\",\"folderId\":2,\"version\":1}", Set.of("WRITE"));
        assertFalse(json.valueToTree(body).path("result").path("isError").asBoolean());
        verify(notes).update(eq(1L), argThat(w -> "token:8".equals(w.getSourceTool()) && w.getVersion() == 1L && w.getFolderId() == 2L), any());
    }
    @Test void toolDiscoveryDescribesEnforcedNoteContentPolicyOnlyForNotes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}".getBytes());
        JsonNode tools = json.valueToTree(controller.rpc(request, null).getBody()).path("result").path("tools");
        for (JsonNode tool : tools) {
            String name = tool.path("name").asText();
            String policy = tool.path("inputSchema").path("properties").path("content").path("description").asText();
            if (Set.of("notes_create", "notes_update").contains(name)) assertEquals(NoteMarkdownPolicy.INSTRUCTIONS, policy);
            else assertTrue(policy.isEmpty());
        }
    }
    @Test void readOnlyCannotCreateOrUpdate() throws Exception {
        denied(invoke("notes_create","{\"title\":\"x\",\"content\":\"x\",\"folderId\":2}",Set.of("READ")));
        denied(invoke("notes_update","{\"id\":1,\"title\":\"x\",\"content\":\"x\",\"folderId\":2,\"version\":1}",Set.of("READ")));
        verifyNoInteractions(notes);
    }
    @Test void cannotCreateIntoOtherDirectoryOrImplicitInbox() throws Exception {
        denied(invoke("notes_create","{\"title\":\"x\",\"content\":\"x\",\"folderId\":3}",Set.of("WRITE")));
        denied(invoke("notes_create","{\"title\":\"x\",\"content\":\"x\"}",Set.of("WRITE")));
        verifyNoInteractions(notes);
    }
    @Test void cannotMoveOutOfAllowedDirectory() throws Exception {
        NoteView old=new NoteView(); old.setFolderId(2L);
        when(notes.get(eq(1L),any())).thenReturn(old);
        denied(invoke("notes_update","{\"id\":1,\"title\":\"x\",\"content\":\"x\",\"folderId\":3,\"version\":1}",Set.of("WRITE")));
        verify(notes,never()).update(any(),any(),any());
    }
    @Test void cannotMoveFromOutsideDirectoryEvenIfDestinationAllowed() throws Exception {
        NoteView old=new NoteView(); old.setFolderId(3L);
        when(notes.get(eq(1L),any())).thenReturn(old);
        denied(invoke("notes_update","{\"id\":1,\"title\":\"x\",\"content\":\"x\",\"folderId\":2,\"version\":1}",Set.of("WRITE")));
        verify(notes,never()).update(any(),any(),any());
    }
    @Test void searchAlwaysPassesExactScopeAndCannotQueryOutside() throws Exception {
        invoke("notes_search","{\"search\":\"sql\"}",Set.of("READ"));
        verify(notes).list(any(),eq(new KnowledgeScope(Set.of(2L),false,false)));
        denied(invoke("notes_search","{\"folderId\":3}",Set.of("READ")));
    }
    @Test void externalIdIsNamespacedByTokenNotCaller() throws Exception {
        invoke("notes_create","{\"title\":\"x\",\"content\":\"x\",\"folderId\":2,\"externalId\":\"e\"}",Set.of("WRITE"));
        verify(notes).upsert(argThat(w -> "token:8".equals(w.getSourceTool()) && "e".equals(w.getExternalId())),any());
        denied(invoke("notes_create","{\"title\":\"x\",\"content\":\"x\",\"folderId\":2,\"sourceTool\":\"token:9\"}",Set.of("WRITE")));
    }
    @Test void bodyLimitAndProtocolVersion() throws Exception {
        MockHttpServletRequest request=new MockHttpServletRequest(); request.setContent(new byte[McpController.MAX_BODY+1]);
        assertEquals(413,controller.rpc(request,null).getStatusCode().value());
        request=new MockHttpServletRequest(); request.addHeader("MCP-Protocol-Version","unknown");
        assertEquals(400,controller.rpc(request,null).getStatusCode().value());
    }
    @Test void initializePingNotificationsAndParseErrorsAreJsonRpc() throws Exception {
        MockHttpServletRequest request=new MockHttpServletRequest();
        request.setContent("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}}".getBytes());
        var response=controller.rpc(request,null);
        assertEquals(McpController.PROTOCOL,json.valueToTree(response.getBody()).path("result").path("protocolVersion").asText());
        assertTrue(json.valueToTree(response.getBody()).path("result").path("instructions").asText().contains(NoteMarkdownPolicy.INSTRUCTIONS));
        assertNull(response.getHeaders().getFirst("Mcp-Session-Id"));
        request=new MockHttpServletRequest(); request.setContent("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}".getBytes());
        assertEquals(202,controller.rpc(request,null).getStatusCode().value());
        request=new MockHttpServletRequest(); request.setContent("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\"}".getBytes());
        assertTrue(json.valueToTree(controller.rpc(request,null).getBody()).path("result").isObject());
        request=new MockHttpServletRequest(); request.setContent("{".getBytes());
        assertEquals(-32700,json.valueToTree(controller.rpc(request,null).getBody()).path("error").path("code").asInt());
    }
    @Test void strictArgumentTypesCannotBeCoerced() throws Exception {
        denied(invoke("notes_search","{\"folderId\":\"2\"}",Set.of("READ")));
        denied(invoke("notes_create","{\"title\":true,\"content\":\"x\",\"folderId\":2}",Set.of("WRITE")));
        verifyNoInteractions(notes);
    }
    @Test void errorsDoNotLeakDirectoryOrExistence() throws Exception {
        when(notes.get(any(),any())).thenThrow(new RuntimeException("secret directory 837 name"));
        String response=json.writeValueAsString(invoke("notes_get","{\"id\":1}",Set.of("READ")));
        assertFalse(response.contains("837")); assertFalse(response.contains("directory"));
    }
    @Test void scopedTokensCannotChangeTheTreeButListOnlyAuthorizedFolders() throws Exception {
        denied(invoke("folders_create","{\"name\":\"x\"}",Set.of("WRITE")));
        denied(invoke("folders_update","{\"id\":2,\"name\":\"x\",\"parentId\":3}",Set.of("WRITE")));
        denied(invoke("folders_delete","{\"id\":2}",Set.of("WRITE")));
        verifyNoInteractions(folders);
        FolderView child=new FolderView(); child.setId(2L); child.setName("child");
        FolderView parent=new FolderView(); parent.setId(3L); parent.setName("parent"); parent.getChildren().add(child);
        FolderView other=new FolderView(); other.setId(4L);
        when(folders.list()).thenReturn(List.of(parent,other));
        String listed=json.valueToTree(invoke("folders_list","{}",Set.of("READ"))).path("result").path("content").get(0).path("text").asText();
        assertTrue(listed.contains("\"name\":\"child\""));
        assertFalse(listed.contains("\"id\":3"));
        assertFalse(listed.contains("\"id\":4"));
        // 收件箱专用令牌没有可见目录
        when(folders.list()).thenReturn(List.of(other));
        String inbox=json.valueToTree(invoke("folders_list","{}",Set.of("READ"),new KnowledgeTokenService.DirectoryScope(false,true,Set.of()))).path("result").path("content").get(0).path("text").asText();
        assertFalse(inbox.contains("\"id\":4"));
    }
    @Test void unrestrictedTokensManageFolders() throws Exception {
        denied(invoke("folders_create","{\"name\":\"x\"}",Set.of("WRITE")));
        Object created=invokeAll("folders_create","{\"name\":\"x\",\"parentId\":null,\"sort\":0}",Set.of("WRITE"));
        verify(folders).create(argThat(w -> "x".equals(w.getName()) && w.getParentId()==null));
        invokeAll("folders_update","{\"id\":2,\"name\":\"y\"}",Set.of("WRITE"));
        verify(folders).update(eq(2L),argThat(w -> "y".equals(w.getName())));
        invokeAll("folders_delete","{\"id\":2}",Set.of("WRITE"));
        verify(folders).delete(2L);
        // 四次拒绝（无写权限、sort 为负、parentId 类型错误、无 READ 权限）都不允许触达服务层
        denied(invokeAll("folders_create","{\"name\":\"x\",\"sort\":-1}",Set.of("WRITE")));
        denied(invokeAll("folders_create","{\"name\":\"x\",\"parentId\":\"2\"}",Set.of("WRITE")));
        denied(invoke("folders_create","{\"name\":\"x\"}",Set.of("WRITE"),new KnowledgeTokenService.DirectoryScope(false,false,Set.of(2L))));
        denied(invokeAll("folders_list","{}",Set.of()));
        verify(folders).create(argThat(w -> "x".equals(w.getName()) && w.getParentId()==null));
        verify(folders).update(eq(2L),argThat(w -> "y".equals(w.getName())));
        verify(folders).delete(2L);
        verifyNoMoreInteractions(folders);
    }
}
