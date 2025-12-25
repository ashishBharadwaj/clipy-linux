package com.clipy.linux.model;

import java.util.Objects;

public class Snippet {

    private String id;
    private String folder;   // e.g. "My Snippets"
    private String name;     // label shown in menus
    private String content;  // actual text

    public Snippet() {
    }

    public Snippet(String id, String folder, String name, String content) {
        this.id = id;
        this.folder = folder;
        this.name = name;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public String getFolder() {
        return folder;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Snippet)) return false;
        Snippet snippet = (Snippet) o;
        return Objects.equals(id, snippet.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
