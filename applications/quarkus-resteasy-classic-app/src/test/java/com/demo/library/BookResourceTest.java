package com.demo.library;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookResourceTest {

    @Test
    @Order(1)
    void testGetAllBooks() {
        given()
            .when().get("/books")
            .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(3))
                .body("title", hasItem("Effective Java"))
                .body("title", hasItem("Clean Code"))
                .body("title", hasItem("Design Patterns"));
    }

    @Test
    @Order(2)
    void testGetBookById() {
        given()
            .when().get("/books/1")
            .then()
                .statusCode(200)
                .body("title", is("Effective Java"))
                .body("author", is("Joshua Bloch"))
                .body("publicationYear", is(2018))
                .body("isbn", is("978-0134685991"));
    }

    @Test
    @Order(3)
    void testGetBookNotFound() {
        given()
            .when().get("/books/999")
            .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    void testCreateBook() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Refactoring",
                    "author": "Martin Fowler",
                    "publicationYear": 2018,
                    "isbn": "978-0134757599"
                }
                """)
            .when().post("/books")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", is("Refactoring"))
                .body("author", is("Martin Fowler"));
    }

    @Test
    @Order(5)
    void testUpdateBook() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Effective Java (3rd Edition)",
                    "author": "Joshua Bloch",
                    "publicationYear": 2018,
                    "isbn": "978-0134685991"
                }
                """)
            .when().put("/books/1")
            .then()
                .statusCode(200)
                .body("title", is("Effective Java (3rd Edition)"));
    }

    @Test
    @Order(6)
    void testUpdateBookNotFound() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Ghost Book",
                    "author": "Nobody",
                    "publicationYear": 2000,
                    "isbn": "000-0000000000"
                }
                """)
            .when().put("/books/999")
            .then()
                .statusCode(404);
    }

    @Test
    @Order(7)
    void testDeleteBook() {
        given()
            .when().delete("/books/3")
            .then()
                .statusCode(204);

        // Verify it's gone
        given()
            .when().get("/books/3")
            .then()
                .statusCode(404);
    }

    @Test
    @Order(8)
    void testDeleteBookNotFound() {
        given()
            .when().delete("/books/999")
            .then()
                .statusCode(404);
    }
}
