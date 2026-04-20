package Services;

import DatabaseController.dbConnector;
import OtherComponents.Assessment;
import OtherComponents.LearningModule;
import OtherComponents.StudentReportCard;

import java.util.ArrayList;
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

    /** Returns a summary map for any user: keys name, acctType, contactInfo, learningPath. */
    public HashMap<String, Object> getAccountSummary(int userId) {
        return db.searchAccountDB(userId, "first_name, last_name, acct_type, contact_info, learning_path");
    }

    public List<StudentReportCard> getUniversityReportCards(int universityId) {
        return buildReportCards(db.searchEnrolledStudentsDB(universityId));
    }

    public List<StudentReportCard> getParentReportCards(int parentId) {
        return buildReportCards(db.searchGuardedStudentsDB(parentId));
    }

    private List<StudentReportCard> buildReportCards(List<Integer> studentIds) {
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

    private StudentReportCard buildStudentReportCard(int studentId) {
        HashMap<String, Object> account = db.searchAccountDB(studentId, "first_name, last_name, learning_path");
        List<LearningModule> modules = db.searchModulesDB(studentId, "Student");
        List<Assessment> assessments = db.searchAssessmentDB(studentId, "Student");

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
