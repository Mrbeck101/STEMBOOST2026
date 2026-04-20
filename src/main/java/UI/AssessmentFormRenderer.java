package UI;

import OtherComponents.Assessment;
import OtherComponents.AssessmentForm;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
        UIComponents.preparePopupForStudentTts(dialog);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != submitType) {
            return null;
        }

        return buildSubmissionJson(assessment, form, answerControls);
    }

    public static String buildSubmissionJson(Assessment assessment, AssessmentForm form, List<Object> answerControls) {
        JsonObject submission = new JsonObject();
        submission.addProperty("assessmentId", assessment.getAssessmentID());
        JsonArray responses = new JsonArray();

        if (form == null) {
            JsonObject response = new JsonObject();
            response.addProperty("index", 1);
            response.addProperty("type", "OPEN_ENDED");
            response.addProperty("prompt", assessment.getContent() == null ? "" : assessment.getContent());
            TextArea answerArea = (TextArea) answerControls.get(0);
            String answer = answerArea.getText().trim().isEmpty() ? "No answer provided" : answerArea.getText().trim();
            response.addProperty("answer", answer);
            responses.add(response);
            submission.add("responses", responses);
            return submission.toString();
        }

        List<AssessmentForm.Question> questions = form.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            AssessmentForm.Question question = questions.get(i);
            JsonObject response = new JsonObject();
            response.addProperty("index", i + 1);
            response.addProperty("type", question.getType().name());
            response.addProperty("prompt", question.getPrompt());

            if (question.getType() == AssessmentForm.QuestionType.MULTIPLE_CHOICE) {
                JsonArray options = new JsonArray();
                for (String option : question.getOptions()) {
                    options.add(option);
                }
                response.add("options", options);

                ToggleGroup group = (ToggleGroup) answerControls.get(i);
                String answer = group.getSelectedToggle() instanceof RadioButton radio
                        ? radio.getText()
                        : "No option selected";
                response.addProperty("answer", answer);
            } else {
                TextArea answerArea = (TextArea) answerControls.get(i);
                String answer = answerArea.getText().trim().isEmpty() ? "No answer provided" : answerArea.getText().trim();
                response.addProperty("answer", answer);
            }
            responses.add(response);
        }

        submission.add("responses", responses);
        return submission.toString();
    }

    public static Node createSubmissionPreview(String submissionJson, String fallbackAssessmentContent) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(5));

        if (submissionJson == null || submissionJson.isBlank()) {
            Label noSubmission = new Label("No student response was found for this assessment yet.");
            noSubmission.setWrapText(true);
            noSubmission.setStyle("-fx-text-fill: #c9d1d9;");
            box.getChildren().add(noSubmission);
            return box;
        }

        try {
            JsonElement root = JsonParser.parseString(submissionJson);
            if (!root.isJsonObject()) {
                return createLegacySubmissionPreview(submissionJson, fallbackAssessmentContent);
            }

            JsonObject json = root.getAsJsonObject();
            JsonArray responses = json.has("responses") && json.get("responses").isJsonArray()
                    ? json.getAsJsonArray("responses")
                    : null;
            if (responses == null) {
                return createLegacySubmissionPreview(submissionJson, fallbackAssessmentContent);
            }

            for (JsonElement responseEl : responses) {
                if (responseEl == null || !responseEl.isJsonObject()) {
                    continue;
                }
                JsonObject response = responseEl.getAsJsonObject();
                int index = response.has("index") ? response.get("index").getAsInt() : 0;
                String promptText = response.has("prompt") ? response.get("prompt").getAsString() : "";
                String answerText = response.has("answer") ? response.get("answer").getAsString() : "No answer provided";
                String type = response.has("type") ? response.get("type").getAsString() : "OPEN_ENDED";

                VBox questionBox = new VBox(6);
                Label prompt = new Label((index > 0 ? index + ". " : "") + promptText);
                prompt.setWrapText(true);
                prompt.setStyle("-fx-font-weight: bold; -fx-text-fill: #c9d1d9;");
                questionBox.getChildren().add(prompt);

                if ("MULTIPLE_CHOICE".equalsIgnoreCase(type) && response.has("options") && response.get("options").isJsonArray()) {
                    JsonArray options = response.getAsJsonArray("options");
                    for (JsonElement optionEl : options) {
                        String option = optionEl == null || optionEl.isJsonNull() ? "" : optionEl.getAsString();
                        boolean selected = option.equals(answerText);
                        Label optionLabel = new Label((selected ? "[X] " : "[ ] ") + option);
                        optionLabel.setStyle(selected
                                ? "-fx-text-fill: #58a6ff; -fx-font-weight: bold;"
                                : "-fx-text-fill: #8b949e;");
                        questionBox.getChildren().add(optionLabel);
                    }
                } else {
                    Label answerLabel = new Label("Answer: " + answerText);
                    answerLabel.setWrapText(true);
                    answerLabel.setStyle("-fx-text-fill: #8b949e;");
                    questionBox.getChildren().add(answerLabel);
                }
                box.getChildren().add(questionBox);
            }

            if (box.getChildren().isEmpty()) {
                return createLegacySubmissionPreview(submissionJson, fallbackAssessmentContent);
            }
            return box;
        } catch (Exception ex) {
            return createLegacySubmissionPreview(submissionJson, fallbackAssessmentContent);
        }
    }

    private static Node createLegacySubmissionPreview(String submissionContent, String fallbackAssessmentContent) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(5));

        Label legacy = new Label(submissionContent == null || submissionContent.isBlank()
                ? "No student response was found for this assessment yet."
                : submissionContent);
        legacy.setWrapText(true);
        legacy.setStyle("-fx-text-fill: #c9d1d9;");
        box.getChildren().add(legacy);

        if (fallbackAssessmentContent != null && !fallbackAssessmentContent.isBlank()) {
            Label divider = new Label("Original assessment:");
            divider.setStyle("-fx-text-fill: #8b949e; -fx-font-style: italic;");
            box.getChildren().add(divider);
            box.getChildren().add(createPreview(fallbackAssessmentContent));
        }

        return box;
    }
}

