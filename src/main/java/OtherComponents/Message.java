package OtherComponents;

public class Message {
    private int msgID = -1;
    private int convoID = -1;
    private int senderID;
    private int receiverID;
    private String subject;
    private String content;


    public Message(int senderID, int receiverID, String subject, String content) {
        this.senderID = senderID;
        this.receiverID = receiverID;
        this.subject = subject;
        this.content = content;
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

    public String getSubject() {
        return this.subject;
    }

    public String getContent() {
        return this.content;
    }

}
