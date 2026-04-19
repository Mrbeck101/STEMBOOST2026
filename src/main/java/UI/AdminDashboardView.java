package UI;

import UserFactory.Admin;
import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;

public class AdminDashboardView {

    public static Scene create(SceneRouter router) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Admin admin = (Admin) UserContext.getInstance().getCurrentUser();
        if (admin == null) {
            return LoginView.create(router);
        }

        BorderPane root = new BorderPane();
        root.setTop(createTopBar(admin, router));

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
                new Tab("Users", createUsersContent(admin)),
                new Tab("Modules", createModulesContent(admin)),
                new Tab("Job Programs", createJobsContent(admin)),
                new Tab("Inbox", createInboxContent(router))
        );

        root.setCenter(tabPane);
        return new Scene(root, 1400, 900);
    }

    private static HBox createTopBar(Admin admin, SceneRouter router) {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("STEMBOOST - Admin Dashboard");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label name = new Label("Welcome, " + admin.getName());
        name.setStyle("-fx-font-size: 14; -fx-text-fill: #ffffff;");

        Button contactsBtn = new Button("Contacts");
        contactsBtn.setOnAction(e -> router.goToContacts());

        Button profileBtn = new Button("Profile");
        profileBtn.setOnAction(e -> router.goToProfile());

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> router.goToLogin());

        topBar.getChildren().addAll(title, spacer, name, contactsBtn, profileBtn, logoutBtn);
        return topBar;
    }

    private static VBox createUsersContent(Admin admin) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("All Users");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);

        VBox rows = new VBox(8);
        List<HashMap<String, Object>> users = admin.getAllUsers();
        for (HashMap<String, Object> user : users) {
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
                showAlert(deleted ? "Success" : "Error", deleted ? "User deleted." : "Delete failed.");
            });

            row.getChildren().addAll(info, spacer, deleteBtn);
            rows.getChildren().add(row);
        }

        scrollPane.setContent(rows);
        content.getChildren().addAll(title, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return content;
    }

    private static VBox createModulesContent(Admin admin) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("All Modules");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);

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
                showAlert(deleted ? "Success" : "Error", deleted ? "Module deleted." : "Delete failed.");
            });

            row.getChildren().addAll(info, spacer, deleteBtn);
            rows.getChildren().add(row);
        }

        scrollPane.setContent(rows);
        content.getChildren().addAll(title, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return content;
    }

    private static VBox createJobsContent(Admin admin) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("All Job Programs");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);

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
                showAlert(deleted ? "Success" : "Error", deleted ? "Job program deleted." : "Delete failed.");
            });

            row.getChildren().addAll(info, spacer, deleteBtn);
            rows.getChildren().add(row);
        }

        scrollPane.setContent(rows);
        content.getChildren().addAll(title, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return content;
    }

    private static VBox createInboxContent(SceneRouter router) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label title = new Label("Inbox");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Button viewMessagesBtn = new Button("View Messages");
        viewMessagesBtn.setOnAction(e -> router.goToInbox());

        content.getChildren().addAll(title, viewMessagesBtn);
        return content;
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

