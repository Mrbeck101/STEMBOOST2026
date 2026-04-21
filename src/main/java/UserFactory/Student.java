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
    private String learningPath;
    private int counselorID;




    public Student(int id) {
        super(id, "Student");
        initializeUser();
    }

     protected void initializeUser() {
        HashMap<String,Object> profile = DB.searchAccountDB(super.getId(), "first_name, last_name, contact_info, university, learning_path, assigned_counselor");
        super.setName((String) profile.get("name"));
        this.university = (String) profile.get("university");

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

    public String getUniversity() {
        return this.university;
    }

    public String getLearningPath() {
        return this.learningPath;
    }


    private void setCounselorID() {
        Integer counselorID = DB.findAvailableCounselor();
        if (DB.assignStudent(super.getId(), counselorID)) {
            this.counselorID = counselorID;
            System.out.println("Successfully assigned to a counselor");
        };
    }

    public boolean requestLearningModule(String learningPath, String details) {
        return DB.notifyCounselorOfModuleRequest(super.getId(), learningPath, details);
    }

    public boolean updateLearningPath(String learningPath) {
        boolean updated = DB.updateStudentLearningPath(super.getId(), learningPath);
        if (updated) {
            this.learningPath = learningPath;
        }
        return updated;
    }

    public Integer findEducatorForModule(int moduleId) {
        return DB.findEducatorForModule(moduleId);
    }

    public boolean updateModuleProgress(int moduleId, int progress) {
        boolean updated = DB.updateModuleProgress(super.getId(), moduleId, progress);
        if (updated && this.modules != null) {
            for (LearningModule module : this.modules) {
                if (module.getModuleID() == moduleId) {
                    module.setProgress(progress);
                    break;
                }
            }
        }
        return updated;
    }

    public boolean updateAssessmentCompletion(int assessmentId, boolean completed) {
        return updateAssessmentCompletion(assessmentId, completed, null);
    }

    public boolean updateAssessmentCompletion(int assessmentId, boolean completed, String submissionJson) {
        boolean updated = DB.updateAssessmentCompletion(super.getId(), assessmentId, completed, submissionJson);
        if (updated && this.assessments != null) {
            this.assessments = DB.searchAssessmentDB(super.getId(), super.getAcctType());
        }
        return updated;
    }

    public boolean requestWorkProgram(String details){
        return DB.notifyCounselorOfJobProgramRequest(super.getId(), details);
    }

    public boolean updateUniversity(String university) {
        boolean updated = DB.updateStudentUniversity(super.getId(), university);
        if (updated) {
            this.university = university;
        }
        return updated;
    }
}
