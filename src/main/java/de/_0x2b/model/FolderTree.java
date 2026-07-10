package de._0x2b.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public static FolderTree from(List<Folder> folders, List<Feed> feeds) {
        Map<Integer, Folder> byId = new LinkedHashMap<>();
        for (Folder folder : folders) {
            Folder copy = new Folder(folder.getId(), folder.getName(), new ArrayList<>(), folder.getColor());
            byId.put(copy.getId(), copy);
        }
        List<Feed> unfiled = new ArrayList<>();
        for (Feed feed : feeds) {
            if (feed.getFolderId() == null) {
                unfiled.add(feed);
            } else {
                Folder parent = byId.get(feed.getFolderId());
                if (parent != null) {
                    parent.getFeeds().add(feed);
                } else {
                    unfiled.add(feed);
                }
            }
        }
        return new FolderTree(new ArrayList<>(byId.values()), unfiled);
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