package OtherComponents;

import DatabaseController.dbConnector;
import java.util.List;

public class InboxHandler {
    private final int inboxID;
    private List<Message> inbox;
    private final dbConnector DB;


    public InboxHandler(int id, dbConnector DB) {
        this.inboxID = id;
        this.DB = DB;
        getMessages();
    }

    private void getMessages() {
        this.inbox = DB.searchMessagesDB(this.inboxID);
    }

    public boolean sendMessage(Message message) throws Exception {
        return DB.addMessage(message);
    }

    //TODO: create remMessage function

    public boolean refreshInbox() {
        int msgCnt = this.inbox.size();
        getMessages();
        return this.inbox.size() > msgCnt;
    }

}
