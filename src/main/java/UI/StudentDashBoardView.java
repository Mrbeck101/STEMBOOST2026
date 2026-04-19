package UI;

import UserFactory.Student;
import DatabaseController.dbConnector;
import OtherComponents.LearningModule;
import OtherComponents.Assessment;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.AccessibleRole;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;

/**
 * Student Dashboard View with accessibility features for visually impaired learners
 */
public class StudentDashBoardView {

    public static Scene create(SceneRouter router) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Student student = (Student) UserContext.getInstance().getCurrentUser();
        if (student == null) {
            return LoginView.create(router);
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // ================= TOP BAR =================
        HBox topBar = createTopBar(student, router);
        root.setTop(topBar);

        // ================= MAIN CONTENT =================
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Dashboard Tab
        Tab dashboardTab = new Tab("Dashboard", createDashboardContent(student));
        dashboardTab.setStyle("-fx-text-base-color: #ffffff;");

        // Modules Tab
        Tab modulesTab = new Tab("Learning Modules", createModulesContent(student, router));
        modulesTab.setStyle("-fx-text-base-color: #ffffff;");

        // Assessments Tab
        Tab assessmentsTab = new Tab("Assessments", createAssessmentsContent(student));
        assessmentsTab.setStyle("-fx-text-base-color: #ffffff;");

        // Inbox Tab
        Tab inboxTab = new Tab("Inbox", createInboxContent(student, router));
        inboxTab.setStyle("-fx-text-base-color: #ffffff;");

        tabPane.getTabs().addAll(dashboardTab, modulesTab, assessmentsTab, inboxTab);

        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1400, 900);
        return scene;
    }

    private static HBox createTopBar(Student student, SceneRouter router) {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label appTitle = new Label("STEMBOOST - Student Dashboard");
        appTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        appTitle.setAccessibleText("Stemboost Student Dashboard");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Student Info
        VBox studentInfo = new VBox(5);
        Label nameLabel = new Label("Welcome, " + student.getName());
        nameLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #ffffff;");
        nameLabel.setAccessibleText("Welcome, " + student.getName());

        Label uniLabel = new Label("University: " + student.getUniversity());
        uniLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #aaaaaa;");
        uniLabel.setAccessibleText("University: " + student.getUniversity());

        studentInfo.getChildren().addAll(nameLabel, uniLabel);

        // Logout Button
        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-font-size: 12;");
        logoutBtn.setAccessibleText("Logout button");
        logoutBtn.setOnAction(e -> router.goToLogin());

        topBar.getChildren().addAll(appTitle, spacer, studentInfo, logoutBtn);
        return topBar;
    }

    private static VBox createDashboardContent(Student student) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        // Welcome Card
        VBox welcomeCard = new VBox(15);
        welcomeCard.setPadding(new Insets(20));
        welcomeCard.setStyle("-fx-background-color: #161B22; -fx-border-radius: 8; -fx-padding: 20;");
        welcomeCard.setAccessibleText("Welcome section containing your learning path and progress overview");

        Label welcomeTitle = new Label("Your Learning Journey");
        welcomeTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        welcomeTitle.setAccessibleText("Your Learning Journey");

        Label learningPathLabel = new Label("Learning Path: " + student.getLearningPath());
        learningPathLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #c9d1d9;");
        learningPathLabel.setAccessibleText("Learning Path: " + student.getLearningPath());

        Label progressLabel = new Label("Overall Progress: In Progress");
        progressLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #c9d1d9;");
        progressLabel.setAccessibleText("Overall Progress: In Progress");

        welcomeCard.getChildren().addAll(welcomeTitle, learningPathLabel, progressLabel);

        // Quick Stats
        HBox statsBox = new HBox(15);
        statsBox.setAccessibleRole(AccessibleRole.NODE);
        statsBox.setAccessibleText("Quick statistics section");

        VBox modulesStatsCard = createStatCard("Active Modules", "0", "Modules you are currently enrolled in");
        VBox assessmentsStatsCard = createStatCard("Pending Assessments", "0", "Assessments awaiting completion");
        VBox completionCard = createStatCard("Completion Rate", "0%", "Overall course completion percentage");

        statsBox.getChildren().addAll(modulesStatsCard, assessmentsStatsCard, completionCard);
        HBox.setHgrow(modulesStatsCard, Priority.ALWAYS);
        HBox.setHgrow(assessmentsStatsCard, Priority.ALWAYS);
        HBox.setHgrow(completionCard, Priority.ALWAYS);

        content.getChildren().addAll(welcomeCard, statsBox);
        return content;
    }

    private static VBox createModulesContent(Student student, SceneRouter router) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Learning Modules");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        title.setAccessibleText("Learning Modules section");

        Button requestModuleBtn = new Button("Request New Learning Module");
        requestModuleBtn.setOnAction(e -> showModuleRequestDialog(student));

        Button requestJobBtn = new Button("Request Job Program");
        requestJobBtn.setOnAction(e -> showJobRequestDialog(student));

        HBox requestButtons = new HBox(10, requestModuleBtn, requestJobBtn);

        List<LearningModule> modules = student.getLearningModules();

        if (modules == null || modules.isEmpty()) {
            Label noModules = new Label("No modules enrolled yet");
            noModules.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            noModules.setAccessibleText("You have no modules enrolled yet");
            content.getChildren().addAll(title, requestButtons, noModules);
        } else {
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: #0D1117; -fx-control-inner-background: #0D1117;");

            VBox modulesVBox = new VBox(10);
            modulesVBox.setPadding(new Insets(10));

            for (LearningModule module : modules) {
                VBox moduleCard = createModuleCard(module);
                modulesVBox.getChildren().add(moduleCard);
            }

            scrollPane.setContent(modulesVBox);
            content.getChildren().addAll(title, requestButtons, scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }

        return content;
    }

    private static VBox createModuleCard(LearningModule module) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");
        card.setAccessibleRole(AccessibleRole.NODE);

        Label moduleTitle = new Label(module.getSubject());
        moduleTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");
        moduleTitle.setAccessibleText("Module: " + module.getSubject());

        Label modulePath = new Label("Path: " + module.getLearningPath());
        modulePath.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
        modulePath.setAccessibleText("Learning Path: " + module.getLearningPath());

        // Progress Bar with accessible label
        double progress = module.getProgress() / 100.0;
        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setAccessibleText("Module progress: " + module.getProgress() + " percent complete");
        progressBar.setStyle("-fx-accent: #238636;");

        Label progressLabel = new Label(module.getProgress() + "% Complete");
        progressLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #8b949e;");
        progressLabel.setAccessibleText(module.getProgress() + "% Complete");

        card.getChildren().addAll(moduleTitle, modulePath, progressBar, progressLabel);
        return card;
    }

    private static VBox createAssessmentsContent(Student student) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Assessments");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        title.setAccessibleText("Assessments section");

        List<Assessment> assessments = student.getAssessmentResults();

        if (assessments == null || assessments.isEmpty()) {
            Label noAssessments = new Label("No assessments available");
            noAssessments.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            noAssessments.setAccessibleText("You have no pending assessments");
            content.getChildren().addAll(title, noAssessments);
        } else {
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: #0D1117; -fx-control-inner-background: #0D1117;");

            VBox assessmentsVBox = new VBox(10);
            assessmentsVBox.setPadding(new Insets(10));

            for (Assessment assessment : assessments) {
                VBox assessmentCard = createAssessmentCard(assessment);
                assessmentsVBox.getChildren().add(assessmentCard);
            }

            scrollPane.setContent(assessmentsVBox);
            content.getChildren().addAll(title, scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }

        return content;
    }

    private static VBox createAssessmentCard(Assessment assessment) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");
        card.setAccessibleRole(AccessibleRole.NODE);

        Label assessmentTitle = new Label("Assessment for: " + assessment.getLearningPath());
        assessmentTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ffa657;");
        assessmentTitle.setAccessibleText("Assessment for: " + assessment.getLearningPath());

        Label gradeLabel = new Label("Grade: " + (assessment.getGrade() >= 0 ? assessment.getGrade() + "%" : "Not yet graded"));
        gradeLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
        gradeLabel.setAccessibleText("Grade: " + (assessment.getGrade() >= 0 ? assessment.getGrade() + " percent" : "Not yet graded"));

        Node renderedContent = AssessmentFormRenderer.createPreview(assessment.getContent());

        Button takeButton = new Button("Take Assessment");
        takeButton.setStyle("-fx-font-size: 11;");
        takeButton.setAccessibleText("Take this assessment");
        takeButton.setOnAction(e -> {
            Student currentStudent = (Student) UserContext.getInstance().getCurrentUser();
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
                boolean sent = currentStudent.sendMessage(educatorId, "Assessment Submission", responses);
                showInfo(sent ? "Assessment responses submitted successfully." : "Failed to submit assessment.");
            } catch (Exception ex) {
                showInfo("Failed to submit assessment: " + ex.getMessage());
            }
        });

        card.getChildren().addAll(assessmentTitle, renderedContent, gradeLabel, takeButton);
        return card;
    }

    private static VBox createInboxContent(Student student, SceneRouter router) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Inbox");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        title.setAccessibleText("Inbox section for messages");

        Button viewMessagesBtn = new Button("View Messages");
        viewMessagesBtn.setAccessibleText("View all messages button");
        viewMessagesBtn.setOnAction(e -> router.goToInbox());

        Label messageCount = new Label("You have no new messages");
        messageCount.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
        messageCount.setAccessibleText("You have no new messages");

        content.getChildren().addAll(title, viewMessagesBtn, messageCount);
        return content;
    }

    private static VBox createStatCard(String title, String value, String description) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6;");
        card.setAccessibleText(description);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
        titleLabel.setAccessibleText(title);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");
        valueLabel.setAccessibleText(value);

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private static void showModuleRequestDialog(Student student) {
        TextInputDialog pathDialog = new TextInputDialog(student.getLearningPath());
        pathDialog.setTitle("Module Request");
        pathDialog.setHeaderText("Request a new learning module");
        pathDialog.setContentText("Learning path:");

        pathDialog.showAndWait().ifPresent(path -> {
            boolean sent = student.requestLearningModule(path, "Requested from student dashboard");
            showInfo(sent ? "Request sent to counselor." : "Failed to send request.");
        });
    }

    private static void showJobRequestDialog(Student student) {
        TextInputDialog jobDialog = new TextInputDialog();
        jobDialog.setTitle("Job Program Request");
        jobDialog.setHeaderText("Request a job program");
        jobDialog.setContentText("Job Program ID:");

        jobDialog.showAndWait().ifPresent(value -> {
            try {
                int jobId = Integer.parseInt(value.trim());
                boolean sent = student.requestWorkProgram(jobId, "Requested from student dashboard");
                showInfo(sent ? "Request sent to counselor." : "Failed to send request.");
            } catch (NumberFormatException ex) {
                showInfo("Please enter a valid numeric job program ID.");
            }
        });
    }

    private static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
