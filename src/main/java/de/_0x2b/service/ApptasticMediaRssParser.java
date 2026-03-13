package de._0x2b.service;

import com.apptasticsoftware.rssreader.module.mediarss.MediaRssItem;
import com.apptasticsoftware.rssreader.module.mediarss.MediaRssReader;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.InputStream;
import java.util.List;

@ApplicationScoped
public class ApptasticMediaRssParser implements MediaRssParser {
    private final MediaRssReader reader = new MediaRssReader();

    @Override
    public List<MediaRssItem> parse(InputStream inputStream) {
        return reader.read(inputStream).toList();
    }
}