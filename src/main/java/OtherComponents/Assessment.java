package OtherComponents;

public class Assessment {
    private int moduleID;
    private int grade;
    private LearningPath lp;

    public Assessment(int moduleID, int grade, LearningPath lp) {
        this.moduleID = moduleID;
        this.grade = grade;
        this.lp = lp;
    }

    public LearningPath getLearningPath() {
        return this.lp;
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
}
