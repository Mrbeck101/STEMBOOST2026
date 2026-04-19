package UserFactory;

import OtherComponents.Assessment;
import OtherComponents.LearningModule;
import com.google.gson.*;
import java.util.HashMap;
import java.util.List;

public class Student extends User {
    private String university;
    private List<Assessment> assessments;
    private List<LearningModule> modules;
    private int guardian = -1;
    private String learningPath;
    private int counselorID;




    public Student(int id) {
        super(id, "Student");
        initializeUser();
    }

     protected void initializeUser() {
        HashMap<String,Object> profile = DB.searchAccountDB(super.getId(), "first_name, last_name, contact_info, associated_id, university, learning_path, assigned_counselor");
        super.setName((String) profile.get("name"));
        this.university = (String) profile.get("university");

        if ((Integer) profile.get("associatedID") != 0) {
            this.guardian = (Integer) profile.get("associatedID");
        }

        if (profile.get("learningPath") != null) {
            this.learningPath = (String) profile.get("learningPath");
        } else {
            this.learningPath = "Unknown";
        }

        if ((Integer) profile.get("counselorID") != 0) {
            this.counselorID = (Integer) profile.get("counselorID");
        } else {
            setCounselorID();
        }


        JsonObject contactJson = JsonParser.parseString((String) profile.get("contactInfo")).getAsJsonObject();
        super.setEmail(contactJson.get("email").getAsString());
        super.setPhone(contactJson.get("phone").getAsString());
        super.setAddress(contactJson.get("address").getAsString());

        this.modules = DB.searchModulesDB(super.getId(), super.getAcctType());
        this.assessments = DB.searchAssessmentDB(super.getId(), super.getAcctType());

    }

    public List<Assessment> getAssessmentResults(int... id) {
        return this.assessments;
    };

    public List<LearningModule> getLearningModules() {
        return this.modules;
    }

    public void addAssessment(Assessment test) {
        this.assessments.add(test);
    }

    public void addModule(LearningModule mod) {
        this.modules.add(mod);
    }

    public int getGuardian() {
        return this.guardian;
    }

    public void setGuardian(int guardian) {
        this.guardian = guardian;
    }

    public String getUniversity() {
        return this.university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getLearningPath() {
        return this.learningPath;
    }

    public void setLearningPath(String learningPath) {
        this.learningPath = learningPath;
    }

    private void setCounselorID() {
        Integer counselorID = DB.findAvailableCounselor();
        if (DB.assignStudent(super.getId(), counselorID)) {
            this.counselorID = counselorID;
            System.out.println("Successfully assigned to a counselor");
        };
    }
    //TODO write request program function
    public void requestWorkProgram(){}
}
