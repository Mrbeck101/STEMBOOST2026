package OtherComponents;

public class Message {
    private int msgID = -1;
    private int convoID = -1;
    private int senderID;
    private int receiverID;
    private String content;

    public Message(int senderID, int receiverID, String content) {
        int safeSender = senderID <= 0 ? 1 : senderID;
        int safeReceiver = receiverID <= 0 ? 1 : receiverID;
        if (safeSender == safeReceiver) {
            safeReceiver = safeSender + 1;
        }

        this.senderID = safeSender;
        this.receiverID = safeReceiver;
        if (content == null) {
            this.content = null;
        } else {
            String clean = content.trim();
            this.content = clean.isEmpty() ? "No content provided" : clean;
        }
    }

    public void setConvoID(int id) {
        this.convoID = id;
    }

    public void setMsgID(int id) {
        this.msgID = id;
    }

    public int getConvoID() {
        return this.convoID;
    }

    public int getSenderID() {
        return this.senderID;
    }

    public int getReceiverID() {
        return this.receiverID;
    }


    public String getContent() {
        return this.content;
    }

}
