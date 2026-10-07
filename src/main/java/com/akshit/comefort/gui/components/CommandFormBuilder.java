package com.akshit.comefort.gui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable form builder that creates GUI controls from command parameters.
 * Every CLI flag/option/parameter gets a matching GUI control.
 *
 * <p>Also generates a live "CLI equivalent" preview as the user fills the form.</p>
 */
public class CommandFormBuilder {

    private final String commandPrefix;
    private final List<FormField> fields = new ArrayList<>();
    private final VBox formContainer = new VBox(12);
    private final Label commandPreview = new Label();
    private Consumer<String> onExecute;

    /**
     * @param commandPrefix e.g. "cf task add"
     */
    public CommandFormBuilder(String commandPrefix) {
        this.commandPrefix = commandPrefix;
        commandPreview.getStyleClass().add("command-preview");
        commandPreview.setWrapText(true);
    }

    /**
     * Adds a required text parameter (positional argument).
     *
     * @param name        field name (e.g., "title")
     * @param label       display label (e.g., "Task Title")
     * @param placeholder prompt text
     * @param cliHint     CLI usage hint (e.g., "cf task add <title>")
     */
    public CommandFormBuilder addTextParam(String name, String label,
                                           String placeholder, String cliHint,
                                           boolean required) {
        TextField field = new TextField();
        field.setPromptText(placeholder);
        field.textProperty().addListener((obs, o, n) -> updatePreview());
        fields.add(new FormField(name, label, field, FieldType.POSITIONAL,
                null, cliHint, required));
        return this;
    }

    /**
     * Adds a text area for multiline content.
     */
    public CommandFormBuilder addTextAreaParam(String name, String label,
                                               String placeholder, String flag,
                                               String cliHint) {
        TextArea area = new TextArea();
        area.setPromptText(placeholder);
        area.setPrefRowCount(4);
        area.textProperty().addListener((obs, o, n) -> updatePreview());
        fields.add(new FormField(name, label, area, FieldType.FLAG,
                flag, cliHint, false));
        return this;
    }

    /**
     * Adds a text option (--flag value).
     */
    public CommandFormBuilder addTextOption(String name, String label,
                                            String placeholder, String flag,
                                            String cliHint) {
        TextField field = new TextField();
        field.setPromptText(placeholder);
        field.textProperty().addListener((obs, o, n) -> updatePreview());
        fields.add(new FormField(name, label, field, FieldType.FLAG,
                flag, cliHint, false));
        return this;
    }

    /**
     * Adds a dropdown/combo box option.
     */
    public CommandFormBuilder addChoiceOption(String name, String label,
                                              List<String> choices, String defaultValue,
                                              String flag, String cliHint) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(choices);
        if (defaultValue != null) combo.setValue(defaultValue);
        combo.valueProperty().addListener((obs, o, n) -> updatePreview());
        fields.add(new FormField(name, label, combo, FieldType.FLAG,
                flag, cliHint, false));
        return this;
    }

    /**
     * Adds a boolean flag checkbox.
     */
    public CommandFormBuilder addBooleanFlag(String name, String label,
                                             String flag, String cliHint) {
        CheckBox checkBox = new CheckBox(label);
        checkBox.selectedProperty().addListener((obs, o, n) -> updatePreview());
        fields.add(new FormField(name, label, checkBox, FieldType.BOOLEAN_FLAG,
                flag, cliHint, false));
        return this;
    }

    /**
     * Adds a date picker option.
     */
    public CommandFormBuilder addDateOption(String name, String label,
                                            String flag, String cliHint) {
        DatePicker picker = new DatePicker();
        picker.setPromptText("Select date or type: today, tomorrow, yyyy-MM-dd");
        picker.setEditable(true);
        picker.valueProperty().addListener((obs, o, n) -> updatePreview());
        fields.add(new FormField(name, label, picker, FieldType.FLAG,
                flag, cliHint, false));
        return this;
    }

    /**
     * Sets the execute callback.
     */
    public CommandFormBuilder onExecute(Consumer<String> callback) {
        this.onExecute = callback;
        return this;
    }

    /**
     * Builds the complete form VBox.
     */
    public VBox build() {
        formContainer.setPadding(new Insets(0));

        for (FormField field : fields) {
            VBox group = new VBox(4);
            group.getStyleClass().add("form-group");

            // Label (skip for boolean checkboxes which have their own label)
            if (field.type != FieldType.BOOLEAN_FLAG) {
                Label label = new Label(field.label
                        + (field.required ? " *" : ""));
                label.getStyleClass().add("form-label");
                group.getChildren().add(label);
            }

            // Control
            group.getChildren().add(field.control);

            // CLI hint tooltip
            if (field.cliHint != null && !field.cliHint.isBlank()) {
                Label hint = new Label("CLI: " + field.cliHint);
                hint.getStyleClass().add("form-hint");
                group.getChildren().add(hint);
            }

            formContainer.getChildren().add(group);
        }

        // Separator
        formContainer.getChildren().add(new Separator());

        // CLI preview section
        VBox previewSection = new VBox(4);
        Label previewLabel = new Label("CLI Equivalent");
        previewLabel.getStyleClass().add("form-label");
        previewSection.getChildren().addAll(previewLabel, commandPreview);
        formContainer.getChildren().add(previewSection);

        // Buttons
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(12, 0, 0, 0));

        Button executeBtn = new Button("Execute");
        executeBtn.getStyleClass().add("btn-primary");
        executeBtn.setOnAction(e -> {
            if (onExecute != null) {
                onExecute.accept(buildCommandString());
            }
        });

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setOnAction(e -> clearForm());

        buttons.getChildren().addAll(clearBtn, executeBtn);
        formContainer.getChildren().add(buttons);

        updatePreview();
        return formContainer;
    }

    /**
     * Builds the CLI command string from the current form state.
     */
    public String buildCommandString() {
        StringBuilder sb = new StringBuilder(commandPrefix);

        for (FormField field : fields) {
            String value = getFieldValue(field);
            if (value == null || value.isBlank()) continue;

            if (field.type == FieldType.POSITIONAL) {
                // Positional arguments — quote if contains spaces
                if (value.contains(" ")) {
                    sb.append(" \"").append(value).append("\"");
                } else {
                    sb.append(" ").append(value);
                }
            } else if (field.type == FieldType.BOOLEAN_FLAG) {
                if ("true".equals(value)) {
                    sb.append(" ").append(field.flag);
                }
            } else if (field.type == FieldType.FLAG && field.flag != null) {
                if (value.contains(" ")) {
                    sb.append(" ").append(field.flag).append(" \"").append(value).append("\"");
                } else {
                    sb.append(" ").append(field.flag).append(" ").append(value);
                }
            }
        }

        return sb.toString();
    }

    /**
     * Returns the value of a field by name.
     */
    public String getFieldValueByName(String name) {
        for (FormField field : fields) {
            if (field.name.equals(name)) {
                return getFieldValue(field);
            }
        }
        return null;
    }

    /**
     * Extracts the string value from any control type.
     */
    private String getFieldValue(FormField field) {
        return switch (field.control) {
            case TextField tf -> tf.getText();
            case TextArea ta -> ta.getText();
            case ComboBox<?> cb -> cb.getValue() != null ? cb.getValue().toString() : null;
            case CheckBox chk -> chk.isSelected() ? "true" : null;
            case DatePicker dp -> dp.getValue() != null ? dp.getValue().toString() : null;
            default -> null;
        };
    }

    /**
     * Updates the CLI preview label.
     */
    private void updatePreview() {
        commandPreview.setText(buildCommandString());
    }

    /**
     * Clears all form fields.
     */
    private void clearForm() {
        for (FormField field : fields) {
            switch (field.control) {
                case TextField tf -> tf.clear();
                case TextArea ta -> ta.clear();
                case ComboBox<?> cb -> cb.getSelectionModel().clearSelection();
                case CheckBox chk -> chk.setSelected(false);
                case DatePicker dp -> dp.setValue(null);
                default -> {}
            }
        }
        updatePreview();
    }

    // --- Internal types ---

    private enum FieldType {
        POSITIONAL,    // Positional argument
        FLAG,          // --flag value
        BOOLEAN_FLAG   // --flag (boolean)
    }

    private record FormField(
            String name,
            String label,
            javafx.scene.control.Control control,
            FieldType type,
            String flag,
            String cliHint,
            boolean required
    ) {
    }
}
