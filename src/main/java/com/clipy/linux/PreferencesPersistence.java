package com.clipy.linux;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class PreferencesPersistence {

    private final Path prefsFile;
    private final Gson gson;

    public PreferencesPersistence() {
        String home = System.getProperty("user.home");
        Path configDir = Path.of(home, ".config", "clipy-linux");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.prefsFile = configDir.resolve("preferences.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create(); // robust JSON read/write[web:209][web:230]
    }

    public PreferencesModel load() {
        if (!Files.exists(prefsFile)) {
            return PreferencesModel.defaultPrefs();
        }
        try (Reader r = Files.newBufferedReader(prefsFile)) {
            PreferencesModel m = gson.fromJson(r, PreferencesModel.class);
            if (m == null) return PreferencesModel.defaultPrefs();
            if (m.maxHistory <= 0) m.maxHistory = 100;
            if (m.maxTrayItems <= 0) m.maxTrayItems = 10;
            return m;
        } catch (Exception e) {
            e.printStackTrace();
            return PreferencesModel.defaultPrefs();
        }
    }

    public void save(PreferencesModel model) {
        try (Writer w = Files.newBufferedWriter(prefsFile)) {
            gson.toJson(model, w);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
