package UI;

import Services.AuthService;
import Services.KeyboardTtsService;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.application.Application;
import javafx.application.Platform;

public class RegisterView {

    //private static final String INTRO_TTS = "Welcome To Stem Boost, a learning assistance app for the visually impaired to learn more about stem. To disable dictation at any time you can press the f1 key.";
    private static final String REGISTER_PAGE_TTS = "You are currently on the register page. Complete the form fields to create your account.";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    public static Scene create(SceneRouter router) {

        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setFocusTraversable(true);

        // ================= CARD =================
        VBox card = new VBox(16);
        card.setAlignment(Pos.TOP_LEFT);
        card.setMaxWidth(480);
        card.setPadding(new Insets(40));
        card.getStyleClass().add("card");
        card.setMaxWidth(480);

        VBox container = new VBox(card);
        container.setAlignment(Pos.TOP_CENTER);
        // ================= SCROLL PANE WRAPPER =================
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setMaxWidth(Double.MAX_VALUE);

        scrollPane.getStyleClass().add("register-scroll");

        // Ensure smooth UX
        scrollPane.setFocusTraversable(false);

        // ================= HEADER =================
        Label appTitle = new Label("STEMBOOST");
        appTitle.getStyleClass().add("title");
        appTitle.setMaxWidth(Double.MAX_VALUE);
        appTitle.setAlignment(Pos.CENTER);

        Label title = new Label("Create Account");
        title.getStyleClass().add("subtitle");

        // ================= FIELDS =================
        TextField firstName = new TextField();
        TextField lastName = new TextField();

        ComboBox<String> acctType = new ComboBox<>();
        acctType.getItems().addAll("Educator", "Student", "Parent", "Employer");
        acctType.setMaxWidth(Double.MAX_VALUE);

        TextField studentId = new TextField();
        Label studentIdLabel = new Label("Associated Student ID");

        TextField companyField = new TextField();
        Label companyLabel = new Label("Company Name");

        studentIdLabel.setVisible(false);
        studentId.setVisible(false);
        companyLabel.setVisible(false);
        companyField.setVisible(false);

        acctType.setOnAction(e -> {
            boolean isParent = "Parent".equals(acctType.getValue());
            boolean isEmployer = "Employer".equals(acctType.getValue());
            studentIdLabel.setVisible(isParent);
            studentId.setVisible(isParent);
            companyLabel.setVisible(isEmployer);
            companyField.setVisible(isEmployer);

            if (isParent) studentId.requestFocus();
        });

        TextField email = new TextField();
        PasswordField password = new PasswordField();
        PasswordField confirm = new PasswordField();

        Button registerBtn = new Button("Create Account");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setDefaultButton(true);

        Hyperlink backToLogin = new Hyperlink("Back to login");

        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setWrapText(true);

        // ================= LOGIC =================
        Runnable registerAction = () -> {
            KeyboardTtsService tts = KeyboardTtsService.getInstance();
            String validationError = validateRegistration(firstName, lastName, acctType, studentId, email, password, confirm);
            if (validationError != null) {
                error.setText(validationError);
                tts.speakNow(validationError);
                error.requestFocus();
                return;
            }

            Integer associatedStudentId = null;
            if ("Parent".equals(acctType.getValue())) {
                try {
                    associatedStudentId = Integer.parseInt(studentId.getText().trim());
                } catch (NumberFormatException ex) {
                    error.setText("Associated Student ID must be a valid number");
                    tts.speakNow("Associated Student ID must be a valid number");
                    error.requestFocus();
                    return;
                }
            }

            if (!password.getText().equals(confirm.getText())) {
                error.setText("Passwords do not match");
                tts.speakNow("Passwords do not match");
                error.requestFocus();
                return;
            }

            boolean success = AuthService.register(
                    email.getText(),
                    password.getText(),
                    firstName.getText(),
                    lastName.getText(),
                    acctType.getValue(),
                    companyField.getText(),
                    associatedStudentId,
                    null
            );

            if (success) {
                error.setText("Account Created");
                UIComponents.showAlert("Registration Successful", "Successfully registered.");
                router.goToLogin();
            } else {
                String emailText = email.getText() == null ? "" : email.getText().trim();
                if (AuthService.emailAlreadyRegistered(emailText)) {
                    error.setText("An account with this email already exists.");
                } else {
                    error.setText("Registration failed. Please review your information and try again.");
                }
                tts.speakNow(error.getText());
                error.requestFocus();
            }
        };

        registerBtn.setOnAction(e -> registerAction.run());
        backToLogin.setOnAction(e -> router.goToLogin());

        // ================= KEYBOARD NAV =================
        Control[] order = {
                firstName,
                lastName,
                acctType,
                studentId,
                companyField,
                email,
                password,
                confirm,
                registerBtn,
                backToLogin
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

        firstName.setOnAction(e -> lastName.requestFocus());
        lastName.setOnAction(e -> acctType.requestFocus());
        email.setOnAction(e -> password.requestFocus());
        password.setOnAction(e -> confirm.requestFocus());
        confirm.setOnAction(e -> registerAction.run());

        addFocusNarration(firstName, "You are currently in the First name Field");
        addFocusNarration(lastName, "You are currently in the Last name Field");
        addFocusNarration(acctType, "You are in the account type field, you can use up and down arrows to navigate");
        addFocusNarration(email, "you are currently in the Email field");
        addFocusNarration(password, "you are currently in the password field");
        addFocusNarration(confirm, "you are currently in the confirm password field");
        addFocusNarration(registerBtn, "Press enter to create your account");

        acctType.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (acctType.isFocused() && newValue != null && !newValue.isBlank()) {
                KeyboardTtsService.getInstance().speakNow(newValue);
            }
        });

        // ================= LAYOUT =================
        card.getChildren().addAll(
                appTitle,
                title,

                new Label("First Name"), firstName,
                new Label("Last Name"), lastName,

                new Label("Account Type"), acctType,

                studentIdLabel, studentId,

                companyLabel, companyField,

                new Label("Email"), email,
                new Label("Password"), password,
                new Label("Confirm Password"), confirm,

                registerBtn,
                backToLogin,
                error
        );

        root.setCenter(scrollPane);


        Scene scene = new Scene(root, 1280, 800);
        Platform.runLater(root::requestFocus);

        try {
            if (RegisterView.class.getResource("/register_styles.css") != null) {
                scene.getStylesheets().add(
                        RegisterView.class.getResource("/register_styles.css").toExternalForm()
                );
            }
        } catch (Exception e) {
            // CSS file not found, continue without styles
            System.err.println("Warning: register_styles.css not found");
        }
        //scene.setOnShown(e -> firstName.requestFocus());

        KeyboardTtsService.getInstance().bindScene(
                scene,
                KeyboardTtsService.AccessMode.PUBLIC_TOGGLE,
                () -> new KeyboardTtsService.ReadingContent(
                        REGISTER_PAGE_TTS
                )
                //() -> KeyboardTtsService.getInstance().speakNow(INTRO_TTS, firstName::requestFocus)
        );

        return scene;
    }

    private static void addFocusNarration(Control control, String message) {
        control.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                KeyboardTtsService.getInstance().speakNow(message);
            }
        });
    }

    private static String validateRegistration(TextField firstName,
                                               TextField lastName,
                                               ComboBox<String> acctType,
                                               TextField studentId,
                                               TextField email,
                                               PasswordField password,
                                               PasswordField confirm) {
        if (firstName.getText() == null || firstName.getText().trim().isEmpty()) {
            return "First name is required";
        }
        if (lastName.getText() == null || lastName.getText().trim().isEmpty()) {
            return "Last name is required";
        }
        if (acctType.getValue() == null || acctType.getValue().trim().isEmpty()) {
            return "Please select an account type";
        }
        if ("Parent".equals(acctType.getValue())) {
            String rawId = studentId.getText() == null ? "" : studentId.getText().trim();
            if (rawId.isEmpty()) {
                return "Associated Student ID is required for parent accounts";
            }
            try {
                int id = Integer.parseInt(rawId);
                if (id <= 0) {
                    return "Associated Student ID must be a valid ID";
                }
            } catch (NumberFormatException ex) {
                return "Associated Student ID must be a valid ID";
            }
        }
        if (email.getText() == null || email.getText().trim().isEmpty()) {
            return "Email is required";
        }
        if (!email.getText().trim().matches(EMAIL_PATTERN)) {
            return "Please enter a valid email address";
        }
        if (password.getText() == null || password.getText().trim().isEmpty()) {
            return "Password is required";
        }
        if (confirm.getText() == null || confirm.getText().trim().isEmpty()) {
            return "Please confirm your password";
        }
        if (!password.getText().equals(confirm.getText())) {
            return "Passwords do not match";
        }
        return null;
    }
}

