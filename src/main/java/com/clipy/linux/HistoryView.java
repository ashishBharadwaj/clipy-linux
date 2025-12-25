package com.clipy.linux;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class HistoryView {

    private final ClipboardService clipboardService;
    private Stage stage;
    private ListView<String> listView;
    private TextField searchField;
    private FilteredList<String> filteredItems;

    public HistoryView(ClipboardService clipboardService) {
        this.clipboardService = clipboardService;
    }

    public void init(Stage owner) {
        stage = new Stage(StageStyle.UTILITY);
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setTitle("Clipboard History");
        stage.setAlwaysOnTop(true);

        listView = new ListView<>();
        listView.setFocusTraversable(false);

        searchField = new TextField();
        searchField.setPromptText("Search history...");

        Deque<String> history = clipboardService.getHistory();
        ObservableList<String> baseList =
                FXCollections.observableArrayList(new ArrayList<>(history));
        filteredItems = new FilteredList<>(baseList, s -> true);
        listView.setItems(filteredItems);

        searchField.textProperty().addListener((obs, oldV, newV) -> {
            String query = newV == null ? "" : newV.trim().toLowerCase();

            filteredItems.setPredicate(s -> {
                if (query.isEmpty()) return true;
                if (s == null) return false;
                String text = s.toLowerCase();
                return text.contains(query);
            });

            if (!filteredItems.isEmpty()) {
                listView.getSelectionModel().selectFirst();
            }
        });

        listView.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                useSelected();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hide();
            }
        });

        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                listView.requestFocus();
                listView.getSelectionModel().selectFirst();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hide();
            }
        });

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                useSelected();
            }
        });

        Label hint = new Label("Type to filter, Enter to select, Esc to close.");
        hint.setPadding(new Insets(2, 0, 4, 0));

        VBox topBox = new VBox(4, searchField, hint);
        topBox.setPadding(new Insets(5));

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(listView);

        Scene scene = new Scene(root, 600, 350);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hide();
            }
        });

        stage.setScene(scene);

        stage.focusedProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                Platform.runLater(() -> searchField.requestFocus());
            }
        });
    }

    // Show centered (used from main window)
    public void show() {
        Platform.runLater(() -> {
            refreshItems();
            stage.centerOnScreen();
            stage.show();
            stage.toFront();
            Platform.runLater(() -> {
                searchField.requestFocus();
                searchField.positionCaret(searchField.getText().length());
            });
        });
    }

    // Show near given screen coordinates (used from tray)
    public void showAt(double screenX, double screenY) {
        Platform.runLater(() -> {
            refreshItems();
            stage.setX(screenX - 300); // half width to the left
            stage.setY(screenY + 10);  // just below cursor
            stage.show();
            stage.toFront();
            Platform.runLater(() -> {
                searchField.requestFocus();
                searchField.positionCaret(searchField.getText().length());
            });
        });
    }

    private void refreshItems() {
        Deque<String> history = clipboardService.getHistory();
        List<String> list = new ArrayList<>(history);
        ObservableList<String> baseList = FXCollections.observableArrayList(list);
        filteredItems = new FilteredList<>(baseList, s -> true);
        listView.setItems(filteredItems);
        searchField.clear();
        if (!filteredItems.isEmpty()) {
            listView.getSelectionModel().selectFirst();
        }
    }

    public void hide() {
        Platform.runLater(() -> stage.hide());
    }

    private void useSelected() {
        String selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            clipboardService.setClipboardText(selected);
        }
        hide();
    }
}
