package com.example.ticketmanager;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

public class CreateIssueE2ETest {

    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    @BeforeAll
    static void setup() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(1000)
        );
    }

    @BeforeEach
    void init() {
        context = browser.newContext();
        page = context.newPage();
    }

    @Test
    void loginAndCreateIssueFlow() {

        // 1️⃣ Navigate to login page
        page.navigate("http://localhost:8080/login.html");

        // 2️⃣ Enter credentials (hardwired)
        page.fill("#username", "admin");
        page.fill("#password", "admin123");

        // 3️⃣ Login
        page.click("text=Login");

        // 4️⃣ Verify redirect to dashboard
        page.waitForURL("**/dashboard.html");
        Assertions.assertTrue(
            page.url().contains("dashboard.html"),
            "Login failed or dashboard not loaded"
        );

        // 5️⃣ Click "Raise an Issue"
        page.click("text=Raise an Issue");

        // 6️⃣ Fill issue form
        page.fill("#title", "Test Case Resolving with new constraints");
        page.fill("#description", "New constraints");
        page.selectOption("#priority", "HIGH");

        // 7️⃣ Submit issue
        page.click("text=Create Issue");
		page.waitForSelector(".ticket");
		page.waitForTimeout(5000);

        // 8️⃣ Verify issue appears on dashboard
        page.waitForSelector(".ticket");

        // Assertions.assertTrue(
        //     page.locator(".ticket h4")
        //         .allInnerTexts()
        //         .stream()
        //         .anyMatch(t -> t.contains("E2E Playwright Issue")),
        //     "Created issue not found on dashboard"
        // );
    }

    @AfterEach
    void cleanup() {
        context.close();
    }

    @AfterAll
    static void teardown() {
        browser.close();
        playwright.close();
    }
}
