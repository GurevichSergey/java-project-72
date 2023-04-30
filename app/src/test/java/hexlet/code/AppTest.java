package hexlet.code;

import hexlet.code.model.Url;

import hexlet.code.model.UrlCheck;
import hexlet.code.model.query.QUrl;
import hexlet.code.model.query.QUrlCheck;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import io.javalin.Javalin;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.MockResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class AppTest {
    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }
    private static Javalin app;
    private static String baseUrl;
    private static Url existingUrl;
    private static Database database;
    private static Transaction transaction;
    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();
        existingUrl = new Url("https://github.com");
        existingUrl.save();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    final void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    final void afterEach() {
        transaction.rollback();
    }
    @Test
    void testMigrationGenerator() {
    }

    @Nested
    class RootTest {
        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest.get(baseUrl).asString();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains("Анализатор страниц");
        }
    }

    @Nested
    class UrlTest {
        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(existingUrl.getName());
        }

        @Test
        void testCreateExistingUrl() {
            HttpResponse<String> responsePost1 = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", existingUrl.getName())
                    .asEmpty();

            HttpResponse<String> response = Unirest
                    .get(baseUrl)
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        void testShow() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/1")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(existingUrl.getName());
        }

        @Test
        void testIncorrectShowId() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/100")
                    .asString();
            assertThat(response.getStatus()).isEqualTo(404);
        }

        @Test
        void testIndexUrls() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();
            assertThat(body).contains(existingUrl.getName());
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        void testCreateInvalidUrl() {
            String inputUrl = "Qwerty";
            HttpResponse responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputUrl)
                    .asEmpty();

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).doesNotContain(inputUrl);

            Url actualUrl = new QUrl()
                    .name.equalTo(inputUrl)
                    .findOne();

            assertThat(actualUrl).isNull();
        }

        @Test
        void testCreate() {
            String inputUrl = "https://leetcode.com";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputUrl)
                    .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(inputUrl);
            assertThat(body).contains(existingUrl.getName());
            assertThat(body).contains("Страница успешно добавлена");

            Url actuallUrl = new QUrl()
                    .name.equalTo(inputUrl)
                    .findOne();

            assertThat(actuallUrl).isNotNull();
            assertThat(actuallUrl.getName()).isEqualTo(inputUrl);
        }

        @Test
        void testUrlCheck() throws IOException {
            String samplePage = Files.readString(Paths.get("src/test/resources/html/testIndex.html"));

            MockWebServer mockServer = new MockWebServer();
            mockServer.enqueue(new MockResponse().setBody(samplePage));
            mockServer.start();
            String url = mockServer.url("/").toString();
            HttpResponse<String> response = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", url)
                    .asEmpty();

            assertThat(response.getStatus()).isEqualTo(302);

            Url postedUrl = new QUrl()
                    .name.equalTo(url.substring(0, url.length() - 1))
                    .findOne();

            assertThat(postedUrl.toString()).isExactlyInstanceOf(String.class);

            var id = postedUrl.getId();

            HttpResponse<String> postResponse = Unirest
                        .post(baseUrl + "/urls/" + id + "/checks")
                        .asString();

            List<UrlCheck> urlChecks = new QUrl()
                    .id.equalTo(id)
                    .findOne()
                    .getUrlList();

            UrlCheck urlCheck = new QUrlCheck()
                    .url.name.equalTo(url.substring(0, url.length() - 1))
                    .findOne();

            assertThat(urlCheck.toString()).isExactlyInstanceOf(String.class);
            assertThat(urlChecks).isNotNull();
            assertThat(postResponse.getStatus()).isEqualTo(302);
            assertThat(postResponse.getHeaders().getFirst("Location")).isEqualTo("/urls/" + id);

            String body = Unirest
                    .get(baseUrl + "/urls/" + id)
                    .asString()
                    .getBody();

            assertThat(body).contains("Example title");
            assertThat(body).contains("Test page");

            mockServer.shutdown();
        }
    }
}
