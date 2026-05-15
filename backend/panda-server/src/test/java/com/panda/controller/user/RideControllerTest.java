package com.panda.controller.user;

import com.panda.test.base.BaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

class RideControllerTest extends BaseTest {

    private static final String USER_EMAIL = "test@example.com";
    private static final String USER_PASSWORD = "123456";
    private static final String SCOOTER_CODE = "PDSC000001";
    private static String userToken;
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BOLD = "\u001B[1m";

    private static final Set<String> TESTED_ENDPOINTS = new LinkedHashSet<>();
    private static final AtomicInteger TOTAL_CASES = new AtomicInteger();
    private static final AtomicInteger PASSED_CASES = new AtomicInteger();
    private static final AtomicInteger FAILED_CASES = new AtomicInteger();

    @AfterAll
    static void printSummary() {
        int total = TOTAL_CASES.get();
        int passed = PASSED_CASES.get();
        int failed = FAILED_CASES.get();
        double passRate = total == 0 ? 0 : passed * 100.0 / total;
        String statusColor = failed == 0 ? GREEN : RED;
        String statusText = failed == 0 ? "PASS" : "FAIL";

        System.out.println();
        System.out.println(CYAN + BOLD + "==================== RIDE API TEST REPORT =====================" + RESET);
        System.out.printf("%sOverall Status : %s%s%s%n", BOLD, statusColor, statusText, RESET);
        System.out.printf("Tested APIs    : %s%d%s%n", CYAN, TESTED_ENDPOINTS.size(), RESET);
        System.out.printf("Total Cases    : %d%n", total);
        System.out.printf("Passed Cases   : %s%d%s%n", GREEN, passed, RESET);
        System.out.printf("Failed Cases   : %s%d%s%n", failed == 0 ? GREEN : RED, failed, RESET);
        System.out.printf("Pass Rate      : %s%.2f%%%s%n", failed == 0 ? GREEN : YELLOW, passRate, RESET);
        System.out.println(BOLD + "Covered APIs:" + RESET);
        int index = 1;
        for (String endpoint : TESTED_ENDPOINTS) {
            System.out.printf("  %d. %s%n", index++, endpoint);
        }
        System.out.println(CYAN + BOLD + "===============================================================" + RESET);
    }

    @Test
    @DisplayName("ride api suite")
    void shouldRunRideApiSuite() {
        runCase("scooter-detail", "GET /user/scooter", this::assertScooterDetail);
        runCase("map-data", "GET /user/map", this::assertMapData);
        runCase("ride-history", "GET /user/ride-history", this::assertRideHistory);
        runCase("user-bills", "GET /user/user/bills", this::assertUserBills);
        runCase("user-faults", "GET /user/faults", this::assertUserFaults);
        runCase("user-subscription", "GET /user/subscription", this::assertUserSubscription);
        runCase("scooter-without-token", "GET /user/scooter", this::assertScooterRequiresToken);

        Assertions.assertEquals(0, FAILED_CASES.get(),
                "ride api suite has failed cases, see [RIDE-TEST] output above");
    }

    private void assertScooterDetail() {
        Response response = given()
                .spec(authSpec())
                .queryParam("code", SCOOTER_CODE)
                .when()
                .get("/user/scooter");
        assertBaseResponse("scooter-detail", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.code", org.hamcrest.Matchers.equalTo(SCOOTER_CODE))
                .body("data.latitude", notNullValue())
                .body("data.longitude", notNullValue());
    }

    private void assertMapData() {
        Response response = given()
                .spec(authSpec())
                .queryParam("longitude", "103.989977")
                .queryParam("latitude", "30.762536")
                .queryParam("scale", 16)
                .when()
                .get("/user/map");
        assertBaseResponse("map-data", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.scooters", notNullValue())
                .body("data.noParkingAreas", notNullValue())
                .body("data.parkingPoints", notNullValue());
    }

    private void assertRideHistory() {
        Response response = given()
                .spec(authSpec())
                .when()
                .get("/user/ride-history");
        assertBaseResponse("ride-history", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.history", notNullValue());
    }

    private void assertUserBills() {
        Response response = given()
                .spec(authSpec())
                .when()
                .get("/user/user/bills");
        assertBaseResponse("user-bills", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.bills", notNullValue());
    }

    private void assertUserFaults() {
        Response response = given()
                .spec(authSpec())
                .when()
                .get("/user/faults");
        assertBaseResponse("user-faults", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.faults", notNullValue());
    }

    private void assertUserSubscription() {
        Response response = given()
                .spec(authSpec())
                .when()
                .get("/user/subscription");
        assertBaseResponse("user-subscription", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.packages", notNullValue());
    }

    private void assertScooterRequiresToken() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("code", SCOOTER_CODE)
                .when()
                .get("/user/scooter");
        assertBaseResponse("scooter-without-token", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(1));
    }

    private io.restassured.specification.RequestSpecification authSpec() {
        return given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + userToken());
    }

    private String userToken() {
        if (userToken == null) {
            Map<String, Object> body = new HashMap<>();
            body.put("email", USER_EMAIL);
            body.put("password", USER_PASSWORD);
            Response response = given()
                    .spec(requestSpec)
                    .body(body)
                    .when()
                    .post("/user/user/login");
            Assertions.assertEquals("0", response.jsonPath().getString("code"), "user login failed");
            userToken = response.jsonPath().getString("data.token");
        }
        return userToken;
    }

    private void runCase(String caseName, String endpoint, CaseAssertion assertion) {
        TOTAL_CASES.incrementAndGet();
        TESTED_ENDPOINTS.add(endpoint);
        System.out.printf("%n[RIDE-TEST] START case=%s, endpoint=%s%n", caseName, endpoint);
        try {
            assertion.run();
            PASSED_CASES.incrementAndGet();
            System.out.printf("[RIDE-TEST] PASS case=%s%n", caseName);
        } catch (Throwable e) {
            FAILED_CASES.incrementAndGet();
            System.out.printf("[RIDE-TEST] FAIL case=%s, reason=%s%n", caseName, rootMessage(e));
        }
    }

    private void assertBaseResponse(String caseName, Response response) {
        System.out.printf("[RIDE-TEST] RESULT case=%s, httpStatus=%d, code=%s, msg=%s%n",
                caseName,
                response.getStatusCode(),
                response.jsonPath().getString("code"),
                response.jsonPath().getString("msg"));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return current.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }

    @FunctionalInterface
    private interface CaseAssertion {
        void run() throws Exception;
    }
}
