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
        backBtn.setOnAction(e -> {
            String userType = currentUser.getAcctType();
            switch (userType) {
                case "Student" -> router.goToDashboard(currentUser.getId(), "Student");
                case "Educator" -> router.goToDashboard(currentUser.getId(), "Educator");
                default -> router.goToLogin();
            }
        });

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
                VBox assessmentCard = createAssessmentDetailCard(assessment, currentUser instanceof Student);
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

    private static VBox createAssessmentDetailCard(Assessment assessment, boolean isStudent) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 8; -fx-border-color: #30363D;");
        card.setAccessibleRole(AccessibleRole.NODE);

        Label title = new Label(assessment.getLearningPath());
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #ffa657;");
        title.setAccessibleText("Assessment: " + assessment.getLearningPath());
        title.setWrapText(true);

        Label statusLabel;
        if (isStudent) {
            String status = assessment.getGrade() >= 0 ? "Completed" : "Pending";
            statusLabel = new Label("Status: " + status);
            statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: " + (assessment.getGrade() >= 0 ? "#238636" : "#f85149") + ";");
            statusLabel.setAccessibleText("Status: " + status);
        } else {
            statusLabel = new Label("Assessment ID: " + assessment.getAssessmentID());
            statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
            statusLabel.setAccessibleText("Assessment ID: " + assessment.getAssessmentID());
        }

        Label content = new Label(assessment.getContent());
        content.setStyle("-fx-font-size: 12; -fx-text-fill: #c9d1d9; -fx-wrap-text: true;");
        content.setWrapText(true);
        content.setAccessibleText("Assessment content: " + assessment.getContent());

        if (isStudent) {
            Label gradeLabel = new Label("Grade: " + (assessment.getGrade() >= 0 ? assessment.getGrade() + "%" : "Not Graded"));
            gradeLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");
            gradeLabel.setAccessibleText("Your grade: " + (assessment.getGrade() >= 0 ? assessment.getGrade() + " percent" : "Not Graded"));

            Button takeBtn = new Button("Take Assessment");
            takeBtn.setStyle("-fx-font-size: 12; -fx-padding: 10;");
            takeBtn.setAccessibleText("Start this assessment");
            takeBtn.setOnAction(e -> {
                User user = UserContext.getInstance().getCurrentUser();
                String responses = AssessmentFormRenderer.collectStudentResponses(assessment);
                if (responses == null) {
                    return;
                }

                try {
                    Integer educatorId = new dbConnector().findEducatorForModule(assessment.getModuleID());
                    if (educatorId == null) {
                        showInfo("Could not locate the educator for this assessment.");
                        return;
                    }
                    boolean sent = user.sendMessage(educatorId, "Assessment Submission", responses);
                    showInfo(sent ? "Assessment responses submitted to the educator." : "Failed to submit assessment responses.");
                } catch (Exception ex) {
                    showInfo("Failed to submit assessment: " + ex.getMessage());
                }
            });

            Node renderedContent = AssessmentFormRenderer.createPreview(assessment.getContent());
            card.getChildren().addAll(title, statusLabel, renderedContent, gradeLabel, takeBtn);
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
                });
            });

            viewResultsBtn.setOnAction(e -> {
                java.util.List<Assessment> moduleAssessments = new dbConnector().searchAssessmentDB(assessment.getModuleID(), "Educator");
                long gradedCount = moduleAssessments.stream().filter(a -> a.getGrade() >= 0).count();
                showInfo("Module #" + assessment.getModuleID() + " results: " + gradedCount + " graded submissions out of " + moduleAssessments.size());
            });
            buttonBox.getChildren().addAll(editBtn, viewResultsBtn);

            Node renderedContent = AssessmentFormRenderer.createPreview(assessment.getContent());
            card.getChildren().addAll(title, statusLabel, renderedContent, buttonBox);
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
