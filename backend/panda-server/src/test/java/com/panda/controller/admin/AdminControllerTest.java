package com.panda.controller.admin;

import com.panda.test.base.BaseTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

class AdminControllerTest extends BaseTest {

    private static final String ADMIN_EMAIL = "admin@panda.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final Long AREA_ID = 1L;
    private static final String DISPATCHER_NAME = "\u5218\u5bb8\u8c6a";
    private static final String OVERVIEW_START_DATE = "2026-04-19";
    private static final String OVERVIEW_END_DATE = "2026-04-20";

    private static String adminToken;
    private static final Set<String> TESTED_ENDPOINTS = new LinkedHashSet<>();
    private static final AtomicInteger TOTAL_CASES = new AtomicInteger();
    private static final AtomicInteger PASSED_CASES = new AtomicInteger();
    private static final AtomicInteger FAILED_CASES = new AtomicInteger();
    private static final List<CaseResult> CASE_RESULTS = new ArrayList<>();
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BOLD = "\u001B[1m";

    @AfterAll
    static void printSummary() {
        int total = TOTAL_CASES.get();
        int passed = PASSED_CASES.get();
        int failed = FAILED_CASES.get();
        String statusColor = failed == 0 ? GREEN : RED;
        String statusText = failed == 0 ? "PASS" : "FAIL";
        double passRate = total == 0 ? 0 : passed * 100.0 / total;

        System.out.println();
        System.out.println(CYAN + BOLD + "==================== ADMIN API TEST REPORT ====================" + RESET);
        System.out.printf("%sOverall Status : %s%s%s%n", BOLD, statusColor, statusText, RESET);
        System.out.printf("Tested APIs    : %s%d%s%n", CYAN, TESTED_ENDPOINTS.size(), RESET);
        System.out.printf("Total Cases    : %d%n", total);
        System.out.printf("Passed Cases   : %s%d%s%n", GREEN, passed, RESET);
        System.out.printf("Failed Cases   : %s%d%s%n", failed == 0 ? GREEN : RED, failed, RESET);
        System.out.printf("Pass Rate      : %s%.2f%%%s%n", failed == 0 ? GREEN : YELLOW, passRate, RESET);
        System.out.println();

        System.out.println(BOLD + "Covered APIs:" + RESET);
        int index = 1;
        for (String endpoint : TESTED_ENDPOINTS) {
            System.out.printf("  %d. %s%n", index++, endpoint);
        }
        System.out.println();

        System.out.println(BOLD + "Case Details:" + RESET);
        for (CaseResult result : CASE_RESULTS) {
            String color = result.passed ? GREEN : RED;
            String label = result.passed ? "PASS" : "FAIL";
            System.out.printf("  [%s%s%s] %-36s %-44s %s%n",
                    color, label, RESET, result.caseName, result.endpoint, result.message);
        }
        System.out.println(CYAN + BOLD + "===============================================================" + RESET);
    }

    @Test
    @DisplayName("admin api suite")
    void shouldRunAdminApiSuite() {
        runCase("admin-login-success", "POST /admin/log/login", this::assertAdminLoginSuccess);
        runCase("admin-login-wrong-password", "POST /admin/log/login", this::assertWrongAdminPassword);
        runCase("zone-detail", "GET /admin/zones/getZoneDetail", this::assertZoneDetail);
        runCase("zone-list", "GET /admin/zones/getZoneList", this::assertZoneList);
        runCase("dispatcher-list", "GET /admin/dispatchers/getDispatcherList", this::assertDispatcherList);
        runCase("package-list", "GET /admin/packages/getPackageList", this::assertPackageList);
        runCase("parking-point-list", "GET /admin/ParkingPoint/getPointList", this::assertParkingPointList);
        runCase("no-parking-zone-list", "GET /admin/noParkingZones/getZoneList", this::assertNoParkingZoneList);
        runCase("overview-day", "GET /admin/data/overview", this::assertOverviewDay);
        runCase("dispatcher-clear-and-restore-area", "POST /admin/dispatchers/editDispatcher",
                this::assertClearAndRestoreDispatcherArea);

        Assertions.assertEquals(0, FAILED_CASES.get(),
                "admin api suite has failed cases, see [ADMIN-TEST] output above");
    }

    private void assertAdminLoginSuccess() {
        Response response = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        assertBaseResponse("admin-login-success", response);
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("0", response.jsonPath().getString("code"));
        Assertions.assertNotNull(response.jsonPath().getString("data.token"));
    }

    private void assertWrongAdminPassword() {
        Response response = login(ADMIN_EMAIL, "wrong-password");
        assertBaseResponse("admin-login-wrong-password", response);
        Assertions.assertEquals(200, response.getStatusCode());
        Assertions.assertEquals("1", response.jsonPath().getString("code"));
    }

    private void assertZoneDetail() {
        Response response = given()
                .spec(authSpec())
                .queryParam("areaId", AREA_ID)
                .when()
                .get("/admin/zones/getZoneDetail");
        assertBaseResponse("zone-detail", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.id", org.hamcrest.Matchers.equalTo(AREA_ID.intValue()))
                .body("data.dispatchers", notNullValue())
                .body("data.vehicleCount", notNullValue());
    }

    private void assertZoneList() {
        Response response = given()
                .spec(authSpec())
                .queryParam("page", 1)
                .queryParam("pagesize", 10)
                .when()
                .get("/admin/zones/getZoneList");
        assertBaseResponse("zone-list", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.page", org.hamcrest.Matchers.equalTo(1))
                .body("data.pagesize", org.hamcrest.Matchers.equalTo(10))
                .body("data.total", greaterThanOrEqualTo(1))
                .body("data.areaList[0].dispatchers", notNullValue());
    }

    private void assertDispatcherList() {
        Response response = given()
                .spec(authSpec())
                .queryParam("page", 1)
                .queryParam("pagesize", 10)
                .queryParam("areaId", AREA_ID)
                .when()
                .get("/admin/dispatchers/getDispatcherList");
        assertBaseResponse("dispatcher-list", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.pagesize", org.hamcrest.Matchers.equalTo(10))
                .body("data.total", greaterThanOrEqualTo(1))
                .body("data.dispatcherList.find { it.name == '" + DISPATCHER_NAME + "' }.areaId",
                        org.hamcrest.Matchers.equalTo(AREA_ID.intValue()));
    }

    private void assertOverviewDay() {
        Response response = given()
                .spec(authSpec())
                .queryParam("startDate", OVERVIEW_START_DATE)
                .queryParam("endDate", OVERVIEW_END_DATE)
                .queryParam("granularity", "day")
                .queryParam("areaId", AREA_ID)
                .when()
                .get("/admin/data/overview");
        assertBaseResponse("overview-day", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.startDate", org.hamcrest.Matchers.equalTo(OVERVIEW_START_DATE))
                .body("data.endDate", org.hamcrest.Matchers.equalTo(OVERVIEW_END_DATE))
                .body("data.granularity", org.hamcrest.Matchers.equalTo("day"))
                .body("data.series.size()", org.hamcrest.Matchers.equalTo(2))
                .body("data.series[0].orderCount", notNullValue())
                .body("data.series[0].revenue", notNullValue());
    }

    private void assertPackageList() {
        Response response = given()
                .spec(authSpec())
                .queryParam("page", 1)
                .queryParam("pagesize", 10)
                .when()
                .get("/admin/packages/getPackageList");
        assertBaseResponse("package-list", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.page", org.hamcrest.Matchers.equalTo(1))
                .body("data.pagesize", org.hamcrest.Matchers.equalTo(10))
                .body("data.total", greaterThanOrEqualTo(3))
                .body("data.list[0].title", notNullValue())
                .body("data.list[0].price", notNullValue());
    }

    private void assertParkingPointList() {
        Response response = given()
                .spec(authSpec())
                .queryParam("page", 1)
                .queryParam("pagesize", 10)
                .when()
                .get("/admin/ParkingPoint/getPointList");
        assertBaseResponse("parking-point-list", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.page", org.hamcrest.Matchers.equalTo(1))
                .body("data.pagesize", org.hamcrest.Matchers.equalTo(10))
                .body("data.total", greaterThanOrEqualTo(7))
                .body("data.areaList[0].longitude", notNullValue());
    }

    private void assertNoParkingZoneList() {
        Response response = given()
                .spec(authSpec())
                .queryParam("page", 1)
                .queryParam("pagesize", 10)
                .when()
                .get("/admin/noParkingZones/getZoneList");
        assertBaseResponse("no-parking-zone-list", response);

        response.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.page", org.hamcrest.Matchers.equalTo(1))
                .body("data.pagesize", org.hamcrest.Matchers.equalTo(10))
                .body("data.total", greaterThanOrEqualTo(3))
                .body("data.areaList[0].polygon", notNullValue());
    }

    private void assertClearAndRestoreDispatcherArea() {
        DispatcherFixture dispatcher = resolveDispatcherFixture();
        Map<String, Object> clearBody = new HashMap<>();
        clearBody.put("id", dispatcher.id);
        clearBody.put("areaId", null);

        Response clearResponse = given()
                .spec(authSpec())
                .body(clearBody)
                .when()
                .post("/admin/dispatchers/editDispatcher");
        assertBaseResponse("dispatcher-clear-area", clearResponse);

        clearResponse.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.dispatcher.areaId", org.hamcrest.Matchers.nullValue());

        Map<String, Object> restoreBody = new HashMap<>();
        restoreBody.put("id", dispatcher.id);
        restoreBody.put("name", dispatcher.name);
        restoreBody.put("email", dispatcher.email);
        restoreBody.put("areaId", AREA_ID);

        Response restoreResponse = given()
                .spec(authSpec())
                .body(restoreBody)
                .when()
                .post("/admin/dispatchers/editDispatcher");
        assertBaseResponse("dispatcher-restore-area", restoreResponse);

        restoreResponse.then()
                .statusCode(200)
                .body("code", org.hamcrest.Matchers.equalTo(0))
                .body("data.dispatcher.areaId", org.hamcrest.Matchers.equalTo(AREA_ID.intValue()));
    }

    private DispatcherFixture resolveDispatcherFixture() {
        Response response = given()
                .spec(authSpec())
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
        Long id = Long.valueOf(dispatcher.get("id").toString());
        String name = dispatcher.get("name").toString();
        String email = dispatcher.get("email").toString();
        System.out.printf("[ADMIN-TEST] fixture dispatcher id=%d, name=%s, email=%s%n", id, name, email);
        return new DispatcherFixture(id, name, email);
    }

    private io.restassured.specification.RequestSpecification authSpec() {
        return given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + getAdminToken());
    }

    private String getAdminToken() {
        if (adminToken == null) {
            Response response = login(ADMIN_EMAIL, ADMIN_PASSWORD);
            Assertions.assertEquals("0", response.jsonPath().getString("code"), "admin login failed");
            adminToken = response.jsonPath().getString("data.token");
        }
        return adminToken;
    }

    private Response login(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/admin/log/login");
    }

    private void runCase(String caseName, String endpoint, CaseAssertion assertion) {
        TOTAL_CASES.incrementAndGet();
        TESTED_ENDPOINTS.add(endpoint);
        System.out.printf("%n[ADMIN-TEST] START case=%s, endpoint=%s%n", caseName, endpoint);
        try {
            assertion.run();
            PASSED_CASES.incrementAndGet();
            CASE_RESULTS.add(new CaseResult(caseName, endpoint, true, "ok"));
            System.out.printf("[ADMIN-TEST] PASS case=%s%n", caseName);
        } catch (Throwable e) {
            FAILED_CASES.incrementAndGet();
            String reason = rootMessage(e);
            CASE_RESULTS.add(new CaseResult(caseName, endpoint, false, reason));
            System.out.printf("[ADMIN-TEST] FAIL case=%s, reason=%s%n", caseName, reason);
        }
    }

    private void assertBaseResponse(String caseName, Response response) {
        System.out.printf("[ADMIN-TEST] RESULT case=%s, httpStatus=%d, code=%s, msg=%s%n",
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

    private static class CaseResult {
        private final String caseName;
        private final String endpoint;
        private final boolean passed;
        private final String message;

        private CaseResult(String caseName, String endpoint, boolean passed, String message) {
            this.caseName = caseName;
            this.endpoint = endpoint;
            this.passed = passed;
            this.message = message;
        }
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
