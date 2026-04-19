package Services;

import DatabaseController.dbConnector;
import OtherComponents.LearningModule;

import java.util.HashMap;
import java.util.List;

public class FetchProfileService {
    private final dbConnector db = new dbConnector();

    public List<HashMap<String, Object>> getContactsByRole(int userId, String acctType) {
        return switch (acctType) {
            case "Student" -> db.getStudentContacts(userId);
            case "Parent" -> db.getParentContacts(userId);
            case "Counselor" -> db.getCounselorContacts(userId);
            case "Employer" -> db.getEmployerContacts(userId);
            case "University" -> db.getUniversityContacts(userId);
            case "Educator" -> db.getEducatorContacts(userId);
            default -> List.of();
        };
    }

    public List<LearningModule> browseModules(String learningPath) {
        if (learningPath == null || learningPath.isBlank() || "All".equalsIgnoreCase(learningPath)) {
            return db.searchAllModulesDB();
        }
        return db.searchModulesByLearningPathDB(learningPath);
    }

    public List<HashMap<String, Object>> searchJobPrograms(String learningPath, String keyword) {
        return db.searchAllJobProgramsDB(learningPath, keyword);
    }

    public boolean updateContactInfo(int userId, String email, String phone, String address) {
        return db.updateContactInfo(userId, email, phone, address);
    }

    public boolean requestLearningModule(int studentId, String requestedPath, String details) {
        return db.notifyCounselorOfModuleRequest(studentId, requestedPath, details);
    }

    public boolean requestJobProgram(int studentId, int jobId, String details) {
        return db.notifyCounselorOfJobProgramRequest(studentId, jobId, details);
    }

    public List<HashMap<String, Object>> listUsers() {
        return db.listAllUsersSummary();
    }

    public List<HashMap<String, Object>> listModules() {
        return db.listAllModulesSummary();
    }

    public List<HashMap<String, Object>> listJobPrograms() {
        return db.listAllJobProgramsSummary();
    }

    public boolean deleteUser(int userId) {
        return db.deleteUserCascade(userId);
    }

    public boolean deleteModule(int moduleId) {
        return db.deleteModuleById(moduleId);
    }

    public boolean deleteJobProgram(int jobId) {
        return db.deleteJobProgramById(jobId);
    }
}
