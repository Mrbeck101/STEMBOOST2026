package UserFactory;

import OtherComponents.Assessment;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.List;

public class Parent extends User {

    private List<Integer> guardedStudents;

    public Parent(int id) {
        super(id, "Parent");
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

        this.guardedStudents = DB.searchGuardedStudentsDB(super.getId());
    }

    @Override
    public List<Assessment> getAssessmentResults(int... id) {
        return DB.searchAssessmentDB(id[0], super.getAcctType());
    }

    public List<Integer> getGuardedStudents() {
        return this.guardedStudents;
    }

    public void addGuardedStudent(int studentID) {
        this.guardedStudents.add(studentID);
    }

}
