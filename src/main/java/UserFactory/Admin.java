package UserFactory;

import OtherComponents.Assessment;
import OtherComponents.ValidationException;
import Services.AuthService;
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

    public boolean createUser(String email, String password, String fname, String lname, String acctType,
                              String company, Integer associatedStudentId, String university) {
        if (acctType == null || acctType.isBlank()) {
            throw new ValidationException("Account type is required");
        }
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required");
        }
        if (fname == null || fname.isBlank() || lname == null || lname.isBlank()) {
            throw new ValidationException("First and last name are required");
        }
        if ("Employer".equals(acctType) && (company == null || company.isBlank())) {
            throw new ValidationException("Company name is required for employer accounts");
        }
        if (("Student".equals(acctType) || "University".equals(acctType)) && (university == null || university.isBlank())) {
            throw new ValidationException("University name is required for " + acctType + " accounts");
        }
        if ("Parent".equals(acctType)) {
            if (associatedStudentId == null) {
                throw new ValidationException("Associated student ID is required for parent accounts");
            }
            HashMap<String, Object> student = DB.searchAccountDB(associatedStudentId, "first_name, last_name, acct_type");
            if (student == null || !"Student".equals(student.get("acctType"))) {
                throw new ValidationException("Associated student ID must belong to an existing student");
            }
        }

        return AuthService.register(
                email.trim(),
                password,
                fname.trim(),
                lname.trim(),
                acctType,
                company == null ? "" : company.trim(),
                associatedStudentId,
                university == null ? null : university.trim()
        );
    }
}

