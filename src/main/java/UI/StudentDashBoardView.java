package UI;

import UserFactory.Student;
import OtherComponents.LearningModule;
import OtherComponents.Assessment;
import Services.KeyboardTtsService;
import Services.UIRefreshService;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.AccessibleRole;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.beans.value.ChangeListener;
import java.util.List;

/**
 * Student Dashboard View with accessibility features for visually impaired learners
 */
public class StudentDashBoardView {

    public static Scene create(SceneRouter router) {
        Student student = (Student) UIComponents.guardLogin(router);
        if (student == null) return LoginView.create(router);

        UIRefreshService.getInstance().startPolling(student);

        Button[] inboxViewButtonRef = new Button[1];

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab dashboardTab = new Tab("Dashboard", createDashboardContent(student));
        Tab modulesTab = new Tab("Learning Modules", createModulesContent(student, router));
        Tab assessmentsTab = new Tab("Assessments", createAssessmentsContent(student, router));
        Tab inboxTab = new Tab("Inbox", createInboxContent(student, router, inboxViewButtonRef));

        tabPane.getTabs().addAll(
                dashboardTab,
                modulesTab,
                assessmentsTab,
                inboxTab
        );

        // Student top bar includes university subtitle
        HBox topBar = UIComponents.topBarWithSubtitle(
                "STEMBOOST - Student Dashboard", student.getName(), "University: " + student.getUniversity(), router);

        Scene scene = UIComponents.buildScene(topBar, tabPane);

        KeyboardTtsService.getInstance().bindScene(
                scene,
                KeyboardTtsService.AccessMode.STUDENT_ONLY,
                () -> new KeyboardTtsService.ReadingContent(
                        buildDashboardNarration(student)
                ),
                tabPane::requestFocus
        );

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, selectedTab) -> {
            if (selectedTab == null) {
                return;
            }

            KeyboardTtsService tts = KeyboardTtsService.getInstance();
            if (selectedTab == dashboardTab) {
                tts.speakNow(buildDashboardNarration(student));
            } else if (selectedTab == modulesTab) {
                tts.speakNow("You are currently on learning modules, you can Request a new learning Module or request job support from your counselor, as well as go through your active modules");
            } else if (selectedTab == assessmentsTab) {
                tts.speakNow(buildAssessmentNarration(student));
            } else if (selectedTab == inboxTab) {
                int newMessages = student.getUnreadMessageCount();
                if (inboxViewButtonRef[0] != null) {
                    inboxViewButtonRef[0].requestFocus();
                }
                tts.speakNow("You have " + newMessages + " new message" + (newMessages == 1 ? "" : "s") + ". If you want to see your inbox press enter");
            }
        });

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() != javafx.scene.input.KeyCode.ESCAPE) {
                return;
            }

            if (tabPane.getSelectionModel().getSelectedItem() == dashboardTab) {
                router.goToLogin();
            } else {
                tabPane.getSelectionModel().select(dashboardTab);
            }
            e.consume();
        });

        UIRefreshService.UIRefreshListener modulesListener = (updateType, data) -> {
            if ("MODULES_UPDATED".equals(updateType)) {
                modulesTab.setContent(createModulesContent(student, router));
            }
        };
        UIRefreshService.getInstance().addListener(modulesListener);

        scene.windowProperty().addListener((obs, oldWindow, newWindow) -> {
            if (newWindow instanceof Stage stage) {
                ChangeListener<Scene> sceneChangeListener = new ChangeListener<>() {
                    @Override
                    public void changed(javafx.beans.value.ObservableValue<? extends Scene> observable, Scene previous, Scene current) {
                        if (current != scene) {
                            UIRefreshService.getInstance().removeListener(modulesListener);
                            stage.sceneProperty().removeListener(this);
                        }
                    }
                };
                stage.sceneProperty().addListener(sceneChangeListener);
            }
        });

        return scene;
    }

    private static VBox createDashboardContent(Student student) {
        VBox content = UIComponents.contentBox(20);

        VBox welcomeCard = new VBox(15);
        welcomeCard.setPadding(new Insets(20));
        welcomeCard.setStyle("-fx-background-color: #161B22; -fx-border-radius: 8;");
        welcomeCard.setAccessibleText("Welcome section containing your learning path and progress overview");

        Label welcomeTitle = new Label("Your Learning Journey");
        welcomeTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label learningPathLabel = new Label("Learning Path: " + student.getLearningPath());
        learningPathLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #c9d1d9;");
        Label progressLabel = new Label("Overall Progress: In Progress");
        progressLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #c9d1d9;");

        welcomeCard.getChildren().addAll(welcomeTitle, learningPathLabel, progressLabel);

        List<LearningModule> modules = student.getDbConnector().searchModulesDB(student.getId(), "Student");
        int activeModules = modules == null ? 0 : modules.size();
        List<Assessment> assessments = student.getAssessmentResults();
        int pendingAssessments = assessments == null ? 0 : (int) assessments.stream().filter(a -> !a.isCompleted()).count();
        double averageGrade = assessments == null ? 0.0 : assessments.stream()
                .filter(a -> a.getGrade() >= 0).mapToInt(Assessment::getGrade).average().orElse(0.0);

        HBox statsBox = UIComponents.statsRow(
                UIComponents.statCard("Active Modules", String.valueOf(activeModules)),
                UIComponents.statCard("Pending Assessments", String.valueOf(pendingAssessments)),
                UIComponents.statCard("Average Assessment", String.format("%.1f%%", averageGrade))
        );
        statsBox.setAccessibleText("Quick statistics section");

        content.getChildren().addAll(welcomeCard, statsBox);
        return content;
    }

    private static VBox createModulesContent(Student student, SceneRouter router) {
        VBox content = UIComponents.contentBox(15);

        Label title = UIComponents.sectionTitle("Learning Modules");
        title.setAccessibleText("Learning Modules section");

        Button requestModuleBtn = new Button("Request New Learning Module");
        requestModuleBtn.setOnAction(e -> showModuleRequestDialog(student));
        Button requestJobBtn = new Button("Request Job Module");
        requestJobBtn.setOnAction(e -> showJobRequestDialog(student));
        addFocusNarration(requestModuleBtn, "If you would like to request a new learning modules press Enter");
        addFocusNarration(requestJobBtn, "If you would like to request job support from your counselor press enter");

        List<LearningModule> modules = student.getLearningModules();

        if (modules == null || modules.isEmpty()) {
            Label noModules = new Label("No modules enrolled yet");
            noModules.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            noModules.setAccessibleText("You have no modules enrolled yet");
            content.getChildren().addAll(title, new HBox(10, requestModuleBtn, requestJobBtn), noModules);
        } else {
            ScrollPane scrollPane = UIComponents.darkScrollPane();
            VBox modulesVBox = new VBox(10);
            modulesVBox.setPadding(new Insets(10));
            for (LearningModule module : modules) {
                modulesVBox.getChildren().add(createModuleCard(module, router));
            }
            scrollPane.setContent(modulesVBox);
            content.getChildren().addAll(title, new HBox(10, requestModuleBtn, requestJobBtn), scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }
        return content;
    }

    private static VBox createModuleCard(LearningModule module, SceneRouter router) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");
        card.setAccessibleRole(AccessibleRole.NODE);

        Label moduleTitle = new Label(module.getSubject());
        moduleTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");
        moduleTitle.setAccessibleText("Module: " + module.getSubject());

        Label modulePath = new Label("Path: " + module.getLearningPath());
        modulePath.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");

        double progress = module.getProgress() / 100.0;
        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setAccessibleText("Module progress: " + module.getProgress() + " percent complete");
        progressBar.setStyle("-fx-accent: #238636;");

        Label progressLabel = new Label(module.getProgress() + "% Complete");
        progressLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #8b949e;");

        Button openModuleBtn = new Button("Open Module");
        openModuleBtn.setOnAction(e -> { UserContext.getInstance().setSelectedModuleId(module.getModuleID()); router.goToModules(); });
        addFocusNarration(openModuleBtn,
                module.getSubject() + ", " + module.getProgress() + " percent completed, if you wish to continue this module press enter");

        card.getChildren().addAll(moduleTitle, modulePath, progressBar, progressLabel, openModuleBtn);
        return card;
    }

    private static VBox createAssessmentsContent(Student student, SceneRouter router) {
        VBox content = UIComponents.contentBox(15);

        Label title = UIComponents.sectionTitle("Assessments");
        title.setAccessibleText("Assessments section");

        List<Assessment> assessments = student.getAssessmentResults();

        if (assessments == null || assessments.isEmpty()) {
            Label noAssessments = new Label("No assessments available");
            noAssessments.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            noAssessments.setAccessibleText("You have no pending assessments");
            content.getChildren().addAll(title, noAssessments);
        } else {
            ScrollPane scrollPane = UIComponents.darkScrollPane();
            VBox assessmentsVBox = new VBox(10);
            assessmentsVBox.setPadding(new Insets(10));
            for (Assessment assessment : assessments) {
                assessmentsVBox.getChildren().add(createAssessmentCard(assessment, router));
            }
            scrollPane.setContent(assessmentsVBox);
            content.getChildren().addAll(title, scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }
        return content;
    }

    private static VBox createAssessmentCard(Assessment assessment, SceneRouter router) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");
        card.setAccessibleRole(AccessibleRole.NODE);

        String moduleSubject = (assessment.getModuleSubject() == null || assessment.getModuleSubject().isBlank())
                ? "Module #" + assessment.getModuleID() : assessment.getModuleSubject();

        Label assessmentTitle = new Label("Assessment: " + moduleSubject);
        assessmentTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ffa657;");
        assessmentTitle.setAccessibleText("Assessment for module subject: " + moduleSubject);

        Label pathLabel = new Label("Learning Path: " + assessment.getLearningPath());
        pathLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");

        Label gradeLabel = new Label("Grade: " + (assessment.getGrade() >= 0 ? assessment.getGrade() + "%" : "Not yet graded"));
        gradeLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
        gradeLabel.setAccessibleText("Grade: " + (assessment.getGrade() >= 0 ? assessment.getGrade() + " percent" : "Not yet graded"));

        if (assessment.isCompleted()) {
            Label completedLabel = new Label("Assessment " + assessment.getAssessmentID() + ": " + moduleSubject);
            completedLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #c9d1d9;");
            card.getChildren().addAll(assessmentTitle, pathLabel, completedLabel, gradeLabel);
            return card;
        }

        Button takeButton = new Button("Take Assessment");
        takeButton.setStyle("-fx-font-size: 11;");
        takeButton.setAccessibleText("Take this assessment");
        takeButton.setOnAction(e -> {
            Student currentStudent = (Student) UserContext.getInstance().getCurrentUser();
            String submissionJson = AssessmentFormRenderer.collectStudentResponses(assessment);
            if (submissionJson == null) return;
            try {
                boolean markedComplete = currentStudent.updateAssessmentCompletion(assessment.getAssessmentID(), true, submissionJson);
                UIComponents.showInfo(markedComplete
                        ? "Assessment responses submitted successfully."
                        : "Could not save your assessment submission.");
                if (markedComplete) router.goToDashboard(currentStudent.getId(), "Student");
            } catch (Exception ex) {
                UIComponents.showInfo("Failed to submit assessment: " + ex.getMessage());
            }
        });

        card.getChildren().addAll(assessmentTitle, pathLabel, gradeLabel, takeButton);
        return card;
    }

    private static void showModuleRequestDialog(Student student) {
        TextInputDialog pathDialog = new TextInputDialog(student.getLearningPath());
        pathDialog.setTitle("Module Request");
        pathDialog.setHeaderText("Request a new learning module");
        pathDialog.setContentText("Learning path:");
        UIComponents.preparePopupForStudentTts(pathDialog);
        pathDialog.showAndWait().ifPresent(path -> {
            boolean sent = student.requestLearningModule(path, "Requested from student dashboard");
            UIComponents.showInfo(sent ? "Request sent to counselor." : "Failed to send request.");
        });
    }

    private static void showJobRequestDialog(Student student) {
        try {
            boolean sent = student.requestWorkProgram("Student requested job support from the dashboard.");
            UIComponents.showInfo(sent
                    ? "A request has been sent to your counselor."
                    : "Failed to send your request to your counselor.");
        } catch (Exception ex) {
            UIComponents.showInfo("Failed to send your request to your counselor: " + ex.getMessage());
        }
    }

    private static VBox createInboxContent(Student student, SceneRouter router, Button[] inboxViewButtonRef) {
        VBox content = UIComponents.contentBox(15);

        Label title = UIComponents.sectionTitle("Inbox");
        Button viewBtn = new Button("View Messages");
        viewBtn.setOnAction(e -> router.goToInbox());
        addFocusNarration(viewBtn, "If you want to see your inbox press enter");

        int count = student.getUnreadMessageCount();
        Label countLabel = new Label(count > 0
                ? "You have " + count + " new message" + (count == 1 ? "" : "s")
                : "No new messages");
        countLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");

        inboxViewButtonRef[0] = viewBtn;
        content.getChildren().addAll(title, viewBtn, countLabel);
        return content;
    }

    private static String buildDashboardNarration(Student student) {
        List<LearningModule> modules = student.getDbConnector().searchModulesDB(student.getId(), "Student");
        int activeModules = modules == null ? 0 : modules.size();
        List<Assessment> assessments = student.getAssessmentResults();
        int pendingAssessments = assessments == null ? 0 : (int) assessments.stream().filter(a -> !a.isCompleted()).count();
        return "You are currently on your dashboard, their are 4 tabs that being Dashboard, Learning Modules, Assessments, and Inbox. You can use your arrow keys to navigate through these menus. "
                + "You currently have " + activeModules + " active module" + (activeModules == 1 ? "" : "s")
                + " and " + pendingAssessments + " pending assessment" + (pendingAssessments == 1 ? "" : "s") + ".";
    }

    private static String buildAssessmentNarration(Student student) {
        List<Assessment> assessments = student.getAssessmentResults();
        if (assessments == null || assessments.isEmpty()) {
            return "You currently do not have any assessments available.";
        }

        List<Assessment> pending = assessments.stream().filter(a -> !a.isCompleted()).toList();
        List<Assessment> completed = assessments.stream().filter(Assessment::isCompleted).toList();
        StringBuilder text = new StringBuilder("You are currently on assessments. ");
        List<Assessment> target = pending.isEmpty() ? completed : pending;
        for (Assessment assessment : target) {
            String subject = (assessment.getModuleSubject() == null || assessment.getModuleSubject().isBlank())
                    ? "Module " + assessment.getModuleID()
                    : assessment.getModuleSubject();
            text.append("Assessment ").append(assessment.getAssessmentID())
                    .append(" for ").append(subject).append(". ");
            if (assessment.isCompleted()) {
                text.append("Completed with grade ")
                        .append(assessment.getGrade() >= 0 ? assessment.getGrade() + " percent. " : "not yet graded. ");
            } else {
                text.append("Pending. ");
            }
        }
        return text.toString();
    }

    private static void addFocusNarration(Control control, String message) {
        control.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                KeyboardTtsService.getInstance().speakNow(message);
            }
        });
    }
}
