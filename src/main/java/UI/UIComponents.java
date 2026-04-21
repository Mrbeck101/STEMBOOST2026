package UI;

import Services.KeyboardTtsService;
import UserFactory.User;
import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Shared UI component builders used across all dashboard and view classes.
 */
public final class UIComponents {

    /** All available STEM learning paths. */
    public static final String[] LEARNING_PATHS = {
        "Electrical Engineering", "Software Engineering", "Information Technology",
        "Cybersecurity", "Computer Engineering", "Artificial Intelligence"
    };

    private UIComponents() {}

    // ── Theme ──────────────────────────────────────────────────────────────────

    /** Apply the app-wide dark theme. Safe to call multiple times. */
    public static void applyTheme() {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
    }

    // ── Login guard ────────────────────────────────────────────────────────────

    /**
     * Returns true if the current user is null and loads the login scene.
     * Usage: {@code if (UIComponents.guardLogin(router)) return ...;}
     */
    public static User guardLogin(SceneRouter router) {
        return UserContext.getInstance().getCurrentUser();
    }

    // ── Top bar ────────────────────────────────────────────────────────────────

    /**
     * Standard dashboard top bar with a title, welcome label, and logout button.
     * Optionally adds extra buttons between the welcome label and logout button.
     */
    public static HBox topBar(String pageTitle, String welcomeName, SceneRouter router, Button... extraButtons) {
        HBox bar = new HBox(20);
        bar.setPadding(new Insets(15, 20, 15, 20));
        bar.setStyle("-fx-background-color: #1C2128;");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(pageTitle);
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label name = new Label("Welcome, " + welcomeName);
        name.setStyle("-fx-font-size: 14; -fx-text-fill: #ffffff;");

        Button logout = new Button("Logout");
        logout.setStyle("-fx-font-size: 12;");
        logout.setOnAction(e -> router.goToLogin());

        bar.getChildren().addAll(title, spacer, name);
        for (Button btn : extraButtons) {
            bar.getChildren().add(btn);
        }
        bar.getChildren().add(logout);
        return bar;
    }

    /**
     * Top bar that includes a secondary info line beneath the name (e.g. company, university).
     */
    public static HBox topBarWithSubtitle(String pageTitle, String welcomeName, String subtitle, SceneRouter router, Button... extraButtons) {
        HBox bar = new HBox(20);
        bar.setPadding(new Insets(15, 20, 15, 20));
        bar.setStyle("-fx-background-color: #1C2128;");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(pageTitle);
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox infoBox = new VBox(3);
        Label nameLabel = new Label("Welcome, " + welcomeName);
        nameLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #ffffff;");
        Label subLabel = new Label(subtitle);
        subLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #aaaaaa;");
        infoBox.getChildren().addAll(nameLabel, subLabel);

        Button logout = new Button("Logout");
        logout.setStyle("-fx-font-size: 12;");
        logout.setOnAction(e -> router.goToLogin());

        bar.getChildren().addAll(titleLabel, spacer, infoBox);
        for (Button btn : extraButtons) {
            bar.getChildren().add(btn);
        }
        bar.getChildren().add(logout);
        return bar;
    }

    // ── Stat card ──────────────────────────────────────────────────────────────

    /** Standard dark-themed stat card showing a big value under a small title. */
    public static VBox statCard(String title, String value) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    /** Lays out a row of stat cards, each growing to fill available width. */
    public static HBox statsRow(VBox... cards) {
        HBox row = new HBox(15);
        for (VBox card : cards) {
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    // ── Inbox tab content ──────────────────────────────────────────────────────

    /** Standard inbox tab pane content for any user role. */
    public static VBox inboxTab(User user, SceneRouter router) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Inbox");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Button viewBtn = new Button("View Messages");
        viewBtn.setOnAction(e -> router.goToInbox());

        int count = user.getUnreadMessageCount();
        String text = count > 0
                ? "You have " + count + " new message" + (count == 1 ? "" : "s")
                : "No new messages";
        Label countLabel = new Label(text);
        countLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");

        content.getChildren().addAll(title, viewBtn, countLabel);
        return content;
    }

    // ── Browse-modules link tab ────────────────────────────────────────────────

    /** Simple tab content that links to the module browser, with an optional label. */
    public static VBox browseModulesTab(String tabTitle, String description, SceneRouter router) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label(tabTitle);
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label desc = new Label(description);
        desc.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");

        Button openBtn = new Button("Open Module Browser");
        openBtn.setOnAction(e -> router.goToModules());

        content.getChildren().addAll(title, desc, openBtn);
        return content;
    }

    // ── Content section title ──────────────────────────────────────────────────

    /** Standard section-heading label. */
    public static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        return label;
    }

    // ── Content wrapper ────────────────────────────────────────────────────────

    /** Standard dark-background VBox for tab content. */
    public static VBox contentBox(int spacing) {
        VBox box = new VBox(spacing);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #0D1117;");
        return box;
    }

    // ── ScrollPane ─────────────────────────────────────────────────────────────

    /** Standard dark scrollpane that fills its parent width. */
    public static ScrollPane darkScrollPane() {
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #0D1117;");
        return sp;
    }

    // ── Alerts ─────────────────────────────────────────────────────────────────

    /** Generic contact info tab for any user role (email, phone, address). */
    public static VBox contactInfoTab(User user) {
        VBox content = contentBox(15);
        content.getChildren().add(sectionTitle("Update Contact Information"));

        TextField emailField = new TextField(user.getEmail() == null ? "" : user.getEmail());
        TextField phoneField = new TextField(user.getPhone() == null ? "" : user.getPhone());
        TextArea addressField = new TextArea(user.getAddress() == null ? "" : user.getAddress());
        addressField.setPrefRowCount(3);

        Button saveBtn = new Button("Save Changes");
        saveBtn.setOnAction(e -> {
            try {
                boolean updated = user.updateContactInformation(
                        emailField.getText().trim(),
                        phoneField.getText().trim(),
                        addressField.getText().trim()
                );
                showInfo(updated ? "Contact information updated successfully." : "No changes were saved.");
            } catch (Exception ex) {
                showInfo("Update failed: " + ex.getMessage());
            }
        });

        VBox form = new VBox(10,
                new Label("Email"), emailField,
                new Label("Phone"), phoneField,
                new Label("Address"), addressField,
                saveBtn
        );
        form.setPadding(new Insets(10));
        form.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-radius: 6;");
        content.getChildren().add(form);
        return content;
    }

    public static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        preparePopupForStudentTts(alert);
        alert.showAndWait();
    }

    public static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        preparePopupForStudentTts(alert);
        alert.showAndWait();
    }

    public static void preparePopupForStudentTts(Dialog<?> dialog) {
        if (dialog == null || !isStudentPopupTtsAllowed()) {
            return;
        }
        dialog.setOnShown(event -> {
            if (!isStudentPopupTtsAllowed()) {
                return;
            }
            String narration = buildDialogNarration(dialog);
            if (!narration.isBlank()) {
                KeyboardTtsService.getInstance().speakNow(narration);
            }
        });
    }

    private static boolean isStudentPopupTtsAllowed() {
        User currentUser = UserContext.getInstance().getCurrentUser();
        return currentUser != null && "Student".equals(currentUser.getAcctType());
    }

    private static String buildDialogNarration(Dialog<?> dialog) {
        StringBuilder text = new StringBuilder("Popup window. ");
        appendIfPresent(text, dialog.getHeaderText());
        appendIfPresent(text, dialog.getContentText());

        DialogPane pane = dialog.getDialogPane();
        if (pane != null) {
            appendIfPresent(text, extractReadableText(pane.getContent()));
            appendIfPresent(text, extractReadableText(pane.getExpandableContent()));

            String actions = pane.getButtonTypes().stream()
                    .map(ButtonType::getText)
                    .filter(label -> label != null && !label.isBlank())
                    .distinct()
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            if (!actions.isBlank()) {
                appendIfPresent(text, "Available actions: " + actions + ".");
            }
        }

        return text.toString().trim();
    }

    private static String extractReadableText(Node node) {
        if (node == null) {
            return "";
        }

        Set<String> parts = new LinkedHashSet<>();
        collectReadableText(node, parts);
        return String.join(" ", parts).trim();
    }

    private static void collectReadableText(Node node, Set<String> parts) {
        if (node == null) {
            return;
        }

        if (node instanceof Labeled labeled) {
            addPart(parts, labeled.getText());
        }

        if (node instanceof TextInputControl input) {
            addPart(parts, input.getPromptText());
            if (input.getText() != null && !input.getText().isBlank()) {
                addPart(parts, "Current value: " + input.getText().trim());
            }
        }

        if (node instanceof ComboBoxBase<?> comboBox) {
            Object value = comboBox.getValue();
            if (value != null) {
                addPart(parts, "Current selection: " + value);
            }
        }

        if (node instanceof ScrollPane scrollPane) {
            collectReadableText(scrollPane.getContent(), parts);
        }

        if (node instanceof TitledPane titledPane) {
            addPart(parts, titledPane.getText());
            collectReadableText(titledPane.getContent(), parts);
        }

        if (node instanceof Accordion accordion) {
            for (TitledPane pane : accordion.getPanes()) {
                collectReadableText(pane, parts);
            }
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectReadableText(child, parts);
            }
        }
    }

    private static void addPart(Set<String> parts, String value) {
        if (value == null) {
            return;
        }
        String clean = value.replace('\r', ' ').replace('\n', ' ').trim();
        if (!clean.isBlank()) {
            parts.add(clean);
        }
    }

    private static void appendIfPresent(StringBuilder builder, String value) {
        if (value == null) {
            return;
        }
        String clean = value.replace('\r', ' ').replace('\n', ' ').trim();
        if (clean.isBlank()) {
            return;
        }
        if (builder.length() > 0 && !Character.isWhitespace(builder.charAt(builder.length() - 1))) {
            builder.append(' ');
        }
        builder.append(clean);
        if (!clean.endsWith(".") && !clean.endsWith("!") && !clean.endsWith("?")) {
            builder.append('.');
        }
        builder.append(' ');
    }

    // ── Learning-path ComboBox ─────────────────────────────────────────────────

    /** A ComboBox pre-loaded with all STEM learning paths (no "All" entry). */
    public static ComboBox<String> learningPathCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(LEARNING_PATHS);
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    /** A ComboBox pre-loaded with all STEM learning paths plus an "All" entry at the top. */
    public static ComboBox<String> learningPathComboWithAll() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().add("All");
        combo.getItems().addAll(LEARNING_PATHS);
        combo.setValue("All");
        return combo;
    }

    // ── Standard scene builder ─────────────────────────────────────────────────

    /** Wraps a top bar + center content into a full 1400×900 scene with the dark theme applied. */
    public static Scene buildScene(HBox topBar, TabPane tabs) {
        applyTheme();
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setTop(topBar);
        root.setCenter(tabs);
        return new Scene(root, 1400, 900);
    }
}

