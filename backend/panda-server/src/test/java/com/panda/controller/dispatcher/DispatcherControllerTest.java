package com.panda.controller.dispatcher;

import com.panda.test.base.BaseTest;
import com.panda.utils.JwtUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

class DispatcherControllerTest extends BaseTest {

    private static final String JWT_SECRET = "12345678901234567890123456789012";
    private static final long JWT_TTL = 604800000L;
    private static final String ADMIN_EMAIL = "admin@panda.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final Long AREA_ID = 1L;
    private static final String DISPATCHER_NAME = "\u5218\u5bb8\u8c6a";
    private static final String SCOOTER_CODE = "PDSC000001";
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BOLD = "\u001B[1m";

    private static String adminToken;
    private static DispatcherFixture dispatcherFixture;
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
        System.out.println(CYAN + BOLD + "================= DISPATCHER API TEST REPORT =================" + RESET);
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
    @DisplayName("dispatcher api suite")
    void shouldRunDispatcherApiSuite() {
        runCase("dispatcher-info", "GET /dispatcher/user/info", this::assertDispatcherInfo);
        runCase("dispatcher-map", "GET /dispatcher/map", this::assertDispatcherMap);
        runCase("dispatcher-history", "GET /dispatcher/user/dispatch-history", this::assertDispatchHistory);
        runCase("dispatcher-scooter-info", "GET /dispatcher/scooter/info", this::assertScooterInfo);
        runCase("dispatcher-without-token", "GET /dispatcher/scooter/info", this::assertScooterRequiresToken);

        Assertions.assertEquals(0, FAILED_CASES.get(),
                "dispatcher api suite has failed cases, see [DISPATCHER-TEST] output above");
    }

    private void assertDispatcherInfo() {
        Response response = given()
                .spec(authSpec())
                .when()
                .get("/dispatcher/user/info");
        assertBaseResponse("dispatcher-info", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.name", org.hamcrest.Matchers.equalTo(dispatcher().name))
                .body("data.email", org.hamcrest.Matchers.equalTo(dispatcher().email))
                .body("data.todayDispatchedNum", notNullValue());
    }

    private void assertDispatcherMap() {
        Response response = given()
                .spec(authSpec())
                .queryParam("longitude", "103.989977")
                .queryParam("latitude", "30.762536")
                .queryParam("scale", 16)
                .when()
                .get("/dispatcher/map");
        assertBaseResponse("dispatcher-map", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.scooters", notNullValue())
                .body("data.noParkingAreas", notNullValue())
                .body("data.parkingPoints", notNullValue());
    }

    private void assertDispatchHistory() {
        Response response = given()
                .spec(authSpec())
                .when()
                .get("/dispatcher/user/dispatch-history");
        assertBaseResponse("dispatcher-history", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.history", notNullValue());
    }

    private void assertScooterInfo() {
        Response response = given()
                .spec(authSpec())
                .queryParam("code", SCOOTER_CODE)
                .when()
                .get("/dispatcher/scooter/info");
        assertBaseResponse("dispatcher-scooter-info", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.code", org.hamcrest.Matchers.equalTo(SCOOTER_CODE))
                .body("data.latitude", notNullValue())
                .body("data.longitude", notNullValue());
    }

    private void assertScooterRequiresToken() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("code", SCOOTER_CODE)
                .when()
                .get("/dispatcher/scooter/info");
        assertBaseResponse("dispatcher-without-token", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(1));
    }

    private io.restassured.specification.RequestSpecification authSpec() {
        return given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + dispatcherToken());
    }

    private String dispatcherToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("dispatcherId", dispatcher().id);
        return JwtUtil.createJWT(JWT_SECRET, JWT_TTL, claims);
    }

    private DispatcherFixture dispatcher() {
        if (dispatcherFixture == null) {
            Response response = given()
                    .spec(requestSpec)
                    .header("Authorization", "Bearer " + adminToken())
                    .queryParam("page", 1)
                    .queryParam("pagesize", 10)
                    .queryParam("areaId", AREA_ID)
                    .when()
                    .get("/admin/dispatchers/getDispatcherList");
            Assertions.assertEquals("0", response.jsonPath().getString("code"), "dispatcher list failed");
            List<Map<String, Object>> dispatchers = response.jsonPath().getList("data.dispatcherList");
            Assertions.assertFalse(dispatchers == null || dispatchers.isEmpty(), "no dispatcher bound to area " + AREA_ID);
            Map<String, Object> dispatcher = dispatchers.stream()
                    .filter(item -> DISPATCHER_NAME.equals(item.get("name")))
                    .findFirst()
                    .orElse(dispatchers.get(0));
            dispatcherFixture = new DispatcherFixture(
                    Long.valueOf(dispatcher.get("id").toString()),
                    dispatcher.get("name").toString(),
                    dispatcher.get("email").toString()
            );
            System.out.printf("[DISPATCHER-TEST] fixture dispatcher id=%d, name=%s, email=%s%n",
                    dispatcherFixture.id, dispatcherFixture.name, dispatcherFixture.email);
        }
        return dispatcherFixture;
    }

    private String adminToken() {
        if (adminToken == null) {
            Map<String, Object> body = new HashMap<>();
            body.put("email", ADMIN_EMAIL);
            body.put("password", ADMIN_PASSWORD);
            Response response = given()
                    .spec(requestSpec)
                    .body(body)
                    .when()
                    .post("/admin/log/login");
            Assertions.assertEquals("0", response.jsonPath().getString("code"), "admin login failed");
            adminToken = response.jsonPath().getString("data.token");
        }
        return adminToken;
    }

    private void runCase(String caseName, String endpoint, CaseAssertion assertion) {
        TOTAL_CASES.incrementAndGet();
        TESTED_ENDPOINTS.add(endpoint);
        System.out.printf("%n[DISPATCHER-TEST] START case=%s, endpoint=%s%n", caseName, endpoint);
        try {
            assertion.run();
            PASSED_CASES.incrementAndGet();
            System.out.printf("[DISPATCHER-TEST] PASS case=%s%n", caseName);
        } catch (Throwable e) {
            FAILED_CASES.incrementAndGet();
            System.out.printf("[DISPATCHER-TEST] FAIL case=%s, reason=%s%n", caseName, rootMessage(e));
        }
    }

    private void assertBaseResponse(String caseName, Response response) {
        System.out.printf("[DISPATCHER-TEST] RESULT case=%s, httpStatus=%d, code=%s, msg=%s%n",
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

    private static class DispatcherFixture {
        private final Long id;
        private final String name;
        private final String email;

        private DispatcherFixture(Long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }
}
