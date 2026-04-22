package UI;

import Services.KeyboardTtsService;
import UserFactory.Student;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import java.util.HashMap;
import java.util.Map;

/**
 * Learning Path Selection View - For students to select or determine their learning path
 */
public class LearningPathSelectionView {

    private static Map<Integer, String> questionnaireAnswers = new HashMap<>();

    public static Scene create(SceneRouter router) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Student student = (Student) UserContext.getInstance().getCurrentUser();
        if (student == null) {
            return LoginView.create(router);
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Top bar
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Select Your Learning Path");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        title.setAccessibleText("Select Your Learning Path");
        topBar.getChildren().add(title);
        root.setTop(topBar);

        // Content with tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: #0D1117;");

        // Tab 1: Manual Selection
        Tab manualTab = new Tab("Manual Selection", createManualSelectionContent(router, student));
        manualTab.setStyle("-fx-text-base-color: #ffffff;");

        // Tab 2: Questionnaire
        Tab questionnaireTab = new Tab("Take Questionnaire", createQuestionnaireContent(router, student));
        questionnaireTab.setStyle("-fx-text-base-color: #ffffff;");

        tabPane.getTabs().addAll(manualTab, questionnaireTab);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1400, 900);

        KeyboardTtsService.getInstance().bindScene(
                scene,
                KeyboardTtsService.AccessMode.STUDENT_ONLY,
                () -> new KeyboardTtsService.ReadingContent(buildManualSelectionNarration()),
                tabPane::requestFocus
        );

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, selectedTab) -> {
            if (selectedTab == null) {
                return;
            }
            if (selectedTab == manualTab) {
                KeyboardTtsService.getInstance().speakNow(buildManualSelectionNarration());
            } else if (selectedTab == questionnaireTab) {
                KeyboardTtsService.getInstance().speakNow(buildQuestionnaireNarration());
            }
        });

        return scene;
    }

    private static VBox createManualSelectionContent(SceneRouter router, Student student) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(40));
        content.setStyle("-fx-background-color: #0D1117;");
        content.setAlignment(Pos.TOP_CENTER);

        VBox card = new VBox(25);
        card.setPadding(new Insets(30));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 8;");
        card.setMaxWidth(700);

        Label cardTitle = new Label("Choose Your Learning Path");
        cardTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label description = new Label("Select the learning path that best matches your interests and goals:");
        description.setStyle("-fx-font-size: 14; -fx-text-fill: #c9d1d9;");
        description.setWrapText(true);

        VBox pathsBox = new VBox(15);
        ToggleGroup pathGroup = new ToggleGroup();

        String[] paths = {
            "Electrical Engineering",
            "Software Engineering",
            "Information Technology",
            "Cybersecurity",
            "Computer Engineering",
            "Artificial Intelligence"
        };

        String[] descriptions = {
            "Learn circuit design, power systems, and electromagnetics",
            "Master programming, software design, and development",
            "Focus on IT infrastructure, networks, and systems",
            "Specialize in security, encryption, and threat protection",
            "Combine hardware and software engineering principles",
            "Explore machine learning, neural networks, and AI"
        };

        for (int i = 0; i < paths.length; i++) {
            VBox pathCard = createPathCard(paths[i], descriptions[i], pathGroup, paths[i]);
            pathsBox.getChildren().add(pathCard);
        }

        // Button box
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button confirmBtn = new Button("Confirm Selection");
        confirmBtn.setStyle("-fx-font-size: 12; -fx-padding: 10 30 10 30;");
        confirmBtn.setAccessibleText("Confirm learning path selection");

        confirmBtn.setOnAction(e -> {
            RadioButton selected = (RadioButton) pathGroup.getSelectedToggle();
            if (selected != null) {
                String selectedPath = (String) selected.getUserData();

                try {
                    if (student.updateLearningPath(selectedPath)) {
                        showAlert("Success", "Your learning path has been set to:\n" + selectedPath);
                        router.goToDashboard(student.getId(), "Student");
                    } else {
                        showAlert("Error", "Failed to save learning path. Please try again.");
                    }
                } catch (Exception ex) {
                    showAlert("Error", "Database error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                showAlert("Error", "Please select a learning path");
            }
        });

        buttonBox.getChildren().add(confirmBtn);

        card.getChildren().addAll(cardTitle, description, new Separator(), pathsBox, buttonBox);

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #0D1117;");
        content.getChildren().add(scroll);

        return content;
    }

    private static VBox createPathCard(String pathName, String pathDescription, ToggleGroup group, String userData) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #0D1117; -fx-border-color: #30363D; -fx-border-width: 1; -fx-border-radius: 6;");

        RadioButton rb = new RadioButton(pathName);
        rb.setToggleGroup(group);
        rb.setStyle("-fx-font-size: 13; -fx-text-fill: #58a6ff; -fx-font-weight: bold;");
        rb.setUserData(userData);

        Label desc = new Label(pathDescription);
        desc.setStyle("-fx-font-size: 11; -fx-text-fill: #8b949e;");
        desc.setWrapText(true);

        card.getChildren().addAll(rb, desc);
        return card;
    }

    private static VBox createQuestionnaireContent(SceneRouter router, Student student) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #0D1117;");

        VBox questionsBox = new VBox(20);
        questionsBox.setPadding(new Insets(20));

        Label instructions = new Label("Answer the following questions to determine your ideal learning path:");
        instructions.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        questionsBox.getChildren().add(instructions);

        questionsBox.getChildren().add(new Separator());

        // Question 1
        VBox q1 = createQuestion(1, "What interests you most?",
            new String[]{"Building electronic circuits and systems", "Writing software and code", "Managing IT networks", "Protecting against cyber threats", "Combining hardware and software", "Creating intelligent systems"},
            new String[]{"Electrical Engineering", "Software Engineering", "Information Technology", "Cybersecurity", "Computer Engineering", "Artificial Intelligence"});
        questionsBox.getChildren().add(q1);

        // Question 2
        VBox q2 = createQuestion(2, "Which skill would you like to develop first?",
            new String[]{"Circuit analysis and design", "Algorithm development", "System administration", "Security protocols", "Both hardware and software", "Data analysis and AI models"},
            new String[]{"Electrical Engineering", "Software Engineering", "Information Technology", "Cybersecurity", "Computer Engineering", "Artificial Intelligence"});
        questionsBox.getChildren().add(q2);

        // Question 3
        VBox q3 = createQuestion(3, "What type of projects excite you?",
            new String[]{"Power systems and electronics", "Applications and websites", "Infrastructure and servers", "Security systems and testing", "Embedded systems and robotics", "Smart algorithms and predictions"},
            new String[]{"Electrical Engineering", "Software Engineering", "Information Technology", "Cybersecurity", "Computer Engineering", "Artificial Intelligence"});
        questionsBox.getChildren().add(q3);

        // Question 4
        VBox q4 = createQuestion(4, "Where do you see yourself in 5 years?",
            new String[]{"Electrical engineer at a tech company", "Software developer at a startup", "IT manager at a corporation", "Cybersecurity specialist", "Embedded systems designer", "AI researcher"},
            new String[]{"Electrical Engineering", "Software Engineering", "Information Technology", "Cybersecurity", "Computer Engineering", "Artificial Intelligence"});
        questionsBox.getChildren().add(q4);

        // Submit button
        HBox submitBox = new HBox(10);
        submitBox.setAlignment(Pos.CENTER_RIGHT);
        submitBox.setPadding(new Insets(20));

        Button submitBtn = new Button("Submit Questionnaire");
        submitBtn.setStyle("-fx-font-size: 12; -fx-padding: 10 30 10 30;");
        submitBtn.setAccessibleText("Submit questionnaire answers");

        submitBtn.setOnAction(e -> {
            String recommendedPath = calculateRecommendedPath(questionsBox);
            if (recommendedPath != null) {
                try {
                    if (student.updateLearningPath(recommendedPath)) {
                        showAlert("Success", "Based on your answers, we recommend:\n" + recommendedPath);
                        router.goToDashboard(student.getId(), "Student");
                    } else {
                        showAlert("Error", "Failed to save learning path. Please try again.");
                    }
                } catch (Exception ex) {
                    showAlert("Error", "Database error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                showAlert("Error", "Please answer all questions");
            }
        });

        submitBox.getChildren().add(submitBtn);
        questionsBox.getChildren().add(submitBox);

        scrollPane.setContent(questionsBox);
        content.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return content;
    }

    private static VBox createQuestion(int questionNumber, String question, String[] options, String[] paths) {
        VBox questionBox = new VBox(12);
        questionBox.setPadding(new Insets(15));
        questionBox.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");

        Label questionLabel = new Label("Q" + questionNumber + ": " + question);
        questionLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        questionBox.getChildren().add(questionLabel);

        ToggleGroup group = new ToggleGroup();
        group.setUserData(new Object[]{questionNumber, new HashMap<Integer, String>()});

        for (int i = 0; i < options.length; i++) {
            RadioButton rb = new RadioButton(options[i]);
            rb.setToggleGroup(group);
            rb.setStyle("-fx-font-size: 11; -fx-text-fill: #c9d1d9;");
            rb.setWrapText(true);
            rb.setUserData(paths[i]);
            questionBox.getChildren().add(rb);
        }

        questionBox.setUserData(group);
        return questionBox;
    }

    private static String calculateRecommendedPath(VBox questionsBox) {
        Map<String, Integer> pathVotes = new HashMap<>();

        // Initialize all paths
        String[] allPaths = {"Electrical Engineering", "Software Engineering", "Information Technology", "Cybersecurity", "Computer Engineering", "Artificial Intelligence"};
        for (String path : allPaths) {
            pathVotes.put(path, 0);
        }

        // Count votes from each question
        for (javafx.scene.Node node : questionsBox.getChildren()) {
            if (node instanceof VBox && node.getUserData() instanceof ToggleGroup) {
                ToggleGroup group = (ToggleGroup) node.getUserData();
                RadioButton selected = (RadioButton) group.getSelectedToggle();

                if (selected != null && selected.getUserData() instanceof String) {
                    String path = (String) selected.getUserData();
                    pathVotes.put(path, pathVotes.get(path) + 1);
                } else {
                    // User didn't answer all questions
                    return null;
                }
            }
        }

        // Find path with most votes
        String recommendedPath = null;
        int maxVotes = 0;
        for (Map.Entry<String, Integer> entry : pathVotes.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                recommendedPath = entry.getKey();
            }
        }

        return recommendedPath;
    }

    private static String buildManualSelectionNarration() {
        return "Select your learning path. "
                + "You are on manual selection. "
                + "Choose one learning path that best matches your interests and goals, then press confirm selection. "
                + "Available paths are Electrical Engineering, Software Engineering, Information Technology, "
                + "Cybersecurity, Computer Engineering, and Artificial Intelligence.";
    }

    private static String buildQuestionnaireNarration() {
        return "You are on the learning path questionnaire. "
                + "Answer all four questions to get a recommended path. "
                + "Question one, what interests you most. "
                + "Question two, which skill would you like to develop first. "
                + "Question three, what type of projects excite you. "
                + "Question four, where do you see yourself in five years. "
                + "After answering all questions, press submit questionnaire.";
    }

    private static void showAlert(String title, String message) {
        UIComponents.showAlert(title, message);
    }
}
