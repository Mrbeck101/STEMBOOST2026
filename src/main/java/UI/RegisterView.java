package UI;

import Services.AuthService;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.application.Application;

public class RegisterView {

    public static Scene create(SceneRouter router) {

        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

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
        acctType.getItems().addAll("Educator", "Student", "Parent");
        acctType.setMaxWidth(Double.MAX_VALUE);

        TextField studentId = new TextField();
        Label studentIdLabel = new Label("Associated Student ID");

        studentIdLabel.setVisible(false);
        studentId.setVisible(false);

        acctType.setOnAction(e -> {
            boolean isParent = "Parent".equals(acctType.getValue());
            studentIdLabel.setVisible(isParent);
            studentId.setVisible(isParent);

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
        backToLogin.setOnAction(e -> router.goToLogin());

        // ================= KEYBOARD NAV =================
        Control[] order = {
                firstName,
                lastName,
                acctType,
                studentId,
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

        acctType.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && acctType.getValue() != null) {
                email.requestFocus();
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

                new Label("Email"), email,
                new Label("Password"), password,
                new Label("Confirm Password"), confirm,

                registerBtn,
                backToLogin,
                error
        );

        root.setCenter(scrollPane);


        Scene scene = new Scene(root, 1280, 800);

        scene.getStylesheets().add(
                RegisterView.class.getResource("/register_styles.css").toExternalForm()
        );
        //scene.setOnShown(e -> firstName.requestFocus());

        return scene;
    }
}