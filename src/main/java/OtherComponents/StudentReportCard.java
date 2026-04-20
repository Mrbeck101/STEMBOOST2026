package OtherComponents;

import java.util.List;

public class StudentReportCard {
    private final int studentId;
    private final String studentName;
    private final String learningPath;
    private final List<ModuleProgressSummary> modules;
    private final int completedAssessments;
    private final int totalAssessments;
    private final double averageModuleProgress;
    private final double averageAssessmentGrade;

    public StudentReportCard(int studentId,
                             String studentName,
                             String learningPath,
                             List<ModuleProgressSummary> modules,
                             int completedAssessments,
                             int totalAssessments,
                             double averageModuleProgress,
                             double averageAssessmentGrade) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.learningPath = learningPath;
        this.modules = modules;
        this.completedAssessments = completedAssessments;
        this.totalAssessments = totalAssessments;
        this.averageModuleProgress = averageModuleProgress;
        this.averageAssessmentGrade = averageAssessmentGrade;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getLearningPath() {
        return learningPath;
    }

    public List<ModuleProgressSummary> getModules() {
        return modules;
    }

    public int getCompletedAssessments() {
        return completedAssessments;
    }

    public int getTotalAssessments() {
        return totalAssessments;
    }

    public double getAverageModuleProgress() {
        return averageModuleProgress;
    }

    public double getAverageAssessmentGrade() {
        return averageAssessmentGrade;
    }

    public double getAssessmentCompletionRate() {
        if (totalAssessments <= 0) {
            return 0.0;
        }
        return (completedAssessments * 100.0) / totalAssessments;
    }

    public static class ModuleProgressSummary {
        private final int moduleId;
        private final String subject;
        private final String learningPath;
        private final int progress;

        public ModuleProgressSummary(int moduleId, String subject, String learningPath, int progress) {
            this.moduleId = moduleId;
            this.subject = subject;
            this.learningPath = learningPath;
            this.progress = progress;
        }

        public int getModuleId() {
            return moduleId;
        }

        public String getSubject() {
            return subject;
        }

        public String getLearningPath() {
            return learningPath;
        }

        public int getProgress() {
            return progress;
        }
    }
}

