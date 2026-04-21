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
                new Tab("Users", createUsersContent(admin, router)),
                new Tab("Modules", createModulesContent(admin)),
                new Tab("Job Programs", createJobsContent(admin)),
                new Tab("Inbox", UIComponents.inboxTab(admin, router)),
                new Tab("Contact Info", UIComponents.contactInfoTab(admin))
        );

        return UIComponents.buildScene(
                UIComponents.topBar("STEMBOOST - Admin Dashboard", admin.getName(), router, contactsBtn, profileBtn),
                tabPane
        );
    }

    private static VBox createUsersContent(Admin admin, SceneRouter router) {
        VBox content = UIComponents.contentBox(10);
        content.getChildren().addAll(
                UIComponents.sectionTitle("Create User"),
                createUserForm(admin, router),
                new Separator(),
                UIComponents.sectionTitle("All Users")
        );

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

    private static VBox createUserForm(Admin admin, SceneRouter router) {
        VBox form = new VBox(10);
        form.setPadding(new Insets(15));
        form.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-radius: 6;");

        GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(10);

        TextField firstName = new TextField();
        TextField lastName = new TextField();
        TextField email = new TextField();
        PasswordField password = new PasswordField();
        PasswordField confirmPassword = new PasswordField();
        ComboBox<String> acctType = new ComboBox<>();
        acctType.getItems().addAll("Educator", "Student", "Parent", "Counselor", "Employer", "University", "Admin");
        acctType.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> associatedStudent = new ComboBox<>();
        associatedStudent.setPromptText("Select a student");
        associatedStudent.setMaxWidth(Double.MAX_VALUE);
        TextField company = new TextField();
        TextField university = new TextField();

        Label associatedStudentLabel = new Label("Associated Student ID");
        Label companyLabel = new Label("Company Name");
        Label universityLabel = new Label("University Name");
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ff7b72;");
        errorLabel.setWrapText(true);

        HashMap<String, Integer> studentLabelToId = new HashMap<>();
        List<HashMap<String, Object>> users = admin.getAllUsers();
        if (users != null) {
            for (HashMap<String, Object> user : users) {
                if (!"Student".equals(user.get("acctType"))) {
                    continue;
                }
                Integer userId = user.get("userId") instanceof Integer ? (Integer) user.get("userId") : null;
                String name = user.get("name") == null ? "Student" : String.valueOf(user.get("name"));
                if (userId != null) {
                    String label = name + " (ID #" + userId + ")";
                    associatedStudent.getItems().add(label);
                    studentLabelToId.put(label, userId);
                }
            }
        }

        Runnable updateFieldVisibility = () -> {
            String type = acctType.getValue();
            boolean showParent = "Parent".equals(type);
            boolean showEmployer = "Employer".equals(type);
            boolean showUniversity = "Student".equals(type) || "University".equals(type);

            associatedStudentLabel.setManaged(showParent);
            associatedStudentLabel.setVisible(showParent);
            associatedStudent.setManaged(showParent);
            associatedStudent.setVisible(showParent);

            companyLabel.setManaged(showEmployer);
            companyLabel.setVisible(showEmployer);
            company.setManaged(showEmployer);
            company.setVisible(showEmployer);

            universityLabel.setManaged(showUniversity);
            universityLabel.setVisible(showUniversity);
            university.setManaged(showUniversity);
            university.setVisible(showUniversity);
        };

        acctType.setOnAction(e -> updateFieldVisibility.run());
        updateFieldVisibility.run();

        int row = 0;
        fields.add(new Label("First Name"), 0, row);
        fields.add(firstName, 1, row++);
        fields.add(new Label("Last Name"), 0, row);
        fields.add(lastName, 1, row++);
        fields.add(new Label("Email"), 0, row);
        fields.add(email, 1, row++);
        fields.add(new Label("Password"), 0, row);
        fields.add(password, 1, row++);
        fields.add(new Label("Confirm Password"), 0, row);
        fields.add(confirmPassword, 1, row++);
        fields.add(new Label("Account Type"), 0, row);
        fields.add(acctType, 1, row++);
        fields.add(associatedStudentLabel, 0, row);
        fields.add(associatedStudent, 1, row++);
        fields.add(companyLabel, 0, row);
        fields.add(company, 1, row++);
        fields.add(universityLabel, 0, row);
        fields.add(university, 1, row);

        ColumnConstraints left = new ColumnConstraints();
        left.setMinWidth(160);
        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        fields.getColumnConstraints().addAll(left, right);

        Button createBtn = new Button("Create User");
        Button clearBtn = new Button("Clear");

        Runnable clearForm = () -> {
            firstName.clear();
            lastName.clear();
            email.clear();
            password.clear();
            confirmPassword.clear();
            acctType.setValue(null);
            associatedStudent.setValue(null);
            company.clear();
            university.clear();
            errorLabel.setText("");
            updateFieldVisibility.run();
        };

        createBtn.setOnAction(e -> {
            errorLabel.setText("");

            if (!password.getText().equals(confirmPassword.getText())) {
                errorLabel.setText("Passwords do not match.");
                return;
            }

            try {
                Integer associatedStudentId = null;
                if ("Parent".equals(acctType.getValue())) {
                    associatedStudentId = studentLabelToId.get(associatedStudent.getValue());
                    if (associatedStudentId == null) {
                        errorLabel.setText("Please select an associated student.");
                        return;
                    }
                }

                boolean created = admin.createUser(
                        email.getText(),
                        password.getText(),
                        firstName.getText(),
                        lastName.getText(),
                        acctType.getValue(),
                        company.getText(),
                        associatedStudentId,
                        university.getText()
                );

                if (created) {
                    UIComponents.showAlert("Success", "User account created.");
                    router.goToDashboard(admin.getId(), admin.getAcctType());
                } else {
                    errorLabel.setText("User creation failed.");
                }
            } catch (RuntimeException ex) {
                errorLabel.setText(ex.getMessage());
            }
        });

        clearBtn.setOnAction(e -> clearForm.run());

        HBox actions = new HBox(10, createBtn, clearBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        form.getChildren().addAll(fields, actions, errorLabel);
        return form;
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
