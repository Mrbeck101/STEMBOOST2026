package OtherComponents;

public class JobProgram {
    private final int employerID;
    private final int jobID;
    private boolean isAvailable;
    private int modRequired;
    private int assessmentRequired;
    private String preferredLearningPath;
    private String description;
    private String jobType;

    public JobProgram(int employerID, int jobID, boolean isAvailable, int modRequired, int assessmentRequired, String preferredLearningPath, String description, String jobType) {
        this.employerID = employerID;
        this.jobID = jobID;
        this.isAvailable = isAvailable;
        this.modRequired = modRequired;
        this.assessmentRequired = assessmentRequired;
        this.preferredLearningPath = preferredLearningPath;
        this.description = description;
        this.jobType = jobType;
    }

    public int getJobID() {
        return jobID;
    }

    public int getEmployerID() {
        return employerID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public int getModRequired() {
        return modRequired;
    }

    public void setModRequired(int modRequired) {
        this.modRequired = modRequired;
    }

    public int getAssessmentRequired() {
        return assessmentRequired;
    }

    public void setAssessmentRequired(int assessmentRequired) {
        this.assessmentRequired = assessmentRequired;
    }

    public String getPreferredLearningPath() {
        return preferredLearningPath;
    }

    public void setPreferredLearningPath(String preferredLearningPath) {
        this.preferredLearningPath = preferredLearningPath;
    }
}
