package UI;

import OtherComponents.Assessment;
import Services.UIRefreshService;
import UserFactory.*;
import javafx.stage.Stage;

public class SceneRouter {

    private final Stage stage;

    public SceneRouter(Stage stage) {
        this.stage = stage;
    }

    public void goToLogin() {
        UIRefreshService.getInstance().stopPolling();
        UserContext.getInstance().logout();
        stage.setScene(LoginView.create(this));
    }

    public void goToRegister() {
        stage.setScene(RegisterView.create(this));
    }

    private void refreshCurrentUserFromDb() {
        User currentUser = UserContext.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        try {
            int userId = currentUser.getId();
            String accountType = currentUser.getAcctType();
            User refreshed = switch (accountType) {
                case "Student" -> new Student(userId);
                case "Educator" -> new Educator(userId);
                case "Counselor" -> new Counselor(userId);
                case "Parent" -> new Parent(userId);
                case "Employer" -> new Employer(userId);
                case "University" -> new University(userId);
                case "Admin" -> new Admin(userId);
                default -> null;
            };
            if (refreshed != null) {
                UserContext.getInstance().setCurrentUser(refreshed);
            }
        } catch (Exception ignored) {
            // Keep existing in-memory user if refresh fails transiently.
        }
    }

    public void goToDashboard(int id, String accountType) {
        refreshCurrentUserFromDb();
        stage.setTitle("STEMBOOST - " + accountType + " Dashboard");

        switch (accountType) {
            case "Student" -> stage.setScene(StudentDashBoardView.create(this));
            case "Educator" -> stage.setScene(EducatorDashBoardView.create(this));
            case "Counselor" -> stage.setScene(CounselorDashBoardView.create(this));
            case "Parent" -> stage.setScene(ParentDashBoardView.create(this));
            case "Employer" -> stage.setScene(EmployerDashBoardView.create(this));
            case "University" -> stage.setScene(UniversityDashboardView.create(this));
            case "Admin" -> stage.setScene(AdminDashboardView.create(this));
            default -> stage.setScene(LoginView.create(this));
        }
    }

    public void goToCurrentUserDashboard() {
        refreshCurrentUserFromDb();
        var currentUser = UserContext.getInstance().getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }
        goToDashboard(currentUser.getId(), currentUser.getAcctType());
    }

    public void goToModules() {
        refreshCurrentUserFromDb();
        stage.setScene(ModuleView.create(this));
    }

    public void goToAssessments() {
        refreshCurrentUserFromDb();
        stage.setScene(AssessmentView.create(this));
    }

    public void goToInbox() {
        refreshCurrentUserFromDb();
        stage.setScene(InboxView.create(this, -1));
    }

    public void goToInboxWithContact(int contactId) {
        refreshCurrentUserFromDb();
        stage.setTitle("STEMBOOST - Inbox");
        stage.setScene(InboxView.create(this, contactId));
    }

    public void goToTakeAssessment(Assessment assessment) {
        stage.setTitle("STEMBOOST - Take Assessment");
        stage.setScene(TakeAssessmentView.create(this, assessment));
    }

    public void goToLearningPathSelection() {
        refreshCurrentUserFromDb();
        stage.setTitle("STEMBOOST - Learning Path Selection");
        stage.setScene(LearningPathSelectionView.create(this));
    }

    public void goToContacts() {
        refreshCurrentUserFromDb();
        stage.setTitle("STEMBOOST - Contacts");
        stage.setScene(ContactsView.create(this));
    }

    public void goToProfile() {
        refreshCurrentUserFromDb();
        stage.setTitle("STEMBOOST - Profile");
        stage.setScene(ProfileView.create(this));
    }

    public void goToStudentProfile(int studentId) {
        stage.setTitle("STEMBOOST - Student Profile");
        stage.setScene(ProfileView.createReadOnly(this, studentId));
    }
}
