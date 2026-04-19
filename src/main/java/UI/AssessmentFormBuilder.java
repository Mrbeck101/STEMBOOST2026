package UI;

import OtherComponents.AssessmentForm;
import OtherComponents.ValidationException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class AssessmentFormBuilder {
    private final VBox root = new VBox(12);
    private final VBox questionsBox = new VBox(12);

    public AssessmentFormBuilder() {
        Button addMultipleChoiceBtn = new Button("Add Multiple Choice Question");
        addMultipleChoiceBtn.setOnAction(e -> questionsBox.getChildren().add(createQuestionCard(true)));

        Button addOpenEndedBtn = new Button("Add Open Ended Question");
        addOpenEndedBtn.setOnAction(e -> questionsBox.getChildren().add(createQuestionCard(false)));

        Label helper = new Label("Build an assessment by adding multiple choice or open ended questions.");
        helper.setStyle("-fx-text-fill: #8b949e;");

        root.getChildren().addAll(helper, new HBox(10, addMultipleChoiceBtn, addOpenEndedBtn), questionsBox);
    }

    public VBox getView() {
        return root;
    }

    public AssessmentForm buildForm() {
        if (questionsBox.getChildren().isEmpty()) {
            throw new ValidationException("Add at least one question to the assessment.");
        }

        List<AssessmentForm.Question> questions = new ArrayList<>();
        for (javafx.scene.Node node : questionsBox.getChildren()) {
            if (!(node instanceof VBox card)) {
                continue;
            }

            AssessmentForm.QuestionType type = (AssessmentForm.QuestionType) card.getProperties().get("type");
            TextField promptField = (TextField) card.getProperties().get("promptField");
            String prompt = promptField.getText().trim();
            if (prompt.isEmpty()) {
                throw new ValidationException("Each question must include a prompt.");
            }

            List<String> options = new ArrayList<>();
            if (type == AssessmentForm.QuestionType.MULTIPLE_CHOICE) {
                @SuppressWarnings("unchecked")
                List<TextField> optionFields = (List<TextField>) card.getProperties().get("optionFields");
                for (TextField optionField : optionFields) {
                    String value = optionField.getText().trim();
                    if (!value.isEmpty()) {
                        options.add(value);
                    }
                }
                if (options.size() < 2) {
                    throw new ValidationException("Multiple choice questions must have at least two options.");
                }
            }

            questions.add(new AssessmentForm.Question(type, prompt, options));
        }

        return new AssessmentForm(questions);
    }

    private VBox createQuestionCard(boolean multipleChoice) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #0D1117; -fx-border-color: #30363D; -fx-border-radius: 6;");

        AssessmentForm.QuestionType type = multipleChoice
                ? AssessmentForm.QuestionType.MULTIPLE_CHOICE
                : AssessmentForm.QuestionType.OPEN_ENDED;
        card.getProperties().put("type", type);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(multipleChoice ? "Multiple Choice Question" : "Open Ended Question");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffffff;");
        Button removeBtn = new Button("Remove");
        removeBtn.setOnAction(e -> questionsBox.getChildren().remove(card));
        header.getChildren().addAll(title, removeBtn);

        TextField promptField = new TextField();
        promptField.setPromptText("Question prompt");
        card.getProperties().put("promptField", promptField);

        card.getChildren().addAll(header, new Label("Prompt"), promptField);

        if (multipleChoice) {
            List<TextField> optionFields = new ArrayList<>();
            VBox optionsBox = new VBox(8);
            for (int i = 1; i <= 4; i++) {
                TextField optionField = new TextField();
                optionField.setPromptText("Option " + i);
                optionFields.add(optionField);
                optionsBox.getChildren().add(optionField);
            }
            card.getProperties().put("optionFields", optionFields);
            card.getChildren().addAll(new Label("Answer Options"), optionsBox);
        } else {
            TextArea previewAnswer = new TextArea();
            previewAnswer.setPromptText("Students will receive an empty answer box here.");
            previewAnswer.setDisable(true);
            previewAnswer.setPrefRowCount(3);
            VBox.setVgrow(previewAnswer, Priority.NEVER);
            card.getChildren().addAll(new Label("Student Answer Box Preview"), previewAnswer);
        }

        return card;
    }
}

