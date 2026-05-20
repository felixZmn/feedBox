package de._0x2b.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import org.eclipse.microprofile.config.ConfigProvider;
import java.io.IOException;

/**
 * Jsoup provider to allow mocking in tests
 */
public class JsoupProvider {
    public Connection.Response execute(String url) throws IOException {
        var config = ConfigProvider.getConfig();
        var userAgent = config.getValue("app.http.user-agent", String.class);

        return Jsoup.connect(url)
                .followRedirects(true)
                .ignoreContentType(true)
                .userAgent(userAgent)
                .execute();
    }
}
