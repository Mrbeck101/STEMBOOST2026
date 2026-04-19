package UI;

import UserFactory.Educator;
import OtherComponents.LearningModule;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;

/**
 * Create Module View - For educators to create new learning modules
 */
public class CreateModuleView {

    public static Scene create(SceneRouter router) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Educator educator = (Educator) UserContext.getInstance().getCurrentUser();
        if (educator == null || !educator.getAcctType().equals("Educator")) {
            return LoginView.create(router);
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Top bar
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-font-size: 12;");
        backBtn.setOnAction(e -> router.goToDashboard(educator.getId(), "Educator"));

        Label title = new Label("Create New Module");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        topBar.getChildren().addAll(backBtn, title);
        root.setTop(topBar);

        // Content
        VBox content = new VBox(20);
        content.setPadding(new Insets(40));
        content.setStyle("-fx-background-color: #0D1117;");
        content.setAlignment(Pos.TOP_CENTER);

        VBox form = new VBox(18);
        form.setPadding(new Insets(30));
        form.setStyle("-fx-background-color: #161B22; -fx-border-radius: 8;");
        form.setMaxWidth(700);

        Label formTitle = new Label("Module Details");
        formTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        // Subject field
        Label subjectLabel = new Label("Subject *");
        subjectLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #c9d1d9;");
        TextField subjectField = new TextField();
        subjectField.setPromptText("e.g., Introduction to Java");
        subjectField.setStyle("-fx-font-size: 12;");
        subjectLabel.setLabelFor(subjectField);
        subjectField.setAccessibleText("Subject input field");

        // Learning Path field
        Label pathLabel = new Label("Learning Path *");
        pathLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #c9d1d9;");
        ComboBox<String> pathCombo = new ComboBox<>();
        pathCombo.getItems().addAll("Electrical Engineering", "Software Engineering", "Information Technology", "Cybersecurity", "Computer Engineering", "Artificial Intelligence");
        pathCombo.setMaxWidth(Double.MAX_VALUE);
        pathCombo.setStyle("-fx-font-size: 12;");
        pathLabel.setLabelFor(pathCombo);
        pathCombo.setAccessibleText("Learning path selection");

        // Content field
        Label contentLabel = new Label("Module Content *");
        contentLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #c9d1d9;");
        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Enter the module content here...");
        contentArea.setWrapText(true);
        contentArea.setPrefRowCount(12);
        contentArea.setStyle("-fx-font-size: 11; -fx-control-inner-background: #0D1117; -fx-text-fill: #c9d1d9;");
        contentLabel.setLabelFor(contentArea);
        contentArea.setAccessibleText("Module content textarea");

        // Button box
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button createBtn = new Button("Create Module");
        createBtn.setStyle("-fx-font-size: 12; -fx-padding: 10 30 10 30;");
        createBtn.setAccessibleText("Create this module");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-font-size: 12;");
        cancelBtn.setAccessibleText("Cancel and go back");
        cancelBtn.setOnAction(e -> router.goToDashboard(educator.getId(), "Educator"));

        buttonBox.getChildren().addAll(cancelBtn, createBtn);

        form.getChildren().addAll(
                formTitle,
                subjectLabel, subjectField,
                pathLabel, pathCombo,
                contentLabel, contentArea,
                buttonBox
        );

        // Create button action
        createBtn.setOnAction(e -> {
            String subject = subjectField.getText().trim();
            String path = pathCombo.getValue();
            String contentText = contentArea.getText().trim();

            if (subject.isEmpty() || path == null || contentText.isEmpty()) {
                showAlert("Validation Error", "Please fill in all required fields");
                return;
            }

            // Create module
            LearningModule newModule = new LearningModule(
                    0, // ID will be assigned by database
                    0,
                    educator.getId(),
                    path,
                    contentText,
                    subject
            );

            try {
                educator.addModule(newModule);
                showAlert("Success", "Module created and saved successfully!");
                router.goToDashboard(educator.getId(), "Educator");
            } catch (Exception ex) {
                showAlert("Error", "Failed to save module: " + ex.getMessage());
            }
        });

        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #0D1117;");

        content.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.setCenter(content);

        Scene scene = new Scene(root, 1400, 900);
        return scene;
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
