package UI;

import OtherComponents.StudentReportCard;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.function.Supplier;

public final class StudentReportCardView {

    private StudentReportCardView() {
    }

    public static VBox createReportTab(String title,
                                       String description,
                                       String emptyMessage,
                                       Supplier<List<StudentReportCard>> loader,
                                       SceneRouter router) {
        VBox content = UIComponents.contentBox(15);
        Label titleLabel = UIComponents.sectionTitle(title);

        Label descriptionLabel = new Label(description);
        descriptionLabel.setStyle("-fx-text-fill: #8b949e;");
        descriptionLabel.setWrapText(true);

        ComboBox<String> learningPathFilter = UIComponents.learningPathComboWithAll();
        learningPathFilter.setPromptText("Filter by learning path");

        Button refreshBtn = new Button("Generate / Refresh Report");
        VBox results = new VBox(12);

        ScrollPane scrollPane = UIComponents.darkScrollPane();
        scrollPane.setContent(results);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Runnable render = () -> renderReports(
                results,
                emptyMessage,
                loader.get(),
                learningPathFilter.getValue(),
                router
        );
        refreshBtn.setOnAction(e -> render.run());
        learningPathFilter.setOnAction(e -> render.run());
        render.run();

        content.getChildren().addAll(titleLabel, descriptionLabel, new HBox(10, learningPathFilter, refreshBtn), scrollPane);
        return content;
    }

    private static void renderReports(VBox results,
                                      String emptyMessage,
                                      List<StudentReportCard> reports,
                                      String selectedLearningPath,
                                      SceneRouter router) {
        results.getChildren().clear();

        List<StudentReportCard> filteredReports = filterByLearningPath(reports, selectedLearningPath);

        if (filteredReports.isEmpty()) {
            Label emptyLabel = new Label(buildEmptyMessage(emptyMessage, selectedLearningPath));
            emptyLabel.setStyle("-fx-text-fill: #8b949e;");
            results.getChildren().add(emptyLabel);
            return;
        }

        results.getChildren().add(createSummaryBanner(filteredReports, selectedLearningPath));
        for (StudentReportCard report : filteredReports) {
            results.getChildren().add(createStudentReportCard(report, router));
        }
    }

    private static VBox createSummaryBanner(List<StudentReportCard> reports, String selectedLearningPath) {
        int totalStudents = reports.size();
        int totalModules = reports.stream().mapToInt(r -> r.getModules().size()).sum();
        double avgModuleProgress = reports.stream().mapToDouble(StudentReportCard::getAverageModuleProgress).average().orElse(0.0);
        double avgAssessmentCompletion = reports.stream().mapToDouble(StudentReportCard::getAssessmentCompletionRate).average().orElse(0.0);

        VBox summary = new VBox(8);
        summary.setPadding(new Insets(15));
        summary.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-radius: 6;");
        summary.getChildren().addAll(
                styledLabel("Report Summary", "-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #ffffff;"),
                styledLabel("Learning Path Filter: " + displayLearningPath(selectedLearningPath), "-fx-text-fill: #8b949e;"),
                styledLabel("Students Included: " + totalStudents, "-fx-text-fill: #c9d1d9;"),
                styledLabel("Total Module Enrollments: " + totalModules, "-fx-text-fill: #c9d1d9;"),
                styledLabel(String.format("Average Module Progress: %.1f%%", avgModuleProgress), "-fx-text-fill: #c9d1d9;"),
                styledLabel(String.format("Average Assessment Completion: %.1f%%", avgAssessmentCompletion), "-fx-text-fill: #c9d1d9;")
        );
        return summary;
    }

    private static VBox createStudentReportCard(StudentReportCard report, SceneRouter router) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-radius: 6;");

        HBox header = new HBox(10);
        Label nameLabel = styledLabel(
                report.getStudentName() + "  (ID #" + report.getStudentId() + ")",
                "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #58a6ff;"
        );
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button profileBtn = new Button("View Profile");
        profileBtn.setOnAction(e -> router.goToStudentProfile(report.getStudentId()));
        header.getChildren().addAll(nameLabel, spacer, profileBtn);

        card.getChildren().addAll(
                header,
                styledLabel("Learning Path: " + safeText(report.getLearningPath()), "-fx-text-fill: #c9d1d9;"),
                styledLabel(String.format("Module Progress: %.1f%% average across %d module(s)", report.getAverageModuleProgress(), report.getModules().size()), "-fx-text-fill: #c9d1d9;"),
                styledLabel(String.format("Assessment Progress: %d/%d completed (%.1f%%)", report.getCompletedAssessments(), report.getTotalAssessments(), report.getAssessmentCompletionRate()), "-fx-text-fill: #c9d1d9;"),
                styledLabel(
                        report.getAverageAssessmentGrade() >= 0
                                ? String.format("Average Graded Assessment: %.1f%%", report.getAverageAssessmentGrade())
                                : "Average Graded Assessment: Not yet graded",
                        "-fx-text-fill: #c9d1d9;"
                )
        );

        Label modulesTitle = styledLabel("Enrolled Modules", "-fx-font-weight: bold; -fx-text-fill: #ffffff;");
        card.getChildren().add(modulesTitle);

        if (report.getModules().isEmpty()) {
            card.getChildren().add(styledLabel("No modules enrolled.", "-fx-text-fill: #8b949e;"));
        } else {
            for (StudentReportCard.ModuleProgressSummary module : report.getModules()) {
                card.getChildren().add(createModuleRow(module));
            }
        }

        return card;
    }

    private static VBox createModuleRow(StudentReportCard.ModuleProgressSummary module) {
        VBox row = new VBox(6);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: #0D1117; -fx-border-color: #30363D; -fx-border-radius: 4;");

        Label title = styledLabel(
                module.getSubject() + "  (Module #" + module.getModuleId() + ")",
                "-fx-font-weight: bold; -fx-text-fill: #ffa657;"
        );
        Label path = styledLabel("Path: " + safeText(module.getLearningPath()), "-fx-text-fill: #8b949e;");

        ProgressBar progressBar = new ProgressBar(module.getProgress() / 100.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        Label progress = styledLabel(module.getProgress() + "% complete", "-fx-text-fill: #8b949e;");

        row.getChildren().addAll(title, path, progressBar, progress);
        return row;
    }

    private static Label styledLabel(String text, String style) {
        Label label = new Label(text);
        label.setStyle(style);
        label.setWrapText(true);
        return label;
    }

    private static String safeText(String text) {
        return text == null || text.isBlank() ? "Not set" : text;
    }

    private static List<StudentReportCard> filterByLearningPath(List<StudentReportCard> reports, String selectedLearningPath) {
        if (reports == null || reports.isEmpty()) {
            return List.of();
        }
        if (selectedLearningPath == null || selectedLearningPath.isBlank() || "All".equalsIgnoreCase(selectedLearningPath)) {
            return reports;
        }

        String normalizedFilter = normalize(selectedLearningPath);
        return reports.stream()
                .filter(report -> normalize(report.getLearningPath()).equals(normalizedFilter))
                .collect(Collectors.toList());
    }

    private static String buildEmptyMessage(String emptyMessage, String selectedLearningPath) {
        if (selectedLearningPath == null || selectedLearningPath.isBlank() || "All".equalsIgnoreCase(selectedLearningPath)) {
            return emptyMessage;
        }
        return "No report cards found for learning path: " + selectedLearningPath;
    }

    private static String displayLearningPath(String selectedLearningPath) {
        return selectedLearningPath == null || selectedLearningPath.isBlank() ? "All" : selectedLearningPath;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

