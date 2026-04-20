package UI;

import UserFactory.Admin;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.List;

public class AdminDashboardView {

    public static Scene create(SceneRouter router) {
        Admin admin = (Admin) UIComponents.guardLogin(router);
        if (admin == null) return LoginView.create(router);

        Button contactsBtn = new Button("Contacts");
        contactsBtn.setOnAction(e -> router.goToContacts());
        Button profileBtn = new Button("Profile");
        profileBtn.setOnAction(e -> router.goToProfile());

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
                new Tab("Users", createUsersContent(admin)),
                new Tab("Modules", createModulesContent(admin)),
                new Tab("Job Programs", createJobsContent(admin)),
                new Tab("Inbox", UIComponents.inboxTab(admin, router))
        );

        return UIComponents.buildScene(
                UIComponents.topBar("STEMBOOST - Admin Dashboard", admin.getName(), router, contactsBtn, profileBtn),
                tabPane
        );
    }

    private static VBox createUsersContent(Admin admin) {
        VBox content = UIComponents.contentBox(10);
        content.getChildren().add(UIComponents.sectionTitle("All Users"));

        ScrollPane scrollPane = UIComponents.darkScrollPane();
        VBox rows = new VBox(8);

        for (HashMap<String, Object> user : admin.getAllUsers()) {
            HBox row = new HBox(10);
            row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-radius: 4;");

            Label info = new Label("#" + user.get("userId") + " | " + user.get("name") + " | " + user.get("acctType"));
            info.setStyle("-fx-text-fill: #c9d1d9;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button deleteBtn = new Button("Delete");
            deleteBtn.setOnAction(e -> {
                boolean deleted = admin.deleteUser((Integer) user.get("userId"));
                UIComponents.showAlert(deleted ? "Success" : "Error", deleted ? "User deleted." : "Delete failed.");
            });

            row.getChildren().addAll(info, spacer, deleteBtn);
            rows.getChildren().add(row);
        }

        scrollPane.setContent(rows);
        content.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return content;
    }

    private static VBox createModulesContent(Admin admin) {
        VBox content = UIComponents.contentBox(10);
        content.getChildren().add(UIComponents.sectionTitle("All Modules"));

        ScrollPane scrollPane = UIComponents.darkScrollPane();
        VBox rows = new VBox(8);

        for (HashMap<String, Object> module : admin.getAllModules()) {
            HBox row = new HBox(10);
            row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-radius: 4;");

            Label info = new Label("#" + module.get("modId") + " | " + module.get("subject") + " | " + module.get("learningPath"));
            info.setStyle("-fx-text-fill: #c9d1d9;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button deleteBtn = new Button("Delete");
            deleteBtn.setOnAction(e -> {
                boolean deleted = admin.deleteModule((Integer) module.get("modId"));
                UIComponents.showAlert(deleted ? "Success" : "Error", deleted ? "Module deleted." : "Delete failed.");
            });

            row.getChildren().addAll(info, spacer, deleteBtn);
            rows.getChildren().add(row);
        }

        scrollPane.setContent(rows);
        content.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return content;
    }

    private static VBox createJobsContent(Admin admin) {
        VBox content = UIComponents.contentBox(10);
        content.getChildren().add(UIComponents.sectionTitle("All Job Programs"));

        ScrollPane scrollPane = UIComponents.darkScrollPane();
        VBox rows = new VBox(8);

        for (HashMap<String, Object> job : admin.getAllJobPrograms()) {
            HBox row = new HBox(10);
            row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-radius: 4;");

            Label info = new Label("#" + job.get("jobId") + " | " + job.get("jobType") + " | " + job.get("company"));
            info.setStyle("-fx-text-fill: #c9d1d9;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button deleteBtn = new Button("Delete");
            deleteBtn.setOnAction(e -> {
                boolean deleted = admin.deleteJobProgram((Integer) job.get("jobId"));
                UIComponents.showAlert(deleted ? "Success" : "Error", deleted ? "Job program deleted." : "Delete failed.");
            });

            row.getChildren().addAll(info, spacer, deleteBtn);
            rows.getChildren().add(row);
        }

        scrollPane.setContent(rows);
        content.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return content;
    }
}
