package demo;

import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class ReqresTest {

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "https://reqres.in/api";
    }

    @Test
    public void testGetUsers() {
        given()
            .header("x-api-key", "reqres-free-v1")
        .when()
            .get("/users?page=2")
        .then()
            .statusCode(200)
            .body("page", equalTo(2))
            .body("data", not(empty()));
    }

    @Test
    public void testCreateUser() {
        String body = "{ \"name\": \"morpheus\", \"job\": \"leader\" }";

        given()
            .header("x-api-key", "reqres-free-v1")
            .contentType("application/json")
            .accept("application/json")
            .body(body)
        .when()
            .post("/users")
        .then()
            .statusCode(201)
            .body("name", equalTo("morpheus"))
            .body("job", equalTo("leader"));
    }

    @Test
    public void testDeleteUser() {
        given()
            .header("x-api-key", "reqres-free-v1")
        .when()
            .delete("/users/2")
        .then()
            .statusCode(204);
    }

    @Test
    public void testGetUserNotFound() {
        given()
            .header("x-api-key", "reqres-free-v1")
        .when()
            .get("/users/9999")
        .then()
            .statusCode(404);
    }

    @Test
    public void testDeleteUserNotFound() {
        given()
            .header("x-api-key", "reqres-free-v1")
        .when()
            .delete("/users/9999")
        .then()
            .statusCode(204);
    }

    @Test
    public void testCreateUserWithEmptyBody() {
        given()
            .header("x-api-key", "reqres-free-v1")
            .contentType("application/json")
            .accept("application/json")
            .body("{}")
        .when()
            .post("/users")
        .then()
            .statusCode(201);
    }
}
