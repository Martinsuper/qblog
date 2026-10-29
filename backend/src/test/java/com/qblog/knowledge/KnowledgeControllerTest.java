package com.qblog.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qblog.common.GlobalExceptionHandler;
import com.qblog.knowledge.KnowledgeModels.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class KnowledgeControllerTest {
    KnowledgeNoteService notes; MockMvc mvc;
    @BeforeEach void setup() {
        notes = mock(KnowledgeNoteService.class);
        mvc = standaloneSetup(new KnowledgeController(notes, mock(KnowledgeFolderService.class)))
            .setControllerAdvice(new GlobalExceptionHandler(), new KnowledgeRequestAdvice()).build();
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void admin() { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))); }
    @Test void unprivilegedCannotReadNotesOrFolders() throws Exception {
        mvc.perform(get("/knowledge/notes")).andExpect(status().isForbidden());
        mvc.perform(get("/knowledge/folders")).andExpect(status().isForbidden()); verifyNoInteractions(notes);
    }
    @Test void browserCannotForgeIntegrationIdentity() throws Exception {
        admin(); mvc.perform(post("/knowledge/notes").contentType("application/json").content("{\"title\":\"a\",\"source\":\"mcp\",\"sourceTool\":\"token:5\",\"externalId\":\"x\"}"))
            .andExpect(status().isBadRequest()); verifyNoInteractions(notes);
    }
    @Test void missingDeleteVersionIs400Not500() throws Exception {
        admin(); mvc.perform(delete("/knowledge/notes/1")).andExpect(status().isBadRequest()); verifyNoInteractions(notes);
    }
    @Test void malformedJsonIs400Not500() throws Exception {
        admin(); mvc.perform(post("/knowledge/notes").contentType("application/json").content("{"))
            .andExpect(status().isBadRequest());
    }
    @Test void browserEditPreservesMcpIdentity() throws Exception {
        admin(); NoteView old = new NoteView(); old.setId(1L); old.setSource("mcp"); old.setSourceTool("token:9"); old.setExternalId("external");
        when(notes.get(1L)).thenReturn(old); when(notes.update(eq(1L), any(NoteWrite.class))).thenReturn(old);
        mvc.perform(put("/knowledge/notes/1").contentType("application/json").content("{\"title\":\"edited\",\"content\":\"body\",\"source\":\"mcp\",\"version\":4}"))
            .andExpect(status().isOk());
        verify(notes).update(eq(1L), argThat(w -> "token:9".equals(w.getSourceTool()) && "external".equals(w.getExternalId()) && w.getVersion()==4));
    }
    @Test void inboxFieldIsExplicitNullWhileListBodyIsOmitted() throws Exception {
        String encoded = new ObjectMapper().writeValueAsString(new NoteView());
        org.junit.jupiter.api.Assertions.assertTrue(encoded.contains("\"folderId\":null"));
        org.junit.jupiter.api.Assertions.assertFalse(encoded.contains("\"content\""));
    }
}
