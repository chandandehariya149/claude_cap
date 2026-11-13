package com.example.todo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class TodoServiceTest {

    private TodoService service;

    @BeforeEach
    void setUp() {
        service = new TodoService();
    }

    @Test
    void addTodo_success() {
        Todo todo = service.add("Buy milk");
        assertNotNull(todo.getId());
        assertEquals("Buy milk", todo.getText());
        assertFalse(todo.isDone());
    }

    @Test
    void addTodo_emptyText_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.add(""));
    }

    @Test
    void addTodo_blankText_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.add("   "));
    }

    @Test
    void addTodo_nullText_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.add(null));
    }

    @Test
    void addTodo_tooLongText_throws() {
        String longText = "a".repeat(501);
        assertThrows(IllegalArgumentException.class, () -> service.add(longText));
    }

    @Test
    void toggleTodo_flipsDone() {
        Todo todo = service.add("Write tests");
        assertFalse(todo.isDone());
        Todo toggled = service.toggle(todo.getId());
        assertTrue(toggled.isDone());
        service.toggle(todo.getId());
        assertFalse(todo.isDone());
    }

    @Test
    void toggleTodo_notFound_throws() {
        assertThrows(NoSuchElementException.class, () -> service.toggle(999L));
    }

    @Test
    void deleteTodo_success() {
        Todo first = service.add("Delete me");
        Todo second = service.add("Keep me");
        service.delete(first.getId());
        assertEquals(1, service.getAll().size());
        assertEquals(second.getId(), service.getAll().get(0).getId());
    }

    @Test
    void deleteTodo_notFound_throws() {
        assertThrows(NoSuchElementException.class, () -> service.delete(999L));
    }

    @Test
    void getAll_returnsAllTodos() {
        service.add("Task 1");
        service.add("Task 2");
        service.add("Task 3");
        assertEquals(3, service.getAll().size());
    }

    // ── 9 new priority tests ──────────────────────────────────────────────────

    @Test
    void add_withHighPriority_returnsTodoWithHighPriority() {
        Todo todo = service.add("task", "HIGH");
        assertEquals("HIGH", todo.getPriority());
    }

    @Test
    void add_withLowercasePriority_normalizesToUppercase() {
        Todo todo = service.add("task", "low");
        assertEquals("LOW", todo.getPriority());
    }

    @Test
    void add_withMixedCasePriority_normalizesToUppercase() {
        Todo todo = service.add("task", "Medium");
        assertEquals("MEDIUM", todo.getPriority());
    }

    @Test
    void add_withNullPriority_defaultsToMedium() {
        Todo todo = service.add("task", null);
        assertEquals("MEDIUM", todo.getPriority());
    }

    @Test
    void add_withBlankPriority_defaultsToMedium() {
        Todo todo = service.add("task", "  ");
        assertEquals("MEDIUM", todo.getPriority());
    }

    @Test
    void add_withInvalidPriority_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.add("task", "URGENT"));
    }

    @Test
    void add_withWhitespacePaddedPriority_normalizesCorrectly() {
        Todo todo = service.add("task", "  HIGH  ");
        assertEquals("HIGH", todo.getPriority());
    }

    @Test
    void add_withLowPriority_returnsTodoWithLowPriority() {
        Todo todo = service.add("task", "LOW");
        assertEquals("LOW", todo.getPriority());
    }

    @Test
    void getAll_returnsTasksWithPriorityField() {
        service.add("Task A", "HIGH");
        service.add("Task B", "LOW");
        List<Todo> all = service.getAll();
        assertEquals(2, all.size());
        assertNotNull(all.get(0).getPriority());
        assertNotNull(all.get(1).getPriority());
        assertEquals("HIGH", all.get(0).getPriority());
        assertEquals("LOW", all.get(1).getPriority());
    }
}
