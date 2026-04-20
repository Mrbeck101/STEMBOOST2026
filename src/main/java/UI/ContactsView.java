package UI;

import UserFactory.*;
import Services.FetchProfileService;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import java.util.HashMap;
import java.util.List;

/**
 * Contacts View - Display and manage user contacts for messaging
 */
public class ContactsView {

    public static Scene create(SceneRouter router) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        User currentUser = UserContext.getInstance().getCurrentUser();
        if (currentUser == null) {
            return LoginView.create(router);
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Top bar
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-font-size: 12;");
        backBtn.setOnAction(e -> router.goToInbox());

        Label title = new Label("Contacts");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        title.setAccessibleText("Contacts page");

        topBar.getChildren().addAll(backBtn, title);
        root.setTop(topBar);

        // Content
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0D1117;");

        Label contentTitle = new Label("Your Contacts");
        contentTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        contentTitle.setAccessibleText("Your Contacts section");

        // Get contacts based on user type
        List<HashMap<String, Object>> contacts = getUserContacts(currentUser);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #0D1117;");

        VBox contactsVBox = new VBox(10);
        contactsVBox.setPadding(new Insets(10));

        if (contacts != null && !contacts.isEmpty()) {
            for (HashMap<String, Object> contact : contacts) {
                VBox contactCard = createContactCard(contact, router, currentUser);
                contactsVBox.getChildren().add(contactCard);
            }
        } else {
            Label noContacts = new Label("No contacts available");
            noContacts.setStyle("-fx-font-size: 14; -fx-text-fill: #aaaaaa;");
            noContacts.setAccessibleText("You have no contacts available");
            contactsVBox.getChildren().add(noContacts);
        }

        scrollPane.setContent(contactsVBox);
        content.getChildren().addAll(contentTitle, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.setCenter(content);

        Scene scene = new Scene(root, 1400, 900);
        return scene;
    }

    private static List<HashMap<String, Object>> getUserContacts(User user) {
        try {
            FetchProfileService profileService = new FetchProfileService();
            if ("Admin".equals(user.getAcctType())) {
                return profileService.listUsers();
            }
            return profileService.getContactsByRole(user.getId(), user.getAcctType());
        } catch (Exception e) {
            System.err.println("Error retrieving contacts: " + e.getMessage());
            return null;
        }
    }

    private static VBox createContactCard(HashMap<String, Object> contact, SceneRouter router, User currentUser) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #161B22; -fx-border-radius: 6; -fx-border-color: #30363D;");
        card.setAccessibleRole(AccessibleRole.NODE);

        Label nameLabel = new Label((String) contact.get("name"));
        nameLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");
        nameLabel.setAccessibleText("Contact: " + contact.get("name"));

        Label typeLabel = new Label((String) contact.get("acct_type"));
        typeLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
        typeLabel.setAccessibleText("Account type: " + contact.get("acct_type"));

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button messageBtn = new Button("Send Message");
        messageBtn.setStyle("-fx-font-size: 12; -fx-padding: 8 15 8 15;");
        messageBtn.setAccessibleText("Send message to " + contact.get("name"));

        messageBtn.setOnAction(e -> {
            int contactId = (Integer) contact.get("user_id");
            router.goToInboxWithContact(contactId);
        });

        buttonBox.getChildren().add(messageBtn);

        card.getChildren().addAll(nameLabel, typeLabel, buttonBox);
        return card;
    }


    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
