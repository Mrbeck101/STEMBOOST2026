package Services;
import DatabaseController.dbConnector;
import eu.hansolo.toolbox.tuples.Pair;

public class AuthService {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    public static Pair<Integer, String> login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return new Pair<>(-1, "Not Found");
        }
        dbConnector DB = dbConnector.getInstance();
        Pair<Integer, String> result = DB.searchUserDB(email, password);
        if (result.getA() > 0) {
            DB.initializeContactListIfNull(result.getA());
        }
        return result;
    }

    public static boolean emailAlreadyRegistered(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        dbConnector DB = dbConnector.getInstance();
        return DB.userEmailExists(email.trim());
    }

    public static boolean register(String email, String password, String fname, String lname, String acctType) {
        return register(email, password, fname, lname, acctType, "");
    }

    public static boolean register(String email, String password, String fname, String lname, String acctType, String company) {
        return register(email, password, fname, lname, acctType, company, null, null);
    }

    public static boolean register(String email, String password, String fname, String lname, String acctType,
                                   String company, Integer associatedStudentId, String university) {
        if (email == null || email.isBlank() || !email.matches(EMAIL_PATTERN)) {
            return false;
        }
        if (password == null || password.isBlank()) {
            return false;
        }
        if (fname == null || fname.isBlank() || lname == null || lname.isBlank()) {
            return false;
        }
        if (acctType == null || acctType.isBlank()) {
            return false;
        }
        if ("Parent".equals(acctType) && (associatedStudentId == null || associatedStudentId <= 0)) {
            return false;
        }

        try  {
            dbConnector DB = dbConnector.getInstance();
            return DB.addUser(email.trim(), password, fname.trim(), lname.trim(), acctType,
                    company == null ? "" : company.trim(), associatedStudentId, university);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}