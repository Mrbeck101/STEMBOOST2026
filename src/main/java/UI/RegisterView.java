package UI;

import Services.AuthService;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

public class RegisterView {

    public static Scene create(SceneRouter router) {

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f172a, #1e293b);");

        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(400);
        card.setPadding(new Insets(40));
        card.setStyle("-fx-background-color: #111827; -fx-background-radius: 12;");

        Label appTitle = new Label("Stemboost");
        appTitle.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 32px; -fx-font-weight: bold;");

        Label title = new Label("Register");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");

        // First Name
        Label firstNameLabel = new Label("First Name");
        firstNameLabel.setStyle("-fx-text-fill: white;");
        TextField firstName = new TextField();
        firstName.setPrefHeight(40);
        firstNameLabel.setLabelFor(firstName);

        // Last Name
        Label lastNameLabel = new Label("Last Name");
        lastNameLabel.setStyle("-fx-text-fill: white;");
        TextField lastName = new TextField();
        lastName.setPrefHeight(40);
        lastNameLabel.setLabelFor(lastName);

        // Account Type
        Label acctTypeLabel = new Label("Account Type");
        acctTypeLabel.setStyle("-fx-text-fill: white;");
        ComboBox<String> acctType = new ComboBox<>();
        acctType.getItems().addAll("Educator", "Student", "Parent");
        acctType.setPrefHeight(40);
        acctType.setMaxWidth(Double.MAX_VALUE);

        // Associated Student ID (only for Parent)
        Label studentIdLabel = new Label("Associated Student ID");
        studentIdLabel.setStyle("-fx-text-fill: white;");
        TextField studentId = new TextField();
        studentId.setPrefHeight(40);
        studentIdLabel.setLabelFor(studentId);

        // Hide initially
        studentIdLabel.setVisible(false);
        studentId.setVisible(false);

        // Toggle visibility based on user type
        acctType.setOnAction(e -> {
            boolean isParent = "Parent".equals(acctType.getValue());
            studentIdLabel.setVisible(isParent);
            studentId.setVisible(isParent);
        });

        // Email
        Label emailLabel = new Label("Email");
        emailLabel.setStyle("-fx-text-fill: white;");
        TextField email = new TextField();
        email.setPrefHeight(40);
        emailLabel.setLabelFor(email);

        // Password
        Label passwordLabel = new Label("Password");
        passwordLabel.setStyle("-fx-text-fill: white;");
        PasswordField password = new PasswordField();
        password.setPrefHeight(40);
        passwordLabel.setLabelFor(password);

        // Confirm
        Label confirmLabel = new Label("Confirm Password");
        confirmLabel.setStyle("-fx-text-fill: white;");
        PasswordField confirm = new PasswordField();
        confirm.setPrefHeight(40);
        confirmLabel.setLabelFor(confirm);

        Button registerBtn = new Button("Register");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setDefaultButton(true);

        Label error = new Label();
        error.setStyle("-fx-text-fill: #f87171;");
        error.setWrapText(true);

        Runnable registerAction = () -> {

            if (!password.getText().equals(confirm.getText())) {
                error.setText("Passwords do not match");
                error.requestFocus();
                return;
            }

            boolean success = AuthService.register(
                    email.getText(),
                    password.getText(),
                    firstName.getText(),
                    lastName.getText(),
                    acctType.getValue()
            );

            if (success) {
                router.goToLogin();
            } else {
                error.setText("Registration failed");
                error.requestFocus();
            }
        };

        registerBtn.setOnAction(e -> registerAction.run());

        // Keyboard navigation
        firstName.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) lastName.requestFocus();
        });

        lastName.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) acctType.requestFocus();
        });

        email.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) password.requestFocus();
        });

        password.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) confirm.requestFocus();
        });

        confirm.setOnKeyPressed(e -> {
//            if (e.getCode() == KeyCode.ENTER) registerAction.run();
        });

        card.getChildren().addAll(
                appTitle,
                title,

                firstNameLabel, firstName,
                lastNameLabel, lastName,

                acctTypeLabel, acctType,
                studentIdLabel, studentId,

                emailLabel, email,
                passwordLabel, password,
                confirmLabel, confirm,

                registerBtn,
                error
        );

        root.getChildren().add(card);

        Scene scene = new Scene(root, 1980, 1080);

        return scene;
    }
}