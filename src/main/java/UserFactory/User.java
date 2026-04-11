package UserFactory;
import DatabaseController.dbConnector;
import OtherComponents.Assessment;
import org.w3c.dom.NameList;
import java.util.List;

public interface User {
    final dbConnector DB = new dbConnector();

    public List<Assessment> getAssessmentResults();
    

}
