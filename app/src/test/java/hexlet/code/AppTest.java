package hexlet.code;

import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AppTest {
    private static Javalin app;
    private static String baseUrl;
    private static final int SUCCESS_STATUS_RESPONSE = 200;

    // BEGIN
    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start();
        int port = app.port();
        baseUrl = "http://localhost:" + port;
    }
    @AfterAll
    public static void afterAll() {
        app.stop();
    }
    @Test void testConnection() {
        HttpResponse<String> response = Unirest
                .get(baseUrl + "/")
                .asString();
        assertThat(response.getStatus()).isEqualTo(SUCCESS_STATUS_RESPONSE);
    }
}
