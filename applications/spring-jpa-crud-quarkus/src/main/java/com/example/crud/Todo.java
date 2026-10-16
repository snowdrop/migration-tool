package com.example.crud;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Entity
@Table(name = "todos")
public class Todo extends PanacheEntity {

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    public String title;

    @Size(max = 1000)
    public String description;

    @Column(nullable = false)
    public boolean completed = false;

    public Todo() {}

    public Todo(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public static List<Todo> findByCompleted(boolean completed) {
        return list("completed", completed);
    }

    public static List<Todo> searchByTitle(String keyword) {
        return list("title like ?1", "%" + keyword + "%");
    }
}
