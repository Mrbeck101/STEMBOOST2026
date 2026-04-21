package UI;

import UserFactory.Parent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.HashMap;
import java.util.List;

/**
 * Parent Dashboard View
 */
public class ParentDashBoardView {

    public static Scene create(SceneRouter router) {
        Parent parent = (Parent) UIComponents.guardLogin(router);
        if (parent == null) return LoginView.create(router);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
                new Tab("Dashboard", createDashboardContent(parent)),
                new Tab("My Children", createChildrenContent(parent, router)),
                new Tab("Report Cards", createReportCardsContent(parent, router)),
                new Tab("Inbox", UIComponents.inboxTab(parent, router)),
                new Tab("Contact Info", UIComponents.contactInfoTab(parent))
        );

        return UIComponents.buildScene(
                UIComponents.topBar("STEMBOOST - Parent Portal", parent.getName(), router),
                tabPane
        );
    }

    private static VBox createDashboardContent(Parent parent) {
        VBox content = UIComponents.contentBox(20);

        List<Integer> children = parent.getGuardedStudents();
        int childCount = (children != null) ? children.size() : 0;

        content.getChildren().addAll(
                UIComponents.sectionTitle("Parent Dashboard"),
                UIComponents.statsRow(
                        UIComponents.statCard("Children Enrolled", String.valueOf(childCount)),
                        UIComponents.statCard("Recent Updates", "0")
                )
        );
        return content;
    }

    private static VBox createChildrenContent(Parent parent, SceneRouter router) {
        VBox content = UIComponents.contentBox(15);
        Label title = UIComponents.sectionTitle("My Children");

        List<Integer> childIds = parent.getGuardedStudents();

        if (childIds == null || childIds.isEmpty()) {
            Label noChildren = new Label("No children linked to your account");
            noChildren.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            content.getChildren().addAll(title, noChildren);
        } else {
            ScrollPane scrollPane = UIComponents.darkScrollPane();
            VBox childrenVBox = new VBox(10);
            childrenVBox.setPadding(new Insets(10));
            for (Integer childId : childIds) {
                HashMap<String, Object> summary = parent.getAccountSummary(childId);
                String childName = summary != null && summary.get("name") != null
                        ? (String) summary.get("name")
                        : "Student #" + childId;
                childrenVBox.getChildren().add(createChildCard(childId, childName, router));
            }

            scrollPane.setContent(childrenVBox);
            content.getChildren().addAll(title, scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
        }
        return content;
    }

    private static VBox createChildCard(int childId, String childName, SceneRouter router) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");

        Label childLabel = new Label(childName + "  (ID #" + childId + ")");
        childLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        Label statusLabel = new Label("Status: Active");
        statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #238636;");

        Button viewBtn = new Button("View Progress");
        Button messageBtn = new Button("Contact Counselor");
        viewBtn.setOnAction(e -> router.goToStudentProfile(childId));
        messageBtn.setOnAction(e -> router.goToContacts());

        card.getChildren().addAll(childLabel, statusLabel, new HBox(10, viewBtn, messageBtn));
        return card;
    }

    private static VBox createReportCardsContent(Parent parent, SceneRouter router) {
        return StudentReportCardView.createReportTab(
                "Child Report Cards",
                "Generate a progress report for each student linked to your parent account. Reports summarize learning path, enrolled modules, module progress, and assessment progress.",
                "No linked student report cards are available yet.",
                parent::getChildReportCards,
                router
        );
    }
}
