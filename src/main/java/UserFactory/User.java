package UserFactory;

import DatabaseController.dbConnector;
import OtherComponents.Assessment;
import OtherComponents.ValidationException;
import OtherComponents.ContactInfo;
import OtherComponents.InboxHandler;
import OtherComponents.Message;

import java.util.HashMap;
import java.util.List;

public abstract class User {
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
    protected abstract void initializeUser();


    public boolean sendMessage(int receiverID, String content) throws Exception {
        Message msg = new Message(id, receiverID, content);
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

    public boolean updateContactInformation(String email, String phone, String address) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
        boolean updated = DB.updateContactInfo(this.id, email, phone, address);
        if (updated) {
            setEmail(email);
            setPhone(phone);
            setAddress(address);
        }
        return updated;
    }

    public String getEmail() {
        return this.contactInfo.getContactInfo().get("email");
    }

    public String getPhone() {
        return this.contactInfo.getContactInfo().get("phone");
    }

    public String getAddress() {
        return this.contactInfo.getContactInfo().get("address");
    }



}
