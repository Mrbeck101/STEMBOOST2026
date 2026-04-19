# STEMBOOST UI - Code Examples & Best Practices

## UserContext - Persistent User Management

### Basic Usage
```java
// After successful login
Student student = new Student(userId);
UserContext.getInstance().setCurrentUser(student);

// In any other view
Student currentStudent = (Student) UserContext.getInstance().getCurrentUser();
List<LearningModule> modules = currentStudent.getLearningModules();

// Check if user is logged in
if (UserContext.getInstance().isUserLoggedIn()) {
    String userType = UserContext.getInstance().getUserType();
    System.out.println("Logged in as: " + userType);
}

// On logout
UserContext.getInstance().logout();
router.goToLogin();
```

## Accessing User in Views

### Pattern Used in All Dashboards
```java
public static Scene create(SceneRouter router) {
    Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

    // 1. Get current user from context
    Student student = (Student) UserContext.getInstance().getCurrentUser();
    
    // 2. Verify user is logged in
    if (student == null) {
        return LoginView.create(router);
    }
    
    // 3. Build UI with user data
    BorderPane root = new BorderPane();
    
    // ... rest of view construction
}
```

## Accessibility Implementation Examples

### Example 1: Screen Reader Support on Labels
```java
// ✅ Good - Screen reader friendly
Label title = new Label("Learning Modules");
title.setAccessibleText("Learning Modules section containing your course materials");
title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

// ✅ Good - Input label association
Label emailLabel = new Label("Email");
TextField emailField = new TextField();
emailLabel.setLabelFor(emailField);
emailField.setAccessibleText("Email address input field");
```

### Example 2: Color with Text Labels
```java
// ✅ Good - Color + text conveys information
Label status = new Label("Status: Completed");
status.setStyle("-fx-text-fill: #238636;"); // Green
status.setAccessibleText("Status: Completed");

// ❌ Bad - Only color
Label status = new Label("");
status.setStyle("-fx-background-color: #238636;"); // Only green circle
// Screen readers can't read this!
```

### Example 3: Progress Indicator with Accessible Label
```java
// ✅ Good - Multiple ways to convey progress
ProgressBar progressBar = new ProgressBar(0.75);
progressBar.setAccessibleText("Module progress: 75 percent complete");

Label percentLabel = new Label("75% Complete");
percentLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #8b949e;");
percentLabel.setAccessibleText("75% Complete");

// Users can understand progress visually AND via screen reader
```

### Example 4: Semantic Containers
```java
// ✅ Good - Semantic structure for screen readers
VBox welcomeCard = new VBox(15);
welcomeCard.setAccessibleRole(AccessibleRole.PANE);
welcomeCard.setAccessibleText("Welcome section containing your learning path and progress overview");

Label title = new Label("Your Learning Journey");
title.setAccessibleText("Your Learning Journey");

welcomeCard.getChildren().add(title);
```

## Navigation & Routing Examples

### SceneRouter Usage
```java
// Route to specific dashboard
router.goToDashboard(userId, "Student");
router.goToDashboard(userId, "Educator");
router.goToDashboard(userId, "Counselor");

// Navigate to specific views
router.goToModules();
router.goToAssessments();
router.goToInbox();

// Return to login
router.goToLogin();
```

### Back Button Pattern (Used in All Views)
```java
Button backBtn = new Button("← Back");
backBtn.setOnAction(e -> {
    User user = UserContext.getInstance().getCurrentUser();
    String userType = user.getAcctType();
    
    switch (userType) {
        case "Student" -> router.goToDashboard(user.getId(), "Student");
        case "Educator" -> router.goToDashboard(user.getId(), "Educator");
        default -> router.goToLogin();
    }
});
```

## Theme & Styling Patterns

### Consistent Color Usage
```java
// Title styling (all dashboards)
Label title = new Label("Dashboard");
title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

// Top bar (all dashboards)
HBox topBar = new HBox(20);
topBar.setStyle("-fx-background-color: #1C2128;");
topBar.setPadding(new Insets(15, 20, 15, 20));

// Card background (all dashboards)
VBox card = new VBox(10);
card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");
card.setPadding(new Insets(15));

// Accent/Info text
Label info = new Label("Important information");
info.setStyle("-fx-font-size: 12; -fx-text-fill: #58a6ff;");
```

### Reusable Card Pattern
```java
private static VBox createStatCard(String title, String value, String description) {
    VBox card = new VBox(8);
    card.setPadding(new Insets(15));
    card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6;");
    card.setAccessibleRole(AccessibleRole.PANE);
    card.setAccessibleText(description);

    Label titleLabel = new Label(title);
    titleLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
    titleLabel.setAccessibleText(title);

    Label valueLabel = new Label(value);
    valueLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");
    valueLabel.setAccessibleText(value);

    card.getChildren().addAll(titleLabel, valueLabel);
    return card;
}

// Usage
VBox statsCard = createStatCard("Active Modules", "5", "Number of active modules");
```

## Form Validation Pattern

```java
Button submitBtn = new Button("Submit");
submitBtn.setOnAction(e -> {
    // 1. Validate input
    String subject = subjectField.getText();
    String path = pathCombo.getValue();
    String content = contentArea.getText();

    if (subject.isEmpty() || path == null || content.isEmpty()) {
        showAlert("Validation Error", "Please fill in all required fields");
        return;
    }

    try {
        // 2. Process input
        LearningModule module = new LearningModule(
            0, 0, educator.getId(), path, content, subject
        );
        
        // 3. Database operation (TODO)
        // database.addModule(module);
        
        // 4. Show success feedback
        showAlert("Success", "Module created successfully!");
        
        // 5. Navigate away
        router.goToDashboard(educator.getId(), "Educator");
        
    } catch (NumberFormatException ex) {
        showAlert("Error", "Invalid input format");
    }
});
```

## Keyboard Navigation Pattern

```java
// Define tab order
Control[] order = {emailField, passwordField, loginBtn, registerLink};

for (int i = 0; i < order.length; i++) {
    final int index = i;
    order[i].setOnKeyPressed(e -> {
        if (e.getCode() == KeyCode.DOWN) {
            if (index < order.length - 1) {
                order[index + 1].requestFocus();
            }
        } else if (e.getCode() == KeyCode.UP) {
            if (index > 0) {
                order[index - 1].requestFocus();
            }
        }
    });
}

// Also handle Enter key
emailField.setOnAction(e -> passwordField.requestFocus());
passwordField.setOnAction(e -> loginAction.run());
```

## Layout Patterns

### Dashboard Layout
```java
BorderPane root = new BorderPane();
root.setTop(createTopBar());      // Navigation bar
root.setCenter(createContent()); // Main content
// root.setLeft(createSidebar());   // Optional sidebar
// root.setRight(createRightPanel()); // Optional right panel
// root.setBottom(createFooter());  // Optional footer
```

### Content Organization
```java
VBox mainContent = new VBox(20);  // 20px spacing
mainContent.setPadding(new Insets(20));
mainContent.setStyle("-fx-background-color: #0D1117;");

// Add title
Label title = new Label("Section Title");
mainContent.getChildren().add(title);

// Add scrollable content
ScrollPane scrollPane = new ScrollPane(innerContent);
scrollPane.setFitToWidth(true);
VBox.setVgrow(scrollPane, Priority.ALWAYS); // Take remaining space
mainContent.getChildren().add(scrollPane);
```

### Tab-based Navigation
```java
TabPane tabPane = new TabPane();
tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

Tab tab1 = new Tab("Dashboard", createDashboardContent());
Tab tab2 = new Tab("Modules", createModulesContent());
Tab tab3 = new Tab("Settings", createSettingsContent());

tab1.setStyle("-fx-text-base-color: #ffffff;");
tab2.setStyle("-fx-text-base-color: #ffffff;");
tab3.setStyle("-fx-text-base-color: #ffffff;");

tabPane.getTabs().addAll(tab1, tab2, tab3);
```

## Null Safety Pattern

```java
// Always check for null when retrieving data
List<LearningModule> modules = educator.getLearningModules();

if (modules == null || modules.isEmpty()) {
    Label noData = new Label("No modules available");
    noData.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
    content.getChildren().add(noData);
} else {
    for (LearningModule module : modules) {
        // Process module
    }
}
```

## Error Handling

```java
private static void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
}

// Usage
try {
    int moduleId = Integer.parseInt(moduleField.getText());
    // Process
} catch (NumberFormatException ex) {
    showAlert("Error", "Module ID must be a valid number");
} catch (Exception ex) {
    showAlert("Error", "An error occurred: " + ex.getMessage());
}
```

## Best Practices Summary

1. ✅ **Always check for logged-in user** at start of view creation
2. ✅ **Use UserContext for persistent access** to current user
3. ✅ **Provide accessible text** for all interactive elements
4. ✅ **Use semantic roles** for containers
5. ✅ **Support keyboard navigation** with Tab and Arrow keys
6. ✅ **Validate form input** before processing
7. ✅ **Provide user feedback** via alerts
8. ✅ **Use consistent styling** across all views
9. ✅ **Handle null safely** when accessing data
10. ✅ **Test with screen readers** (NVDA)

---

**Last Updated:** April 19, 2026

