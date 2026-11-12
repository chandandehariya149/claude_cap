package com.example.todo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
