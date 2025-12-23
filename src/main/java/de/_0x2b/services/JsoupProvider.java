package de._0x2b.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;

public class JsoupProvider{
    public Connection.Response execute(String url) throws IOException {
        return Jsoup.connect(url).followRedirects(true).ignoreContentType(true).execute();
    }
}
