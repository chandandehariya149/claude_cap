package com.example.todo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TodoController.class)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TodoService todoService;

    // ── Original 7 tests (stubs updated to two-arg form) ─────────────────────

    @Test
    void getAll_returns200AndJsonArray() throws Exception {
        when(todoService.getAll()).thenReturn(List.of(new Todo(1L, "Test task", false, "MEDIUM")));
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Test task"));
    }

    @Test
    void create_validText_returns201() throws Exception {
        when(todoService.add(eq("New task"), any())).thenReturn(new Todo(1L, "New task", false, "MEDIUM"));
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"New task\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("New task"));
    }

    @Test
    void create_emptyText_returns400() throws Exception {
        when(todoService.add(any(), any())).thenThrow(new IllegalArgumentException("Task text cannot be empty."));
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void toggle_validId_returns200() throws Exception {
        when(todoService.toggle(1L)).thenReturn(new Todo(1L, "Task", true, "MEDIUM"));
        mockMvc.perform(put("/api/todos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true));
    }

    @Test
    void toggle_notFound_returns404() throws Exception {
        when(todoService.toggle(99L)).thenThrow(new NoSuchElementException("Todo not found: 99"));
        mockMvc.perform(put("/api/todos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void delete_validId_returns204() throws Exception {
        mockMvc.perform(delete("/api/todos/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new NoSuchElementException("Todo not found: 99")).when(todoService).delete(99L);
        mockMvc.perform(delete("/api/todos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── 7 new priority tests ──────────────────────────────────────────────────

    @Test
    void createTodo_withExplicitHighPriority_returns201WithPriority() throws Exception {
        when(todoService.add(eq("task"), eq("HIGH"))).thenReturn(new Todo(1L, "task", false, "HIGH"));
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"task\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void createTodo_withoutPriority_defaultsToMedium() throws Exception {
        when(todoService.add(anyString(), any())).thenReturn(new Todo(1L, "task", false, "MEDIUM"));
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"task\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void createTodo_withNullPriority_defaultsToMedium() throws Exception {
        when(todoService.add(anyString(), any())).thenReturn(new Todo(1L, "task", false, "MEDIUM"));
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"task\",\"priority\":null}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void createTodo_withInvalidPriority_returns400() throws Exception {
        when(todoService.add(anyString(), any())).thenThrow(
                new IllegalArgumentException("Invalid priority: must be HIGH, MEDIUM, or LOW"));
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"task\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createTodo_withLowercasePriority_normalizesAndReturns201() throws Exception {
        when(todoService.add(anyString(), eq("high"))).thenReturn(new Todo(1L, "task", false, "HIGH"));
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"task\",\"priority\":\"high\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void getAllTodos_includesPriorityField() throws Exception {
        when(todoService.getAll()).thenReturn(List.of(
                new Todo(1L, "Task A", false, "HIGH"),
                new Todo(2L, "Task B", true, "LOW")));
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[1].priority").value("LOW"));
    }

    @Test
    void toggleTodo_doesNotChangePriority() throws Exception {
        when(todoService.toggle(1L)).thenReturn(new Todo(1L, "Task", true, "HIGH"));
        mockMvc.perform(put("/api/todos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.done").value(true));
    }
}
