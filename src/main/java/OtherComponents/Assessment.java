package OtherComponents;

public class Assessment {
    private final int studentId;
    private final String studentName;
    private final int assessmentID;
    private final int moduleID;
    private int grade;
    private String content;
    private final String learningPath;
    private final String moduleSubject;
    private final boolean completed;


    public Assessment(int assessmentID, int moduleID, int grade, String lp, String moduleSubject, String content, boolean completed) {
        this(-1, null, assessmentID, moduleID, grade, lp, moduleSubject, content, completed);
    }

    public Assessment(int studentId, String studentName, int assessmentID, int moduleID,
                      int grade, String lp, String moduleSubject, String content, boolean completed) {
        this.studentId = studentId;
        this.studentName = (studentName == null || studentName.isBlank()) ? null : studentName.trim();
        this.assessmentID = assessmentID;
        this.moduleID = moduleID;
        String safePath = lp == null ? "" : lp.trim();
        this.learningPath = safePath.isEmpty() ? "Unknown" : safePath;
        String safeSubject = moduleSubject == null ? "" : moduleSubject.trim();
        this.moduleSubject = safeSubject.isEmpty() ? "Unknown Module" : safeSubject;
        this.grade = Math.max(0, Math.min(100, grade));
        this.content = content == null ? "" : content;
        this.completed = completed;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public int getAssessmentID() {
        return this.assessmentID;
    }
    public String getLearningPath() {
        return this.learningPath;
    }

    public String getModuleSubject() {
        return this.moduleSubject;
    }

    public int getGrade() {
        return this.grade;
    }

    public int getModuleID() {
        return this.moduleID;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setGrade(int grade) {
        this.grade = Math.max(0, Math.min(100, grade));
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }
}
