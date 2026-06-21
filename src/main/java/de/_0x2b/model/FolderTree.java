package de._0x2b.model;

import java.util.List;
import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class FolderTree {
    private List<Folder> folders;
    private List<Feed> unfiledFeeds;

    public FolderTree() {
    }

    public FolderTree(List<Folder> folders, List<Feed> unfiledFeeds) {
        this.folders = folders;
        this.unfiledFeeds = unfiledFeeds;
    }

    public List<Folder> getFolders() {
        return folders;
    }

    public void setFolders(List<Folder> folders) {
        this.folders = folders;
    }

    public List<Feed> getUnfiledFeeds() {
        return unfiledFeeds;
    }

    public void setUnfiledFeeds(List<Feed> unfiledFeeds) {
        this.unfiledFeeds = unfiledFeeds;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        FolderTree that = (FolderTree) o;
        return Objects.equals(folders, that.folders) && Objects.equals(unfiledFeeds, that.unfiledFeeds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(folders, unfiledFeeds);
    }
}