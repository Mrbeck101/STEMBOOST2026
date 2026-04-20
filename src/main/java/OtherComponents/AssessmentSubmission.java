package OtherComponents;

public class AssessmentSubmission {
    private final int studentId;
    private final String studentName;
    private final int assessmentId;
    private final int moduleId;
    private final String learningPath;
    private final String moduleSubject;
    private final int grade;
    private final boolean completed;
    private final String submissionContent;

    public AssessmentSubmission(int studentId, String studentName, int assessmentId, int moduleId, String learningPath,
                                String moduleSubject, int grade, boolean completed, String submissionContent) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.assessmentId = assessmentId;
        this.moduleId = moduleId;
        this.learningPath = learningPath;
        this.moduleSubject = moduleSubject;
        this.grade = grade;
        this.completed = completed;
        this.submissionContent = submissionContent;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public int getAssessmentId() {
        return assessmentId;
    }

    public int getModuleId() {
        return moduleId;
    }

    public String getLearningPath() {
        return learningPath;
    }

    public int getGrade() {
        return grade;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getModuleSubject() {
        return moduleSubject;
    }

    public String getSubmissionContent() {
        return submissionContent;
    }
}

