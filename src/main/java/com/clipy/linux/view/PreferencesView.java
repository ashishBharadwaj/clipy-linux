package com.clipy.linux.view;

import com.clipy.linux.model.PreferencesModel;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.UnaryOperator;

public class PreferencesView {

    public interface Listener {
        void onPreferencesChanged(PreferencesModel updatedPrefs);
    }

    private final Stage stage;
    private final TextField maxHistoryField;
    private final TextField maxTrayField;
    private final CheckBox plainTextCheck;
    private final PreferencesModel current;
    private final Listener listener;

    public PreferencesView(Stage owner, PreferencesModel prefs, Listener listener) {
        this.listener = listener;
        this.current = prefs; // mutate this instance directly

        stage = new Stage(StageStyle.UTILITY);
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Preferences");

        Label maxHistoryLabel = new Label("Max history items:");
        maxHistoryField = createNumericField(10, 100000, prefs.maxHistory);

        Label maxTrayLabel = new Label("Max tray items:");
        maxTrayField = createNumericField(3, 1000, prefs.maxTrayItems);

        plainTextCheck = new CheckBox("Plain text only");
        plainTextCheck.setSelected(prefs.plainTextOnly);

        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");

        saveBtn.setOnAction(e -> {
            int maxHistory = parseOrClamp(maxHistoryField.getText(), 10, 100000);
            int maxTray = parseOrClamp(maxTrayField.getText(), 3, 1000);
            boolean plainOnly = plainTextCheck.isSelected();

            // mutate existing model
            current.maxHistory = maxHistory;
            current.maxTrayItems = maxTray;
            current.plainTextOnly = plainOnly;

            if (listener != null) {
                listener.onPreferencesChanged(current);
            }
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(maxHistoryLabel, 0, 0);
        grid.add(maxHistoryField, 1, 0);
        grid.add(maxTrayLabel, 0, 1);
        grid.add(maxTrayField, 1, 1);
        grid.add(plainTextCheck, 0, 2, 2, 1);
        grid.add(saveBtn, 0, 3);
        grid.add(cancelBtn, 1, 3);

        Scene scene = new Scene(grid, 320, 180);
        stage.setScene(scene);
    }

    private TextField createNumericField(int min, int max, int initial) {
        TextField field = new TextField(String.valueOf(initial));

        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                return change;
            }
            if (!newText.matches("\\d+")) {
                return null;
            }
            try {
                int val = Integer.parseInt(newText);
                if (val < min || val > max) {
                    return change;
                }
            } catch (NumberFormatException e) {
                return null;
            }
            return change;
        };

        field.setTextFormatter(new TextFormatter<>(filter));
        return field;
    }

    private int parseOrClamp(String text, int min, int max) {
        try {
            int val = Integer.parseInt(text.trim());
            if (val < min) return min;
            if (val > max) return max;
            return val;
        } catch (Exception e) {
            return min;
        }
    }

    public void show() {
        maxHistoryField.setText(String.valueOf(current.maxHistory));
        maxTrayField.setText(String.valueOf(current.maxTrayItems));
        plainTextCheck.setSelected(current.plainTextOnly);
        stage.showAndWait();
    }
}
