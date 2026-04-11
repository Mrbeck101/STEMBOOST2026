package UI;

import Services.AuthService;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

public class LoginView {

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
        appTitle.setAccessibleText("Stemboost application");

        Label subtitle = new Label("Login");
        subtitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");

        // Email
        Label emailLabel = new Label("Email");
        emailLabel.setStyle("-fx-text-fill: white;");
        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");
        emailField.setPrefHeight(40);
        emailLabel.setLabelFor(emailField);
        emailField.setAccessibleText("Email input field");

        // Password
        Label passwordLabel = new Label("Password");
        passwordLabel.setStyle("-fx-text-fill: white;");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setPrefHeight(40);
        passwordLabel.setLabelFor(passwordField);
        passwordField.setAccessibleText("Password input field");

        Button loginBtn = new Button("Login");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setDefaultButton(true); // ENTER key support

        Label registerLink = new Label("Press Enter to Register if you do not have an account");
        registerLink.setStyle("-fx-text-fill: #93c5fd;");
        registerLink.setFocusTraversable(true);

        Label error = new Label();
        error.setStyle("-fx-text-fill: #f87171;");
        error.setWrapText(true);
        error.setAccessibleText("Error message");

        // Action logic
        Runnable loginAction = () -> {
            boolean success = AuthService.login(
                    emailField.getText(),
                    passwordField.getText()
            );

            if (success) {
                router.goToDashboard();
                System.out.println("Successfully logged in");
            } else {
                error.setText("Invalid email or password");
                System.out.println("Invalid email & password");
                error.requestFocus(); // announce error
            }
        };

        loginBtn.setOnAction(e -> loginAction.run());

        // ENTER key navigation
        emailField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                passwordField.requestFocus();
            }
        });

        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                loginAction.run();
            }
        });

        registerLink.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                router.goToRegister();
            }
        });

        registerLink.setOnMouseClicked(e -> router.goToRegister());

        // Focus order
        card.getChildren().addAll(
                appTitle,
                subtitle,
                emailLabel, emailField,
                passwordLabel, passwordField,
                loginBtn,
                registerLink,
                error
        );

        root.getChildren().add(card);

        Scene scene = new Scene(root, 1980, 1080);

//        // Set initial focus
//        scene.setOnShown(e -> emailField.requestFocus());

        return scene;
    }
}
