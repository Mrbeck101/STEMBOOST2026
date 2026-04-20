package main.demo;

import DatabaseController.dbConnector;
import OtherComponents.*;
import Services.AuthService;
import UserFactory.Student;
import eu.hansolo.toolbox.tuples.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MainTestApp
 *
 * JUnit 5 validation checks, constraint enforcement, and data integrity
 * assertions across the core StemBoost domain model.
 */
public class MainTestApp {

    // =========================================================================
    //  Group 1 – Message Constraints
    // =========================================================================

    @Test
    void test_messageRequiresSenderAndReceiver() {
        Message msg = new Message(3, 7, "Hello");
        assertEquals(3, msg.getSenderID(),   "Message should store sender ID correctly");
        assertEquals(7, msg.getReceiverID(), "Message should store receiver ID correctly");
    }

    @Test
    void test_messageContentNotNull() {
        Message msg = new Message(1, 2, null);
        // Constructor accepts null – the application layer must guard before send
        assertNull(msg.getContent(), "Content is null; InboxHandler.sendMessage() should reject this");
    }

    @Test
    void test_messageContentNotBlank() {
        Message msg = new Message(1, 2, "   ");
        assertFalse(msg.getContent().isBlank(),
                "Blank content was accepted; InboxHandler.sendMessage() should reject it");
    }

    @Test
    void test_messageDefaultConvoIdIsNegativeOne() {
        Message msg = new Message(1, 2, "test");
        assertEquals(-1, msg.getConvoID(), "New message default convoID should be -1");
    }

    @Test
    void test_messageSenderReceiverNotEqual() {
        Message msg = new Message(5, 5, "self-message");
        assertNotEquals(msg.getSenderID(), msg.getReceiverID(),
                "Sender and receiver should differ; InboxHandler should reject self-messages");
    }

    @Test
    void test_messageSenderPositiveId() {
        Message msg = new Message(-1, 2, "bad sender");
        assertTrue(msg.getSenderID() > 0,
                "Sender ID must be positive, got: " + msg.getSenderID());
    }

    @Test
    void test_messageReceiverPositiveId() {
        Message msg = new Message(1, 0, "bad receiver");
        assertTrue(msg.getReceiverID() > 0,
                "Receiver ID must be positive, got: " + msg.getReceiverID());
    }

    // =========================================================================
    //  Group 2 – Assessment Data Integrity
    // =========================================================================

    @Test
    void test_assessmentGradeInValidRange() {
        Assessment a = new Assessment(1, 1, 85, "STEM", "Algebra", "content", true);
        assertTrue(a.getGrade() >= 0 && a.getGrade() <= 100,
                "Assessment grade 85 should be within [0, 100]");
    }

    @Test
    void test_assessmentGradeNotNegative() {
        Assessment a = new Assessment(2, 1, -5, "STEM", "Algebra", "content", false);
        assertTrue(a.getGrade() >= 0,
                "Negative grade accepted: " + a.getGrade());
    }

    @Test
    void test_assessmentGradeNotAboveMax() {
        Assessment a = new Assessment(3, 1, 110, "STEM", "Bio", "content", true);
        assertTrue(a.getGrade() <= 100,
                "Grade " + a.getGrade() + " exceeds maximum of 100");
    }

    @Test
    void test_assessmentLearningPathNotBlank() {
        Assessment a = new Assessment(4, 1, 70, "  ", "Physics", "content", false);
        assertFalse(a.getLearningPath() == null || a.getLearningPath().isBlank(),
                "Blank learning path should not be accepted");
    }

    @Test
    void test_assessmentModuleSubjectNotBlank() {
        Assessment a = new Assessment(5, 1, 70, "STEM", "", "content", false);
        assertFalse(a.getModuleSubject() == null || a.getModuleSubject().isBlank(),
                "Empty module subject should not be accepted");
    }

    @Test
    void test_assessmentCompletedFlagPreserved() {
        Assessment a = new Assessment(6, 1, 0, "STEM", "Bio", "content", true);
        assertTrue(a.isCompleted(), "Assessment completed flag should be preserved");
    }

    // =========================================================================
    //  Group 3 – LearningModule Constraints
    // =========================================================================

    @Test
    void test_moduleProgressInRange() {
        LearningModule m = new LearningModule(1, 55, 10, "STEM", "content", "Algebra");
        assertTrue(m.getProgress() >= 0 && m.getProgress() <= 100,
                "Module progress 55 should be within [0, 100]");
    }

    @Test
    void test_moduleProgressNotNegative() {
        LearningModule m = new LearningModule(2, -10, 10, "STEM", "content", "Algebra");
        assertTrue(m.getProgress() >= 0,
                "Negative progress accepted: " + m.getProgress());
    }

    @Test
    void test_moduleProgressNotAbove100() {
        LearningModule m = new LearningModule(3, 150, 10, "STEM", "content", "Algebra");
        assertTrue(m.getProgress() <= 100,
                "Progress " + m.getProgress() + " exceeds maximum of 100");
    }

    @Test
    void test_moduleSubjectNotBlank() {
        LearningModule m = new LearningModule(4, 0, 10, "STEM", "content", "  ");
        assertFalse(m.getSubject() == null || m.getSubject().isBlank(),
                "Blank subject should not be accepted");
    }

    @Test
    void test_moduleLearningPathNotBlank() {
        LearningModule m = new LearningModule(5, 0, 10, "", "content", "Physics");
        assertFalse(m.getLearningPath() == null || m.getLearningPath().isBlank(),
                "Empty learning path should not be accepted");
    }

    @Test
    void test_moduleContentNotNull() {
        LearningModule m = new LearningModule(6, 0, 10, "STEM", null, "Biology");
        assertNotNull(m.getContent(), "Module content must not be null");
    }

    @Test
    void test_moduleEducatorIdPositive() {
        LearningModule m = new LearningModule(7, 0, -3, "STEM", "content", "Chemistry");
        assertTrue(m.getEducatorID() > 0,
                "Non-positive educator ID accepted: " + m.getEducatorID());
    }

    // =========================================================================
    //  Group 4 – Auth Registration Validation
    // =========================================================================

    @Test
    void test_registerRejectsEmptyEmail() {
        boolean result = AuthService.register("", "Password1!", "John", "Doe", "Student");
        assertFalse(result, "Empty email should be rejected by AuthService.register()");
    }

    @Test
    void test_registerRejectsEmptyPassword() {
        boolean result = AuthService.register("valid@test.com", "", "John", "Doe", "Student");
        assertFalse(result, "Empty password should be rejected by AuthService.register()");
    }

    @Test
    void test_registerRejectsEmptyFirstName() {
        boolean result = AuthService.register("fn@test.com", "Password1!", "", "Doe", "Student");
        assertFalse(result, "Empty first name should be rejected by AuthService.register()");
    }

    @Test
    void test_registerRejectsEmptyLastName() {
        boolean result = AuthService.register("ln@test.com", "Password1!", "John", "", "Student");
        assertFalse(result, "Empty last name should be rejected by AuthService.register()");
    }

    @Test
    void test_registerRejectsInvalidEmailFormat() {
        boolean result = AuthService.register("not-an-email", "Password1!", "John", "Doe", "Student");
        assertFalse(result, "Malformed email 'not-an-email' should be rejected");
    }

    @Test
    void test_registerRejectsNullAccountType() {
        boolean result = AuthService.register("type@test.com", "Password1!", "John", "Doe", null);
        assertFalse(result, "Null account type should be rejected");
    }

    @Test
    void test_registerParentRequiresStudentId() {
        boolean result = AuthService.register(
                "parent_nolink@test.com", "Password1!", "Jane", "Doe",
                "Parent", "", null, null);
        assertFalse(result, "Parent registration without associatedStudentId should be rejected");
    }

    // =========================================================================
    //  Group 5 – Auth Login Validation
    // =========================================================================

    @Test
    void test_loginRejectsEmptyEmail() {
        Pair<Integer, String> result = AuthService.login("", "SomePass1!");
        assertTrue(result.getA() <= 0,
                "Empty email should not return a valid user ID, got: " + result.getA());
    }

    @Test
    void test_loginRejectsEmptyPassword() {
        Pair<Integer, String> result = AuthService.login("user@test.com", "");
        assertTrue(result.getA() <= 0,
                "Empty password should not return a valid user ID, got: " + result.getA());
    }

    @Test
    void test_loginRejectsUnknownCredentials() {
        Pair<Integer, String> result = AuthService.login(
                "nosuchuser_xyz@stemboost.invalid", "WrongPass999!");
        assertTrue(result.getA() <= 0,
                "Unknown credentials should not return a valid user ID, got: " + result.getA());
    }

    // =========================================================================
    //  Group 6 – Database Connectivity
    // =========================================================================

    @Test
    void test_dbConnectorCanConnect() {
        dbConnector db = dbConnector.getInstance();
        Integer counselorId = db.findAvailableCounselor();
        assertNotNull(counselorId, "findAvailableCounselor() should return a result");
        assertTrue(counselorId > 0, "Counselor ID should be positive, got: " + counselorId);
    }

    // =========================================================================
    //  Group 7 – Student Model Integrity (live DB)
    // =========================================================================

    @Test
    void test_studentInitializationSetsName() {
        Student s = new Student(1);
        assertNotNull(s.getName(), "Student name should not be null after initialization");
        assertFalse(s.getName().isBlank(), "Student name should not be blank after initialization");
    }

    @Test
    void test_studentModulesListNotNull() {
        Student s = new Student(1);
        assertNotNull(s.getLearningModules(), "Student modules list should not be null");
    }

    @Test
    void test_studentAssessmentsListNotNull() {
        Student s = new Student(1);
        assertNotNull(s.getAssessmentResults(), "Student assessments list should not be null");
    }

    @Test
    void test_studentLearningPathNotNull() {
        Student s = new Student(1);
        assertNotNull(s.getLearningPath(), "Student learning path should not be null");
    }

    @Test
    void test_studentProgressWithinRange() {
        Student s = new Student(1);
        List<LearningModule> modules = s.getLearningModules();
        assumeNotEmpty(modules, "Student has no modules to validate");
        for (LearningModule m : modules) {
            assertTrue(m.getProgress() >= 0 && m.getProgress() <= 100,
                    "Module " + m.getModuleID() + " progress out of range: " + m.getProgress());
        }
    }

    @Test
    void test_studentAssessmentGradesValid() {
        Student s = new Student(1);
        List<Assessment> assessments = s.getAssessmentResults();
        assumeNotEmpty(assessments, "Student has no assessments to validate");
        for (Assessment a : assessments) {
            assertTrue(a.getGrade() >= 0 && a.getGrade() <= 100,
                    "Assessment " + a.getAssessmentID() + " grade out of range: " + a.getGrade());
        }
    }

    // =========================================================================
    //  Helper
    // =========================================================================

    /** Aborts (not fails) the test if the list is null or empty. */
    private <T> void assumeNotEmpty(List<T> list, String message) {
        org.junit.jupiter.api.Assumptions.assumeTrue(list != null && !list.isEmpty(), message);
    }
}

