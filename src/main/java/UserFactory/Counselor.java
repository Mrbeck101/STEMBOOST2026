package UserFactory;

import OtherComponents.Assessment;
import OtherComponents.AuthorizationException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.List;

public class Counselor extends User {

    private List<Integer> assignedStudents;

    public Counselor(int id) {
        super(id, "Counselor");
        initializeUser();
    }

    @Override
    protected void initializeUser() {
        HashMap<String, Object> profile = DB.searchAccountDB(super.getId(), "first_name, last_name, contact_info");
        super.setName((String) profile.get("name"));

        JsonObject contactJson = JsonParser.parseString((String) profile.get("contactInfo")).getAsJsonObject();
        super.setEmail(contactJson.get("email").getAsString());
        super.setPhone(contactJson.get("phone").getAsString());
        super.setAddress(contactJson.get("address").getAsString());

        this.assignedStudents = DB.searchStudentsCounselorDB(super.getId());
    }

    @Override
    public List<Assessment> getAssessmentResults(int... id) {
        return DB.searchAssessmentDB(id[0], super.getAcctType());
    }

    public List<Integer> getAssignedStudents() {
        return this.assignedStudents;
    }

    public void addAssignedStudent(int studentID) {
        this.assignedStudents.add(studentID);
    }

    public boolean enrollAssignedStudentInModule(int studentId, int moduleId) {
        if (!this.assignedStudents.contains(studentId)) {
            throw new AuthorizationException("Counselor can only enroll assigned students");
        }
        return DB.addStudentToModule(studentId, moduleId);
    }

    public List<HashMap<String, Object>> searchJobPrograms(String learningPath, String keyword) {
        return DB.searchAllJobProgramsDB(learningPath, keyword);
    }

}
