package com.clipy.linux;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;

public class ClipyLinuxApp extends Application {

    private ClipboardService clipboardService;
    private HistoryView historyView;
    private JsonPersistence jsonPersistence;
    private PreferencesPersistence prefsPersistence;
    private PreferencesModel preferences;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        jsonPersistence = new JsonPersistence();
        prefsPersistence = new PreferencesPersistence();
        preferences = prefsPersistence.load();

        clipboardService = new ClipboardService(preferences.maxHistory);
        var loaded = jsonPersistence.loadHistory();
        clipboardService.loadFrom(loaded);
        clipboardService.start();

        historyView = new HistoryView(clipboardService);
        historyView.init(primaryStage);

        Label label = new Label(
                "Clipy Linux - Clipboard watcher running.\n" +
                        "Copy some text in any app.\n" +
                        "Press Ctrl+H here or use the tray menu."
        );

        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 520, 260);

        KeyCombination showHistoryCombo = new KeyCodeCombination(
                javafx.scene.input.KeyCode.H,
                KeyCombination.CONTROL_DOWN
        );
        scene.setOnKeyPressed(e -> {
            if (showHistoryCombo.match(e)) {
                historyView.show(); // centered when from main window
            }
        });

        primaryStage.setTitle("Clipy Linux (Prototype)");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            saveHistory();
            savePreferences();
        });

        Platform.setImplicitExit(false);
        setupTray();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveHistory();
            savePreferences();
        }));
    }

    private void setupTray() {
        try {
            TrayController trayController = new TrayController(
                    this::showMainWindow,
                    this::showHistoryWindow,      // from tray, uses mouse position
                    this::showPreferencesWindow,
                    text -> clipboardService.setClipboardText(text),
                    max -> clipboardService.getRecent(max),
                    () -> preferences.maxTrayItems
            );
            trayController.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMainWindow() {
        Platform.runLater(() -> {
            primaryStage.show();
            primaryStage.toFront();
        });
    }

    // Called from tray: position near mouse
    private void showHistoryWindow() {
        Platform.runLater(() -> {
            try {
                PointerInfo pi = MouseInfo.getPointerInfo(); // AWT mouse info[web:242][web:244][web:246]
                if (pi != null) {
                    Point p = pi.getLocation();
                    historyView.showAt(p.getX(), p.getY());
                } else {
                    historyView.show(); // fallback
                }
            } catch (Exception e) {
                e.printStackTrace();
                historyView.show(); // fallback
            }
        });
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
        if (jsonPersistence != null && clipboardService != null) {
            var list = clipboardService.snapshot();
            jsonPersistence.saveHistory(list);
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
