package UI;

import UserFactory.Educator;
import DatabaseController.dbConnector;
import OtherComponents.LearningModule;
import OtherComponents.Assessment;
import OtherComponents.AssessmentForm;
import Services.FetchProfileService;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import javafx.util.StringConverter;
import java.util.List;

/**
 * Educator Dashboard View
 */
public class EducatorDashBoardView {

    public static Scene create(SceneRouter router) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Educator educator = (Educator) UserContext.getInstance().getCurrentUser();
        if (educator == null) {
            return LoginView.create(router);
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // ================= TOP BAR =================
        HBox topBar = createTopBar(educator, router);
        root.setTop(topBar);

        // ================= MAIN CONTENT =================
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab dashboardTab = new Tab("Dashboard", createDashboardContent(educator));
        Tab modulesTab = new Tab("My Modules", createModulesContent(educator, router));
        Tab browseTab = new Tab("Browse All Modules", createBrowseModulesContent(router));
        Tab createTab = new Tab("Create Module", createCreateModuleContent(router));
        Tab createAssessmentTab = new Tab("Create Assessment", createCreateAssessmentContent(educator, router));
        Tab inboxTab = new Tab("Inbox", createInboxContent(educator, router));

        tabPane.getTabs().addAll(dashboardTab, modulesTab, browseTab, createTab, createAssessmentTab, inboxTab);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1400, 900);
        return scene;
    }

    private static HBox createTopBar(Educator educator, SceneRouter router) {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label appTitle = new Label("STEMBOOST - Educator Dashboard");
        appTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label nameLabel = new Label("Welcome, " + educator.getName());
        nameLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #ffffff;");

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-font-size: 12;");
        logoutBtn.setOnAction(e -> router.goToLogin());

        topBar.getChildren().addAll(appTitle, spacer, nameLabel, logoutBtn);
        return topBar;
    }

    private static VBox createDashboardContent(Educator educator) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Educator Dashboard");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        int moduleCount = educator.getLearningModules() == null ? 0 : educator.getLearningModules().size();
        int assessmentCount = educator.getAssessmentResults() == null ? 0 : educator.getAssessmentResults().size();

        HBox statsBox = new HBox(15);
        VBox modulesCard = createStatCard("Total Modules", String.valueOf(moduleCount), "Modules you have created");
        VBox assessmentsCard = createStatCard("Total Assessments", String.valueOf(assessmentCount), "Assessments created");

        statsBox.getChildren().addAll(modulesCard, assessmentsCard);
        HBox.setHgrow(modulesCard, Priority.ALWAYS);
        HBox.setHgrow(assessmentsCard, Priority.ALWAYS);

        content.getChildren().addAll(title, statsBox);
        return content;
    }

    private static VBox createModulesContent(Educator educator, SceneRouter router) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("My Learning Modules");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        List<LearningModule> modules = educator.getLearningModules();

        if (modules == null || modules.isEmpty()) {
            Label noModules = new Label("No modules created yet");
            noModules.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            content.getChildren().addAll(title, noModules);
        } else {
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: #0D1117;");

            VBox modulesVBox = new VBox(10);
            modulesVBox.setPadding(new Insets(10));

            for (LearningModule module : modules) {
                VBox moduleCard = createModuleCard(module, educator, router);
                modulesVBox.getChildren().add(moduleCard);
            }

            scrollPane.setContent(modulesVBox);
            content.getChildren().addAll(title, scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }

        return content;
    }

    private static VBox createModuleCard(LearningModule module, Educator educator, SceneRouter router) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");

        Label moduleTitle = new Label(module.getSubject());
        moduleTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        Label modulePath = new Label("Path: " + module.getLearningPath());
        modulePath.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");

        HBox buttonsBox = new HBox(10);
        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete");

        editBtn.setOnAction(e -> {
            TextInputDialog contentDialog = new TextInputDialog(module.getContent());
            contentDialog.setTitle("Edit Module");
            contentDialog.setHeaderText("Update module content for " + module.getSubject());
            contentDialog.setContentText("Content:");
            contentDialog.showAndWait().ifPresent(updatedContent -> {
                if (updatedContent.trim().isEmpty()) {
                    showInfo("Module content cannot be empty.");
                    return;
                }
                LearningModule updatedModule = new LearningModule(
                        module.getModuleID(),
                        module.getProgress(),
                        module.getEducatorID(),
                        module.getLearningPath(),
                        updatedContent.trim(),
                        module.getSubject()
                );
                boolean ok = new dbConnector().updateModuleDB(updatedModule);
                showInfo(ok ? "Module updated." : "Module update failed.");
                if (ok) {
                    router.goToDashboard(educator.getId(), "Educator");
                }
            });
        });

        deleteBtn.setOnAction(e -> {
            boolean deleted = new FetchProfileService().deleteModule(module.getModuleID());
            showInfo(deleted ? "Module deleted." : "Module delete failed.");
            if (deleted) {
                router.goToDashboard(educator.getId(), "Educator");
            }
        });
        buttonsBox.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(moduleTitle, modulePath, buttonsBox);
        return card;
    }

    private static VBox createCreateModuleContent(SceneRouter router) {
        Educator educator = (Educator) UserContext.getInstance().getCurrentUser();
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Create New Module");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6;");
        form.setMaxWidth(600);

        TextField subjectField = new TextField();
        subjectField.setPromptText("Subject");

        ComboBox<String> pathCombo = new ComboBox<>();
        pathCombo.getItems().addAll("Electrical Engineering", "Software Engineering", "Information Technology", "Cybersecurity", "Computer Engineering", "Artificial Intelligence");
        pathCombo.setMaxWidth(Double.MAX_VALUE);

        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Module Content");
        contentArea.setWrapText(true);
        contentArea.setPrefRowCount(10);

        Button createBtn = new Button("Create Module");
        createBtn.setStyle("-fx-font-size: 12; -fx-padding: 10;");
        createBtn.setOnAction(e -> {
            String subject = subjectField.getText().trim();
            String learningPath = pathCombo.getValue();
            String moduleContent = contentArea.getText().trim();

            if (subject.isEmpty() || learningPath == null || moduleContent.isEmpty()) {
                showInfo("Please complete all required fields.");
                return;
            }

            try {
                LearningModule module = new LearningModule(0, 0, educator.getId(), learningPath, moduleContent, subject);
                educator.addModule(module);
                showInfo("Module created and saved successfully.");
                router.goToDashboard(educator.getId(), "Educator");
            } catch (Exception ex) {
                showInfo("Failed to create module: " + ex.getMessage());
            }
        });

        form.getChildren().addAll(
                new Label("Subject"), subjectField,
                new Label("Learning Path"), pathCombo,
                new Label("Content"), contentArea,
                createBtn
        );

        content.getChildren().addAll(title, form);
        return content;
    }

    private static VBox createCreateAssessmentContent(Educator educator, SceneRouter router) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Create Assessment for a Module Group");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        List<LearningModule> modules = educator.getLearningModules();
        if (modules == null || modules.isEmpty()) {
            Label noModules = new Label("Create a module before assigning an assessment.");
            noModules.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            content.getChildren().addAll(title, noModules);
            return content;
        }

        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6;");
        form.setMaxWidth(700);

        ComboBox<LearningModule> moduleCombo = new ComboBox<>();
        moduleCombo.getItems().addAll(modules);
        moduleCombo.setMaxWidth(Double.MAX_VALUE);
        moduleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(LearningModule module) {
                if (module == null) {
                    return "";
                }
                return module.getSubject() + " (Module ID: " + module.getModuleID() + ")";
            }

            @Override
            public LearningModule fromString(String string) {
                return null;
            }
        });

        Label learningPathLabel = new Label("Learning Path: Select a module");
        learningPathLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
        moduleCombo.setOnAction(e -> {
            LearningModule selectedModule = moduleCombo.getValue();
            if (selectedModule != null) {
                learningPathLabel.setText("Learning Path: " + selectedModule.getLearningPath());
            }
        });

        AssessmentFormBuilder formBuilder = new AssessmentFormBuilder();

        Button createAssessmentBtn = new Button("Create and Assign Assessment");
        createAssessmentBtn.setOnAction(e -> {
            LearningModule selectedModule = moduleCombo.getValue();

            if (selectedModule == null) {
                showInfo("Please select a module group before creating an assessment.");
                return;
            }

            try {
                AssessmentForm generatedForm = formBuilder.buildForm();
                Assessment assessment = new Assessment(
                        0,
                        selectedModule.getModuleID(),
                        -1,
                        selectedModule.getLearningPath(),
                        generatedForm.toJson()
                );
                educator.addAssessment(assessment);
                showInfo("Assessment created and assigned to " + selectedModule.getSubject() + ".");
                router.goToDashboard(educator.getId(), "Educator");
            } catch (Exception ex) {
                showInfo("Failed to create assessment: " + ex.getMessage());
            }
        });

        form.getChildren().addAll(
                new Label("Module Group"), moduleCombo,
                learningPathLabel,
                new Label("Assessment Builder"), formBuilder.getView(),
                createAssessmentBtn
        );

        ScrollPane scrollPane = new ScrollPane(form);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #0D1117;");

        content.getChildren().addAll(title, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return content;
    }

    private static VBox createInboxContent(Educator educator, SceneRouter router) {
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

        Label title = new Label("Browse All Modules");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label description = new Label("Open the full module catalog and filter by learning path.");
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
