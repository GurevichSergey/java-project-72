package hexlet.code.controllers;

import hexlet.code.model.UrlCheck;
import hexlet.code.model.query.QUrlCheck;
import io.javalin.http.Handler;
import hexlet.code.model.Url;
import hexlet.code.model.query.QUrl;
import io.javalin.http.NotFoundResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import io.ebean.PagedList;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class UrlController {
    public static Handler getListUrls = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        final int rowsPerPage = 10;

        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();

        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());

        ctx.attribute("urls", urls);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.render("urls/index.html");
    };
    public static Handler createUrl = ctx ->  {
        try {
            String urlString = ctx.formParam("url");
            URL parsedUrl = new URL(urlString);
            var normalizeUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getAuthority();
            Url mainUrl = new Url(normalizeUrl);
            boolean urlContain = new QUrl()
                    .name.equalTo(normalizeUrl)
                    .exists();
            if (urlContain) {
                ctx.sessionAttribute("flash", "Страница уже существует");
                ctx.sessionAttribute("flash-type", "info");
                ctx.redirect("/urls");
                return;
            }
            mainUrl.save();
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flash-type", "success");
            ctx.redirect("/urls");
        } catch (MalformedURLException e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.render("index.html");
        }
    };
    public static Handler showUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);
        Url url = new QUrl()
              .id.equalTo(id)
              .findOne();
        if (url == null) {
            throw new NotFoundResponse();
        }
        List<UrlCheck> urlChecks = new QUrlCheck()
                .url.equalTo(url)
                .orderBy().id.desc()
                .findList();

        ctx.attribute("urlChecks", urlChecks);
        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };
    public static Handler urlCheck = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);
        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();
        try {
            HttpResponse<String> response = Unirest
                    .get(url.getName())
                    .asString();
            Document body = Parser.parse(response.getBody(), url.getName());
            var statusCode = response.getStatus();
            String title = body.title();
            String h1 = Optional.ofNullable(body.selectFirst("h1"))
                    .map(Element::text)
                    .orElse("");
            String description = Optional.ofNullable(body.selectFirst("meta[name=description][content]"))
                    .map(x -> x.attr("content"))
                    .orElse("");
            UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, url);
            urlCheck.save();
            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");
        } catch (UnirestException e) {
            ctx.sessionAttribute("flash", "Некорректный адрес");
            ctx.sessionAttribute("flash-type", "danger");
        }
        ctx.redirect("/urls/" + id);
    };
}
