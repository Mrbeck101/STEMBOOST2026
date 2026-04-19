package UserFactory;

import OtherComponents.Assessment;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.List;

public class Admin extends User {

    public Admin(int id) {
        super(id, "Admin");
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
    }

    @Override
    public List<Assessment> getAssessmentResults(int... id) {
        return List.of();
    }

    public List<HashMap<String, Object>> getAllUsers() {
        return DB.listAllUsersSummary();
    }

    public List<HashMap<String, Object>> getAllModules() {
        return DB.listAllModulesSummary();
    }

    public List<HashMap<String, Object>> getAllJobPrograms() {
        return DB.listAllJobProgramsSummary();
    }

    public boolean deleteUser(int userId) {
        return DB.deleteUserCascade(userId);
    }

    public boolean deleteModule(int moduleId) {
        return DB.deleteModuleById(moduleId);
    }

    public boolean deleteJobProgram(int jobId) {
        return DB.deleteJobProgramById(jobId);
    }
}

