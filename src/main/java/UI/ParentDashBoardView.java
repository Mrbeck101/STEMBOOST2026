package UI;

import UserFactory.Parent;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import java.util.List;

/**
 * Parent Dashboard View
 */
public class ParentDashBoardView {

    public static Scene create(SceneRouter router) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Parent parent = (Parent) UserContext.getInstance().getCurrentUser();
        if (parent == null) {
            return LoginView.create(router);
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        HBox topBar = createTopBar(parent, router);
        root.setTop(topBar);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab dashboardTab = new Tab("Dashboard", createDashboardContent(parent));
        Tab childrenTab = new Tab("My Children", createChildrenContent(parent, router));
        Tab inboxTab = new Tab("Inbox", createInboxContent(parent, router));

        tabPane.getTabs().addAll(dashboardTab, childrenTab, inboxTab);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1400, 900);
        return scene;
    }

    private static HBox createTopBar(Parent parent, SceneRouter router) {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label appTitle = new Label("STEMBOOST - Parent Portal");
        appTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label nameLabel = new Label("Welcome, " + parent.getName());
        nameLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #ffffff;");

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> router.goToLogin());

        topBar.getChildren().addAll(appTitle, spacer, nameLabel, logoutBtn);
        return topBar;
    }

    private static VBox createDashboardContent(Parent parent) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Parent Dashboard");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        List<Integer> children = parent.getGuardedStudents();
        int childCount = (children != null) ? children.size() : 0;

        HBox statsBox = new HBox(15);
        VBox childCard = createStatCard("Children Enrolled", String.valueOf(childCount), "Number of children in your care");
        VBox updateCard = createStatCard("Recent Updates", "0", "New updates about your children");

        statsBox.getChildren().addAll(childCard, updateCard);
        HBox.setHgrow(childCard, Priority.ALWAYS);
        HBox.setHgrow(updateCard, Priority.ALWAYS);

        content.getChildren().addAll(title, statsBox);
        return content;
    }

    private static VBox createChildrenContent(Parent parent, SceneRouter router) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("My Children");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        List<Integer> childIds = parent.getGuardedStudents();

        if (childIds == null || childIds.isEmpty()) {
            Label noChildren = new Label("No children linked to your account");
            noChildren.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            content.getChildren().addAll(title, noChildren);
        } else {
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: #0D1117;");

            VBox childrenVBox = new VBox(10);
            childrenVBox.setPadding(new Insets(10));

            for (Integer childId : childIds) {
                VBox childCard = createChildCard(childId, router);
                childrenVBox.getChildren().add(childCard);
            }

            scrollPane.setContent(childrenVBox);
            content.getChildren().addAll(title, scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }

        return content;
    }

    private static VBox createChildCard(int childId, SceneRouter router) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");

        Label childLabel = new Label("Child ID: " + childId);
        childLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        Label statusLabel = new Label("Status: Active");
        statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #238636;");

        HBox buttonsBox = new HBox(10);
        Button viewBtn = new Button("View Progress");
        Button messageBtn = new Button("Contact Counselor");
        viewBtn.setOnAction(e -> showInfo("Progress tracking opened for child ID #" + childId));
        messageBtn.setOnAction(e -> router.goToContacts());
        buttonsBox.getChildren().addAll(viewBtn, messageBtn);

        card.getChildren().addAll(childLabel, statusLabel, buttonsBox);
        return card;
    }

    private static VBox createInboxContent(Parent parent, SceneRouter router) {
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
