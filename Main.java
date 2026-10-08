package com.customermanager;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    // Icons drawn with code (no image files needed)
    private static final String ICON_CHECK = "M3 13 L6 10 L10 14 L19 5 L22 8 L10 20 Z";
    private static final String ICON_CROSS = "M4 7 L7 4 L12 9 L17 4 L20 7 L15 12 L20 17 L17 20 L12 15 L7 20 L4 17 L9 12 Z";
    private static final String ICON_TRASH = "M5 6 H19 V8 H5 Z M9 3 H15 V6 H9 Z M6 9 H18 L17 21 H7 Z";

    // DATA (slide 16): the list the table listens to
    private final ObservableList<Customer> customers = FXCollections.observableArrayList();

    // CONTROLS
    private final TextField nameField = new TextField();
    private final ComboBox<String> provinceBox = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final TableView<Customer> table = new TableView<>();
    private final Label status = new Label();
    private final FadeTransition fade = new FadeTransition(Duration.seconds(1.5), status);

    // RGB pieces
    private final Region rgbStrip = new Region();
    private final Circle ring = new Circle(26, Color.web("#0f172a"));
    private final DropShadow glow = new DropShadow(30, Color.CYAN);
    private double hue = 0;

    @Override
    public void start(Stage stage) {
        // Sample rows so the table is not empty (delete these two lines if you like)
        customers.addAll(new Customer("Mary Banda", "Central"),
                new Customer("John Phiri", "Lusaka"));

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildHeader());

        HBox body = new HBox(24, buildFormCard(), buildTableCard());
        body.setPadding(new Insets(24));
        root.setCenter(body);

        Scene scene = new Scene(root, 980, 620);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("Customer Manager");
        stage.setMinWidth(860);
        stage.setMinHeight(540);
        stage.setScene(scene);
        stage.show();

        startRgbAnimation();
        nameField.requestFocus();
    }

    // ---------- HEADER ----------
    private VBox buildHeader() {
        Label cm = new Label("CM");
        cm.getStyleClass().add("badge-text");
        ring.setStrokeWidth(4);
        StackPane badge = new StackPane(ring, cm);

        Label title = new Label("Customer Manager");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("ICT261 · Advanced Java Programming");
        subtitle.getStyleClass().add("app-subtitle");

        HBox top = new HBox(16, badge, new VBox(2, title, subtitle));
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(18, 28, 18, 28));

        rgbStrip.setMinHeight(5);
        rgbStrip.setPrefHeight(5);

        VBox header = new VBox(top, rgbStrip);
        header.getStyleClass().add("header");
        return header;
    }

    // ---------- LEFT CARD: THE FORM (slides 7, 9, 10) ----------
    private VBox buildFormCard() {
        Label formTitle = new Label("New customer");
        formTitle.getStyleClass().add("card-title");

        Label nameLabel = new Label("Customer name");
        nameLabel.getStyleClass().add("field-label");
        nameField.setPromptText("e.g., Mary Banda");
        nameLabel.setLabelFor(nameField);

        Label provinceLabel = new Label("Province");
        provinceLabel.getStyleClass().add("field-label");
        provinceBox.getItems().addAll("Central", "Copperbelt", "Eastern", "Luapula", "Lusaka",
                "Muchinga", "Northern", "North-Western", "Southern", "Western");
        provinceBox.setPromptText("Choose a province");
        provinceBox.setMaxWidth(Double.MAX_VALUE);
        // Keeps the "Choose a province" text visible again after the form is cleared
        provinceBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Choose a province" : item);
            }
        });
        provinceLabel.setLabelFor(provinceBox);

        Button saveButton = makeButton("Save customer", ICON_CHECK, "btn-save");
        saveButton.setDefaultButton(true);              // Enter key = Save (slide 26)
        saveButton.setOnAction(e -> saveCustomer());

        Button clearButton = makeButton("Clear", ICON_CROSS, "btn-clear");
        clearButton.setOnAction(e -> clearForm());

        HBox buttons = new HBox(12, saveButton, clearButton);
        VBox.setMargin(buttons, new Insets(14, 0, 0, 0));

        status.getStyleClass().add("status");
        status.setWrapText(true);
        status.setMinHeight(40);
        status.setOpacity(0);

        VBox card = new VBox(10, formTitle, nameLabel, nameField,
                provinceLabel, provinceBox, buttons, status);
        card.getStyleClass().add("card");
        card.setPrefWidth(340);
        card.setMinWidth(340);
        card.setEffect(glow);                           // RGB glow
        return card;
    }

    // ---------- RIGHT CARD: THE TABLE (slides 16-19, 23) ----------
    private VBox buildTableCard() {
        Label tableTitle = new Label("Saved customers");
        tableTitle.getStyleClass().add("card-title");

        searchField.setPromptText("Search by name or province...");
        searchField.setOnAction(e -> { });   // stops Enter here from triggering Save

        TableColumn<Customer, String> nameCol = new TableColumn<>("Customer name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Customer, String> provinceCol = new TableColumn<>("Province");
        provinceCol.setCellValueFactory(new PropertyValueFactory<>("province"));
        table.getColumns().add(nameCol);
        table.getColumns().add(provinceCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No customers to show"));

        // Bonus: live search. FilteredList wraps the SAME customers list.
        FilteredList<Customer> filtered = new FilteredList<>(customers, c -> true);
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            String q = newText.trim().toLowerCase();
            filtered.setPredicate(c -> q.isEmpty()
                    || c.getName().toLowerCase().contains(q)
                    || c.getProvince().toLowerCase().contains(q));
        });
        table.setItems(filtered);

        // Bonus: press the Delete key on the table
        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) deleteSelected();
        });

        Button deleteButton = makeButton("Delete selected", ICON_TRASH, "btn-delete");
        deleteButton.disableProperty().bind(
                table.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.setOnAction(e -> deleteSelected());

        Label count = new Label();
        count.getStyleClass().add("count");
        count.textProperty().bind(Bindings.size(customers).asString("Total customers: %d"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(12, count, spacer, deleteButton);
        bottom.setAlignment(Pos.CENTER_LEFT);

        VBox.setVgrow(table, Priority.ALWAYS);
        VBox card = new VBox(12, tableTitle, searchField, table, bottom);
        card.getStyleClass().add("card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // ---------- ACTIONS ----------
    private void saveCustomer() {
        String name = nameField.getText().trim();               // slide 12
        if (name.isEmpty()) {
            showStatus("Enter the customer name.", false);
            nameField.requestFocus();
            return;
        }
        String province = provinceBox.getValue();               // slide 20
        if (province == null) {
            showStatus("Choose a province.", false);
            provinceBox.requestFocus();
            return;
        }
        customers.add(new Customer(name, province));
        showStatus("Customer saved.", true);
        clearForm();                                            // clear only after success
    }

    private void deleteSelected() {
        Customer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Select a customer first.", false);
            return;
        }
        ButtonType delete = new ButtonType("Delete");           // slide 23
        Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + selected.getName() + "?", delete, ButtonType.CANCEL);
        ask.setHeaderText("Confirm deletion");
        style(ask);
        if (ask.showAndWait().orElse(ButtonType.CANCEL) == delete) {
            customers.remove(selected);
            showStatus("Customer deleted.", true);
        }
    }

    private void clearForm() {
        nameField.clear();
        provinceBox.getSelectionModel().clearSelection();
        nameField.requestFocus();
    }

    // ---------- HELPERS ----------
    private Button makeButton(String text, String svgPath, String styleClass) {
        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.setFill(Color.WHITE);
        Button b = new Button(text, icon);
        b.setGraphicTextGap(10);
        b.getStyleClass().addAll("btn", styleClass);
        return b;
    }

    private void showStatus(String message, boolean success) {
        status.setText(message);
        status.setTextFill(success ? Color.web("#4ade80") : Color.web("#f87171"));
        status.setOpacity(1);
        fade.stop();
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setDelay(Duration.seconds(3));   // message stays 3 s, then fades out
        fade.playFromStart();
    }

    private void style(Alert alert) {
        alert.getDialogPane().getStylesheets()
                .add(getClass().getResource("/style.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("dark-dialog");
    }

    // RGB trick: shift the colour (hue) a little every 40 ms
    private void startRgbAnimation() {
        Timeline rgb = new Timeline(new KeyFrame(Duration.millis(40), e -> {
            hue = (hue + 1.5) % 360;
            Color c1 = Color.hsb(hue, 0.85, 1.0);
            Color c2 = Color.hsb((hue + 120) % 360, 0.85, 1.0);
            Color c3 = Color.hsb((hue + 240) % 360, 0.85, 1.0);

            rgbStrip.setBackground(new Background(new BackgroundFill(
                    new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                            new Stop(0, c1), new Stop(0.5, c2), new Stop(1, c3)),
                    CornerRadii.EMPTY, Insets.EMPTY)));
            ring.setStroke(c1);
            glow.setColor(c1.deriveColor(0, 1, 1, 0.55));
        }));
        rgb.setCycleCount(Animation.INDEFINITE);
        rgb.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}