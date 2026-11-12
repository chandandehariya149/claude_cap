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

    @Test
    void getAll_returns200AndJsonArray() throws Exception {
        when(todoService.getAll()).thenReturn(List.of(new Todo(1L, "Test task", false)));
        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Test task"));
    }

    @Test
    void create_validText_returns201() throws Exception {
        when(todoService.add("New task")).thenReturn(new Todo(1L, "New task", false));
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"New task\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("New task"));
    }

    @Test
    void create_emptyText_returns400() throws Exception {
        when(todoService.add(any())).thenThrow(new IllegalArgumentException("Task text cannot be empty."));
        mockMvc.perform(post("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void toggle_validId_returns200() throws Exception {
        when(todoService.toggle(1L)).thenReturn(new Todo(1L, "Task", true));
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
}
