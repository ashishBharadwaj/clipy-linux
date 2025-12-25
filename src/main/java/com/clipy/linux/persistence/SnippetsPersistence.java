package com.clipy.linux.persistence;

import com.clipy.linux.model.Snippet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SnippetsPersistence {

    private static final Path SNIPPETS_FILE = Paths.get(
            System.getProperty("user.home"),
            ".config",
            "clipy-linux",
            "snippets.json"
    );

    private static final Gson gson = new Gson();

    public static List<Snippet> loadSnippets() {
        try {
            if (Files.exists(SNIPPETS_FILE)) {
                String json = Files.readString(SNIPPETS_FILE, StandardCharsets.UTF_8);
                Type listType = new TypeToken<List<Snippet>>() {}.getType();
                List<Snippet> result = gson.fromJson(json, listType);
                return result != null ? result : new ArrayList<>();
            }
        } catch (IOException e) {
            System.err.println("Failed to load snippets: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public static void saveSnippets(List<Snippet> snippets) {
        try {
            Files.createDirectories(SNIPPETS_FILE.getParent());
            String json = gson.toJson(snippets);
            Files.writeString(SNIPPETS_FILE, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save snippets: " + e.getMessage());
        }
    }
}
