package hexlet.code.controllers;

import io.javalin.http.Handler;
import hexlet.code.model.Url;
import hexlet.code.model.query.QUrl;
import io.javalin.http.NotFoundResponse;
import org.apache.commons.validator.routines.UrlValidator;
import io.ebean.PagedList;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class UrlController {
    public static Handler getListUrls = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;

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
        String name = ctx.formParam("url");
        UrlValidator urlValidator = new UrlValidator();
        if (!urlValidator.isValid(name)) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.render("index.html");
            return;
        }
        URL url = new URL(name);
        Url mainUrl = new Url(url.getProtocol() + "://" + url.getAuthority());
        List<Url> urls = new QUrl()
                .orderBy()
                .id.asc()
                .findList();
        if (urls.contains(mainUrl)) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/urls");
            return;
        }
        mainUrl.save();
        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flash-type", "success");
        ctx.redirect("/urls");
    };
    public static Handler showUrl = ctx -> {
      long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);
      Url url = new QUrl()
              .id.equalTo(id)
              .findOne();
        if (url == null) {
            throw new NotFoundResponse();
        }
      ctx.attribute("url", url);
      ctx.render("urls/show.html");
    };
}
