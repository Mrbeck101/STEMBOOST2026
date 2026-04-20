package UI;

import Services.KeyboardTtsService;
import Services.UIRefreshService;
import UserFactory.User;
import OtherComponents.Message;
import atlantafx.base.theme.PrimerDark;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.application.Application;
import java.util.ArrayList;
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
        final String[] activePartnerName = {""};
        final List<Message>[] activeConversation = new List[]{List.of()};
        final List<HBox>[] activeBubbles = new List[]{new ArrayList<>()};
        final int[] activeMessageIndex = {-1};
        final List<VBox>[] conversationItems = new List[]{new ArrayList<>()};
        final boolean[] isCurrentlySpeaking = {false}; // Track if TTS is active to prevent repeated announcements

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
                List<Message> updatedConversation = currentUser.getConversationMessages(activePartnerId[0]);
                int currentBubbleCount = chatBubblesBox.getChildren().size();
                if (currentBubbleCount > 0 && !(chatBubblesBox.getChildren().get(0) instanceof Label)) {
                    // Only show new messages beyond what's already displayed
                    if (updatedConversation.size() > currentBubbleCount) {
                        for (int i = currentBubbleCount; i < updatedConversation.size(); i++) {
                            Message msg = updatedConversation.get(i);
                            boolean isMe = msg.getSenderID() == currentUser.getId();
                            HBox bubble = createBubble(msg.getContent(), isMe);
                            chatBubblesBox.getChildren().add(bubble);
                            activeBubbles[0].add(bubble);
                        }
                        activeConversation[0] = updatedConversation;
                        activeMessageIndex[0] = updatedConversation.size() - 1;
                        highlightMessage(activeBubbles[0], activeMessageIndex[0], chatScrollPane, detailTitle, activePartnerName[0]);
                        // Only announce new messages if no user input is occurring and TTS is not currently speaking
                        if (!isCurrentlySpeaking[0]) {
                            announceConversationMessage(activeConversation[0], activeMessageIndex[0], activePartnerName[0]);
                            isCurrentlySpeaking[0] = true;
                        }
                        chatScrollPane.setVvalue(1.0);
                    }
                }
            }
        };
        UIRefreshService.getInstance().addListener(messageRefreshListener);

        java.util.function.BiConsumer<Integer, String> openConversation = (partnerId, partnerName) -> {
            activePartnerId[0] = partnerId;
            activePartnerName[0] = partnerName;
            detailTitle.setText("Chat with " + partnerName);

            try {
                currentUser.markConversationAsRead(partnerId);
            } catch (Exception ignored) {
            }

            chatBubblesBox.getChildren().clear();
            List<Message> conversation = currentUser.getConversationMessages(partnerId);
            activeConversation[0] = conversation;
            activeBubbles[0].clear();
            for (Message msg : conversation) {
                boolean isMe = msg.getSenderID() == currentUser.getId();
                HBox bubble = createBubble(msg.getContent(), isMe);
                chatBubblesBox.getChildren().add(bubble);
                activeBubbles[0].add(bubble);
            }
            if (conversation.isEmpty()) {
                Label empty = new Label("No messages yet. Send the first one!");
                empty.setStyle("-fx-text-fill: #8b949e; -fx-font-style: italic;");
                chatBubblesBox.getChildren().add(empty);
                activeMessageIndex[0] = -1;
                KeyboardTtsService.getInstance().speakNow("There are no messages in this conversation yet.", () -> isCurrentlySpeaking[0] = false);
                isCurrentlySpeaking[0] = true;
            } else {
                activeMessageIndex[0] = conversation.size() - 1;
                highlightMessage(activeBubbles[0], activeMessageIndex[0], chatScrollPane, detailTitle, partnerName);
                announceConversationMessage(activeConversation[0], activeMessageIndex[0], activePartnerName[0], () -> isCurrentlySpeaking[0] = false);
                isCurrentlySpeaking[0] = true;
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
                    HBox bubble = createBubble(text, true);
                    chatBubblesBox.getChildren().add(bubble);
                    activeBubbles[0].add(bubble);
                    List<Message> mutable = new ArrayList<>(activeConversation[0]);
                    mutable.add(new Message(currentUser.getId(), activePartnerId[0], text));
                    activeConversation[0] = mutable;
                    activeMessageIndex[0] = mutable.size() - 1;
                    highlightMessage(activeBubbles[0], activeMessageIndex[0], chatScrollPane, detailTitle, activePartnerName[0]);
                    announceConversationMessage(activeConversation[0], activeMessageIndex[0], activePartnerName[0], () -> isCurrentlySpeaking[0] = false);
                    isCurrentlySpeaking[0] = true;
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
                String partnerName = resolveUserLabel(currentUser, partnerId, partnerNameCache);
                VBox item = createConversationItem(partnerName, () -> openConversation.accept(partnerId, partnerName));
                item.focusedProperty().addListener((obs, oldValue, focused) -> {
                    if (focused) {
                        KeyboardTtsService.getInstance().speakNow(partnerName + ". Press enter to view conversation");
                    }
                });
                conversationItems[0].add(item);
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
                msgBtn.focusedProperty().addListener((obs, oldValue, focused) -> {
                    if (focused) {
                        KeyboardTtsService.getInstance().speakNow(resolvedName + ". Press enter to view conversation");
                    }
                });

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
            String preSelectedName = resolveUserLabel(currentUser, preSelectedContactId, partnerNameCache);
            openConversation.accept(preSelectedContactId, preSelectedName);
        }

        Scene scene = new Scene(root, 1400, 900);

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            boolean isStudent = currentUser != null && "Student".equals(currentUser.getAcctType());
            if (e.getCode() == KeyCode.ESCAPE) {
                router.goToCurrentUserDashboard();
                e.consume();
                return;
            }

            if (!isStudent || activeConversation[0].isEmpty()) {
                return;
            }

            boolean forward = e.getCode() == KeyCode.DOWN;
            boolean back = e.getCode() == KeyCode.UP;
            if (!forward && !back) {
                return;
            }

            if (forward) {
                activeMessageIndex[0] = Math.min(activeConversation[0].size() - 1, activeMessageIndex[0] + 1);
            } else {
                activeMessageIndex[0] = Math.max(0, activeMessageIndex[0] - 1);
            }

            highlightMessage(activeBubbles[0], activeMessageIndex[0], chatScrollPane, detailTitle, activePartnerName[0]);
            announceConversationMessage(activeConversation[0], activeMessageIndex[0], activePartnerName[0], () -> isCurrentlySpeaking[0] = false);
            isCurrentlySpeaking[0] = true;
            e.consume();
        });

        KeyboardTtsService.getInstance().bindScene(
                scene,
                KeyboardTtsService.AccessMode.STUDENT_ONLY,
                KeyboardTtsService.NavigationMode.NONE,
                () -> {
                    if (!(UserContext.getInstance().getCurrentUser() != null
                            && "Student".equals(UserContext.getInstance().getCurrentUser().getAcctType()))) {
                        return new KeyboardTtsService.ReadingContent("Inbox screen. Student text to speech controls are disabled for this role.");
                    }

                    List<Message> msgs = currentUser.checkInbox();
                    if (msgs == null || msgs.isEmpty()) {
                        return new KeyboardTtsService.ReadingContent(
                                "Student inbox. You currently have no messages."
                        );
                    }

                    return new KeyboardTtsService.ReadingContent(
                            "Student inbox. You have " + msgs.size() + " message" + (msgs.size() == 1 ? "" : "s") + "."
                    );
                },
                () -> {
                    isCurrentlySpeaking[0] = false;
                    if (activePartnerId[0] > 0 && !activeConversation[0].isEmpty()) {
                        announceConversationMessage(activeConversation[0], activeMessageIndex[0], activePartnerName[0], () -> isCurrentlySpeaking[0] = false);
                        isCurrentlySpeaking[0] = true;
                    } else if (!conversationItems[0].isEmpty()) {
                        conversationItems[0].get(0).requestFocus();
                    }
                }
        );

        return scene;
    }

    private static void highlightMessage(List<HBox> bubbles,
                                         int index,
                                         ScrollPane chatScrollPane,
                                         Label detailTitle,
                                         String partnerName) {
        if (bubbles == null || bubbles.isEmpty() || index < 0 || index >= bubbles.size()) {
            return;
        }

        for (int i = 0; i < bubbles.size(); i++) {
            HBox row = bubbles.get(i);
            if (i == index) {
                row.setStyle("-fx-background-color: rgba(88,166,255,0.25); -fx-background-radius: 8;");
            } else {
                row.setStyle("");
            }
        }

        if (bubbles.size() > 1) {
            chatScrollPane.setVvalue(index / (double) (bubbles.size() - 1));
        }
        detailTitle.setText("Chat with " + partnerName + " (message " + (index + 1) + "/" + bubbles.size() + ")");
    }

    private static VBox createConversationItem(String partnerName, Runnable onClick) {
        VBox item = new VBox(4);
        item.setPadding(new Insets(10));
        item.setStyle("-fx-background-color: #0D1117; -fx-border-radius: 4; -fx-cursor: hand;");
        item.setAccessibleRole(AccessibleRole.BUTTON);
        item.setFocusTraversable(true);

        Label nameLabel = new Label(partnerName);
        nameLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        item.setOnMouseClicked(e -> onClick.run());
        item.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                onClick.run();
                e.consume();
            }
        });
        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #161B22; -fx-border-radius: 4; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: #0D1117; -fx-border-radius: 4; -fx-cursor: hand;"));
        item.getChildren().add(nameLabel);
        return item;
    }

    private static void announceConversationMessage(List<Message> conversation, int index, String partnerName) {
        announceConversationMessage(conversation, index, partnerName, null);
    }

    private static void announceConversationMessage(List<Message> conversation, int index, String partnerName, Runnable onComplete) {
        if (conversation == null || conversation.isEmpty() || index < 0 || index >= conversation.size()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        Message message = conversation.get(index);
        String prefix = index == conversation.size() - 1
                ? "last message sent was: "
                : "Message from " + partnerName + ": ";
        KeyboardTtsService.getInstance().speakNow(prefix + message.getContent() + ". You can hear the other message by pressing up and down on your arrow keys", onComplete);
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
            return user.getAvailableContacts();
        } catch (Exception e) {
            System.err.println("Error retrieving contacts: " + e.getMessage());
            return null;
        }
    }


    private static String resolveUserLabel(User currentUser, int userId, HashMap<Integer, String> cache) {
        if (cache.containsKey(userId)) return cache.get(userId);
        String fallback = "User #" + userId;
        try {
            HashMap<String, Object> profile = currentUser.getAccountSummary(userId);
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
        UIComponents.showAlert(title, message);
    }
}
