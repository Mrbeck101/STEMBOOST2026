package UI;

import UserFactory.User;
import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

