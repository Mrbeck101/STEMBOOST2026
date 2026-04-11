package Services;
import DatabaseController.dbConnector;

public class AuthService {

    public static boolean login(String email, String password) {
            try  {
                dbConnector DB = new dbConnector();
                String dbPassword = DB.searchUserDB(email, "password");
                return dbPassword.equals(password);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
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