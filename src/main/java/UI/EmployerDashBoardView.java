package UI;

import UserFactory.Employer;
import DatabaseController.dbConnector;
import OtherComponents.JobProgram;
import Services.FetchProfileService;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import java.util.List;

/**
 * Employer Dashboard View
 */
public class EmployerDashBoardView {

    public static Scene create(SceneRouter router) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Employer employer = (Employer) UserContext.getInstance().getCurrentUser();
        if (employer == null) {
            return LoginView.create(router);
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        HBox topBar = createTopBar(employer, router);
        root.setTop(topBar);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab dashboardTab = new Tab("Dashboard", createDashboardContent(employer));
        Tab jobsTab = new Tab("Job Programs", createJobsContent(employer, router));
        Tab modulesTab = new Tab("Browse Modules", createBrowseModulesContent(router));
        Tab inboxTab = new Tab("Inbox", createInboxContent(employer, router));

        tabPane.getTabs().addAll(dashboardTab, jobsTab, modulesTab, inboxTab);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1400, 900);
        return scene;
    }

    private static HBox createTopBar(Employer employer, SceneRouter router) {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label appTitle = new Label("STEMBOOST - Employer Portal");
        appTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox infoBox = new VBox(5);
        Label nameLabel = new Label("Welcome, " + employer.getName());
        nameLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #ffffff;");
        Label companyLabel = new Label("Company: " + employer.getCompany());
        companyLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #aaaaaa;");
        infoBox.getChildren().addAll(nameLabel, companyLabel);

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> router.goToLogin());

        topBar.getChildren().addAll(appTitle, spacer, infoBox, logoutBtn);
        return topBar;
    }

    private static VBox createDashboardContent(Employer employer) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Employer Dashboard");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        List<JobProgram> jobs = employer.getJobPrograms();
        int jobCount = (jobs != null) ? jobs.size() : 0;

        HBox statsBox = new HBox(15);
        VBox jobCard = createStatCard("Active Job Programs", String.valueOf(jobCount), "Number of active job postings");
        VBox applicantsCard = createStatCard("Qualified Candidates", "0", "Students matching requirements");

        statsBox.getChildren().addAll(jobCard, applicantsCard);
        HBox.setHgrow(jobCard, Priority.ALWAYS);
        HBox.setHgrow(applicantsCard, Priority.ALWAYS);

        content.getChildren().addAll(title, statsBox);
        return content;
    }

    private static VBox createJobsContent(Employer employer, SceneRouter router) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Job Programs");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Button createJobBtn = new Button("Post New Job");
        createJobBtn.setStyle("-fx-font-size: 12;");
        createJobBtn.setOnAction(e -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Post New Job");
            dialog.setHeaderText("Create a new job program");

            TextField jobTypeField = new TextField();
            jobTypeField.setPromptText("Job Type");
            ComboBox<String> pathCombo = new ComboBox<>();
            pathCombo.getItems().addAll("Electrical Engineering", "Software Engineering", "Information Technology", "Cybersecurity", "Computer Engineering", "Artificial Intelligence");
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
                if (result != saveType) {
                    return;
                }
                try {
                    JobProgram job = new JobProgram(
                            employer.getId(),
                            0,
                            true,
                            Integer.parseInt(modReqField.getText().trim()),
                            Integer.parseInt(assessmentReqField.getText().trim()),
                            pathCombo.getValue(),
                            descriptionArea.getText().trim(),
                            jobTypeField.getText().trim()
                    );
                    boolean saved = new dbConnector().addJobProgram(job);
                    showInfo(saved ? "Job program posted." : "Failed to post job program.");
                    if (saved) {
                        router.goToDashboard(employer.getId(), "Employer");
                    }
                } catch (Exception ex) {
                    showInfo("Failed to post job program: " + ex.getMessage());
                }
            });
        });

        List<JobProgram> jobs = employer.getJobPrograms();

        if (jobs == null || jobs.isEmpty()) {
            Label noJobs = new Label("No job programs posted yet");
            noJobs.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            content.getChildren().addAll(title, createJobBtn, noJobs);
        } else {
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: #0D1117;");

            VBox jobsVBox = new VBox(10);
            jobsVBox.setPadding(new Insets(10));

            for (JobProgram job : jobs) {
                VBox jobCard = createJobCard(job, employer, router);
                jobsVBox.getChildren().add(jobCard);
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

        HBox buttonsBox = new HBox(10);
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Close");

        editBtn.setOnAction(e -> {
            TextInputDialog descriptionDialog = new TextInputDialog(job.getDescription());
            descriptionDialog.setTitle("Edit Job Program");
            descriptionDialog.setHeaderText("Update description for " + job.getJobType());
            descriptionDialog.setContentText("Description:");
            descriptionDialog.showAndWait().ifPresent(updatedDescription -> {
                job.setDescription(updatedDescription.trim());
                boolean updated = new dbConnector().updateJobProgram(job);
                showInfo(updated ? "Job program updated." : "Job update failed.");
                if (updated) {
                    router.goToDashboard(employer.getId(), "Employer");
                }
            });
        });

        deleteBtn.setOnAction(e -> {
            boolean deleted = new FetchProfileService().deleteJobProgram(job.getJobID());
            showInfo(deleted ? "Job program closed." : "Failed to close job program.");
            if (deleted) {
                router.goToDashboard(employer.getId(), "Employer");
            }
        });
        buttonsBox.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(jobTitle, pathLabel, descLabel, buttonsBox);
        return card;
    }

    private static VBox createInboxContent(Employer employer, SceneRouter router) {
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

        Label description = new Label("Review all available learning modules and filter by learning path.");
        description.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");

        Button openModulesBtn = new Button("Open Module Browser");
        openModulesBtn.setOnAction(e -> router.goToModules());

        content.getChildren().addAll(title, description, openModulesBtn);
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
