package UI;

import UserFactory.Counselor;
import Services.FetchProfileService;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import java.util.List;

/**
 * Counselor Dashboard View
 */
public class CounselorDashBoardView {

    public static Scene create(SceneRouter router) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Counselor counselor = (Counselor) UserContext.getInstance().getCurrentUser();
        if (counselor == null) {
            return LoginView.create(router);
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        HBox topBar = createTopBar(counselor, router);
        root.setTop(topBar);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab dashboardTab = new Tab("Dashboard", createDashboardContent(counselor));
        Tab studentsTab = new Tab("My Students", createStudentsContent(counselor, router));
        Tab modulesTab = new Tab("Browse Modules", createBrowseModulesContent(router));
        Tab jobsTab = new Tab("Job Programs", createJobProgramSearchContent());
        Tab inboxTab = new Tab("Inbox", createInboxContent(counselor, router));

        tabPane.getTabs().addAll(dashboardTab, studentsTab, modulesTab, jobsTab, inboxTab);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1400, 900);
        return scene;
    }

    private static HBox createTopBar(Counselor counselor, SceneRouter router) {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label appTitle = new Label("STEMBOOST - Counselor Dashboard");
        appTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label nameLabel = new Label("Welcome, " + counselor.getName());
        nameLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #ffffff;");

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> router.goToLogin());

        topBar.getChildren().addAll(appTitle, spacer, nameLabel, logoutBtn);
        return topBar;
    }

    private static VBox createDashboardContent(Counselor counselor) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Counselor Dashboard");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        List<Integer> students = counselor.getAssignedStudents();
        int studentCount = (students != null) ? students.size() : 0;

        HBox statsBox = new HBox(15);
        VBox studentCard = createStatCard("Assigned Students", String.valueOf(studentCount), "Students under your counseling");
        VBox activeCard = createStatCard("Active Sessions", "0", "Ongoing counseling sessions");

        statsBox.getChildren().addAll(studentCard, activeCard);
        HBox.setHgrow(studentCard, Priority.ALWAYS);
        HBox.setHgrow(activeCard, Priority.ALWAYS);

        content.getChildren().addAll(title, statsBox);
        return content;
    }

    private static VBox createStudentsContent(Counselor counselor, SceneRouter router) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("My Students");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        List<Integer> studentIds = counselor.getAssignedStudents();

        if (studentIds == null || studentIds.isEmpty()) {
            Label noStudents = new Label("No students assigned yet");
            noStudents.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            content.getChildren().addAll(title, noStudents);
        } else {
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: #0D1117;");

            VBox studentsVBox = new VBox(10);
            studentsVBox.setPadding(new Insets(10));

            for (Integer studentId : studentIds) {
                VBox studentCard = createStudentCard(counselor, studentId, router);
                studentsVBox.getChildren().add(studentCard);
            }

            scrollPane.setContent(studentsVBox);
            content.getChildren().addAll(title, scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }

        return content;
    }

    private static VBox createStudentCard(Counselor counselor, int studentId, SceneRouter router) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");

        Label studentLabel = new Label("Student ID: " + studentId);
        studentLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        HBox buttonsBox = new HBox(10);
        Button viewBtn = new Button("View Profile");
        Button messageBtn = new Button("Send Message");
        viewBtn.setOnAction(e -> showInfo("Student profile view for ID #" + studentId + " is shown in student services pane."));
        messageBtn.setOnAction(e -> router.goToContacts());

        Button enrollBtn = new Button("Enroll in Module");
        enrollBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Enroll Student");
            dialog.setHeaderText("Enroll student #" + studentId + " in a module");
            dialog.setContentText("Module ID:");

            dialog.showAndWait().ifPresent(moduleValue -> {
                try {
                    int moduleId = Integer.parseInt(moduleValue.trim());
                    boolean enrolled = counselor.enrollAssignedStudentInModule(studentId, moduleId);
                    showInfo(enrolled ? "Student enrolled successfully." : "Enrollment failed.");
                } catch (NumberFormatException ex) {
                    showInfo("Please enter a valid numeric module ID.");
                } catch (Exception ex) {
                    showInfo("Failed to enroll student: " + ex.getMessage());
                }
            });
        });

        buttonsBox.getChildren().addAll(viewBtn, messageBtn, enrollBtn);

        card.getChildren().addAll(studentLabel, buttonsBox);
        return card;
    }

    private static VBox createInboxContent(Counselor counselor, SceneRouter router) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Inbox");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Button viewMessagesBtn = new Button("View Messages");
        viewMessagesBtn.setOnAction(e -> router.goToInbox());

        Label messageCount = new Label("No new messages");
        messageCount.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");

        content.getChildren().addAll(title, viewMessagesBtn, messageCount);
        return content;
    }

    private static VBox createBrowseModulesContent(SceneRouter router) {
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Browse Modules");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label description = new Label("Open the module catalog and filter by learning path.");
        description.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");

        Button openModulesBtn = new Button("Open Module Browser");
        openModulesBtn.setOnAction(e -> router.goToModules());

        content.getChildren().addAll(title, description, openModulesBtn);
        return content;
    }

    private static VBox createJobProgramSearchContent() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Search Job Programs");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by role, description, or company...");

        ComboBox<String> pathFilter = new ComboBox<>();
        pathFilter.getItems().addAll("All", "Electrical Engineering", "Software Engineering", "Information Technology", "Cybersecurity", "Computer Engineering", "Artificial Intelligence");
        pathFilter.setValue("All");

        Button searchBtn = new Button("Search");
        VBox results = new VBox(8);

        FetchProfileService service = new FetchProfileService();
        Runnable searchAction = () -> {
            results.getChildren().clear();
            String learningPath = "All".equals(pathFilter.getValue()) ? null : pathFilter.getValue();
            List<java.util.HashMap<String, Object>> jobs = service.searchJobPrograms(learningPath, searchField.getText());

            if (jobs.isEmpty()) {
                Label noResults = new Label("No job programs found");
                noResults.setStyle("-fx-text-fill: #8b949e;");
                results.getChildren().add(noResults);
                return;
            }

            for (java.util.HashMap<String, Object> job : jobs) {
                VBox card = new VBox(4);
                card.setPadding(new Insets(10));
                card.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-radius: 5;");
                card.getChildren().addAll(
                        new Label("Job: " + job.get("jobType")),
                        new Label("Learning Path: " + job.get("learningPath")),
                        new Label("Company: " + job.get("company")),
                        new Label("Employer: " + job.get("employerName")),
                        new Label("Description: " + job.get("description"))
                );
                results.getChildren().add(card);
            }
        };

        searchBtn.setOnAction(e -> searchAction.run());
        searchAction.run();

        content.getChildren().addAll(title, pathFilter, searchField, searchBtn, results);
        return content;
    }

    private static VBox createStatCard(String title, String value, String description) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
