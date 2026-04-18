package OtherComponents;

public class Assessment {
    private final int assessmentID;
    private final int moduleID;
    private int grade;
    private String content;
    private final String learningPath;


    public Assessment(int assessmentID, int moduleID, int grade, String lp, String content) {
        this.assessmentID = assessmentID;
        this.moduleID = moduleID;
        this.learningPath = lp;
        this.grade = grade;
        this.content = content;
    }

    public int getAssessmentID() {
        return this.assessmentID;
    }
    public String getLearningPath() {
        return this.learningPath;
    }

    public int getGrade() {
        return this.grade;
    }

    public int getModuleID() {
        return this.moduleID;
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
