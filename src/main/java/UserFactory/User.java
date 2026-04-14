package UserFactory;

import DatabaseController.dbConnector;
import OtherComponents.Assessment;
import OtherComponents.InboxHandler;
import OtherComponents.Message;

import java.util.List;

abstract class User {
    private int id;
    private String name;

    private enum acctType {
        Student,
        Educator,
        Employer,
        Parent,
        University,
        Counselor
    };

    private String contactInfo;
    private dbConnector DB = new dbConnector();
    private InboxHandler inbox = new InboxHandler(this.id, DB);


    public abstract List<Assessment> getAssessmentResults();

    public abstract List<Message> checkInbox();

    public abstract boolean sendMessage(int receiverID, String subject, String content);
    //TODO: Add delMessage method

    public abstract void setContactInfo(String contact);
    public abstract void getContactInfo();
}
