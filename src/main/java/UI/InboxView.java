package UI;

import DatabaseController.dbConnector;
import Services.FetchProfileService;
import Services.UIRefreshService;
import UserFactory.User;
import OtherComponents.Message;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import java.util.HashMap;
import java.util.List;

/**
 * Inbox View - Modern chat window with contacts sidebar
 */
public class InboxView {

    public static Scene create(SceneRouter router) {
        return create(router, -1);
    }

    public static Scene create(SceneRouter router, int preSelectedContactId) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        User currentUser = UserContext.getInstance().getCurrentUser();
        if (currentUser == null) {
            return LoginView.create(router);
        }

        // Start refresh polling
        UIRefreshService.getInstance().startPolling(currentUser);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Top bar
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: #1C2128;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-font-size: 12;");
        backBtn.setOnAction(e -> router.goToCurrentUserDashboard());

        Label title = new Label("Messages");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        topBar.getChildren().addAll(backBtn, title);
        root.setTop(topBar);

        // === Name cache ===
        HashMap<Integer, String> partnerNameCache = new HashMap<>();

        // === Chat area (right) ===
        VBox chatArea = new VBox(10);
        chatArea.setPadding(new Insets(15));
        chatArea.setStyle("-fx-background-color: #0D1117;");
        HBox.setHgrow(chatArea, Priority.ALWAYS);

        Label detailTitle = new Label("Select a conversation");
        detailTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        ScrollPane chatScrollPane = new ScrollPane();
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setStyle("-fx-background-color: #0D1117;");
        VBox chatBubblesBox = new VBox(10);
        chatBubblesBox.setPadding(new Insets(10));
        chatScrollPane.setContent(chatBubblesBox);
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

        // Send box
        HBox sendBox = new HBox(10);
        sendBox.setPadding(new Insets(8, 0, 0, 0));
        TextField messageField = new TextField();
        messageField.setPromptText("Type a message...");
        Button sendBtn = new Button("Send");
        sendBtn.setStyle("-fx-font-size: 11;");
        sendBox.getChildren().addAll(messageField, sendBtn);
        HBox.setHgrow(messageField, Priority.ALWAYS);
        sendBox.setVisible(false); // hidden until a conversation is selected

        chatArea.getChildren().addAll(detailTitle, chatScrollPane, sendBox);

        // === Helper: open a conversation with a given partner ===
        final int[] activePartnerId = {-1};

        // === Conversations list (center) ===
        VBox messagesList = new VBox(10);
        messagesList.setPrefWidth(250);
        messagesList.setMinWidth(200);
        messagesList.setStyle("-fx-border-color: #30363D; -fx-border-width: 0 1 0 0;");
        messagesList.setPadding(new Insets(10));

        Label messagesTitle = new Label("Conversations");
        messagesTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        ScrollPane messageScrollPane = new ScrollPane();
        messageScrollPane.setFitToWidth(true);
        messageScrollPane.setStyle("-fx-background-color: #0D1117;");
        VBox messagesVBox = new VBox(8);
        messagesVBox.setPadding(new Insets(5));

        // === Build conversations from inbox ===
        List<Message> inbox = currentUser.checkInbox();

        // Register listener for message updates
        UIRefreshService.UIRefreshListener messageRefreshListener = (updateType, data) -> {
            if ("MESSAGES_UPDATED".equals(updateType) && activePartnerId[0] > 0) {
                // Refresh the active conversation
                List<Message> updatedConversation = new dbConnector().searchConversationMessages(currentUser.getId(), activePartnerId[0]);
                int currentBubbleCount = chatBubblesBox.getChildren().size();
                if (currentBubbleCount > 0 && !(chatBubblesBox.getChildren().get(0) instanceof Label)) {
                    // Only show new messages beyond what's already displayed
                    if (updatedConversation.size() > currentBubbleCount) {
                        for (int i = currentBubbleCount; i < updatedConversation.size(); i++) {
                            Message msg = updatedConversation.get(i);
                            boolean isMe = msg.getSenderID() == currentUser.getId();
                            chatBubblesBox.getChildren().add(createBubble(msg.getContent(), isMe));
                        }
                        chatScrollPane.setVvalue(1.0);
                    }
                }
            }
        };
        UIRefreshService.getInstance().addListener(messageRefreshListener);

        java.util.function.BiConsumer<Integer, String> openConversation = (partnerId, partnerName) -> {
            activePartnerId[0] = partnerId;
            detailTitle.setText("Chat with " + partnerName);
            chatBubblesBox.getChildren().clear();
            List<Message> conversation = new dbConnector().searchConversationMessages(currentUser.getId(), partnerId);
            for (Message msg : conversation) {
                boolean isMe = msg.getSenderID() == currentUser.getId();
                chatBubblesBox.getChildren().add(createBubble(msg.getContent(), isMe));
            }
            if (conversation.isEmpty()) {
                Label empty = new Label("No messages yet. Send the first one!");
                empty.setStyle("-fx-text-fill: #8b949e; -fx-font-style: italic;");
                chatBubblesBox.getChildren().add(empty);
            }
            chatScrollPane.setVvalue(1.0);
            sendBox.setVisible(true);
        };

        // Wire send button
        sendBtn.setOnAction(e -> {
            String text = messageField.getText().trim();
            if (activePartnerId[0] < 0 || text.isEmpty()) return;
            try {
                boolean sent = currentUser.sendMessage(activePartnerId[0], text);
                if (sent) {
                    // Remove "no messages yet" label if present
                    chatBubblesBox.getChildren().removeIf(n -> n instanceof Label);
                    messageField.clear();
                    chatBubblesBox.getChildren().add(createBubble(text, true));
                    chatScrollPane.setVvalue(1.0);
                } else {
                    showAlert("Error", "Failed to send message.");
                }
            } catch (Exception ex) {
                showAlert("Error", "Failed to send: " + ex.getMessage());
            }
        });

        // Allow Enter key to send
        messageField.setOnAction(e -> sendBtn.fire());

        // Build conversation list from inbox messages (unique partners)
        if (inbox != null && !inbox.isEmpty()) {
            java.util.Set<Integer> seenPartners = new java.util.HashSet<>();
            for (Message msg : inbox) {
                int partnerId = msg.getSenderID() == currentUser.getId() ? msg.getReceiverID() : msg.getSenderID();
                if (!seenPartners.add(partnerId)) continue;
                String partnerName = resolveUserLabel(partnerId, partnerNameCache);
                VBox item = createConversationItem(partnerName, () -> openConversation.accept(partnerId, partnerName));
                messagesVBox.getChildren().add(item);
            }
        } else {
            Label noMessages = new Label("No conversations yet");
            noMessages.setStyle("-fx-font-size: 12; -fx-text-fill: #8b949e;");
            messagesVBox.getChildren().add(noMessages);
        }

        messageScrollPane.setContent(messagesVBox);
        messagesList.getChildren().addAll(messagesTitle, messageScrollPane);
        VBox.setVgrow(messageScrollPane, Priority.ALWAYS);

        // === Contacts Sidebar (left) ===
        VBox contactsSidebar = new VBox(8);
        contactsSidebar.setPrefWidth(220);
        contactsSidebar.setMinWidth(180);
        contactsSidebar.setPadding(new Insets(12));
        contactsSidebar.setStyle("-fx-background-color: #161B22; -fx-border-color: #30363D; -fx-border-width: 0 1 0 0;");

        Label contactsTitle = new Label("Contacts");
        contactsTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        ScrollPane contactsScroll = new ScrollPane();
        contactsScroll.setFitToWidth(true);
        contactsScroll.setStyle("-fx-background-color: #161B22;");
        VBox contactsVBox = new VBox(6);
        contactsVBox.setPadding(new Insets(4));

        List<HashMap<String, Object>> contacts = getContacts(currentUser);
        if (contacts != null && !contacts.isEmpty()) {
            for (HashMap<String, Object> contact : contacts) {
                int contactId = (Integer) contact.get("user_id");
                String contactName = (String) contact.get("name");
                String contactRole = (String) contact.get("acct_type");

                VBox contactItem = new VBox(2);
                contactItem.setPadding(new Insets(8));
                contactItem.setStyle("-fx-background-color: #0D1117; -fx-border-radius: 4; -fx-cursor: hand;");

                Label nameLabel = new Label(contactName);
                nameLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");
                nameLabel.setWrapText(true);

                Label roleLabel = new Label(contactRole);
                roleLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #8b949e;");

                Button msgBtn = new Button("Message");
                msgBtn.setStyle("-fx-font-size: 10; -fx-padding: 4 8 4 8;");

                String resolvedName = contactName;
                partnerNameCache.put(contactId, resolvedName);
                msgBtn.setOnAction(e -> openConversation.accept(contactId, resolvedName));

                contactItem.getChildren().addAll(nameLabel, roleLabel, msgBtn);
                contactsVBox.getChildren().add(contactItem);
            }
        } else {
            Label noContacts = new Label("No contacts");
            noContacts.setStyle("-fx-font-size: 11; -fx-text-fill: #8b949e;");
            contactsVBox.getChildren().add(noContacts);
        }

        contactsScroll.setContent(contactsVBox);
        VBox.setVgrow(contactsScroll, Priority.ALWAYS);
        contactsSidebar.getChildren().addAll(contactsTitle, contactsScroll);

        // Assemble layout
        HBox mainContent = new HBox(0);
        mainContent.setStyle("-fx-background-color: #0D1117;");
        HBox.setHgrow(chatArea, Priority.ALWAYS);
        mainContent.getChildren().addAll(contactsSidebar, messagesList, chatArea);
        root.setCenter(mainContent);

        // Pre-select contact if requested
        if (preSelectedContactId > 0) {
            String preSelectedName = resolveUserLabel(preSelectedContactId, partnerNameCache);
            openConversation.accept(preSelectedContactId, preSelectedName);
        }

        return new Scene(root, 1400, 900);
    }

    private static VBox createConversationItem(String partnerName, Runnable onClick) {
        VBox item = new VBox(4);
        item.setPadding(new Insets(10));
        item.setStyle("-fx-background-color: #0D1117; -fx-border-radius: 4; -fx-cursor: hand;");
        item.setAccessibleRole(AccessibleRole.BUTTON);

        Label nameLabel = new Label(partnerName);
        nameLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        item.setOnMouseClicked(e -> onClick.run());
        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #161B22; -fx-border-radius: 4; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: #0D1117; -fx-border-radius: 4; -fx-cursor: hand;"));
        item.getChildren().add(nameLabel);
        return item;
    }

    private static HBox createBubble(String text, boolean isMe) {
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(450);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        if (isMe) {
            bubble.setStyle("-fx-background-color: #1f6feb; -fx-text-fill: #ffffff; -fx-background-radius: 16 16 4 16; -fx-font-size: 12;");
        } else {
            bubble.setStyle("-fx-background-color: #21262D; -fx-text-fill: #c9d1d9; -fx-background-radius: 16 16 16 4; -fx-border-color: #30363D; -fx-font-size: 12;");
        }
        HBox row = new HBox(bubble);
        row.setPadding(new Insets(2, 8, 2, 8));
        row.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return row;
    }

    private static List<HashMap<String, Object>> getContacts(User user) {
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


    private static String resolveUserLabel(int userId, HashMap<Integer, String> cache) {
        if (cache.containsKey(userId)) return cache.get(userId);
        String fallback = "User #" + userId;
        try {
            HashMap<String, Object> profile = new dbConnector().searchAccountDB(userId, "first_name, last_name");
            if (profile != null) {
                Object nameValue = profile.get("name");
                if (nameValue instanceof String name && !name.isBlank()) {
                    cache.put(userId, name.trim());
                    return name.trim();
                }
            }
        } catch (Exception ignored) {}
        cache.put(userId, fallback);
        return fallback;
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
