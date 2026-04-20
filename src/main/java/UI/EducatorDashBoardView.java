package UI;

import UserFactory.Educator;
import OtherComponents.LearningModule;
import OtherComponents.Assessment;
import OtherComponents.AssessmentForm;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import java.util.List;

/**
 * Educator Dashboard View
 */
public class EducatorDashBoardView {

    public static Scene create(SceneRouter router) {
        Educator educator = (Educator) UIComponents.guardLogin(router);
        if (educator == null) return LoginView.create(router);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
                new Tab("Dashboard", createDashboardContent(educator, router)),
                new Tab("My Modules", createModulesContent(educator, router)),
                new Tab("Browse All Modules", UIComponents.browseModulesTab("Browse All Modules", "Open the full module catalog and filter by learning path.", router)),
                new Tab("Create Module", createCreateModuleContent(router)),
                new Tab("Create Assessment", createCreateAssessmentContent(educator, router)),
                new Tab("Inbox", UIComponents.inboxTab(educator, router))
        );

        return UIComponents.buildScene(
                UIComponents.topBar("STEMBOOST - Educator Dashboard", educator.getName(), router),
                tabPane
        );
    }

    private static VBox createDashboardContent(Educator educator, SceneRouter router) {
        VBox content = UIComponents.contentBox(20);

        int moduleCount = educator.getLearningModules() == null ? 0 : educator.getLearningModules().size();
        int assessmentCount = educator.getAssessmentResults() == null ? 0 : educator.getAssessmentResults().size();
        int awaitingGradingCount = educator.getPendingAssessmentSubmissions().size();
        int gradedCount = educator.getGradedAssessmentSubmissions().size();

        Button openQueueBtn = new Button("Open Grading Queue");
        openQueueBtn.setOnAction(e -> showGradingQueueDialog(educator, router));
        Button openHistoryBtn = new Button("Open Graded History");
        openHistoryBtn.setOnAction(e -> showGradedHistoryDialog(educator));

        content.getChildren().addAll(
                UIComponents.sectionTitle("Educator Dashboard"),
                UIComponents.statsRow(
                        UIComponents.statCard("Total Modules", String.valueOf(moduleCount)),
                        UIComponents.statCard("Total Assessments", String.valueOf(assessmentCount)),
                        UIComponents.statCard("Awaiting Grading", String.valueOf(awaitingGradingCount)),
                        UIComponents.statCard("Graded Submissions", String.valueOf(gradedCount))
                ),
                new HBox(10, openQueueBtn, openHistoryBtn)
        );
        return content;
    }

    private static void showGradingQueueDialog(Educator educator, SceneRouter router) {
        List<Assessment> pendingSubmissions = educator.getPendingAssessmentSubmissions();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Assessments Awaiting Grading");
        dialog.setHeaderText("Grade completed assessments submitted by your students");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox listBox = new VBox(10);
        listBox.setPadding(new Insets(10));

        if (pendingSubmissions.isEmpty()) {
            Label empty = new Label("No completed assessments are awaiting grading.");
            empty.setStyle("-fx-text-fill: #8b949e;");
            listBox.getChildren().add(empty);
        } else {
            for (Assessment submission : pendingSubmissions) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8));
                row.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-radius: 4;");

                String moduleSubject = (submission.getModuleSubject() == null || submission.getModuleSubject().isBlank())
                        ? "Module #" + submission.getModuleID() : submission.getModuleSubject();

                Label info = new Label("Student: " + submission.getStudentName() + " (ID #" + submission.getStudentId() + ")"
                        + " | Module: " + moduleSubject
                        + " | Assessment #" + submission.getAssessmentID());
                info.setStyle("-fx-text-fill: #c9d1d9;");
                info.setWrapText(true);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button gradeBtn = new Button("Grade");
                gradeBtn.setOnAction(e -> {
                    boolean graded = showSubmissionGradingDialog(submission, educator);
                    if (graded) {
                        UIComponents.showInfo("Submission graded successfully.");
                        dialog.close();
                        router.goToDashboard(educator.getId(), "Educator");
                    }
                });

                row.getChildren().addAll(info, spacer, gradeBtn);
                listBox.getChildren().add(row);
            }
        }

        ScrollPane sp = new ScrollPane(listBox);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(600);
        dialog.getDialogPane().setContent(sp);
        dialog.getDialogPane().setMinWidth(1200);
        dialog.getDialogPane().setMinHeight(600);
        dialog.showAndWait();
    }

    private static boolean showSubmissionGradingDialog(Assessment submission, Educator educator) {
        Dialog<ButtonType> gradeDialog = new Dialog<>();
        gradeDialog.setTitle("Grade Submission");
        gradeDialog.setHeaderText(submission.getStudentName() + " (ID #" + submission.getStudentId() + ") - Assessment #" + submission.getAssessmentID());

        ButtonType saveType = new ButtonType("Save Grade", ButtonBar.ButtonData.OK_DONE);
        gradeDialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        Node renderedAssessment = AssessmentFormRenderer.createSubmissionPreview(
                submission.getContent(),
                null
        );
        ScrollPane assessmentPane = new ScrollPane(renderedAssessment);
        assessmentPane.setFitToWidth(true);
        assessmentPane.setPrefViewportHeight(580);

        TextField gradeField = new TextField();
        gradeField.setPromptText("Enter grade between 0 and 100");
        gradeField.setPrefWidth(220);

        VBox assessmentBox = new VBox(8,
                new Label("Submitted Assessment Form:"),
                assessmentPane
        );

        HBox details = new HBox(12, assessmentBox);
        HBox.setHgrow(assessmentBox, Priority.ALWAYS);
        VBox.setVgrow(assessmentPane, Priority.ALWAYS);

        HBox gradingRow = new HBox(10, new Label("Grade (0-100):"), gradeField);
        gradingRow.setAlignment(Pos.CENTER_LEFT);

        VBox form = new VBox(10, details, gradingRow);
        form.setPadding(new Insets(10));
        gradeDialog.getDialogPane().setContent(form);
        gradeDialog.getDialogPane().setPrefWidth(1700);
        gradeDialog.getDialogPane().setPrefHeight(820);

        UIComponents.preparePopupForStudentTts(gradeDialog);

        while (true) {
            var result = gradeDialog.showAndWait();
            if (result.isEmpty() || result.get() != saveType) {
                return false;
            }
            try {
                int grade = Integer.parseInt(gradeField.getText().trim());
                if (grade < 0 || grade > 100) {
                    UIComponents.showInfo("Grade must be between 0 and 100.");
                    continue;
                }
                boolean graded = educator.gradeAssessmentSubmission(submission.getStudentId(), submission.getAssessmentID(), grade);
                if (!graded) {
                    UIComponents.showInfo("Failed to grade submission.");
                    return false;
                }
                return true;
            } catch (NumberFormatException ex) {
                UIComponents.showInfo("Please enter a valid numeric grade.");
            }
        }
    }

    private static void showGradedHistoryDialog(Educator educator) {
        List<Assessment> gradedSubmissions = educator.getGradedAssessmentSubmissions();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Graded Assessment History");
        dialog.setHeaderText("Previously graded assessment submissions");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox listBox = new VBox(10);
        listBox.setPadding(new Insets(10));

        if (gradedSubmissions.isEmpty()) {
            Label empty = new Label("No graded submissions found.");
            empty.setStyle("-fx-text-fill: #8b949e;");
            listBox.getChildren().add(empty);
        } else {
            for (Assessment submission : gradedSubmissions) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8));
                row.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-radius: 4;");

                String moduleSubject = (submission.getModuleSubject() == null || submission.getModuleSubject().isBlank())
                        ? "Module #" + submission.getModuleID() : submission.getModuleSubject();

                Label info = new Label(submission.getStudentName() + " | Assessment #" + submission.getAssessmentID() +
                        " | Module #" + submission.getModuleID() + " (" + moduleSubject + ")" + " | " + submission.getLearningPath() +
                        " | Grade: " + submission.getGrade() + "%");
                info.setStyle("-fx-text-fill: #c9d1d9;");
                row.getChildren().add(info);
                listBox.getChildren().add(row);
            }
        }

        ScrollPane sp = new ScrollPane(listBox);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(450);
        dialog.getDialogPane().setContent(sp);
        dialog.showAndWait();
    }

    private static VBox createModulesContent(Educator educator, SceneRouter router) {
        VBox content = UIComponents.contentBox(15);
        Label title = UIComponents.sectionTitle("My Learning Modules");

        List<LearningModule> modules = educator.getLearningModules();

        if (modules == null || modules.isEmpty()) {
            Label noModules = new Label("No modules created yet");
            noModules.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            content.getChildren().addAll(title, noModules);
        } else {
            ScrollPane scrollPane = UIComponents.darkScrollPane();
            VBox modulesVBox = new VBox(10);
            modulesVBox.setPadding(new Insets(10));
            for (LearningModule module : modules) {
                modulesVBox.getChildren().add(createModuleCard(module, educator, router));
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

        Button editBtn = new Button("Edit");
        Button deleteBtn = new Button("Delete");

        editBtn.setOnAction(e -> {
            TextInputDialog cd = new TextInputDialog(module.getContent());
            cd.setTitle("Edit Module");
            cd.setHeaderText("Update module content for " + module.getSubject());
            cd.setContentText("Content:");
            cd.showAndWait().ifPresent(updatedContent -> {
                if (updatedContent.trim().isEmpty()) { UIComponents.showInfo("Module content cannot be empty."); return; }
                LearningModule updated = new LearningModule(module.getModuleID(), module.getProgress(), module.getEducatorID(), module.getLearningPath(), updatedContent.trim(), module.getSubject());
                boolean ok = educator.updateModule(updated);
                UIComponents.showInfo(ok ? "Module updated." : "Module update failed.");
                if (ok) router.goToDashboard(educator.getId(), "Educator");
            });
        });

        deleteBtn.setOnAction(e -> {
            boolean deleted = educator.deleteModule(module.getModuleID());
            UIComponents.showInfo(deleted ? "Module deleted." : "Module delete failed.");
            if (deleted) router.goToDashboard(educator.getId(), "Educator");
        });

        card.getChildren().addAll(moduleTitle, modulePath, new HBox(10, editBtn, deleteBtn));
        return card;
    }

    private static VBox createCreateModuleContent(SceneRouter router) {
        Educator educator = (Educator) UserContext.getInstance().getCurrentUser();
        VBox content = UIComponents.contentBox(20);
        content.getChildren().add(UIComponents.sectionTitle("Create New Module"));

        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6;");
        form.setMaxWidth(600);

        TextField subjectField = new TextField();
        subjectField.setPromptText("Subject");

        ComboBox<String> pathCombo = UIComponents.learningPathCombo();

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
                UIComponents.showInfo("Please complete all required fields.");
                return;
            }
            try {
                LearningModule module = new LearningModule(0, 0, educator.getId(), learningPath, moduleContent, subject);
                educator.addModule(module);
                UIComponents.showInfo("Module created and saved successfully.");
                router.goToDashboard(educator.getId(), "Educator");
            } catch (Exception ex) {
                UIComponents.showInfo("Failed to create module: " + ex.getMessage());
            }
        });

        form.getChildren().addAll(new Label("Subject"), subjectField, new Label("Learning Path"), pathCombo, new Label("Content"), contentArea, createBtn);
        content.getChildren().add(form);
        return content;
    }

    private static VBox createCreateAssessmentContent(Educator educator, SceneRouter router) {
        VBox content = UIComponents.contentBox(20);
        content.getChildren().add(UIComponents.sectionTitle("Create Assessment for a Module Group"));

        List<LearningModule> modules = educator.getLearningModules();
        if (modules == null || modules.isEmpty()) {
            Label noModules = new Label("Create a module before assigning an assessment.");
            noModules.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            content.getChildren().add(noModules);
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
            @Override public String toString(LearningModule m) { return m == null ? "" : m.getSubject() + " (Module ID: " + m.getModuleID() + ")"; }
            @Override public LearningModule fromString(String s) { return null; }
        });

        Label learningPathLabel = new Label("Learning Path: Select a module");
        learningPathLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
        moduleCombo.setOnAction(e -> { LearningModule sel = moduleCombo.getValue(); if (sel != null) learningPathLabel.setText("Learning Path: " + sel.getLearningPath()); });

        AssessmentFormBuilder formBuilder = new AssessmentFormBuilder();

        Button createBtn = new Button("Create and Assign Assessment");
        createBtn.setOnAction(e -> {
            LearningModule sel = moduleCombo.getValue();
            if (sel == null) { UIComponents.showInfo("Please select a module group before creating an assessment."); return; }
            try {
                AssessmentForm generatedForm = formBuilder.buildForm();
                Assessment assessment = new Assessment(0, sel.getModuleID(), -1, sel.getLearningPath(), sel.getSubject(), generatedForm.toJson(), false);
                educator.addAssessment(assessment);
                UIComponents.showInfo("Assessment created and assigned to " + sel.getSubject() + ".");
                router.goToDashboard(educator.getId(), "Educator");
            } catch (Exception ex) {
                UIComponents.showInfo("Failed to create assessment: " + ex.getMessage());
            }
        });

        form.getChildren().addAll(new Label("Module Group"), moduleCombo, learningPathLabel, new Label("Assessment Builder"), formBuilder.getView(), createBtn);

        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #0D1117;");
        content.getChildren().add(sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return content;
    }
}
