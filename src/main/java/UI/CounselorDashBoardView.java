package UI;

import OtherComponents.LearningModule;
import OtherComponents.Message;
import UserFactory.Counselor;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
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
        Tab jobsTab = new Tab("Job Programs", createJobProgramSearchContent(counselor));
        Tab inboxTab = new Tab("Inbox", UIComponents.inboxTab(counselor, router));
        Tab contactTab = new Tab("Contact Info", UIComponents.contactInfoTab(counselor));

        tabPane.getTabs().addAll(dashboardTab, studentsTab, modulesTab, jobsTab, inboxTab, contactTab);

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
            ScrollPane scrollPane = UIComponents.darkScrollPane();
            VBox studentsVBox = new VBox(10);
            studentsVBox.setPadding(new Insets(10));

            for (Integer studentId : studentIds) {
                HashMap<String, Object> summary = counselor.getAccountSummary(studentId);
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
        viewBtn.setOnAction(e -> router.goToStudentLimitedView(studentId));
        messageBtn.setOnAction(e -> router.goToContacts());

        Button enrollBtn = new Button("Enroll in Module");
        enrollBtn.setOnAction(e -> {
            try {
                HashMap<String, Object> studentSummary = counselor.getAccountSummary(studentId);
                String learningPath = studentSummary != null && studentSummary.get("learningPath") instanceof String
                        ? (String) studentSummary.get("learningPath")
                        : null;

                List<LearningModule> modules = counselor.browseModules(learningPath);
                if (modules == null || modules.isEmpty()) {
                    modules = counselor.browseModules("All");
                }

                if (modules == null || modules.isEmpty()) {
                    UIComponents.showInfo("No modules are currently available for enrollment.");
                    return;
                }

                ComboBox<LearningModule> modulePicker = new ComboBox<>();
                modulePicker.getItems().addAll(modules);
                modulePicker.getSelectionModel().selectFirst();
                modulePicker.setMaxWidth(Double.MAX_VALUE);
                modulePicker.setConverter(new StringConverter<>() {
                    @Override public String toString(LearningModule module) {
                        return module == null
                                ? ""
                                : module.getSubject() + " (ID #" + module.getModuleID() + ") - " + module.getLearningPath();
                    }

                    @Override public LearningModule fromString(String string) { return null; }
                });

                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("Enroll Student");
                dialog.setHeaderText("Enroll " + studentName + " in a module");
                dialog.getDialogPane().setContent(new VBox(10,
                        new Label("Select Module"),
                        modulePicker
                ));
                ButtonType enrollType = new ButtonType("Enroll", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().addAll(enrollType, ButtonType.CANCEL);

                dialog.showAndWait().ifPresent(result -> {
                    if (result != enrollType) return;
                    LearningModule selected = modulePicker.getValue();
                    if (selected == null) {
                        UIComponents.showInfo("Please select a module.");
                        return;
                    }
                    try {
                        boolean enrolled = counselor.enrollAssignedStudentInModule(studentId, selected.getModuleID());
                        UIComponents.showInfo(enrolled ? "Student enrolled successfully." : "Enrollment failed.");
                    } catch (Exception ex) {
                        UIComponents.showInfo("Failed to enroll student: " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                UIComponents.showInfo("Failed to load modules: " + ex.getMessage());
            }
        });

        card.getChildren().addAll(studentLabel, new HBox(10, viewBtn, messageBtn, enrollBtn));
        return card;
    }

    private static VBox createJobProgramSearchContent(Counselor counselor) {
        VBox content = UIComponents.contentBox(12);
        content.getChildren().add(UIComponents.sectionTitle("Search Job Programs"));

        TextField searchField = new TextField();
        searchField.setPromptText("Search by role, description, or company...");

        ComboBox<String> pathFilter = UIComponents.learningPathComboWithAll();
        Button searchBtn = new Button("Search");
        VBox results = new VBox(8);

        Runnable searchAction = () -> {
            results.getChildren().clear();
            String learningPath = "All".equals(pathFilter.getValue()) ? null : pathFilter.getValue();
            List<java.util.HashMap<String, Object>> jobs = counselor.searchJobPrograms(learningPath, searchField.getText());

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
                Button recommendBtn = new Button("Recommend to Student");
                recommendBtn.setOnAction(e -> recommendJobToStudent(counselor, job));
                card.getChildren().addAll(
                        new Label("Job ID: #" + job.get("jobId")),
                        new Label("Employer ID: #" + job.get("employerId")),
                        new Label("Job: " + job.get("jobType")),
                        new Label("Learning Path: " + job.get("learningPath")),
                        new Label("Company: " + job.get("company")),
                        new Label("Employer: " + job.get("employerName")),
                        new Label("Description: " + job.get("description")),
                        recommendBtn
                );
                results.getChildren().add(card);
            }
        };

        searchBtn.setOnAction(e -> searchAction.run());
        searchAction.run();

        content.getChildren().addAll(pathFilter, searchField, searchBtn, results);
        return content;
    }

    private static void recommendJobToStudent(Counselor counselor, java.util.HashMap<String, Object> job) {
        List<Integer> assignedStudents = counselor.getAssignedStudents();
        if (assignedStudents == null || assignedStudents.isEmpty()) {
            UIComponents.showInfo("No assigned students available for recommendation.");
            return;
        }

        ComboBox<String> studentPicker = new ComboBox<>();
        java.util.HashMap<String, Integer> labelToId = new java.util.HashMap<>();

        for (Integer studentId : assignedStudents) {
            HashMap<String, Object> summary = counselor.getAccountSummary(studentId);
            String name = (summary != null && summary.get("name") != null)
                    ? (String) summary.get("name")
                    : "Student #" + studentId;
            String label = name + " (ID #" + studentId + ")";
            studentPicker.getItems().add(label);
            labelToId.put(label, studentId);
        }
        studentPicker.getSelectionModel().selectFirst();
        studentPicker.setMaxWidth(Double.MAX_VALUE);

        TextArea note = new TextArea();
        note.setPromptText("Optional note to student...");
        note.setPrefRowCount(3);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Recommend Job Program");
        dialog.setHeaderText("Send job + employer details to a student");
        dialog.getDialogPane().setContent(new VBox(10,
                new Label("Student"), studentPicker,
                new Label("Additional Note"), note
        ));
        ButtonType sendType = new ButtonType("Send Recommendation", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendType, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result != sendType) return;
            try {
                Integer studentId = labelToId.get(studentPicker.getValue());
                if (studentId == null) {
                    UIComponents.showInfo("Please select a student.");
                    return;
                }

                int jobId = ((Number) job.get("jobId")).intValue();
                int employerId = ((Number) job.get("employerId")).intValue();
                String jobType = String.valueOf(job.get("jobType"));
                String employerName = String.valueOf(job.get("employerName"));
                String company = String.valueOf(job.get("company"));
                String learningPath = String.valueOf(job.get("learningPath"));
                String description = String.valueOf(job.get("description"));

                String content = "Job Recommendation from Counselor\n"
                        + "Job ID: #" + jobId + "\n"
                        + "Employer ID: #" + employerId + "\n"
                        + "Employer: " + employerName + " (" + company + ")\n"
                        + "Job Type: " + jobType + "\n"
                        + "Learning Path: " + learningPath + "\n"
                        + "Description: " + description + "\n"
                        + (note.getText().isBlank() ? "" : ("Counselor Note: " + note.getText().trim()));

                boolean sentToStudent = counselor.sendMessage(studentId, content);
                boolean employerLinked = counselor.getDbConnector().linkEmployerContactForStudent(
                        employerId, studentId, counselor.getId(), jobId, jobType
                );

                if (sentToStudent && employerLinked) {
                    UIComponents.showInfo("Recommendation sent and employer added to student contacts.");
                } else if (sentToStudent) {
                    UIComponents.showInfo("Recommendation sent, but employer contact link could not be confirmed.");
                } else {
                    UIComponents.showInfo("Recommendation failed to send.");
                }
            } catch (Exception ex) {
                UIComponents.showInfo("Failed to send recommendation: " + ex.getMessage());
            }
        });
    }
}
