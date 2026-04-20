package UI;

import UserFactory.User;
import atlantafx.base.theme.PrimerDark;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.HashMap;

public class ProfileView {

    public static Scene create(SceneRouter router) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        User currentUser = UserContext.getInstance().getCurrentUser();
        if (currentUser == null) {
            return LoginView.create(router);
        }

        BorderPane root = new BorderPane();

        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("<- Back");
        backBtn.setOnAction(e -> router.goToDashboard(currentUser.getId(), currentUser.getAcctType()));

        Label title = new Label("Update Contact Information");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        topBar.getChildren().addAll(backBtn, title);
        root.setTop(topBar);

        VBox form = new VBox(12);
        form.setPadding(new Insets(25));
        form.setStyle("-fx-background-color: #0D1117;");

        TextField emailField = new TextField(currentUser.getEmail());
        TextField phoneField = new TextField(currentUser.getPhone());
        TextArea addressField = new TextArea(currentUser.getAddress());
        addressField.setPrefRowCount(3);

        Button saveBtn = new Button("Save Changes");
        saveBtn.setOnAction(e -> {
            try {
                boolean updated = currentUser.updateContactInformation(
                        emailField.getText().trim(),
                        phoneField.getText().trim(),
                        addressField.getText().trim()
                );
                if (updated) {
                    showAlert("Success", "Contact information updated.");
                } else {
                    showAlert("Error", "No rows were updated.");
                }
            } catch (Exception ex) {
                showAlert("Error", "Update failed: " + ex.getMessage());
            }
        });

        form.getChildren().addAll(
                new Label("Email"), emailField,
                new Label("Phone"), phoneField,
                new Label("Address"), addressField,
                saveBtn
        );

        root.setCenter(form);
        return new Scene(root, 1100, 700);
    }

    private static void showAlert(String title, String message) {
        UIComponents.showAlert(title, message);
    }

    /**
     * Read-only view of another user's profile (e.g. counselor viewing a student).
     */
    public static Scene createReadOnly(SceneRouter router, int targetUserId) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        User currentUser = UserContext.getInstance().getCurrentUser();
        HashMap<String, Object> data = currentUser == null ? null : currentUser.getAccountSummary(targetUserId);

        BorderPane root = new BorderPane();

        // ── Top bar ──────────────────────────────────────────────────────────
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("<- Back");
        backBtn.setOnAction(e -> router.goToCurrentUserDashboard());

        String displayName = data != null ? (String) data.get("name") : "Student #" + targetUserId;
        Label title = new Label("Profile: " + displayName);
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        topBar.getChildren().addAll(backBtn, title);
        root.setTop(topBar);

        // ── Body ─────────────────────────────────────────────────────────────
        VBox body = new VBox(16);
        body.setPadding(new Insets(25));
        body.setStyle("-fx-background-color: #0D1117;");

        if (data == null) {
            Label err = new Label("Could not load profile for user #" + targetUserId);
            err.setStyle("-fx-text-fill: #ff7b72;");
            body.getChildren().add(err);
        } else {
            // Parse contact_info JSON
            String email = "", phone = "", address = "";
            try {
                String ci = (String) data.get("contactInfo");
                if (ci != null && !ci.isBlank()) {
                    JsonObject json = new Gson().fromJson(ci, JsonObject.class);
                    email   = json.has("email")   ? json.get("email").getAsString()   : "";
                    phone   = json.has("phone")   ? json.get("phone").getAsString()   : "";
                    address = json.has("address") ? json.get("address").getAsString() : "";
                }
            } catch (Exception ignored) {}

            String acctType    = (String) data.getOrDefault("acctType", "");
            String learningPath = (String) data.getOrDefault("learningPath", "");

            body.getChildren().addAll(
                    infoRow("Name",          displayName),
                    infoRow("Account Type",  acctType),
                    infoRow("Learning Path", learningPath != null && !learningPath.isBlank() ? learningPath : "—"),
                    infoRow("Email",         email.isBlank() ? "—" : email),
                    infoRow("Phone",         phone.isBlank() ? "—" : phone),
                    infoRow("Address",       address.isBlank() ? "—" : address)
            );
        }

        root.setCenter(body);
        return new Scene(root, 1100, 700);
    }

    private static HBox infoRow(String label, String value) {
        Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #8b949e; -fx-min-width: 130;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: #c9d1d9;");
        HBox row = new HBox(10, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10));
        row.setStyle("-fx-background-color: #161B22; -fx-border-radius: 4;");
        return row;
    }
}

