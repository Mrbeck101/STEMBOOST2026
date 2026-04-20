package UI;

import UserFactory.Employer;
import DatabaseController.dbConnector;
import OtherComponents.JobProgram;
import Services.FetchProfileService;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

/**
 * Employer Dashboard View
 */
public class EmployerDashBoardView {

    public static Scene create(SceneRouter router) {
        Employer employer = (Employer) UIComponents.guardLogin(router);
        if (employer == null) return LoginView.create(router);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
                new Tab("Dashboard", createDashboardContent(employer)),
                new Tab("Job Programs", createJobsContent(employer, router)),
                new Tab("Browse Modules", UIComponents.browseModulesTab("Browse Modules", "Review all available learning modules and filter by learning path.", router)),
                new Tab("Inbox", UIComponents.inboxTab(employer, router))
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

            TextField jobTypeField = new TextField();
            jobTypeField.setPromptText("Job Type");
            ComboBox<String> pathCombo = UIComponents.learningPathCombo();
            TextField modReqField = new TextField("0");
            TextField assessmentReqField = new TextField("0");
            TextArea descriptionArea = new TextArea();
            descriptionArea.setPromptText("Job Description");

            VBox form = new VBox(10,
                    new Label("Job Type"), jobTypeField,
                    new Label("Learning Path"), pathCombo,
                    new Label("Module Requirement"), modReqField,
                    new Label("Assessment Requirement"), assessmentReqField,
                    new Label("Description"), descriptionArea
            );
            form.setPadding(new Insets(10));

            dialog.getDialogPane().setContent(form);
            ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

            dialog.showAndWait().ifPresent(result -> {
                if (result != saveType) return;
                try {
                    JobProgram job = new JobProgram(
                            employer.getId(), 0, true,
                            Integer.parseInt(modReqField.getText().trim()),
                            Integer.parseInt(assessmentReqField.getText().trim()),
                            pathCombo.getValue(),
                            descriptionArea.getText().trim(),
                            jobTypeField.getText().trim()
                    );
                    boolean saved = new dbConnector().addJobProgram(job);
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
            TextInputDialog dd = new TextInputDialog(job.getDescription());
            dd.setTitle("Edit Job Program");
            dd.setHeaderText("Update description for " + job.getJobType());
            dd.setContentText("Description:");
            dd.showAndWait().ifPresent(desc -> {
                job.setDescription(desc.trim());
                boolean updated = new dbConnector().updateJobProgram(job);
                UIComponents.showInfo(updated ? "Job program updated." : "Job update failed.");
                if (updated) router.goToDashboard(employer.getId(), "Employer");
            });
        });

        deleteBtn.setOnAction(e -> {
            boolean deleted = new FetchProfileService().deleteJobProgram(job.getJobID());
            UIComponents.showInfo(deleted ? "Job program closed." : "Failed to close job program.");
            if (deleted) router.goToDashboard(employer.getId(), "Employer");
        });

        card.getChildren().addAll(jobTitle, pathLabel, descLabel, new HBox(10, editBtn, deleteBtn));
        return card;
    }
}
