package UI;

import OtherComponents.Assessment;
import Services.UIRefreshService;
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

    public void goToDashboard(int id, String accountType) {
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
        var currentUser = UserContext.getInstance().getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }
        goToDashboard(currentUser.getId(), currentUser.getAcctType());
    }

    public void goToModules() {
        stage.setScene(ModuleView.create(this));
    }

    public void goToAssessments() {
        stage.setScene(AssessmentView.create(this));
    }

    public void goToInbox() {
        stage.setScene(InboxView.create(this, -1));
    }

    public void goToInboxWithContact(int contactId) {
        stage.setTitle("STEMBOOST - Inbox");
        stage.setScene(InboxView.create(this, contactId));
    }

    public void goToTakeAssessment(Assessment assessment) {
        stage.setTitle("STEMBOOST - Take Assessment");
        stage.setScene(TakeAssessmentView.create(this, assessment));
    }

    public void goToLearningPathSelection() {
        stage.setTitle("STEMBOOST - Learning Path Selection");
        stage.setScene(LearningPathSelectionView.create(this));
    }

    public void goToContacts() {
        stage.setTitle("STEMBOOST - Contacts");
        stage.setScene(ContactsView.create(this));
    }

    public void goToProfile() {
        stage.setTitle("STEMBOOST - Profile");
        stage.setScene(ProfileView.create(this));
    }

    public void goToStudentProfile(int studentId) {
        stage.setTitle("STEMBOOST - Student Profile");
        stage.setScene(ProfileView.createReadOnly(this, studentId));
    }
}
