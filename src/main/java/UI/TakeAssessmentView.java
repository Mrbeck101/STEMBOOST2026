package UI;

import DatabaseController.dbConnector;
import OtherComponents.Assessment;
import OtherComponents.AssessmentForm;
import UserFactory.User;
import Services.UIRefreshService;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;

import java.util.ArrayList;
import java.util.List;

/**
 * TakeAssessmentView - Inline assessment form that cannot be exited until submitted.
 */
public class TakeAssessmentView {

    public static Scene create(SceneRouter router, Assessment assessment) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        User currentUser = UserContext.getInstance().getCurrentUser();
        if (currentUser == null) {
            return LoginView.create(router);
        }

        // Start polling if not already started
        UIRefreshService.getInstance().startPolling(currentUser);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0D1117;");

        // Top bar — no back button; assessment must be completed
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        String moduleSubject = (assessment.getModuleSubject() == null || assessment.getModuleSubject().isBlank())
                ? "Module #" + assessment.getModuleID()
                : assessment.getModuleSubject();

        Label titleLabel = new Label("Assessment: " + moduleSubject);
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label warningLabel = new Label("⚠  This assessment must be completed before you can leave.");
        warningLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #f85149;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(titleLabel, spacer, warningLabel);
        root.setTop(topBar);

        // Form area
        VBox formBox = new VBox(20);
        formBox.setPadding(new Insets(20));

        AssessmentForm form = AssessmentForm.fromContent(assessment.getContent());
        List<Object> answerControls = new ArrayList<>();

        if (form == null) {
            // Raw text assessment
            Label promptLabel = new Label(assessment.getContent() == null || assessment.getContent().isBlank()
                    ? "No assessment content available." : assessment.getContent());
            promptLabel.setWrapText(true);
            promptLabel.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 14;");

            TextArea answerArea = new TextArea();
            answerArea.setPromptText("Enter your answer here...");
            answerArea.setWrapText(true);
            answerArea.setPrefRowCount(6);
            answerArea.setStyle("-fx-font-size: 12;");

            formBox.getChildren().addAll(promptLabel, answerArea);
            answerControls.add(answerArea);
        } else {
            int index = 1;
            for (AssessmentForm.Question question : form.getQuestions()) {
                VBox questionBox = new VBox(10);
                questionBox.setPadding(new Insets(15));
                questionBox.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-radius: 6;");

                Label promptLabel = new Label(index + ". " + question.getPrompt());
                promptLabel.setWrapText(true);
                promptLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #c9d1d9; -fx-font-size: 13;");
                questionBox.getChildren().add(promptLabel);

                if (question.getType() == AssessmentForm.QuestionType.MULTIPLE_CHOICE) {
                    ToggleGroup group = new ToggleGroup();
                    for (String option : question.getOptions()) {
                        RadioButton radio = new RadioButton(option);
                        radio.setToggleGroup(group);
                        radio.setStyle("-fx-text-fill: #c9d1d9;");
                        questionBox.getChildren().add(radio);
                    }
                    answerControls.add(group);
                } else {
                    TextArea answerArea = new TextArea();
                    answerArea.setPromptText("Enter your answer here...");
                    answerArea.setWrapText(true);
                    answerArea.setPrefRowCount(4);
                    questionBox.getChildren().add(answerArea);
                    answerControls.add(answerArea);
                }

                formBox.getChildren().add(questionBox);
                index++;
            }
        }

        ScrollPane scrollPane = new ScrollPane(formBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #0D1117;");
        root.setCenter(scrollPane);

        // Bottom bar with submit button only
        HBox bottomBar = new HBox(15);
        bottomBar.setPadding(new Insets(15, 20, 15, 20));
        bottomBar.setStyle("-fx-background-color: #1C2128;");
        bottomBar.setAlignment(Pos.CENTER_RIGHT);

        Button submitBtn = new Button("Submit Assessment");
        submitBtn.setStyle("-fx-font-size: 13; -fx-padding: 10 20 10 20; -fx-background-color: #238636; -fx-text-fill: white;");
        submitBtn.setOnAction(e -> {
            // Collect responses
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("Assessment Submission for assessment #")
                    .append(assessment.getAssessmentID())
                    .append("\n\n");

            if (form == null) {
                TextArea area = (TextArea) answerControls.get(0);
                String answer = area.getText().trim().isEmpty() ? "No answer provided" : area.getText().trim();
                responseBuilder.append("Response:\n").append(answer);
            } else {
                List<AssessmentForm.Question> questions = form.getQuestions();
                for (int i = 0; i < questions.size(); i++) {
                    AssessmentForm.Question question = questions.get(i);
                    responseBuilder.append("Q").append(i + 1).append(": ").append(question.getPrompt()).append("\n");
                    if (question.getType() == AssessmentForm.QuestionType.MULTIPLE_CHOICE) {
                        ToggleGroup group = (ToggleGroup) answerControls.get(i);
                        String answer = group.getSelectedToggle() instanceof RadioButton radio
                                ? radio.getText() : "No option selected";
                        responseBuilder.append("Answer: ").append(answer).append("\n\n");
                    } else {
                        TextArea area = (TextArea) answerControls.get(i);
                        String answer = area.getText().trim().isEmpty() ? "No answer provided" : area.getText().trim();
                        responseBuilder.append("Answer: ").append(answer).append("\n\n");
                    }
                }
            }

            String responses = responseBuilder.toString();
            try {
                Integer educatorId = new dbConnector().findEducatorForModule(assessment.getModuleID());
                if (educatorId == null) {
                    showInfo("Could not locate the educator for this assessment.");
                    return;
                }
                String submissionContent = "Assessment " + assessment.getAssessmentID()
                        + " Submitted for Module: " + moduleSubject + "\n\n" + responses;
                boolean sent = currentUser.sendMessage(educatorId, submissionContent);
                if (sent) {
                    boolean markedComplete = new dbConnector().updateAssessmentCompletion(
                            currentUser.getId(), assessment.getAssessmentID(), true);
                    if (markedComplete) {
                        showInfo("Assessment submitted successfully!");
                        router.goToAssessments();
                    } else {
                        showInfo("Responses sent, but completion state was not updated. Please contact your educator.");
                    }
                } else {
                    showInfo("Failed to submit assessment. Please try again.");
                }
            } catch (Exception ex) {
                showInfo("Failed to submit assessment: " + ex.getMessage());
            }
        });

        bottomBar.getChildren().add(submitBtn);
        root.setBottom(bottomBar);

        return new Scene(root, 1400, 900);
    }

    private static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

