package com.example.crud;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Transactional
public class TodoService {

    public List<Todo> findAll() {
        return Todo.listAll();
    }

    public Optional<Todo> findById(Long id) {
        return Optional.ofNullable(Todo.findById(id));
    }

    public List<Todo> findByCompleted(boolean completed) {
        return Todo.findByCompleted(completed);
    }

    public List<Todo> search(String keyword) {
        return Todo.searchByTitle(keyword);
    }

    public Todo create(Todo todo) {
        todo.persist();
        return todo;
    }

    public Optional<Todo> update(Long id, Todo updated) {
        Todo todo = Todo.findById(id);
        if (todo == null) {
            return Optional.empty();
        }
        todo.title = updated.title;
        todo.description = updated.description;
        todo.completed = updated.completed;
        return Optional.of(todo);
    }

    public boolean delete(Long id) {
        return Todo.deleteById(id);
    }
}
