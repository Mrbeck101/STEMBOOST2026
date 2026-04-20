package UI;

import UserFactory.Counselor;
import Services.FetchProfileService;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.HashMap;
import java.util.List;

/**
 * Counselor Dashboard View
 */
public class CounselorDashBoardView {

    public static Scene create(SceneRouter router) {
        Counselor counselor = (Counselor) UIComponents.guardLogin(router);
        if (counselor == null) return LoginView.create(router);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab dashboardTab = new Tab("Dashboard", createDashboardContent(counselor));
        Tab studentsTab = new Tab("My Students", createStudentsContent(counselor, router));
        Tab modulesTab = new Tab("Browse Modules", UIComponents.browseModulesTab("Browse Modules", "Open the module catalog and filter by learning path.", router));
        Tab jobsTab = new Tab("Job Programs", createJobProgramSearchContent());
        Tab inboxTab = new Tab("Inbox", UIComponents.inboxTab(counselor, router));

        tabPane.getTabs().addAll(dashboardTab, studentsTab, modulesTab, jobsTab, inboxTab);

        return UIComponents.buildScene(
                UIComponents.topBar("STEMBOOST - Counselor Dashboard", counselor.getName(), router),
                tabPane
        );
    }

    private static VBox createDashboardContent(Counselor counselor) {
        VBox content = UIComponents.contentBox(20);

        List<Integer> students = counselor.getAssignedStudents();
        int studentCount = (students != null) ? students.size() : 0;

        content.getChildren().addAll(
                UIComponents.sectionTitle("Counselor Dashboard"),
                UIComponents.statsRow(
                        UIComponents.statCard("Assigned Students", String.valueOf(studentCount)),
                        UIComponents.statCard("Active Sessions", "0")
                )
        );
        return content;
    }

    private static VBox createStudentsContent(Counselor counselor, SceneRouter router) {
        VBox content = UIComponents.contentBox(15);
        Label title = UIComponents.sectionTitle("My Students");

        List<Integer> studentIds = counselor.getAssignedStudents();

        if (studentIds == null || studentIds.isEmpty()) {
            Label noStudents = new Label("No students assigned yet");
            noStudents.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            content.getChildren().addAll(title, noStudents);
        } else {
            FetchProfileService profileService = new FetchProfileService();
            ScrollPane scrollPane = UIComponents.darkScrollPane();
            VBox studentsVBox = new VBox(10);
            studentsVBox.setPadding(new Insets(10));

            for (Integer studentId : studentIds) {
                HashMap<String, Object> summary = profileService.getAccountSummary(studentId);
                String studentName = (summary != null && summary.get("name") != null)
                        ? (String) summary.get("name")
                        : "Student #" + studentId;
                studentsVBox.getChildren().add(createStudentCard(counselor, studentId, studentName, router));
            }

            scrollPane.setContent(studentsVBox);
            content.getChildren().addAll(title, scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }
        return content;
    }

    private static VBox createStudentCard(Counselor counselor, int studentId, String studentName, SceneRouter router) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");

        Label studentLabel = new Label(studentName + "  (ID #" + studentId + ")");
        studentLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        Button viewBtn = new Button("View Profile");
        Button messageBtn = new Button("Send Message");
        viewBtn.setOnAction(e -> router.goToStudentProfile(studentId));
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
                    UIComponents.showInfo(enrolled ? "Student enrolled successfully." : "Enrollment failed.");
                } catch (NumberFormatException ex) {
                    UIComponents.showInfo("Please enter a valid numeric module ID.");
                } catch (Exception ex) {
                    UIComponents.showInfo("Failed to enroll student: " + ex.getMessage());
                }
            });
        });

        card.getChildren().addAll(studentLabel, new HBox(10, viewBtn, messageBtn, enrollBtn));
        return card;
    }

    private static VBox createJobProgramSearchContent() {
        VBox content = UIComponents.contentBox(12);
        content.getChildren().add(UIComponents.sectionTitle("Search Job Programs"));

        TextField searchField = new TextField();
        searchField.setPromptText("Search by role, description, or company...");

        ComboBox<String> pathFilter = UIComponents.learningPathComboWithAll();
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

        content.getChildren().addAll(pathFilter, searchField, searchBtn, results);
        return content;
    }
}
