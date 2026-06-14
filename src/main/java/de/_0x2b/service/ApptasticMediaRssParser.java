package de._0x2b.service;

import com.apptasticsoftware.rssreader.module.mediarss.MediaRssItem;
import com.apptasticsoftware.rssreader.module.mediarss.MediaRssReader;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.InputStream;
import java.util.List;

@ApplicationScoped
public class ApptasticMediaRssParser implements MediaRssParser {

    @Override
    public List<MediaRssItem> parse(InputStream inputStream) {
        // MediaRssReader is NOT thread-safe
        return new MediaRssReader().read(inputStream).toList();
    }
}