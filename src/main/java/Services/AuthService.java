package Services;
import DatabaseController.dbConnector;
import eu.hansolo.toolbox.tuples.Pair;

public class AuthService {

    public static Pair<Integer, String> login(String email, String password) {
        dbConnector DB = new dbConnector();
        return DB.searchUserDB(email, password);
    }

    public static boolean register(String email, String password, String fname, String lname, String acctType) {
        try  {
            dbConnector DB = new dbConnector();
            return DB.addUser(email, password, fname, lname, acctType);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}