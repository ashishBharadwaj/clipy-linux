package com.clipy.linux.view;

import com.clipy.linux.model.Snippet;
import com.clipy.linux.model.SnippetsModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;

public class SnippetsView {

    private final SnippetsModel snippetsModel;

    private Stage stage;
    private ListView<String> folderListView;
    private ListView<Snippet> snippetListView;
    private TextField nameField;
    private TextArea contentArea;

    private final ObservableList<String> folders = FXCollections.observableArrayList();
    private final ObservableList<Snippet> snippetsInFolder = FXCollections.observableArrayList();

    public SnippetsView(SnippetsModel snippetsModel) {
        this.snippetsModel = snippetsModel;
    }

    public void init(Stage owner) {
        stage = new Stage(StageStyle.DECORATED);
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Edit Snippets");
        stage.setAlwaysOnTop(true);

        // ----- left: folders -----
        folderListView = new ListView<>(folders);
        folderListView.setPrefWidth(200);

        Button addFolderButton = new Button("+ Folder");
        Button renameFolderButton = new Button("Rename");

        addFolderButton.setOnAction(e -> addFolder());
        renameFolderButton.setOnAction(e -> renameFolder());

        HBox folderButtons = new HBox(8, addFolderButton, renameFolderButton);
        folderButtons.setPadding(new Insets(6, 0, 0, 0));

        VBox leftBox = new VBox(8, new Label("Folders"), folderListView, folderButtons);
        leftBox.setPadding(new Insets(10));
        VBox.setVgrow(folderListView, Priority.ALWAYS);

        // ----- top-right: snippets in folder -----
        snippetListView = new ListView<>(snippetsInFolder);
        snippetListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Snippet item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String label = item.getName();
                    if (label == null || label.isBlank()) {
                        label = item.getContent();
                        if (label != null && label.length() > 40) {
                            label = label.substring(0, 37) + "...";
                        }
                    }
                    setText(label);
                }
            }
        });

        Button addSnippetButton = new Button("+ Snippet");
        Button deleteSnippetButton = new Button("Delete");

        addSnippetButton.setOnAction(e -> addSnippet());
        deleteSnippetButton.setOnAction(e -> deleteSnippet());

        HBox snippetButtons = new HBox(8, addSnippetButton, deleteSnippetButton);
        snippetButtons.setPadding(new Insets(6, 0, 0, 0));

        VBox snippetsBox = new VBox(8, new Label("Snippets"), snippetListView, snippetButtons);
        VBox.setVgrow(snippetListView, Priority.ALWAYS);

        // ----- bottom-right: detail editor (Name + Content only) -----
        nameField = new TextField();
        contentArea = new TextArea();
        contentArea.setPrefRowCount(10);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label("Name:"), 0, 0);
        form.add(nameField, 1, 0);
        form.add(new Label("Content:"), 0, 1);
        form.add(contentArea, 1, 1);

        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(contentArea, Priority.ALWAYS);
        GridPane.setVgrow(contentArea, Priority.ALWAYS);

        VBox rightBox = new VBox(12, snippetsBox, form);
        rightBox.setPadding(new Insets(10));
        VBox.setVgrow(form, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(leftBox, rightBox);
        splitPane.setDividerPositions(0.25);

        BorderPane root = new BorderPane(splitPane);

        Scene scene = new Scene(root, 1000, 600);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.hide();
            }
        });

        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(550);

        // ----- selection listeners -----
        folderListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldV, newV) -> refreshSnippetsForFolder(newV)
        );

        snippetListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldV, newV) -> showSnippetDetails(newV)
        );

        // change tracking -> update model
        nameField.textProperty().addListener((obs, o, n) -> updateCurrentSnippet());
        contentArea.textProperty().addListener((obs, o, n) -> updateCurrentSnippet());

        refreshFolders();
    }

    public void show() {
        if (stage == null) {
            throw new IllegalStateException("SnippetsView.init must be called first");
        }
        Platform.runLater(() -> {
            refreshFolders();
            stage.show();
            stage.toFront();
        });
    }

    private void refreshFolders() {
        List<String> modelFolders = snippetsModel.getFolders();
        folders.setAll(modelFolders);
        if (!folders.isEmpty() && folderListView.getSelectionModel().getSelectedItem() == null) {
            folderListView.getSelectionModel().selectFirst();
        }
    }

    private void refreshSnippetsForFolder(String folder) {
        if (folder == null) {
            snippetsInFolder.clear();
            showSnippetDetails(null);
            return;
        }
        List<Snippet> list = snippetsModel.getSnippetsInFolder(folder);
        snippetsInFolder.setAll(list);
        if (!snippetsInFolder.isEmpty()) {
            snippetListView.getSelectionModel().selectFirst();
        } else {
            showSnippetDetails(null);
        }
    }

    private void showSnippetDetails(Snippet snippet) {
        if (snippet == null) {
            nameField.setText("");
            contentArea.setText("");
        } else {
            nameField.setText(snippet.getName());
            contentArea.setText(snippet.getContent());
        }
    }

    private void addFolder() {
        TextInputDialog dialog = new TextInputDialog("My Snippets");
        dialog.setTitle("New Folder");
        dialog.setHeaderText(null);
        dialog.setContentText("Folder name:");
        dialog.initOwner(stage);

        dialog.showAndWait().ifPresent(name -> {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                snippetsModel.addFolder(trimmed);
                refreshFolders();
                folderListView.getSelectionModel().select(trimmed);
            }
        });
    }

    private void renameFolder() {
        String selected = folderListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected);
        dialog.setTitle("Rename Folder");
        dialog.setHeaderText(null);
        dialog.setContentText("New name:");
        dialog.initOwner(stage);

        dialog.showAndWait().ifPresent(newName -> {
            String trimmed = newName.trim();
            if (!trimmed.isEmpty()) {
                snippetsModel.renameFolder(selected, trimmed);
                refreshFolders();
                folderListView.getSelectionModel().select(trimmed);
            }
        });
    }

    private void addSnippet() {
        String folder = folderListView.getSelectionModel().getSelectedItem();
        if (folder == null || folder.isBlank()) {
            return;
        }
        Snippet snippet = snippetsModel.addSnippet(folder, "New snippet", "");
        refreshSnippetsForFolder(folder);
        if (snippet != null) {
            snippetListView.getSelectionModel().select(snippet);
        }
    }

    private void deleteSnippet() {
        Snippet selected = snippetListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        snippetsModel.removeSnippet(selected);
        String folder = folderListView.getSelectionModel().getSelectedItem();
        refreshSnippetsForFolder(folder);
    }

    private void updateCurrentSnippet() {
        Snippet selected = snippetListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        snippetsModel.updateSnippet(
                selected,
                nameField.getText(),
                contentArea.getText()
        );
    }
}
