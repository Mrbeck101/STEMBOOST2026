package OtherComponents;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class AssessmentForm {
    private static final Gson GSON = new Gson();

    private int version = 1;
    private List<Question> questions = new ArrayList<>();

    public AssessmentForm() {
    }

    public AssessmentForm(List<Question> questions) {
        this.questions = questions == null ? new ArrayList<>() : questions;
    }

    public int getVersion() {
        return version;
    }

    public List<Question> getQuestions() {
        return questions == null ? new ArrayList<>() : questions;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static boolean isStructuredContent(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        try {
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            return json.has("questions") && json.get("questions").isJsonArray();
        } catch (Exception ex) {
            return false;
        }
    }

    public static AssessmentForm fromContent(String content) {
        if (!isStructuredContent(content)) {
            return null;
        }
        return GSON.fromJson(content, AssessmentForm.class);
    }

    public enum QuestionType {
        MULTIPLE_CHOICE,
        OPEN_ENDED
    }

    public static class Question {
        private QuestionType type;
        private String prompt;
        private List<String> options = new ArrayList<>();

        public Question() {
        }

        public Question(QuestionType type, String prompt, List<String> options) {
            this.type = type;
            this.prompt = prompt;
            this.options = options == null ? new ArrayList<>() : options;
        }

        public QuestionType getType() {
            return type;
        }

        public String getPrompt() {
            return prompt;
        }

        public List<String> getOptions() {
            return options == null ? new ArrayList<>() : options;
        }
    }
}

