package com.example.crud;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class TodoResourceTest {

    @Test
    void crudOperations() {
        // Create
        Long id = given()
                .contentType("application/json")
                .body("{\"title\":\"Buy milk\",\"description\":\"From the store\"}")
                .when().post("/api/todos")
                .then()
                .statusCode(201)
                .body("title", equalTo("Buy milk"))
                .body("id", notNullValue())
                .extract().jsonPath().getLong("id");

        // Read
        given()
                .when().get("/api/todos/{id}", id)
                .then()
                .statusCode(200)
                .body("title", equalTo("Buy milk"));

        // Update
        given()
                .contentType("application/json")
                .body("{\"title\":\"Buy oat milk\",\"description\":\"From the organic store\",\"completed\":true}")
                .when().put("/api/todos/{id}", id)
                .then()
                .statusCode(200)
                .body("title", equalTo("Buy oat milk"))
                .body("completed", equalTo(true));

        // List
        given()
                .when().get("/api/todos")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));

        // Filter by completed
        given()
                .when().get("/api/todos?completed=true")
                .then()
                .statusCode(200);

        // Search
        given()
                .when().get("/api/todos/search?keyword=oat")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));

        // Delete
        given()
                .when().delete("/api/todos/{id}", id)
                .then()
                .statusCode(204);

        // Verify deleted
        given()
                .when().get("/api/todos/{id}", id)
                .then()
                .statusCode(404);
    }

    @Test
    void createWithInvalidInput() {
        given()
                .contentType("application/json")
                .body("{\"title\":\"\",\"description\":null}")
                .when().post("/api/todos")
                .then()
                .statusCode(400);
    }

    @Test
    void getNotFound() {
        given()
                .when().get("/api/todos/{id}", 9999)
                .then()
                .statusCode(404);
    }
}
