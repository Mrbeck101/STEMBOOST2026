package UserFactory;

import OtherComponents.Assessment;
import OtherComponents.LearningModule;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.List;

public class Educator extends User {

    private final List<LearningModule> modules;


    public Educator(int id) {
        super(id, "Educator");

        HashMap<String,Object> profile = DB.searchAccountDB(id, "first_name, last_name, contact_info");
        super.setName((String) profile.get("name"));


        JsonObject contactJson = JsonParser.parseString((String) profile.get("contactInfo")).getAsJsonObject();
        super.setEmail(contactJson.get("email").getAsString());
        super.setPhone(contactJson.get("phone").getAsString());
        super.setAddress(contactJson.get("address").getAsString());

        this.modules = DB.searchModulesDB(id, super.getAcctType());


    }



    public List<Assessment> getAssessmentResults(int... id) {
        return DB.searchAssessmentDB(id[0], super.getAcctType());
    };

    public List<LearningModule> getLearningModules() {
        return this.modules;
    }

    public void addAssessment(Assessment test) {

    }

    public void addModule(LearningModule mod) {
        this.modules.add(mod);
    }

}
