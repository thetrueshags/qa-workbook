package com.staysync.tests;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Base class for Selenium browser tests.
 *
 * Handles WebDriver lifecycle so individual test classes can focus on
 * assertions. Extend this class whenever you need a real browser session.
 *
 * For pure API tests (no browser), you do NOT need to extend this class —
 * see BookingApiTest and RoomApiTest for examples that use REST Assured
 * directly.
 */
public abstract class BaseTest {

    /** Root URL of the running StaySync application. */
    protected static final String BASE_URL = "http://localhost:8080";

    /** WebDriver instance — fresh for every test method. */
    protected WebDriver driver;

    /**
     * One-time setup: let WebDriverManager download / configure the
     * correct ChromeDriver binary for the installed Chrome version.
     */
    @BeforeAll
    static void setupDriver() {
        WebDriverManager.chromedriver().setup();
    }

    /**
     * Before each test: create a new Chrome session so tests are fully
     * isolated from each other (no shared cookies, storage, etc.).
     */
    @BeforeEach
    void createDriver() {
        ChromeOptions options = new ChromeOptions();
        // Run headless in CI environments; remove this line to watch the
        // browser during local development.
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
    }

    /**
     * After each test: close the browser and free resources.
     */
    @AfterEach
    void quitDriver() {
        if (driver != null) {
            driver.quit();
        }
    }
}
