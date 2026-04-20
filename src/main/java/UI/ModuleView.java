package UI;

import UserFactory.*;
import OtherComponents.LearningModule;
import Services.FetchProfileService;
import Services.KeyboardTtsService;
import Services.UIRefreshService;
import DatabaseController.dbConnector;
import atlantafx.base.theme.PrimerDark;
import javafx.beans.value.ChangeListener;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import javafx.stage.Stage;

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
        backBtn.setOnAction(e -> router.goToCurrentUserDashboard());

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
        Integer selectedModuleId = UserContext.getInstance().getSelectedModuleId();
        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("All", "Electrical Engineering", "Software Engineering", "Information Technology", "Cybersecurity", "Computer Engineering", "Artificial Intelligence");
        filterCombo.setValue("All");

        VBox filterRow = new VBox(8);
        if (currentUser instanceof Student) {
            if (selectedModuleId != null) {
                UserContext.getInstance().clearSelectedModuleId();
            }
        } else if (currentUser instanceof Educator || currentUser instanceof Counselor || currentUser instanceof Employer || currentUser instanceof Admin) {
            Label filterLabel = new Label("Filter by Learning Path");
            filterLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
            filterRow.getChildren().addAll(filterLabel, filterCombo);
        }

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #0D1117;");

        VBox modulesVBox = new VBox(15);
        modulesVBox.setPadding(new Insets(10));
        final java.util.List<LearningModule>[] displayedModules = new java.util.List[]{java.util.List.of()};

        Runnable refreshModules = () -> {
            modulesVBox.getChildren().clear();
            java.util.List<LearningModule> modules;

            if (currentUser instanceof Student) {
                modules = new dbConnector().searchModulesDB(currentUser.getId(), "Student");
                if (selectedModuleId != null) {
                    modules = modules.stream().filter(m -> m.getModuleID() == selectedModuleId).toList();
                }
            } else if (currentUser instanceof Educator || currentUser instanceof Counselor || currentUser instanceof Employer || currentUser instanceof Admin) {
                modules = profileService.browseModules(filterCombo.getValue());
            } else {
                modules = java.util.List.of();
            }

            displayedModules[0] = modules;

            if (!modules.isEmpty()) {
                for (LearningModule module : modules) {
                    VBox moduleCard = createModuleDetailCard(module, currentUser instanceof Student, router);
                    modulesVBox.getChildren().add(moduleCard);
                }
            } else {
                Label noModules = new Label("No modules available");
                noModules.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
                modulesVBox.getChildren().add(noModules);
            }
        };

        refreshModules.run();

        scrollPane.setContent(modulesVBox);
        if (!filterRow.getChildren().isEmpty()) {
            filterCombo.setOnAction(e -> refreshModules.run());
            content.getChildren().addAll(contentTitle, filterRow, scrollPane);
        } else {
            content.getChildren().addAll(contentTitle, scrollPane);
        }
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.setCenter(content);

        Scene scene = new Scene(root, 1400, 900);

        KeyboardTtsService.getInstance().bindScene(
                scene,
                KeyboardTtsService.AccessMode.STUDENT_ONLY,
                () -> {
                    if (!(UserContext.getInstance().getCurrentUser() instanceof Student)) {
                        return new KeyboardTtsService.ReadingContent(
                                "Module browser for staff roles. TTS controls are only active for student accounts."
                        );
                    }

                    java.util.List<LearningModule> modules = displayedModules[0] == null ? java.util.List.of() : displayedModules[0];
                    if (modules.isEmpty()) {
                        return new KeyboardTtsService.ReadingContent("No modules are available right now.");
                    }

                    LearningModule target = null;
                    if (selectedModuleId != null) {
                        for (LearningModule m : modules) {
                            if (m.getModuleID() == selectedModuleId) {
                                target = m;
                                break;
                            }
                        }
                    }
                    if (target == null && modules.size() == 1) {
                        target = modules.get(0);
                    }
                    if (target == null) {
                        target = modules.get(0);
                    }

                    String text = "Module " + target.getSubject() + ". Learning path " + target.getLearningPath() + ". "
                            + "Current progress " + target.getProgress() + " percent. "
                            + target.getContent() + " "
                            + "Press F2 to pause or resume. Press plus to skip forward and minus to go back a sentence.";

                    return new KeyboardTtsService.ReadingContent(
                            text,
                            target.getModuleID(),
                            target.getProgress()
                    );
                }
        );

        UIRefreshService.UIRefreshListener modulesListener = (updateType, data) -> {
            if ("MODULES_UPDATED".equals(updateType)) {
                refreshModules.run();
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

    private static VBox createModuleDetailCard(LearningModule module, boolean showProgress, SceneRouter router) {
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
                    if (updated) {
                        router.goToModules();
                    }
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
                    if (updated) {
                        router.goToModules();
                    }
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
