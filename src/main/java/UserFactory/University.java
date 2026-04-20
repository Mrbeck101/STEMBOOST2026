package UserFactory;

import OtherComponents.Assessment;
import OtherComponents.StudentReportCard;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.List;

public class University extends User {

    private String universityName;
    private List<Integer> enrolledStudents;

    public University(int id) {
        super(id, "University");
        initializeUser();
    }

    @Override
    protected void initializeUser() {
        HashMap<String, Object> profile = DB.searchAccountDB(super.getId(), "first_name, last_name, contact_info, university");
        super.setName((String) profile.get("name"));
        this.universityName = (String) profile.get("university");

        JsonObject contactJson = JsonParser.parseString((String) profile.get("contactInfo")).getAsJsonObject();
        super.setEmail(contactJson.get("email").getAsString());
        super.setPhone(contactJson.get("phone").getAsString());
        super.setAddress(contactJson.get("address").getAsString());

        this.enrolledStudents = DB.searchEnrolledStudentsDB(super.getId());
    }

    @Override
    public List<Assessment> getAssessmentResults(int... id) {
        return DB.searchAssessmentDB(id[0], super.getAcctType());
    }

    public String getUniversityName() {
        return this.universityName;
    }


    public List<Integer> getEnrolledStudents() {
        return this.enrolledStudents;
    }

    public List<StudentReportCard> getStudentReportCards() {
        return buildStudentReportCards(this.enrolledStudents);
    }



}
