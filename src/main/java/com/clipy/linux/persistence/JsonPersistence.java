package com.clipy.linux.persistence;

import com.clipy.linux.model.HistoryModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonPersistence {

    private final Path historyFile;
    private final Gson gson;

    public JsonPersistence() {
        String home = System.getProperty("user.home");
        Path configDir = Path.of(home, ".config", "clipy-linux");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.historyFile = configDir.resolve("history.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()   // nicer JSON[web:209][web:212]
                .create();
    }

    public void saveHistory(List<String> items) {
        try (Writer w = Files.newBufferedWriter(historyFile)) {
            HistoryModel model = new HistoryModel(items);
            gson.toJson(model, w);   // robust JSON, handles escaping etc.[web:209][web:212]
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> loadHistory() {
        if (!Files.exists(historyFile)) {
            return new ArrayList<>();
        }
        try (Reader r = Files.newBufferedReader(historyFile)) {
            HistoryModel model = gson.fromJson(r, HistoryModel.class);
            if (model == null || model.items == null) {
                return new ArrayList<>();
            }
            return model.items;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
