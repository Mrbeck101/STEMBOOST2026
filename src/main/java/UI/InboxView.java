package UI;

import UserFactory.User;
import OtherComponents.Message;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import java.util.List;

/**
 * Inbox View - Display and manage messages
 */
public class InboxView {

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
        backBtn.setOnAction(e -> {
            String userType = currentUser.getAcctType();
            switch (userType) {
                case "Student" -> router.goToDashboard(currentUser.getId(), "Student");
                case "Educator" -> router.goToDashboard(currentUser.getId(), "Educator");
                case "Counselor" -> router.goToDashboard(currentUser.getId(), "Counselor");
                case "Parent" -> router.goToDashboard(currentUser.getId(), "Parent");
                case "Employer" -> router.goToDashboard(currentUser.getId(), "Employer");
                case "University" -> router.goToDashboard(currentUser.getId(), "University");
                case "Admin" -> router.goToDashboard(currentUser.getId(), "Admin");
                default -> router.goToLogin();
            }
        });

        Button contactsBtn = new Button("Contacts");
        contactsBtn.setStyle("-fx-font-size: 12;");
        contactsBtn.setOnAction(e -> router.goToContacts());

        Button profileBtn = new Button("Update Contact Info");
        profileBtn.setStyle("-fx-font-size: 12;");
        profileBtn.setOnAction(e -> router.goToProfile());

        Label title = new Label("Inbox");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        title.setAccessibleText("Inbox page");

        topBar.getChildren().addAll(backBtn, title, contactsBtn, profileBtn);
        root.setTop(topBar);

        // Content
        HBox mainContent = new HBox(15);
        mainContent.setPadding(new Insets(20));
        mainContent.setStyle("-fx-background-color: #0D1117;");

        // Messages list
        VBox messagesList = new VBox(10);
        messagesList.setPrefWidth(400);
        messagesList.setStyle("-fx-border-color: #30363D; -fx-border-width: 0 1 0 0;");
        messagesList.setPadding(new Insets(10));

        Label messagesTitle = new Label("Messages");
        messagesTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        messagesTitle.setAccessibleText("Messages list");

        List<Message> inbox = currentUser.checkInbox();
        final Message[] selectedMessage = new Message[1];
        ScrollPane messageScrollPane = new ScrollPane();
        messageScrollPane.setFitToWidth(true);
        messageScrollPane.setStyle("-fx-background-color: #0D1117;");

        VBox messagesVBox = new VBox(8);
        messagesVBox.setPadding(new Insets(5));

        Label detailTitle = new Label("Select a message to view details");
        TextArea detailContent = new TextArea();

        if (inbox != null && !inbox.isEmpty()) {
            for (Message msg : inbox) {
                VBox msgItem = createMessageItem(msg, clickedMessage -> {
                    selectedMessage[0] = clickedMessage;
                    detailTitle.setText(clickedMessage.getSubject());
                    detailContent.setText(clickedMessage.getContent());
                });
                messagesVBox.getChildren().add(msgItem);
            }
        } else {
            Label noMessages = new Label("No messages");
            noMessages.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
            messagesVBox.getChildren().add(noMessages);
        }

        messageScrollPane.setContent(messagesVBox);
        messagesList.getChildren().addAll(messagesTitle, messageScrollPane);
        VBox.setVgrow(messageScrollPane, Priority.ALWAYS);

        // Message detail area
        VBox detailArea = new VBox(15);
        detailArea.setPadding(new Insets(20));
        detailArea.setStyle("-fx-background-color: #161B22; -fx-border-radius: 8;");

        detailTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        detailTitle.setAccessibleText("Message details section");

        detailContent.setWrapText(true);
        detailContent.setEditable(false);
        detailContent.setStyle("-fx-control-inner-background: #0D1117; -fx-text-fill: #c9d1d9;");
        detailContent.setPrefRowCount(10);

        HBox replyBox = new HBox(10);
        TextField replyField = new TextField();
        replyField.setPromptText("Type your reply...");
        Button sendBtn = new Button("Send");
        sendBtn.setStyle("-fx-font-size: 11;");
        sendBtn.setOnAction(e -> {
            String replyText = replyField.getText().trim();
            if (selectedMessage[0] == null) {
                showAlert("Reply", "Select a message before sending a reply.");
                return;
            }
            if (replyText.isEmpty()) {
                showAlert("Reply", "Reply message cannot be empty.");
                return;
            }

            try {
                Message reply = new Message(currentUser.getId(), selectedMessage[0].getSenderID(), "RE: " + selectedMessage[0].getSubject(), replyText);
                boolean sent = currentUser.sendMessage(reply.getReceiverID(), reply.getSubject(), reply.getContent());
                showAlert(sent ? "Success" : "Error", sent ? "Reply sent." : "Failed to send reply.");
                if (sent) {
                    replyField.clear();
                }
            } catch (Exception ex) {
                showAlert("Error", "Failed to send reply: " + ex.getMessage());
            }
        });
        replyBox.getChildren().addAll(replyField, sendBtn);
        HBox.setHgrow(replyField, Priority.ALWAYS);

        detailArea.getChildren().addAll(detailTitle, detailContent, replyBox);
        VBox.setVgrow(detailContent, Priority.ALWAYS);

        mainContent.getChildren().addAll(messagesList, detailArea);
        HBox.setHgrow(detailArea, Priority.ALWAYS);

        root.setCenter(mainContent);

        Scene scene = new Scene(root, 1400, 900);
        return scene;
    }

    private static VBox createMessageItem(Message message, java.util.function.Consumer<Message> onSelect) {
        VBox item = new VBox(5);
        item.setPadding(new Insets(10));
        item.setStyle("-fx-background-color: #0D1117; -fx-border-radius: 4; -fx-cursor: hand;");
        item.setAccessibleRole(AccessibleRole.BUTTON);
        item.setAccessibleText("Message from " + message.getSenderID() + ": " + message.getSubject());

        Label subject = new Label(message.getSubject());
        subject.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        Label sender = new Label("From: " + message.getSenderID());
        sender.setStyle("-fx-font-size: 11; -fx-text-fill: #8b949e;");

        item.setOnMouseClicked(e -> onSelect.accept(message));

        item.getChildren().addAll(subject, sender);
        return item;
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
