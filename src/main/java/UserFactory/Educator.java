package UserFactory;

import OtherComponents.Assessment;
import OtherComponents.AuthorizationException;
import OtherComponents.LearningModule;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

public class Educator extends User {

    public List<LearningModule> modules;


    public Educator(int id) {
        super(id, "Educator");
        initializeUser();
    }

    @Override
    protected void initializeUser() {
        HashMap<String,Object> profile = DB.searchAccountDB(super.getId(), "first_name, last_name, contact_info");
        super.setName((String) profile.get("name"));


        JsonObject contactJson = JsonParser.parseString((String) profile.get("contactInfo")).getAsJsonObject();
        super.setEmail(contactJson.get("email").getAsString());
        super.setPhone(contactJson.get("phone").getAsString());
        super.setAddress(contactJson.get("address").getAsString());

        this.modules = DB.searchModulesDB(super.getId(), super.getAcctType());
    }



    public List<Assessment> getAssessmentResults(int... id) {
        if (id != null && id.length > 0) {
            return DB.searchAssessmentDB(id[0], super.getAcctType());
        }

        List<Assessment> allAssessments = new ArrayList<>();
        if (this.modules != null) {
            for (LearningModule module : this.modules) {
                allAssessments.addAll(DB.searchAssessmentDB(module.getModuleID(), super.getAcctType()));
            }
        }
        return allAssessments;
    }

    public List<LearningModule> getLearningModules() {
        return this.modules;
    }

    public void addAssessment(Assessment test) {
        boolean ownsModule = this.modules != null && this.modules.stream()
                .anyMatch(module -> module.getModuleID() == test.getModuleID());
        if (!ownsModule) {
            throw new AuthorizationException("You can only assign assessments to modules you created.");
        }
        DB.addAssessmentDB(test.getModuleID(), test.getLearningPath(), test.getContent());
    }

    public void addModule(LearningModule mod) {
        DB.addModuleDB(mod);
        this.modules.add(mod);
    }

    public boolean updateModule(LearningModule module) {
        return DB.updateModuleDB(module);
    }

    public boolean deleteModule(int moduleId) {
        return DB.deleteModuleById(moduleId);
    }

    public List<Assessment> getPendingAssessmentSubmissions() {
        return DB.getPendingAssessmentSubmissionsForEducator(super.getId());
    }

    public List<Assessment> getGradedAssessmentSubmissions() {
        return DB.getGradedAssessmentSubmissionsForEducator(super.getId());
    }

    public boolean gradeAssessmentSubmission(int studentId, int assessmentId, int grade) {
        return DB.gradeAssessmentSubmission(super.getId(), studentId, assessmentId, grade);
    }
}
