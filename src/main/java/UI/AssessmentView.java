package UI;

import UserFactory.*;
import DatabaseController.dbConnector;
import OtherComponents.Assessment;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.AccessibleRole;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;

/**
 * Assessment View - Display and manage assessments with accessibility features
 */
public class AssessmentView {

    public static Scene create(SceneRouter router) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        User currentUser = UserContext.getInstance().getCurrentUser();
        if (currentUser == null) {
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
        backBtn.setOnAction(e -> router.goToCurrentUserDashboard());

        Label title = new Label("Assessments");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        title.setAccessibleText("Assessments page");

        topBar.getChildren().addAll(backBtn, title);
        root.setTop(topBar);

        // Content
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label contentTitle = new Label("Your Assessments");
        contentTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        contentTitle.setAccessibleText("Your Assessments section");

        // Get assessments based on user type
        java.util.List<Assessment> assessments = null;
        if (currentUser instanceof Student) {
            assessments = ((Student) currentUser).getAssessmentResults();
        } else if (currentUser instanceof Educator) {
            assessments = ((Educator) currentUser).getAssessmentResults();
        }

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #0D1117;");

        VBox assessmentsVBox = new VBox(15);
        assessmentsVBox.setPadding(new Insets(10));

        if (assessments != null && !assessments.isEmpty()) {
            for (Assessment assessment : assessments) {
                VBox assessmentCard = createAssessmentDetailCard(assessment, currentUser instanceof Student, router);
                assessmentsVBox.getChildren().add(assessmentCard);
            }
        } else {
            Label noAssessments = new Label("No assessments available");
            noAssessments.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            noAssessments.setAccessibleText("You have no assessments available");
            assessmentsVBox.getChildren().add(noAssessments);
        }

        scrollPane.setContent(assessmentsVBox);
        content.getChildren().addAll(contentTitle, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.setCenter(content);

        Scene scene = new Scene(root, 1400, 900);
        return scene;
    }

    private static VBox createAssessmentDetailCard(Assessment assessment, boolean isStudent, SceneRouter router) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 8; -fx-border-color: #30363D;");
        card.setAccessibleRole(AccessibleRole.NODE);

        String moduleSubject = (assessment.getModuleSubject() == null || assessment.getModuleSubject().isBlank())
                ? "Module #" + assessment.getModuleID()
                : assessment.getModuleSubject();

        Label title = new Label(moduleSubject);
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #ffa657;");
        title.setAccessibleText("Assessment: " + moduleSubject);
        title.setWrapText(true);

        Label pathLabel = new Label("Learning Path: " + assessment.getLearningPath());
        pathLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
        pathLabel.setAccessibleText("Learning Path: " + assessment.getLearningPath());

        if (isStudent) {
            String status = assessment.isCompleted() ? "Completed" : "Pending";
            Label statusLabel = new Label("Status: " + status);
            statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: " + (assessment.isCompleted() ? "#238636" : "#f85149") + ";");
            statusLabel.setAccessibleText("Status: " + status);
            Label gradeLabel = new Label("Grade: " + (assessment.getGrade() >= 0 ? assessment.getGrade() + "%" : "Not Graded"));
            gradeLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");
            gradeLabel.setAccessibleText("Your grade: " + (assessment.getGrade() >= 0 ? assessment.getGrade() + " percent" : "Not Graded"));

            Label summaryLabel = new Label("Assessment " + assessment.getAssessmentID() + ": " + moduleSubject);
            summaryLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #c9d1d9;");

            card.getChildren().addAll(title, pathLabel, statusLabel, summaryLabel, gradeLabel);

            if (!assessment.isCompleted()) {
                Button takeBtn = new Button("Take Assessment");
                takeBtn.setStyle("-fx-font-size: 12; -fx-padding: 10; -fx-background-color: #238636; -fx-text-fill: white;");
                takeBtn.setAccessibleText("Start this assessment");
                takeBtn.setOnAction(e -> router.goToTakeAssessment(assessment));
                card.getChildren().add(takeBtn);
            }
        } else {
            HBox buttonBox = new HBox(10);
            Button editBtn = new Button("Edit");
            Button viewResultsBtn = new Button("View Results");

            editBtn.setOnAction(e -> {
                TextInputDialog editDialog = new TextInputDialog(assessment.getContent());
                editDialog.setTitle("Edit Assessment");
                editDialog.setHeaderText("Update assessment content");
                editDialog.setContentText("Content:");
                editDialog.showAndWait().ifPresent(updatedContent -> {
                    if (updatedContent.trim().isEmpty()) {
                        showInfo("Assessment content cannot be empty.");
                        return;
                    }
                    boolean updated = new dbConnector().updateAssessmentContent(assessment.getAssessmentID(), updatedContent.trim());
                    showInfo(updated ? "Assessment updated." : "Assessment update failed.");
                    if (updated) {
                        router.goToAssessments();
                    }
                });
            });

            viewResultsBtn.setOnAction(e -> {
                java.util.List<Assessment> moduleAssessments = new dbConnector().searchAssessmentDB(assessment.getModuleID(), "Educator");
                long gradedCount = moduleAssessments.stream().filter(a -> a.getGrade() >= 0).count();
                showInfo("Module #" + assessment.getModuleID() + " results: " + gradedCount + " graded submissions out of " + moduleAssessments.size());
            });
            buttonBox.getChildren().addAll(editBtn, viewResultsBtn);

            Label educatorStatusLabel = new Label("Assessment ID: " + assessment.getAssessmentID());
            educatorStatusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
            educatorStatusLabel.setAccessibleText("Assessment ID: " + assessment.getAssessmentID());

            Node renderedContent = AssessmentFormRenderer.createPreview(assessment.getContent());
            card.getChildren().addAll(title, pathLabel, educatorStatusLabel, renderedContent, buttonBox);
        }

        return card;
    }

    private static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
