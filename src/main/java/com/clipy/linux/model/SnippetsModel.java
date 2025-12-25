package com.clipy.linux.model;

import com.clipy.linux.persistence.SnippetsPersistence;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;
import java.util.stream.Collectors;

public class SnippetsModel {

    private final ObservableList<Snippet> snippets =
            FXCollections.observableArrayList();

    public SnippetsModel() {
        List<Snippet> loaded = SnippetsPersistence.loadSnippets();
        snippets.setAll(loaded);
    }

    public ObservableList<Snippet> getSnippets() {
        return snippets;
    }

    public List<String> getFolders() {
        return snippets.stream()
                .map(Snippet::getFolder)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }

    public List<Snippet> getSnippetsInFolder(String folder) {
        return snippets.stream()
                .filter(s -> Objects.equals(folder, s.getFolder()))
                .sorted(Comparator.comparing(Snippet::getName,
                        String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public void addFolder(String folderName) {
        if (folderName == null || folderName.isBlank()) return;
        if (getFolders().contains(folderName)) return;

        // Materialize folder by adding a placeholder snippet
        Snippet placeholder = new Snippet(
                UUID.randomUUID().toString(),
                folderName,
                "",   // name
                ""    // content
        );
        snippets.add(placeholder);
        save();
    }

    public Snippet addSnippet(String folder, String name, String content) {
        if (folder == null || folder.isBlank()) return null;
        if (name == null || name.isBlank()) name = content;

        Snippet snippet = new Snippet(
                UUID.randomUUID().toString(),
                folder,
                name,
                content == null ? "" : content
        );
        snippets.add(snippet);
        save();
        return snippet;
    }

    public void removeSnippet(Snippet snippet) {
        if (snippet == null) return;
        snippets.remove(snippet);
        save();
    }

    public void updateSnippet(Snippet target,
                              String name,
                              String content) {
        if (target == null) return;
        target.setName(name);
        target.setContent(content);
        save();
    }

    public void renameFolder(String oldName, String newName) {
        if (oldName == null || newName == null) return;
        for (Snippet s : snippets) {
            if (oldName.equals(s.getFolder())) {
                s.setFolder(newName);
            }
        }
        save();
    }

    private void save() {
        SnippetsPersistence.saveSnippets(new ArrayList<>(snippets));
    }
}
