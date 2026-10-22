package com.example.todo;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TodoService {

    private final CopyOnWriteArrayList<Todo> todos = new CopyOnWriteArrayList<>();
    private final AtomicLong counter = new AtomicLong();

    public List<Todo> getAll() {
        return new ArrayList<>(todos);
    }

    public Todo add(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Task text cannot be empty.");
        }
        if (text.length() > 500) {
            throw new IllegalArgumentException("Task text must be 500 characters or fewer.");
        }
        Todo todo = new Todo(counter.incrementAndGet(), text.trim(), false);
        todos.add(todo);
        return todo;
    }

    public Todo toggle(Long id) {
        Todo todo = findById(id);
        todo.setDone(!todo.isDone());
        return todo;
    }

    public void delete(Long id) {
        Todo todo = findById(id);
        todos.remove(todo);
    }

    private Todo findById(Long id) {
        return todos.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Todo not found: " + id));
    }
}
