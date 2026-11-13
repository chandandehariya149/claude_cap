package com.example.todo;

public class Todo {

    private Long id;
    private String text;
    private boolean done;
    private String priority;

    public Todo() {}

    public Todo(Long id, String text, boolean done) {
        this.id = id;
        this.text = text;
        this.done = done;
    }

    public Todo(Long id, String text, boolean done, String priority) {
        this.id = id;
        this.text = text;
        this.done = done;
        this.priority = priority;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
