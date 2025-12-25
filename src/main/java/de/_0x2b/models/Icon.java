package de._0x2b.models;

import java.util.Arrays;
import java.util.Objects;

public class Icon {
    int id;
    int feedId;
    byte[] image;
    String mimeType;
    String fileName;
    String url;

    public Icon(int id, int feedId, byte[] image, String mimeType, String fileName, String url) {
        this.id = id;
        this.feedId = feedId;
        this.image = image;
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.url = url;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFeedId() {
        return feedId;
    }

    public void setFeedId(int feedId) {
        this.feedId = feedId;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Icon icon = (Icon) o;
        return id == icon.id && feedId == icon.feedId && Objects.deepEquals(image, icon.image)
                && Objects.equals(mimeType, icon.mimeType) && Objects.equals(fileName, icon.fileName)
                && url.equals(icon.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, feedId, Arrays.hashCode(image), mimeType, fileName, url);
    }
}
