package UI;

import UserFactory.*;
import OtherComponents.LearningModule;
import Services.FetchProfileService;
import DatabaseController.dbConnector;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;

/**
 * Module View - Display and manage learning modules
 */
public class ModuleView {

    public static Scene create(SceneRouter router) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        User currentUser = UserContext.getInstance().getCurrentUser();
        if (currentUser == null) {
            return LoginView.create(router);
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Top bar with back button
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-font-size: 12;");
        backBtn.setOnAction(e -> {
            String userType = currentUser.getAcctType();
            switch (userType) {
                case "Student" -> router.goToDashboard(currentUser.getId(), "Student");
                case "Educator" -> router.goToDashboard(currentUser.getId(), "Educator");
                case "Counselor" -> router.goToDashboard(currentUser.getId(), "Counselor");
                case "Employer" -> router.goToDashboard(currentUser.getId(), "Employer");
                case "Admin" -> router.goToDashboard(currentUser.getId(), "Admin");
                default -> router.goToLogin();
            }
        });

        Label title = new Label("Learning Modules");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        topBar.getChildren().addAll(backBtn, title);
        root.setTop(topBar);

        // Content area
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label contentTitle = new Label("Available Modules");
        contentTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        FetchProfileService profileService = new FetchProfileService();

        // Student keeps enrolled-only behavior; staff roles can browse all and filter by path.
        java.util.List<LearningModule> modules;
        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("All", "Electrical Engineering", "Software Engineering", "Information Technology", "Cybersecurity", "Computer Engineering", "Artificial Intelligence");
        filterCombo.setValue("All");

        VBox filterRow = new VBox(8);
        if (currentUser instanceof Student) {
            modules = ((Student) currentUser).getLearningModules();
        } else if (currentUser instanceof Educator || currentUser instanceof Counselor || currentUser instanceof Employer || currentUser instanceof Admin) {
            modules = profileService.browseModules("All");
            Label filterLabel = new Label("Filter by Learning Path");
            filterLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
            filterRow.getChildren().addAll(filterLabel, filterCombo);
        } else {
            modules = java.util.List.of();
        }

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #0D1117;");

        VBox modulesVBox = new VBox(15);
        modulesVBox.setPadding(new Insets(10));

        if (!modules.isEmpty()) {
            for (LearningModule module : modules) {
                VBox moduleCard = createModuleDetailCard(module, currentUser instanceof Student);
                modulesVBox.getChildren().add(moduleCard);
            }
        } else {
            Label noModules = new Label("No modules available");
            noModules.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            modulesVBox.getChildren().add(noModules);
        }

        scrollPane.setContent(modulesVBox);
        if (!filterRow.getChildren().isEmpty()) {
            filterCombo.setOnAction(e -> {
                modulesVBox.getChildren().clear();
                String selectedPath = filterCombo.getValue();
                java.util.List<LearningModule> filtered = profileService.browseModules(selectedPath);
                if (filtered.isEmpty()) {
                    Label noModules = new Label("No modules found for selected filter");
                    noModules.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
                    modulesVBox.getChildren().add(noModules);
                    return;
                }
                for (LearningModule module : filtered) {
                    modulesVBox.getChildren().add(createModuleDetailCard(module, false));
                }
            });
            content.getChildren().addAll(contentTitle, filterRow, scrollPane);
        } else {
            content.getChildren().addAll(contentTitle, scrollPane);
        }
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.setCenter(content);

        Scene scene = new Scene(root, 1400, 900);
        return scene;
    }

    private static VBox createModuleDetailCard(LearningModule module, boolean showProgress) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 8; -fx-border-color: #30363D;");

        Label title = new Label(module.getSubject());
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");
        title.setWrapText(true);

        Label path = new Label("Learning Path: " + module.getLearningPath());
        path.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");

        Label progress = new Label("Progress: " + module.getProgress() + "%");
        progress.setStyle("-fx-font-size: 12; -fx-text-fill: #c9d1d9;");

        ProgressBar progressBar = new ProgressBar(module.getProgress() / 100.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #238636;");

        TextArea contentArea = new TextArea(module.getContent());
        contentArea.setWrapText(true);
        contentArea.setEditable(false);
        contentArea.setPrefRowCount(6);
        contentArea.setStyle("-fx-control-inner-background: #0D1117; -fx-text-fill: #c9d1d9;");

        if (showProgress) {
            HBox buttonBox = new HBox(10);
            Button continueBtn = new Button("Continue Learning");
            Button completeBtn = new Button("Mark Complete");

            continueBtn.setOnAction(e -> {
                try {
                    User currentUser = UserContext.getInstance().getCurrentUser();
                    if (!(currentUser instanceof Student)) {
                        return;
                    }
                    int nextProgress = Math.min(100, module.getProgress() + 10);
                    boolean updated = new dbConnector().updateModuleProgress(currentUser.getId(), module.getModuleID(), nextProgress);
                    showInfo(updated ? "Progress updated to " + nextProgress + "%" : "Progress update failed.");
                } catch (Exception ex) {
                    showInfo("Failed to update progress: " + ex.getMessage());
                }
            });

            completeBtn.setOnAction(e -> {
                try {
                    User currentUser = UserContext.getInstance().getCurrentUser();
                    if (!(currentUser instanceof Student)) {
                        return;
                    }
                    boolean updated = new dbConnector().updateModuleProgress(currentUser.getId(), module.getModuleID(), 100);
                    showInfo(updated ? "Module marked complete." : "Could not mark module complete.");
                } catch (Exception ex) {
                    showInfo("Failed to complete module: " + ex.getMessage());
                }
            });

            buttonBox.getChildren().addAll(continueBtn, completeBtn);
            card.getChildren().addAll(title, path, progress, progressBar, contentArea, buttonBox);
        } else {
            card.getChildren().addAll(title, path, contentArea);
        }
        return card;
    }

    private static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
