package UI;

import OtherComponents.Assessment;
import OtherComponents.AssessmentForm;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssessmentFormRenderer {

    public static Node createPreview(String content) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(5));

        AssessmentForm form = AssessmentForm.fromContent(content);
        if (form == null) {
            Label rawLabel = new Label(content == null || content.isBlank() ? "No assessment content available." : content);
            rawLabel.setWrapText(true);
            rawLabel.setStyle("-fx-text-fill: #c9d1d9;");
            box.getChildren().add(rawLabel);
            return box;
        }

        int index = 1;
        for (AssessmentForm.Question question : form.getQuestions()) {
            VBox questionBox = new VBox(6);
            Label prompt = new Label(index + ". " + question.getPrompt());
            prompt.setWrapText(true);
            prompt.setStyle("-fx-font-weight: bold; -fx-text-fill: #c9d1d9;");
            questionBox.getChildren().add(prompt);

            if (question.getType() == AssessmentForm.QuestionType.MULTIPLE_CHOICE) {
                for (String option : question.getOptions()) {
                    Label optionLabel = new Label("• " + option);
                    optionLabel.setStyle("-fx-text-fill: #8b949e;");
                    questionBox.getChildren().add(optionLabel);
                }
            } else {
                Label openEnded = new Label("Open ended response");
                openEnded.setStyle("-fx-text-fill: #8b949e; -fx-font-style: italic;");
                questionBox.getChildren().add(openEnded);
            }

            box.getChildren().add(questionBox);
            index++;
        }

        return box;
    }

    public static String collectStudentResponses(Assessment assessment) {
        AssessmentForm form = AssessmentForm.fromContent(assessment.getContent());

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Take Assessment");
        dialog.setHeaderText("Complete the assessment and submit your answers");
        ButtonType submitType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);

        VBox formBox = new VBox(15);
        formBox.setPadding(new Insets(15));
        List<Object> answerControls = new ArrayList<>();

        if (form == null) {
            Label prompt = new Label(assessment.getContent());
            prompt.setWrapText(true);
            TextArea answerArea = new TextArea();
            answerArea.setPromptText("Enter your answer here");
            answerArea.setPrefRowCount(5);
            formBox.getChildren().addAll(prompt, answerArea);
            answerControls.add(answerArea);
        } else {
            int index = 1;
            for (AssessmentForm.Question question : form.getQuestions()) {
                VBox questionBox = new VBox(8);
                Label prompt = new Label(index + ". " + question.getPrompt());
                prompt.setWrapText(true);
                questionBox.getChildren().add(prompt);

                if (question.getType() == AssessmentForm.QuestionType.MULTIPLE_CHOICE) {
                    ToggleGroup group = new ToggleGroup();
                    for (String option : question.getOptions()) {
                        RadioButton radio = new RadioButton(option);
                        radio.setToggleGroup(group);
                        questionBox.getChildren().add(radio);
                    }
                    answerControls.add(group);
                } else {
                    TextArea answerArea = new TextArea();
                    answerArea.setPromptText("Enter your answer here");
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
        scrollPane.setPrefViewportHeight(500);
        dialog.getDialogPane().setContent(scrollPane);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != submitType) {
            return null;
        }

        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("Assessment Submission for assessment #")
                .append(assessment.getAssessmentID())
                .append("\n\n");

        if (form == null) {
            TextArea answerArea = (TextArea) answerControls.get(0);
            responseBuilder.append("Response:\n")
                    .append(answerArea.getText().trim().isEmpty() ? "No answer provided" : answerArea.getText().trim());
            return responseBuilder.toString();
        }

        List<AssessmentForm.Question> questions = form.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            AssessmentForm.Question question = questions.get(i);
            responseBuilder.append("Q").append(i + 1).append(": ").append(question.getPrompt()).append("\n");
            if (question.getType() == AssessmentForm.QuestionType.MULTIPLE_CHOICE) {
                ToggleGroup group = (ToggleGroup) answerControls.get(i);
                String answer = group.getSelectedToggle() instanceof RadioButton radio
                        ? radio.getText()
                        : "No option selected";
                responseBuilder.append("Answer: ").append(answer).append("\n\n");
            } else {
                TextArea answerArea = (TextArea) answerControls.get(i);
                String answer = answerArea.getText().trim().isEmpty() ? "No answer provided" : answerArea.getText().trim();
                responseBuilder.append("Answer: ").append(answer).append("\n\n");
            }
        }

        return responseBuilder.toString();
    }
}

