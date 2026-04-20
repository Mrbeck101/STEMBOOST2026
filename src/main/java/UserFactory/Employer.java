package UserFactory;

import OtherComponents.Assessment;
import OtherComponents.JobProgram;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.List;

public class Employer extends User {

    private List<JobProgram> jobPrograms;
    private String company;

    public Employer(int id) {
        super(id, "Employer");
        initializeUser();
    }

    @Override
    protected void initializeUser() {
        HashMap<String, Object> profile = DB.searchAccountDB(super.getId(), "first_name, last_name, contact_info, company");
        super.setName((String) profile.get("name"));
        this.company = profile.get("company") == null ? "" : (String) profile.get("company");

        JsonObject contactJson = JsonParser.parseString((String) profile.get("contactInfo")).getAsJsonObject();
        super.setEmail(contactJson.get("email").getAsString());
        super.setPhone(contactJson.get("phone").getAsString());
        super.setAddress(contactJson.get("address").getAsString());

        this.jobPrograms = DB.searchJobProgramsDB(super.getId());
    }

    @Override
    public List<Assessment> getAssessmentResults(int... id) {
        return DB.searchAssessmentDB(id[0], super.getAcctType());
    }

    public List<JobProgram> getJobPrograms() {
        return this.jobPrograms;
    }

    public void addJobProgram(JobProgram program) {
        this.jobPrograms.add(program);
    }

    public boolean saveJobProgram(JobProgram program) {
        boolean saved = DB.addJobProgram(program);
        if (saved) {
            this.jobPrograms = DB.searchJobProgramsDB(super.getId());
        }
        return saved;
    }

    public boolean updateJobProgram(JobProgram program) {
        boolean updated = DB.updateJobProgram(program);
        if (updated) {
            this.jobPrograms = DB.searchJobProgramsDB(super.getId());
        }
        return updated;
    }

    public boolean deleteJobProgram(int jobId) {
        boolean deleted = DB.deleteJobProgramById(jobId);
        if (deleted) {
            this.jobPrograms = DB.searchJobProgramsDB(super.getId());
        }
        return deleted;
    }

    public List<Assessment> getAssessmentsForModule(int moduleId) {
        return DB.searchAssessmentDB(moduleId, "Educator");
    }

    public String getCompany() {
        return this.company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

}
