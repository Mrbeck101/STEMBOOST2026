package OtherComponents;

public class LearningModule {
    private int moduleID;
    private int progress;
    private final int educatorID;
    private final String learningPath;
    private String content;
    private String subject;

    public LearningModule(int moduleID, int progress, int educatorID, String learningPath, String content, String subject) {
        this.moduleID = moduleID;
        this.progress = progress;
        this.educatorID = educatorID;
        this.learningPath = learningPath;
        this.content = content;
        this.subject = subject;
    }

    public int getModuleID() {
        return moduleID;
    }

    public void setModuleID(int moduleID) {
        this.moduleID = moduleID;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getEducatorID() {
        return educatorID;
    }

    public String getLearningPath() {
        return learningPath;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
