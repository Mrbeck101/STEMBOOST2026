package UI;

import UserFactory.University;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.HashMap;
import java.util.List;

/**
 * University Dashboard View
 */
public class UniversityDashboardView {

    public static Scene create(SceneRouter router) {
        University university = (University) UIComponents.guardLogin(router);
        if (university == null) return LoginView.create(router);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
                new Tab("Dashboard", createDashboardContent(university)),
                new Tab("Enrolled Students", createStudentsContent(university, router)),
                new Tab("Report Cards", createReportCardsContent(university, router)),
                new Tab("Inbox", UIComponents.inboxTab(university, router))
        );

        return UIComponents.buildScene(
                UIComponents.topBarWithSubtitle("STEMBOOST - University Portal", university.getName(), "University: " + university.getUniversityName(), router),
                tabPane
        );
    }

    private static VBox createDashboardContent(University university) {
        VBox content = UIComponents.contentBox(20);

        List<Integer> students = university.getEnrolledStudents();
        int studentCount = (students != null) ? students.size() : 0;

        content.getChildren().addAll(
                UIComponents.sectionTitle("University Dashboard"),
                UIComponents.statsRow(
                        UIComponents.statCard("Total Enrolled", String.valueOf(studentCount)),
                        UIComponents.statCard("Active Learning Paths", "0")
                )
        );
        return content;
    }

    private static VBox createStudentsContent(University university, SceneRouter router) {
        VBox content = UIComponents.contentBox(15);
        Label title = UIComponents.sectionTitle("Enrolled Students");

        List<Integer> studentIds = university.getEnrolledStudents();

        if (studentIds == null || studentIds.isEmpty()) {
            Label noStudents = new Label("No students enrolled yet");
            noStudents.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            content.getChildren().addAll(title, noStudents);
        } else {
            ScrollPane scrollPane = UIComponents.darkScrollPane();
            VBox studentsVBox = new VBox(10);
            studentsVBox.setPadding(new Insets(10));
            for (Integer studentId : studentIds) {
                HashMap<String, Object> summary = university.getAccountSummary(studentId);
                String studentName = summary != null && summary.get("name") != null
                        ? (String) summary.get("name")
                        : "Student #" + studentId;
                studentsVBox.getChildren().add(createStudentCard(studentId, studentName, router));
            }

            scrollPane.setContent(studentsVBox);
            content.getChildren().addAll(title, scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }
        return content;
    }

    private static VBox createStudentCard(int studentId, String studentName, SceneRouter router) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");

        Label studentLabel = new Label(studentName + "  (ID #" + studentId + ")");
        studentLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        Label statusLabel = new Label("Status: Active");
        statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #238636;");

        Button viewBtn = new Button("View Profile");
        Button contactBtn = new Button("Contact");
        viewBtn.setOnAction(e -> router.goToStudentProfile(studentId));
        contactBtn.setOnAction(e -> router.goToContacts());

        card.getChildren().addAll(studentLabel, statusLabel, new HBox(10, viewBtn, contactBtn));
        return card;
    }

    private static VBox createReportCardsContent(University university, SceneRouter router) {
        return StudentReportCardView.createReportTab(
                "University Report Cards",
                "Generate a progress report for every student enrolled under " + university.getUniversityName() + ". Each report summarizes learning path, enrolled modules, module progress, and assessment progress.",
                "No student report cards are available for this university yet.",
                university::getStudentReportCards,
                router
        );
    }
}
