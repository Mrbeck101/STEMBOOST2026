package UI;

import Services.AuthService;
import Services.KeyboardTtsService;
import UserFactory.*;
import UserFactory.Parent;
import atlantafx.base.theme.PrimerDark; // AtlantaFX theme
import eu.hansolo.toolbox.tuples.Pair;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.application.Application;

public class LoginView {

    public static Scene create(SceneRouter router) {

        // Apply AtlantaFX theme
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());


        StackPane root = new StackPane();
        root.getStyleClass().add("root");

        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(420);
        card.setPadding(new Insets(40));
        card.getStyleClass().add("card");

        Label appTitle = new Label("STEMBOOST");
        appTitle.getStyleClass().add("title");
        appTitle.setMaxWidth(Double.MAX_VALUE);
        appTitle.setAlignment(Pos.CENTER); // centers text
        appTitle.setAccessibleText("Stemboost application title");

        Label subtitle = new Label("Login to your account");
        subtitle.getStyleClass().add("subtitle");

        // Email
        Label emailLabel = new Label("Email");
        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");
        emailLabel.setLabelFor(emailField);
        emailField.setAccessibleText("Email input field");

        // Password
        Label passwordLabel = new Label("Password");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordLabel.setLabelFor(passwordField);
        passwordField.setAccessibleText("Password input field");

        Button loginBtn = new Button("Login");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setDefaultButton(true);

        Hyperlink registerLink = new Hyperlink("Create an account");
        registerLink.setAccessibleText("Go to registration page");

        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setWrapText(true);

        // Login logic
        Runnable loginAction = () -> {
            Pair<Integer, String> results = AuthService.login(
                    emailField.getText(),
                    passwordField.getText()
            );

            if (results.getA() != -1) {
                try {
                    // Create appropriate user instance based on account type
                    User user = createUserInstance(results.getA(), results.getB());

                    if (user != null) {
                        // Store user in persistent context
                        UserContext.getInstance().setCurrentUser(user);

                        // Check if student needs to set learning path
                        if (user instanceof UserFactory.Student) {
                            UserFactory.Student student = (UserFactory.Student) user;
                            if (student.getLearningPath() == null || student.getLearningPath().equals("Unknown")) {
                                router.goToLearningPathSelection();
                                return;
                            }
                        }

                        router.goToDashboard(results.getA(), results.getB());
                    } else {
                        error.setText("Failed to load user profile");
                        error.requestFocus();
                    }
                } catch (Exception e) {
                    error.setText("Error: " + e.getMessage());
                    error.requestFocus();
                }
            } else {
                error.setText("Invalid email or password");
                error.requestFocus(); // screen reader announces error
            }
        };

        loginBtn.setOnAction(e -> loginAction.run());

        // KEYBOARD NAVIGATION (Tab already works automatically)

        // Arrow navigation helper
        Control[] order = {
                emailField,
                passwordField,
                loginBtn,
                registerLink
        };

        for (int i = 0; i < order.length; i++) {
            final int index = i;

            order[i].setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.DOWN) {
                    if (index < order.length - 1) {
                        order[index + 1].requestFocus();
                    }
                } else if (e.getCode() == KeyCode.UP) {
                    if (index > 0) {
                        order[index - 1].requestFocus();
                    }
                }
            });
        }

        // ENTER behavior
        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> loginAction.run());

        registerLink.setOnAction(e -> router.goToRegister());

        // Layout
        VBox.setMargin(loginBtn, new Insets(10, 0, 0, 0));
        VBox.setMargin(registerLink, new Insets(5, 0, 0, 0));

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

        Scene scene = new Scene(root, 1280, 800);

        try {
            if (LoginView.class.getResource("/login_styles.css") != null) {
                scene.getStylesheets().add(
                        LoginView.class.getResource("/login_styles.css").toExternalForm()
                );
            }
        } catch (Exception e) {
            // CSS file not found, continue without styles
            System.err.println("Warning: login_styles.css not found");
        }
        // Initial focus (important for accessibility)
        //scene.setOnShown(e -> emailField.requestFocus());

        KeyboardTtsService.getInstance().bindScene(
                scene,
                KeyboardTtsService.AccessMode.PUBLIC_TOGGLE,
                () -> new KeyboardTtsService.ReadingContent(
                        "Login screen. Enter your email and password, then activate the login button. " +
                        "Press F1 to toggle text to speech on or off."
                )
        );

        return scene;
    }

    /**
     * Creates a user instance based on account type
     */
    private static User createUserInstance(int userId, String accountType) {
        try {
            return switch (accountType) {
                case "Student" -> new Student(userId);
                case "Educator" -> new Educator(userId);
                case "Counselor" -> new Counselor(userId);
                case "Parent" -> new Parent(userId);
                case "Employer" -> new Employer(userId);
                case "University" -> new University(userId);
                case "Admin" -> new Admin(userId);
                default -> null;
            };
        } catch (Exception e) {
            System.err.println("Error creating user instance: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}