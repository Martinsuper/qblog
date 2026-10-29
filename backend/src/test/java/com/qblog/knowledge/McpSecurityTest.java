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
    private final McpController controller = new McpController(json,notes);
    private Object invoke(String name,String arguments,Set<String> rights) throws Exception {
        MockHttpServletRequest request=new MockHttpServletRequest();
        request.setContent(("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\""+name+"\",\"arguments\":"+arguments+"}}").getBytes());
        var p=new KnowledgeTokenService.Principal(8L,1L,rights,new KnowledgeTokenService.DirectoryScope(false,false,Set.of(2L)));
        return controller.rpc(request,new UsernamePasswordAuthenticationToken(p,null,List.of())).getBody();
    }
    private void denied(Object body) { assertTrue(json.valueToTree(body).path("result").path("isError").asBoolean()); }
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
}
