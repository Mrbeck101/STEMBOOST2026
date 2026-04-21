package UserFactory;

import DatabaseController.dbConnector;
import OtherComponents.Assessment;
import OtherComponents.ContactInfo;
import OtherComponents.InboxHandler;
import OtherComponents.LearningModule;
import OtherComponents.Message;
import OtherComponents.StudentReportCard;
import OtherComponents.ValidationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class User {
    private final int id;
    private String name;
    private final String acctType;
    private ContactInfo contactInfo = new ContactInfo("", "" , "");

    protected final dbConnector DB = dbConnector.getInstance();
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



    public List<Message> checkInbox() {
        return inboxHandler.getInbox();
    }

    public List<Message> getConversationMessages(int partnerId) {
        return DB.searchConversationMessages(this.id, partnerId);
    }

    public boolean markConversationAsRead(int partnerId) {
        return DB.markConversationMessagesAsRead(this.id, partnerId);
    }

    public int getUnreadMessageCount() {
        return DB.countUnreadMessages(this.id);
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

    public dbConnector getDbConnector() {
        return this.DB;
    }

    public List<HashMap<String, Object>> getAvailableContacts() {
        return DB.getContactsFromContactList(this.id);
    }

    /** Returns a summary map for any user: keys name, acctType, contactInfo, learningPath. */
    public HashMap<String, Object> getAccountSummary(int userId) {
        return DB.searchAccountDB(userId, "first_name, last_name, acct_type, contact_info, learning_path");
    }


    public List<LearningModule> browseModules(String learningPath) {
        if (learningPath == null || learningPath.isBlank() || "All".equalsIgnoreCase(learningPath)) {
            return DB.searchAllModulesDB();
        }
        return DB.searchModulesByLearningPathDB(learningPath);
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

    protected List<StudentReportCard> buildStudentReportCards(List<Integer> studentIds) {
        List<StudentReportCard> reports = new ArrayList<>();
        if (studentIds == null || studentIds.isEmpty()) {
            return reports;
        }

        for (Integer studentId : studentIds) {
            if (studentId != null) {
                reports.add(buildStudentReportCard(studentId));
            }
        }
        return reports;
    }

    protected StudentReportCard buildStudentReportCard(int studentId) {
        HashMap<String, Object> account = getAccountSummary(studentId);
        List<LearningModule> modules = DB.searchModulesDB(studentId, "Student");
        List<Assessment> assessments = DB.searchAssessmentDB(studentId, "Student");

        String studentName = account != null && account.get("name") != null
                ? (String) account.get("name")
                : "Student #" + studentId;
        String learningPath = account != null && account.get("learningPath") != null
                ? (String) account.get("learningPath")
                : "Not set";

        List<StudentReportCard.ModuleProgressSummary> moduleSummaries = new ArrayList<>();
        double averageModuleProgress = 0.0;
        if (modules != null && !modules.isEmpty()) {
            averageModuleProgress = modules.stream().mapToInt(LearningModule::getProgress).average().orElse(0.0);
            for (LearningModule module : modules) {
                moduleSummaries.add(new StudentReportCard.ModuleProgressSummary(
                        module.getModuleID(),
                        module.getSubject(),
                        module.getLearningPath(),
                        module.getProgress()
                ));
            }
        }

        int completedAssessments = 0;
        int totalAssessments = 0;
        double averageAssessmentGrade = -1.0;
        if (assessments != null && !assessments.isEmpty()) {
            totalAssessments = assessments.size();
            completedAssessments = (int) assessments.stream().filter(Assessment::isCompleted).count();
            averageAssessmentGrade = assessments.stream()
                    .filter(a -> a.getGrade() >= 0)
                    .mapToInt(Assessment::getGrade)
                    .average()
                    .orElse(-1.0);
        }

        return new StudentReportCard(
                studentId,
                studentName,
                learningPath,
                moduleSummaries,
                completedAssessments,
                totalAssessments,
                averageModuleProgress,
                averageAssessmentGrade
        );
    }



}
