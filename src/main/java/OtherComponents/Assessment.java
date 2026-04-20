package OtherComponents;

public class Assessment {
    private final int assessmentID;
    private final int moduleID;
    private int grade;
    private String content;
    private final String learningPath;
    private final String moduleSubject;
    private final boolean completed;


    public Assessment(int assessmentID, int moduleID, int grade, String lp, String moduleSubject, String content, boolean completed) {
        this.assessmentID = assessmentID;
        this.moduleID = moduleID;
        this.learningPath = lp;
        this.moduleSubject = moduleSubject;
        this.grade = grade;
        this.content = content;
        this.completed = completed;
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
        this.grade = grade;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
