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
        this.progress = Math.max(0, Math.min(100, progress));
        this.educatorID = educatorID <= 0 ? 1 : educatorID;
        String safePath = learningPath == null ? "" : learningPath.trim();
        this.learningPath = safePath.isEmpty() ? "Unknown" : safePath;
        this.content = content == null ? "" : content;
        String safeSubject = subject == null ? "" : subject.trim();
        this.subject = safeSubject.isEmpty() ? "Untitled Module" : safeSubject;
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
        this.progress = Math.max(0, Math.min(100, progress));
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
        this.content = content == null ? "" : content;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        String safeSubject = subject == null ? "" : subject.trim();
        this.subject = safeSubject.isEmpty() ? "Untitled Module" : safeSubject;
    }
}
