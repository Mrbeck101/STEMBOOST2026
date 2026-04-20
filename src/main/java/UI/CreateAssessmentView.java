package UI;

import UserFactory.Educator;
import OtherComponents.Assessment;
import OtherComponents.AssessmentForm;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import javafx.util.StringConverter;

import java.util.List;

/**
 * Create Assessment View - For educators to create assessments for modules
 */
public class CreateAssessmentView {

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

        Label title = new Label("Create New Assessment");
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

        Label formTitle = new Label("Assessment Details");
        formTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        // Associated Module field
        Label moduleLabel = new Label("Module Group *");
        moduleLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #c9d1d9;");
        ComboBox<OtherComponents.LearningModule> moduleCombo = new ComboBox<>();
        List<OtherComponents.LearningModule> modules = educator.getLearningModules();
        moduleCombo.getItems().addAll(modules);
        moduleCombo.setPromptText("Select one of your modules");
        moduleCombo.setMaxWidth(Double.MAX_VALUE);
        moduleCombo.setStyle("-fx-font-size: 12;");
        moduleLabel.setLabelFor(moduleCombo);
        moduleCombo.setAccessibleText("Module group selection");
        moduleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(OtherComponents.LearningModule module) {
                if (module == null) {
                    return "";
                }
                return module.getSubject() + " (Module ID: " + module.getModuleID() + ")";
            }

            @Override
            public OtherComponents.LearningModule fromString(String string) {
                return null;
            }
        });

        Label pathPreviewLabel = new Label("Learning Path: Select a module");
        pathPreviewLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
        moduleCombo.setOnAction(e -> {
            OtherComponents.LearningModule selectedModule = moduleCombo.getValue();
            if (selectedModule != null) {
                pathPreviewLabel.setText("Learning Path: " + selectedModule.getLearningPath());
            }
        });

        // Assessment builder
        Label contentLabel = new Label("Assessment Questions *");
        contentLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #c9d1d9;");
        AssessmentFormBuilder formBuilder = new AssessmentFormBuilder();

        // Difficulty level
        Label difficultyLabel = new Label("Difficulty Level");
        difficultyLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #c9d1d9;");
        ComboBox<String> difficultyCombo = new ComboBox<>();
        difficultyCombo.getItems().addAll("Easy", "Medium", "Hard", "Expert");
        difficultyCombo.setMaxWidth(Double.MAX_VALUE);
        difficultyCombo.setStyle("-fx-font-size: 12;");
        difficultyLabel.setLabelFor(difficultyCombo);
        difficultyCombo.setAccessibleText("Difficulty level selection");

        // Button box
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button createBtn = new Button("Create Assessment");
        createBtn.setStyle("-fx-font-size: 12; -fx-padding: 10 30 10 30;");
        createBtn.setAccessibleText("Create this assessment");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-font-size: 12;");
        cancelBtn.setAccessibleText("Cancel and go back");
        cancelBtn.setOnAction(e -> router.goToDashboard(educator.getId(), "Educator"));

        buttonBox.getChildren().addAll(cancelBtn, createBtn);

        form.getChildren().addAll(
                formTitle,
                moduleLabel, moduleCombo,
                pathPreviewLabel,
                contentLabel, formBuilder.getView(),
                difficultyLabel, difficultyCombo,
                buttonBox
        );

        // Create button action
        createBtn.setOnAction(e -> {
            OtherComponents.LearningModule selectedModule = moduleCombo.getValue();

            if (selectedModule == null) {
                showAlert("Validation Error", "Please fill in all required fields");
                return;
            }

            try {
                AssessmentForm formContent = formBuilder.buildForm();
                Assessment newAssessment = new Assessment(
                        0,
                        selectedModule.getModuleID(),
                        -1,
                        selectedModule.getLearningPath(),
                        selectedModule.getSubject(),
                        formContent.toJson(),
                        false
                );
                educator.addAssessment(newAssessment);
                showAlert("Success", "Assessment created and saved successfully!");
                router.goToDashboard(educator.getId(), "Educator");
            } catch (Exception ex) {
                showAlert("Error", "Failed to save assessment: " + ex.getMessage());
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
        UIComponents.showAlert(title, message);
    }
}
