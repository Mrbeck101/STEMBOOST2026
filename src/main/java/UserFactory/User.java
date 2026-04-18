package UserFactory;

import DatabaseController.dbConnector;
import OtherComponents.Assessment;
import OtherComponents.ContactInfo;
import OtherComponents.InboxHandler;
import OtherComponents.Message;

import java.util.HashMap;
import java.util.List;

abstract class User {
    private final int id;
    private String name;
    private final String acctType;
    private ContactInfo contactInfo = new ContactInfo("", "" , "");

    protected final dbConnector DB = new dbConnector();
    private final InboxHandler inboxHandler;

    User (int id, String acctType) {
        this.id = id;
        this.acctType = acctType;
        this.inboxHandler = new InboxHandler(this.id, DB);
    }


    public abstract List<Assessment> getAssessmentResults(int... id);


    public boolean sendMessage(int receiverID, String subject, String content) throws Exception {
        Message msg = new Message(id, receiverID, subject, content);
        return inboxHandler.sendMessage(msg);
    }
    //TODO: Add delMessage method


    public List<Message> checkInbox() {
        return inboxHandler.getInbox();
    }

    public int getId() {
        return this.id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getAcctType() {
        return this.acctType;
    }

    public HashMap<String, String> getContactInfo() {
        return this.contactInfo.getContactInfo();
    }

    public void setEmail(String email) {
        this.contactInfo.setEmail(email);
    }

    public void setPhone(String phone) {
        this.contactInfo.setPhone(phone);
    }

    public void setAddress(String address) {
        this.contactInfo.setAddress(address);
    }



}
