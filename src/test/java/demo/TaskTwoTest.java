package demo;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class TaskTwoTest {

    private static final String BASE_URL = "https://bookstore-demo-08zr.onrender.com";
    public static String token;
    private final Random random = new Random();
    private static String existingUsername;
    private static String existingEmail;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = BASE_URL;
    }

    private String generateName(String base) {
        int number = random.nextInt(1000);
        return base.toLowerCase() + number;
    }

    // ================= AUTH TESTING =================

    @Test
    public void loginShouldReturnToken() {
        String body = """
                {
                  "userName": "superuser01",
                  "password": "superuser_password"
                }
                """;

        Response response = given()
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/auth/login")
                .then()
                .extract().response();

        System.out.println("Login Status: " + response.statusCode());
        System.out.println("Login Body: " + response.asString());

        response.then()
                .statusCode(200)
                .body("authToken", notNullValue());

        token = response.jsonPath().getString("authToken");
    }

    @Test
    public void loginWithWrongPasswordShouldFail() {
        String body = """
                {
                  "userName": "superuser01",
                  "password": "wrong_password"
                }
                """;

        given()
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("message", containsString("Bad credentials"));
    }

    @Test
    public void loginWithUnknownUserShouldFail() {
        String body = """
                {
                  "userName": "notexist",
                  "password": "whatever"
                }
                """;

        given()
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("message", containsString("Bad credentials"));
    }

    // ================= SIGNUP TESTING =================

    @Test
    public void signUpRegularUserShouldSucceed() {
        String username = generateName("budi");
        String email = username + "@gmail.com";
        existingUsername = username;
        existingEmail = email;

        String body = """
                {
                  "username": "%s",
                  "userEmail": "%s",
                  "password": "regular_password",
                  "userRoles": ["ROLE_USER"]
                }
                """.formatted(username, email);

        given()
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/auth/signup")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .body("isSuccess", equalTo(true))
                .body("message", equalTo("User registered successfully!"));
    }

    @Test
    public void signUpSuperUserShouldSucceed() {
        String username = generateName("andi");
        String email = username + "@gmail.com";

        String body = """
                {
                  "username": "%s",
                  "userEmail": "%s",
                  "password": "superuser_password",
                  "userRoles": ["ROLE_SUPERUSER"]
                }
                """.formatted(username, email);

        given()
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/auth/signup")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .body("isSuccess", equalTo(true))
                .body("message", equalTo("User registered successfully!"));
    }

    // ================= NEGATIVE SIGNUP TESTING =================

    @Test(dependsOnMethods = "signUpRegularUserShouldSucceed")
    public void signUpWithExistingUsernameShouldFail() {
        String body = """
                {
                  "username": "%s",
                  "userEmail": "new_%s",
                  "password": "regular_password",
                  "userRoles": ["ROLE_USER"]
                }
                """.formatted(existingUsername, existingEmail);

        given()
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/auth/signup")
                .then()
                .statusCode(400)
                .body("isSuccess", equalTo(false))
                .body("message", containsString("Username " + existingUsername + " is already exists"));
    }

    @Test(dependsOnMethods = "signUpRegularUserShouldSucceed")
    public void signUpWithExistingEmailShouldFail() {
        String body = """
                {
                  "username": "newuser_%s",
                  "userEmail": "%s",
                  "password": "regular_password",
                  "userRoles": ["ROLE_USER"]
                }
                """.formatted(existingUsername, existingEmail);

        given()
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/auth/signup")
                .then()
                .statusCode(400)
                .body("isSuccess", equalTo(false))
                .body("message", containsString("Email " + existingEmail + " is already exists"));
    }

    // ================= USER MANAGEMENT =================

    @Test(dependsOnMethods = "loginShouldReturnToken")
    public void getUserDetailsShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/users/details")
                .then()
                .statusCode(200)
                .body("username", equalTo("superuser01"))
                .body("email", equalTo("superuser01@gmail.com"));
    }

    @Test
    public void getUserDetailsWithoutTokenShouldFail() {
        given()
                .when()
                .get("/api/users/details")
                .then()
                .statusCode(401)
                .body("error", equalTo("Unauthorized request"))
                .body("message", containsString("Full authentication is required"));
    }

    @Test(dependsOnMethods = "loginShouldReturnToken")
    public void updateUserDetailsShouldSucceed() {
        String body = """
                {
                  "username": "superuser01",
                  "userEmail": "superuser01@gmail.com",
                  "userAddress": "Jl. Testing No. 123, Depok",
                  "userPhoneNumber": "081234567890",
                  "userBio": "Updated bio from automation test.",
                  "ownedBooksIds": [1, 2]
                }
                """;

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .put("/api/users/details")
                .then()
                .statusCode(200)
                .body("username", equalTo("superuser01"))
                .body("email", equalTo("superuser01@gmail.com"))
                .body("address", containsString("Jl. Testing"))
                .body("phoneNumber", equalTo("081234567890"))
                .body("userBio", equalTo("Updated bio from automation test."));
    }

    @Test
    public void updateUserDetailsWithInvalidTokenShouldFail() {
        String body = """
                {
                  "username": "superuser01",
                  "userEmail": "superuser01@gmail.com",
                  "userAddress": "Jl. Invalid, Jakarta",
                  "userPhoneNumber": "0800000000",
                  "userBio": "This should fail",
                  "ownedBooksIds": [1, 2]
                }
                """;

        given()
                .header("Authorization", "Bearer INVALID_TOKEN")
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .put("/api/users/details")
                .then()
                .statusCode(401)
                .body("error", equalTo("Unauthorized request"))
                .body("message", containsString("Full authentication is required"));
    }

    // ================= SUPERUSER ONLY =================

    @Test(dependsOnMethods = "loginShouldReturnToken")
    public void getAllUsersAsSuperuserShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/users/superuser/userList")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test(dependsOnMethods = "signUpRegularUserShouldSucceed")
    public void getAllUsersWithRegularUserTokenShouldFail() {
        String loginBody = """
                {
                  "userName": "%s",
                  "password": "regular_password"
                }
                """.formatted(existingUsername);

        String userToken = given()
                .header("Content-Type", "application/json")
                .body(loginBody)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("authToken");

        given()
                .header("Authorization", "Bearer " + userToken)
                .when()
                .get("/api/users/superuser/userList")
                .then()
                .statusCode(401)
                .body("error", equalTo("Unauthorized request"))
                .body("message", containsString("Full authentication is required to access this resource"));
    }

    @Test
    public void getAllUsersWithInvalidTokenShouldFail() {
        given()
                .header("Authorization", "Bearer INVALID_TOKEN")
                .when()
                .get("/api/users/superuser/userList")
                .then()
                .statusCode(401)
                .body("error", equalTo("Unauthorized request"))
                .body("message", containsString("Full authentication is required"));
    }

    // ================= BOOKSTORE =================

    @Test(dependsOnMethods = "loginShouldReturnToken")
    public void addNewBookstoreShouldSucceed() {
        int randomNum = random.nextInt(1000);
        String name = "Gramedia Depok " + randomNum;
        String address = "Jl. Raya Margonda No." + (10 + randomNum) + ", Depok";
        String phone = "08" + (1000000000 + random.nextInt(900000000));

        String body = """
                {
                  "bookstoreName": "%s",
                  "bookstoreAddress": "%s",
                  "bookstorePhoneNo": "%s",
                  "isActive": true
                }
                """.formatted(name, address, phone);

        Response response = given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/bookstores/superuser/createBookstore")
                .then()
                .extract().response();

        response.then()
                .statusCode(anyOf(is(200), is(201)))
                .body("bookstoreName", equalTo(name))
                .body("bookstoreAddress", equalTo(address))
                .body("bookstorePhoneNo", equalTo(phone));
    }

    @Test
    public void addNewBookstoreWithInvalidTokenShouldFail() {
        String body = """
                {
                  "bookstoreName": "Invalid Store",
                  "bookstoreAddress": "Jl. Invalid",
                  "bookstorePhoneNo": "0800000000",
                  "isActive": true
                }
                """;

        given()
                .header("Authorization", "Bearer INVALID_TOKEN")
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post("/api/bookstores/superuser/createBookstore")
                .then()
                .statusCode(401)
                .body("error", equalTo("Unauthorized request"))
                .body("message", containsString("Full authentication is required"));
    }

    @Test(dependsOnMethods = "addNewBookstoreShouldSucceed")
    public void updateBookstoreShouldSucceed() {

        int randomNum = random.nextInt(1000);
        String newName = "Gramedia Updated " + randomNum;
        String newAddress = "Jl. Margonda Baru No." + (10 + randomNum) + ", Depok";
        String newPhone = "08" + (1000000000 + random.nextInt(900000000));
        int bookstoreId = 1;

        String body = """
                {
                  "bookstoreName": "%s",
                  "bookstoreAddress": "%s",
                  "bookstorePhoneNo": "%s",
                  "isActive": true,
                  "bookStockRequest": [
                    {
                      "bookId": 1,
                      "bookstoreId": %d,
                      "bookStock": 150
                    }
                  ]
                }
                """.formatted(newName, newAddress, newPhone, bookstoreId);

        given()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .put("/api/bookstores/admin/bookstore/" + bookstoreId)
                .then()
                .statusCode(200)
                .body("bookstoreName", equalTo(newName))
                .body("bookstoreAddress", equalTo(newAddress));

    }

    @Test(dependsOnMethods = "addNewBookstoreShouldSucceed")
    public void updateBookstoreWithInvalidTokenShouldFail() {

        int bookstoreId = 1;
        String body = """
                {
                  "bookstoreName": "Invalid Store Update",
                  "bookstoreAddress": "Jl. Invalid Address",
                  "bookstorePhoneNo": "0800000000",
                  "isActive": true,
                  "bookStockRequest": [
                    {
                      "bookId": 1,
                      "bookstoreId": %d,
                      "bookStock": 50
                    }
                  ]
                }
                """.formatted(bookstoreId);

        given()
                .header("Authorization", "Bearer INVALID_TOKEN")
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .put("/api/bookstores/admin/bookstore/" + bookstoreId)
                .then()
                .statusCode(401)
                .body("error", equalTo("Unauthorized request"))
                .body("message", containsString("Full authentication is required"));
    }
    // ================= BOOKSTORE LIST =================

    @Test(dependsOnMethods = "loginShouldReturnToken")
    public void getBookstoresListShouldSucceed() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/bookstores/list")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    public void getBookstoresListWithInvalidTokenShouldFail() {

        given()
                .header("Authorization", "Bearer INVALID_TOKEN")
                .when()
                .get("/api/bookstores/list")
                .then()
                .statusCode(401)
                .body("error", equalTo("Unauthorized request"))
                .body("message", containsString("Full authentication is required"));
    }

}
