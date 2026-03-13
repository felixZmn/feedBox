package de._0x2b.service;

import com.apptasticsoftware.rssreader.module.mediarss.MediaRssItem;

import java.io.InputStream;
import java.util.List;

public interface MediaRssParser {
    List<MediaRssItem> parse(InputStream inputStream);
}