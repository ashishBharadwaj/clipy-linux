package com.clipy.linux;

import com.clipy.linux.controller.TrayController;
import com.clipy.linux.model.PreferencesModel;
import com.clipy.linux.model.SnippetsModel;
import com.clipy.linux.persistence.HistoryPersistence;
import com.clipy.linux.persistence.PreferencesPersistence;
import com.clipy.linux.view.HistoryView;
import com.clipy.linux.view.PreferencesView;
import com.clipy.linux.view.SnippetsView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class ClipyLinuxApp extends Application {

    private ClipboardService clipboardService;
    private HistoryView historyView;
    private HistoryPersistence historyPersistence;
    private PreferencesPersistence prefsPersistence;
    private PreferencesModel preferences;
    private Stage primaryStage;

    private SnippetsModel snippetsModel;
    private SnippetsView snippetsView;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        historyPersistence = new HistoryPersistence();
        prefsPersistence = new PreferencesPersistence();
        preferences = prefsPersistence.load();

        clipboardService = new ClipboardService(preferences.maxHistory);
        var loaded = historyPersistence.loadHistory();
        clipboardService.loadFrom(loaded);
        clipboardService.start();

        // shared snippets model + window
        snippetsModel = new SnippetsModel();
        snippetsView = new SnippetsView(snippetsModel);
        snippetsView.init(primaryStage);

        // history window (shown centered when requested)
        historyView = new HistoryView(clipboardService);
        historyView.init(primaryStage);

        Platform.setImplicitExit(false); // keep app alive with only tray [web:448]

        setupTray();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveHistory();
            savePreferences();
        }));
    }

    private void setupTray() {
        try {
            TrayController trayController = new TrayController(
                    this::showHistoryWindow,
                    this::showPreferencesWindow,
                    () -> Platform.runLater(snippetsView::show),
                    text -> clipboardService.setClipboardText(text),
                    max -> clipboardService.getRecent(max),
                    () -> preferences.maxTrayItems,
                    snippetsModel
            );
            trayController.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Open history window centered (same style as Edit Snippets)
    private void showHistoryWindow() {
        Platform.runLater(historyView::show);
    }

    private void showPreferencesWindow() {
        Platform.runLater(() -> {
            PreferencesView view = new PreferencesView(primaryStage, preferences, newPrefs -> {
                this.preferences = newPrefs;
                clipboardService.setMaxItems(newPrefs.maxHistory);
                savePreferences();
            });
            view.show();
        });
    }

    private void saveHistory() {
        if (historyPersistence != null && clipboardService != null) {
            var list = clipboardService.snapshot();
            historyPersistence.saveHistory(list);
        }
    }

    private void savePreferences() {
        if (prefsPersistence != null && preferences != null) {
            prefsPersistence.save(preferences);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
