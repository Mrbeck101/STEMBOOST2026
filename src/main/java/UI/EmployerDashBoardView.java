package UI;

import UserFactory.Employer;
import OtherComponents.Assessment;
import OtherComponents.JobProgram;
import OtherComponents.LearningModule;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.HashMap;
import java.util.List;

/**
 * Employer Dashboard View
 */
public class EmployerDashBoardView {

    private static final String[] JOB_TYPES = {
            "Internship",
            "Job Shadowing",
            "Project contributor"
    };

    public static Scene create(SceneRouter router) {
        Employer employer = (Employer) UIComponents.guardLogin(router);
        if (employer == null) return LoginView.create(router);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
                new Tab("Dashboard", createDashboardContent(employer)),
                new Tab("Job Programs", createJobsContent(employer, router)),
                new Tab("Browse Modules", UIComponents.browseModulesTab("Browse Modules", "Review all available learning modules and filter by learning path.", router)),
                new Tab("Inbox", UIComponents.inboxTab(employer, router)),
                new Tab("Contact Info", UIComponents.contactInfoTab(employer))
        );

        return UIComponents.buildScene(
                UIComponents.topBarWithSubtitle("STEMBOOST - Employer Portal", employer.getName(), "Company: " + employer.getCompany(), router),
                tabPane
        );
    }

    private static VBox createDashboardContent(Employer employer) {
        VBox content = UIComponents.contentBox(20);

        List<JobProgram> jobs = employer.getJobPrograms();
        int jobCount = (jobs != null) ? jobs.size() : 0;

        content.getChildren().addAll(
                UIComponents.sectionTitle("Employer Dashboard"),
                UIComponents.statsRow(
                        UIComponents.statCard("Active Job Programs", String.valueOf(jobCount)),
                        UIComponents.statCard("Qualified Candidates", "0")
                )
        );
        return content;
    }

    private static VBox createJobsContent(Employer employer, SceneRouter router) {
        VBox content = UIComponents.contentBox(15);
        Label title = UIComponents.sectionTitle("Job Programs");

        Button createJobBtn = new Button("Post New Job");
        createJobBtn.setStyle("-fx-font-size: 12;");
        createJobBtn.setOnAction(e -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Post New Job");
            dialog.setHeaderText("Create a new job program");

            ComboBox<String> jobTypeCombo = new ComboBox<>();
            jobTypeCombo.getItems().addAll(JOB_TYPES);
            jobTypeCombo.setValue("Internship");
            jobTypeCombo.setMaxWidth(Double.MAX_VALUE);
            ComboBox<String> pathCombo = UIComponents.learningPathCombo();

            var db = employer.getDbConnector();
            ComboBox<String> moduleCombo = new ComboBox<>();
            moduleCombo.setMaxWidth(Double.MAX_VALUE);
            HashMap<String, Integer> moduleLabelToId = new HashMap<>();
            for (LearningModule module : db.searchAllModulesDB()) {
                String label = "#" + module.getModuleID() + " - " + module.getSubject();
                moduleCombo.getItems().add(label);
                moduleLabelToId.put(label, module.getModuleID());
            }
            if (!moduleCombo.getItems().isEmpty()) {
                moduleCombo.getSelectionModel().selectFirst();
            }

            ComboBox<String> assessmentCombo = new ComboBox<>();
            assessmentCombo.setMaxWidth(Double.MAX_VALUE);
            HashMap<String, Integer> assessmentLabelToId = new HashMap<>();

            Runnable refreshAssessments = () -> {
                assessmentCombo.getItems().clear();
                assessmentLabelToId.clear();
                String selectedModule = moduleCombo.getValue();
                if (selectedModule == null) {
                    assessmentCombo.getItems().add("None");
                    assessmentCombo.setValue("None");
                    return;
                }
                Integer moduleId = moduleLabelToId.get(selectedModule);
                List<Assessment> assessments = moduleId == null ? List.of() : db.searchAssessmentDB(moduleId, "Educator");
                if (assessments.isEmpty()) {
                    assessmentCombo.getItems().add("None");
                    assessmentCombo.setValue("None");
                    return;
                }
                for (Assessment assessment : assessments) {
                    String label = "#" + assessment.getAssessmentID() + " - " + assessment.getModuleSubject();
                    assessmentCombo.getItems().add(label);
                    assessmentLabelToId.put(label, assessment.getAssessmentID());
                }
                assessmentCombo.getSelectionModel().selectFirst();
            };
            moduleCombo.setOnAction(e2 -> refreshAssessments.run());
            refreshAssessments.run();

            TextArea descriptionArea = new TextArea();
            descriptionArea.setPromptText("Describe responsibilities, required skills, and include any inclusivity tools the role offers.");

            VBox form = new VBox(10,
                    new Label("Job Type"), jobTypeCombo,
                    new Label("Learning Path"), pathCombo,
                    new Label("Module Requirement"), moduleCombo,
                    new Label("Assessment Requirement"), assessmentCombo,
                    new Label("Description (include any inclusivity tools offered)"), descriptionArea
            );
            form.setPadding(new Insets(10));

            dialog.getDialogPane().setContent(form);
            ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

            dialog.showAndWait().ifPresent(result -> {
                if (result != saveType) return;
                try {
                    Integer selectedModuleId = moduleLabelToId.get(moduleCombo.getValue());
                    if (selectedModuleId == null) {
                        UIComponents.showInfo("Please select a module requirement.");
                        return;
                    }

                    int selectedAssessmentId = 0;
                    String selectedAssessment = assessmentCombo.getValue();
                    if (selectedAssessment != null && !"None".equals(selectedAssessment)) {
                        Integer aid = assessmentLabelToId.get(selectedAssessment);
                        selectedAssessmentId = aid == null ? 0 : aid;
                    }

                    JobProgram job = new JobProgram(
                            employer.getId(), 0, true,
                            selectedModuleId,
                            selectedAssessmentId,
                            pathCombo.getValue(),
                            descriptionArea.getText().trim(),
                            jobTypeCombo.getValue()
                    );
                    boolean saved = employer.saveJobProgram(job);
                    UIComponents.showInfo(saved ? "Job program posted." : "Failed to post job program.");
                    if (saved) router.goToDashboard(employer.getId(), "Employer");
                } catch (Exception ex) {
                    UIComponents.showInfo("Failed to post job program: " + ex.getMessage());
                }
            });
        });

        List<JobProgram> jobs = employer.getJobPrograms();

        if (jobs == null || jobs.isEmpty()) {
            Label noJobs = new Label("No job programs posted yet");
            noJobs.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            content.getChildren().addAll(title, createJobBtn, noJobs);
        } else {
            ScrollPane scrollPane = UIComponents.darkScrollPane();
            VBox jobsVBox = new VBox(10);
            jobsVBox.setPadding(new Insets(10));

            for (JobProgram job : jobs) {
                jobsVBox.getChildren().add(createJobCard(job, employer, router));
            }

            scrollPane.setContent(jobsVBox);
            content.getChildren().addAll(title, createJobBtn, scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }
        return content;
    }

    private static VBox createJobCard(JobProgram job, Employer employer, SceneRouter router) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");

        Label jobTitle = new Label(job.getJobType());
        jobTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        Label pathLabel = new Label("Learning Path: " + job.getPreferredLearningPath());
        pathLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");

        Label descLabel = new Label("Description: " + job.getDescription());
        descLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #c9d1d9;");
        descLabel.setWrapText(true);

        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Close");

        editBtn.setOnAction(e -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Edit Job Program");
            dialog.setHeaderText("Update job details");

            ComboBox<String> jobTypeCombo = new ComboBox<>();
            jobTypeCombo.getItems().addAll(JOB_TYPES);
            if (job.getJobType() != null && !job.getJobType().isBlank()) {
                jobTypeCombo.setValue(job.getJobType());
            }
            if (jobTypeCombo.getValue() == null) {
                jobTypeCombo.setValue("Internship");
            }
            jobTypeCombo.setMaxWidth(Double.MAX_VALUE);

            TextArea descriptionArea = new TextArea(job.getDescription());
            descriptionArea.setPromptText("Describe responsibilities, required skills, and include any inclusivity tools the role offers.");
            descriptionArea.setPrefRowCount(4);

            VBox form = new VBox(10,
                    new Label("Job Type"), jobTypeCombo,
                    new Label("Description (include any inclusivity tools offered)"), descriptionArea
            );
            form.setPadding(new Insets(10));

            dialog.getDialogPane().setContent(form);
            ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

            dialog.showAndWait().ifPresent(result -> {
                if (result != saveType) return;
                job.setJobType(jobTypeCombo.getValue());
                job.setDescription(descriptionArea.getText().trim());
                boolean updated = employer.updateJobProgram(job);
                UIComponents.showInfo(updated ? "Job program updated." : "Job update failed.");
                if (updated) router.goToDashboard(employer.getId(), "Employer");
            });
        });

        deleteBtn.setOnAction(e -> {
            boolean deleted = employer.deleteJobProgram(job.getJobID());
            UIComponents.showInfo(deleted ? "Job program closed." : "Failed to close job program.");
            if (deleted) router.goToDashboard(employer.getId(), "Employer");
        });

        card.getChildren().addAll(jobTitle, pathLabel, descLabel, new HBox(10, editBtn, deleteBtn));
        return card;
    }
}
