package de._0x2b.model;

import java.util.Arrays;
import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;

/** Persisted favicon row (no fetch URL — pass URI into IconService separately). */
@RegisterForReflection
public record Icon(int id, int feedId, byte[] image, String mimeType, String fileName) {

    public Icon withFeedId(int feedId) {
        return new Icon(id, feedId, image, mimeType, fileName);
    }

    public Icon withImage(byte[] image) {
        return new Icon(id, feedId, image, mimeType, fileName);
    }

    public Icon withMimeType(String mimeType) {
        return new Icon(id, feedId, image, mimeType, fileName);
    }

    public int getId() {
        return id;
    }

    public int getFeedId() {
        return feedId;
    }

    public byte[] getImage() {
        return image;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Icon icon))
            return false;
        return id == icon.id && feedId == icon.feedId && Objects.deepEquals(image, icon.image)
                && Objects.equals(mimeType, icon.mimeType) && Objects.equals(fileName, icon.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, feedId, Arrays.hashCode(image), mimeType, fileName);
    }
}
