package UI;

import UserFactory.University;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import java.util.List;

/**
 * University Dashboard View
 */
public class UniversityDashboardView {

    public static Scene create(SceneRouter router) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        University university = (University) UserContext.getInstance().getCurrentUser();
        if (university == null) {
            return LoginView.create(router);
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        HBox topBar = createTopBar(university, router);
        root.setTop(topBar);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab dashboardTab = new Tab("Dashboard", createDashboardContent(university));
        Tab studentsTab = new Tab("Enrolled Students", createStudentsContent(university, router));
        Tab inboxTab = new Tab("Inbox", createInboxContent(university, router));

        tabPane.getTabs().addAll(dashboardTab, studentsTab, inboxTab);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1400, 900);
        return scene;
    }

    private static HBox createTopBar(University university, SceneRouter router) {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label appTitle = new Label("STEMBOOST - University Portal");
        appTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox infoBox = new VBox(5);
        Label nameLabel = new Label("Welcome, " + university.getName());
        nameLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #ffffff;");
        Label uniLabel = new Label("University: " + university.getUniversityName());
        uniLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #aaaaaa;");
        infoBox.getChildren().addAll(nameLabel, uniLabel);

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> router.goToLogin());

        topBar.getChildren().addAll(appTitle, spacer, infoBox, logoutBtn);
        return topBar;
    }

    private static VBox createDashboardContent(University university) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("University Dashboard");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        List<Integer> students = university.getEnrolledStudents();
        int studentCount = (students != null) ? students.size() : 0;

        HBox statsBox = new HBox(15);
        VBox studentCard = createStatCard("Total Enrolled", String.valueOf(studentCount), "Students enrolled in your university");
        VBox activeCard = createStatCard("Active Learning Paths", "0", "Active learning path programs");

        statsBox.getChildren().addAll(studentCard, activeCard);
        HBox.setHgrow(studentCard, Priority.ALWAYS);
        HBox.setHgrow(activeCard, Priority.ALWAYS);

        content.getChildren().addAll(title, statsBox);
        return content;
    }

    private static VBox createStudentsContent(University university, SceneRouter router) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Enrolled Students");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        List<Integer> studentIds = university.getEnrolledStudents();

        if (studentIds == null || studentIds.isEmpty()) {
            Label noStudents = new Label("No students enrolled yet");
            noStudents.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            content.getChildren().addAll(title, noStudents);
        } else {
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: #0D1117;");

            VBox studentsVBox = new VBox(10);
            studentsVBox.setPadding(new Insets(10));

            for (Integer studentId : studentIds) {
                VBox studentCard = createStudentCard(studentId, router);
                studentsVBox.getChildren().add(studentCard);
            }

            scrollPane.setContent(studentsVBox);
            content.getChildren().addAll(title, scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }

        return content;
    }

    private static VBox createStudentCard(int studentId, SceneRouter router) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");

        Label studentLabel = new Label("Student ID: " + studentId);
        studentLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        Label statusLabel = new Label("Status: Active");
        statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #238636;");

        HBox buttonsBox = new HBox(10);
        Button viewBtn = new Button("View Profile");
        Button contactBtn = new Button("Contact");
        viewBtn.setOnAction(e -> showInfo("Viewing profile for student ID #" + studentId));
        contactBtn.setOnAction(e -> router.goToContacts());
        buttonsBox.getChildren().addAll(viewBtn, contactBtn);

        card.getChildren().addAll(studentLabel, statusLabel, buttonsBox);
        return card;
    }

    private static VBox createInboxContent(University university, SceneRouter router) {
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
