package DatabaseController;

import OtherComponents.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.hansolo.toolbox.tuples.Pair;

import java.sql.*;
import java.util.*;

public class dbConnector {

    private static volatile dbConnector instance;

    private final String dbName = "stemboost_db";
    private String ip;
    private String user;
    private String password;
    private final Gson gson = new Gson();

    private dbConnector() {
        try {
            Properties props = DbPropertiesLoader.load();
            this.ip       = props.getProperty("db.ip");
            this.user     = props.getProperty("db.user");
            this.password = props.getProperty("db.password");
        } catch (Exception e) {
            System.out.println("Failed to fetch DB details");
            e.printStackTrace();
        }
    }

    public static dbConnector getInstance() {
        if (instance == null) {
            synchronized (dbConnector.class) {
                if (instance == null) {
                    instance = new dbConnector();
                }
            }
        }
        return instance;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(this.ip + this.dbName, this.user, this.password);
    }

    /** Maps the current ResultSet row to a contact HashMap. */
    private HashMap<String, Object> contactFromRow(ResultSet rs) throws SQLException {
        HashMap<String, Object> c = new HashMap<>();
        c.put("user_id",      rs.getInt("user_id"));
        c.put("name",         rs.getString("first_name") + " " + rs.getString("last_name"));
        c.put("acct_type",    rs.getString("acct_type"));
        c.put("contact_info", rs.getString("contact_info"));
        return c;
    }

    /** Maps the current ResultSet row to an Assessment used in educator submission queues. */
    private Assessment submissionFromRow(ResultSet rs) throws SQLException {
        int studentId = rs.getInt("student_id");
        String name = (rs.getString("first_name") + " " + rs.getString("last_name")).trim();
        if (name.isEmpty()) name = "Student #" + studentId;
        return new Assessment(
                studentId, name,
                rs.getInt("assessment_id"),
                rs.getInt("associated_mod"),
                rs.getInt("grade"),
                rs.getString("learning_path"),
                rs.getString("module_subject"),
                rs.getString("submission_content"),
                rs.getBoolean("completed")
        );
    }

    private Set<Integer> parseContactIds(String json, int selfId) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (json == null || json.isBlank()) {
            return ids;
        }
        try {
            JsonElement element = JsonParser.parseString(json);
            if (element.isJsonArray()) {
                for (JsonElement contact : element.getAsJsonArray()) {
                    if (contact != null && contact.isJsonPrimitive() && contact.getAsJsonPrimitive().isNumber()) {
                        int id = contact.getAsInt();
                        if (id > 0 && id != selfId) {
                            ids.add(id);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Corrupt JSON should not break messaging; start from an empty set.
        }
        return ids;
    }

    private String toContactListJson(Collection<Integer> ids) {
        JsonArray arr = new JsonArray();
        for (Integer id : ids) {
            if (id != null && id > 0) {
                arr.add(id);
            }
        }
        return gson.toJson(arr);
    }

    private Set<Integer> getContactIds(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT contact_list FROM accounts WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return new LinkedHashSet<>();
            }
            return parseContactIds(rs.getString("contact_list"), userId);
        }
    }

    private boolean isContactListNull(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT contact_list FROM accounts WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getString("contact_list") == null;
        }
    }

    private void saveContactIds(Connection conn, int userId, Set<Integer> ids) throws SQLException {
        ids.remove(userId);
        try (PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET contact_list = ? WHERE user_id = ?")) {
            ps.setString(1, toContactListJson(ids));
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private void appendContactId(Connection conn, int userId, int contactId) throws SQLException {
        if (userId <= 0 || contactId <= 0 || userId == contactId) {
            return;
        }
        Set<Integer> ids = getContactIds(conn, userId);
        if (ids.add(contactId)) {
            saveContactIds(conn, userId, ids);
        }
    }

    private Set<Integer> deriveInitialContacts(Connection conn, int userId, String acctType) throws SQLException {
        Set<Integer> contacts = new LinkedHashSet<>();
        contacts.addAll(queryContactIds(conn, """
                SELECT DISTINCT CASE
                    WHEN sender_id = ? THEN receiver_id
                    WHEN receiver_id = ? THEN sender_id
                    ELSE NULL
                END AS contact_id
                FROM messages
                WHERE sender_id = ? OR receiver_id = ?
                """, userId, userId, userId, userId));

        if ("Student".equals(acctType)) {
            contacts.addAll(queryContactIds(conn, """
                    SELECT DISTINCT m.educator_id AS contact_id
                    FROM module_progress mp
                    JOIN modules m ON mp.mod_id = m.mod_id
                    WHERE mp.student_id = ?
                    """, userId));
            contacts.addAll(queryContactIds(conn,
                    "SELECT assigned_counselor AS contact_id FROM accounts WHERE user_id = ? AND assigned_counselor IS NOT NULL",
                    userId));
        } else if ("Parent".equals(acctType)) {
            contacts.addAll(queryContactIds(conn,
                    "SELECT user_id AS contact_id FROM accounts WHERE associated_id = ? AND acct_type = 'Student'",
                    userId));
            contacts.addAll(queryContactIds(conn, """
                    SELECT DISTINCT s.assigned_counselor AS contact_id
                    FROM accounts s
                    WHERE s.associated_id = ? AND s.assigned_counselor IS NOT NULL
                    """, userId));
        } else if ("Counselor".equals(acctType)) {
            contacts.addAll(queryContactIds(conn,
                    "SELECT student_id AS contact_id FROM counselor_students WHERE counselor_id = ?",
                    userId));
            contacts.addAll(queryContactIds(conn,
                    "SELECT user_id AS contact_id FROM accounts WHERE acct_type = 'Employer'"));
        } else if ("Employer".equals(acctType)) {
            contacts.addAll(queryContactIds(conn,
                    "SELECT user_id AS contact_id FROM accounts WHERE acct_type = 'Counselor'"));
        } else if ("University".equals(acctType)) {
            contacts.addAll(queryContactIds(conn,
                    "SELECT user_id AS contact_id FROM accounts WHERE acct_type = 'Admin'"));
        } else if ("Educator".equals(acctType)) {
            contacts.addAll(queryContactIds(conn, """
                    SELECT DISTINCT mp.student_id AS contact_id
                    FROM module_progress mp
                    JOIN modules m ON mp.mod_id = m.mod_id
                    WHERE m.educator_id = ?
                    """, userId));
        }

        contacts.remove(userId);
        return contacts;
    }

    private Set<Integer> queryContactIds(Connection conn, String sql, Integer... params) throws SQLException {
        Set<Integer> ids = new LinkedHashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    Integer p = params[i];
                    if (p == null) ps.setNull(i + 1, Types.INTEGER);
                    else ps.setInt(i + 1, p);
                }
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt(1);
                if (!rs.wasNull() && id > 0) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    public boolean initializeContactListIfNull(int userId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (isContactListNull(conn, userId)) {
                    String acctType;
                    try (PreparedStatement ps = conn.prepareStatement("SELECT acct_type FROM accounts WHERE user_id = ?")) {
                        ps.setInt(1, userId);
                        ResultSet rs = ps.executeQuery();
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        acctType = rs.getString("acct_type");
                    }
                    saveContactIds(conn, userId, deriveInitialContacts(conn, userId, acctType));
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw new DataAccessException("Failed to initialize contact list", e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to initialize contact list", e);
        }
    }

    public boolean addContactForUser(int userId, int contactId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                appendContactId(conn, userId, contactId);
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw new DataAccessException("Failed to add contact", e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add contact", e);
        }
    }

    public boolean addMutualContacts(int userA, int userB) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                appendContactId(conn, userA, userB);
                appendContactId(conn, userB, userA);
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw new DataAccessException("Failed to add mutual contacts", e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add mutual contacts", e);
        }
    }

    public List<HashMap<String, Object>> getContactsFromContactList(int userId) {
        initializeContactListIfNull(userId);
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                Set<Integer> contactIds = getContactIds(conn, userId);
                if (contactIds.isEmpty()) {
                    conn.commit();
                    return List.of();
                }

                StringBuilder sql = new StringBuilder("SELECT user_id, first_name, last_name, acct_type, contact_info FROM accounts WHERE user_id IN (");
                List<Integer> ids = new ArrayList<>(contactIds);
                for (int i = 0; i < ids.size(); i++) {
                    if (i > 0) sql.append(',');
                    sql.append('?');
                }
                sql.append(") ORDER BY first_name, last_name, user_id");

                List<HashMap<String, Object>> contacts = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    for (int i = 0; i < ids.size(); i++) {
                        ps.setInt(i + 1, ids.get(i));
                    }
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        contacts.add(contactFromRow(rs));
                    }
                }

                // Clean up stale IDs that no longer exist and delete associated messages.
                Set<Integer> existing = new LinkedHashSet<>();
                for (HashMap<String, Object> c : contacts) {
                    existing.add((Integer) c.get("user_id"));
                }
                if (!existing.equals(contactIds)) {
                    // Find stale contact IDs and delete their messages.
                    Set<Integer> staleIds = new LinkedHashSet<>(contactIds);
                    staleIds.removeAll(existing);

                    for (Integer staleContactId : staleIds) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "DELETE FROM messages WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)")) {
                            ps.setInt(1, userId);
                            ps.setInt(2, staleContactId);
                            ps.setInt(3, staleContactId);
                            ps.setInt(4, userId);
                            ps.executeUpdate();
                        }
                    }

                    saveContactIds(conn, userId, existing);
                }

                conn.commit();
                return contacts;
            } catch (SQLException e) {
                conn.rollback();
                throw new DataAccessException("Failed to retrieve contact_list contacts", e);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve contact_list contacts", e);
        }
    }

    // ── public methods ───────────────────────────────────────────────────────

    public boolean addUser(String email, String password, String fname, String lname, String acctType, String company) throws SQLException {
        return addUser(email, password, fname, lname, acctType, company, null, null);
    }

    public boolean addUser(String email, String password, String fname, String lname, String acctType,
                           String company, Integer associatedStudentId, String university) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users (email, password, acct_type) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, email);
                    ps.setString(2, CryptoUtil.encrypt(password));
                    ps.setString(3, acctType);
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    if (!keys.next()) throw new SQLException("Failed to create user account");
                    userId = keys.getInt(1);
                }

                try (PreparedStatement ps2 = conn.prepareStatement(
                        "INSERT INTO accounts (user_id, first_name, last_name, acct_type, company, contact_info, associated_id, university) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ps2.setInt(1, userId);
                    ps2.setString(2, fname);
                    ps2.setString(3, lname);
                    ps2.setString(4, acctType);
                    ps2.setString(5, company == null ? "" : company.trim());
                    ps2.setString(6, "{\"email\":\"" + email + "\",\"phone\":\"\",\"address\":\"\"}");
                    if (associatedStudentId == null) {
                        ps2.setNull(7, Types.INTEGER);
                    } else {
                        ps2.setInt(7, associatedStudentId);
                    }
                    if (university == null || university.isBlank()) {
                        ps2.setNull(8, Types.VARCHAR);
                    } else {
                        ps2.setString(8, university.trim());
                    }
                    ps2.executeUpdate();
                }

                if ("Parent".equals(acctType)) {
                    if (associatedStudentId == null || associatedStudentId <= 0) {
                        throw new SQLException("Parent registration requires a valid associated student ID");
                    }
                    try (PreparedStatement ps3 = conn.prepareStatement(
                            "UPDATE accounts SET associated_id = ? WHERE user_id = ? AND acct_type = 'Student'")) {
                        ps3.setInt(1, userId);
                        ps3.setInt(2, associatedStudentId);
                        if (ps3.executeUpdate() == 0) {
                            throw new SQLException("Associated student account was not found");
                        }
                    }
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            }
        }
    }

    public Pair<Integer, String> searchUserDB(String email, String password) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT user_id, acct_type FROM users WHERE email=? AND password=?")) {
            ps.setString(1, email);
            ps.setString(2, CryptoUtil.encrypt(password));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? new Pair<>(rs.getInt("user_id"), rs.getString("acct_type"))
                             : new Pair<>(-1, "Not Found");
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve user from database", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Message> searchMessagesDB(int receiverID) {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, conversation_id, sender_id, receiver_id, content FROM messages WHERE receiver_id=?")) {
            ps.setInt(1, receiverID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Message msg = new Message(rs.getInt("sender_id"), rs.getInt("receiver_id"), rs.getString("content"));
                msg.setMsgID(rs.getInt("id"));
                msg.setConvoID(rs.getInt("conversation_id"));
                messages.add(msg);
            }
            return messages;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve user from database", e);
        }
    }

    public int countUnreadMessages(int receiverId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) AS unread_count FROM messages WHERE receiver_id = ? AND (isRead = FALSE OR isRead IS NULL)")) {
            ps.setInt(1, receiverId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("unread_count");
            }
            return 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count unread messages", e);
        }
    }

    public List<Message> searchConversationMessages(int userAId, int userBId) {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     SELECT id, conversation_id, sender_id, receiver_id, content
                     FROM messages
                     WHERE (sender_id = ? AND receiver_id = ?)
                        OR (sender_id = ? AND receiver_id = ?)
                     ORDER BY id ASC
                     """)) {
            ps.setInt(1, userAId); ps.setInt(2, userBId);
            ps.setInt(3, userBId); ps.setInt(4, userAId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Message msg = new Message(rs.getInt("sender_id"), rs.getInt("receiver_id"), rs.getString("content"));
                msg.setMsgID(rs.getInt("id"));
                msg.setConvoID(rs.getInt("conversation_id"));
                messages.add(msg);
            }
            return messages;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve conversation messages", e);
        }
    }

    public boolean markConversationMessagesAsRead(int receiverId, int senderId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE messages SET isRead = TRUE WHERE receiver_id = ? AND sender_id = ? AND (isRead = FALSE OR isRead IS NULL)")) {
            ps.setInt(1, receiverId);
            ps.setInt(2, senderId);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark conversation messages as read", e);
        }
    }

    public boolean addMessage(Message msg) {
        String sql = msg.getConvoID() == -1
                ? "INSERT INTO messages (sender_id, receiver_id, content) VALUES (?, ?, ?)"
                : "INSERT INTO messages (sender_id, receiver_id, content, conversation_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                appendContactId(conn, msg.getSenderID(), msg.getReceiverID());
                appendContactId(conn, msg.getReceiverID(), msg.getSenderID());

                ps.setInt(1, msg.getSenderID());
                ps.setInt(2, msg.getReceiverID());
                ps.setString(3, msg.getContent());
                if (msg.getConvoID() != -1) ps.setInt(4, msg.getConvoID());

                boolean inserted = ps.executeUpdate() > 0;
                conn.commit();
                return inserted;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve user from database", e);
        }
    }

    /** Creates a message trail between employer and student so the employer appears in student contacts. */
    public boolean linkEmployerContactForStudent(int employerId, int studentId, int counselorId, int jobId, String jobType) {
        String safeType = (jobType == null || jobType.isBlank()) ? "job opportunity" : jobType;
        String content = "Counselor recommendation: You were matched with " + safeType +
                " (Job #" + jobId + "). Reply here to contact the employer directly.";
        addMutualContacts(studentId, employerId);
        return addMessage(new Message(employerId, studentId, content));
    }

    public HashMap<String, Object> searchAccountDB(int id, String query) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT " + query + " FROM accounts WHERE user_id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            HashMap<String, Object> data = new HashMap<>();
            if (query.contains("first_name"))        data.put("name",         rs.getString("first_name") + " " + rs.getString("last_name"));
            if (query.contains("acct_type"))         data.put("acctType",     rs.getString("acct_type"));
            if (query.contains("contact_info"))      data.put("contactInfo",  rs.getString("contact_info"));
            if (query.contains("associated_id"))     data.put("associatedID", rs.getInt("associated_id"));
            if (query.contains("university"))        data.put("university",   rs.getString("university"));
            if (query.contains("learning_path"))     data.put("learningPath", rs.getString("learning_path"));
            if (query.contains("assigned_counselor"))data.put("counselorID",  rs.getInt("assigned_counselor"));
            if (query.contains("company"))           data.put("company",      rs.getString("company"));
            return data;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve user from database", e);
        }
    }

    public List<LearningModule> searchModulesDB(int id, String userType) {
        List<LearningModule> modules = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String sql;
            if (userType.equals("Student")) {
                sql = """
                    SELECT p.mod_id, p.mod_progress, m.learning_path, m.educator_id, m.content, m.subject
                    FROM module_progress p
                    JOIN modules m ON p.mod_id = m.mod_id
                    WHERE p.student_id = ?;
                    """;
            } else if (userType.equals("Educator")) {
                sql = "SELECT * FROM modules WHERE educator_id=?";
            } else {
                sql = "SELECT * FROM modules";
            }

            PreparedStatement ps = conn.prepareStatement(sql);
            if (userType.equals("Student") || userType.equals("Educator")) ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modules.add(new LearningModule(
                        rs.getInt("mod_id"),
                        userType.equals("Student") ? rs.getInt("mod_progress") : -1,
                        rs.getInt("educator_id"),
                        rs.getString("learning_path"),
                        rs.getString("content"),
                        rs.getString("subject")
                ));
            }
            return modules;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve modules from database", e);
        }
    }

    public boolean addModuleDB(LearningModule mod) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO modules (educator_id, learning_path, content, subject) VALUES (?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, mod.getEducatorID());
            ps.setString(2, mod.getLearningPath());
            ps.setString(3, mod.getContent());
            ps.setString(4, mod.getSubject());
            if (ps.executeUpdate() > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) mod.setModuleID(keys.getInt(1));
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add modules to database", e);
        }
    }

    public boolean addStudentToModule(int studentID, int modID) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO module_progress (student_id, mod_id) VALUES (?, ?)")) {
                ps.setInt(1, studentID);
                ps.setInt(2, modID);
                boolean added = ps.executeUpdate() > 0;

                Integer educatorId = findEducatorForModule(modID);
                if (educatorId != null && educatorId > 0) {
                    appendContactId(conn, studentID, educatorId);
                }

                conn.commit();
                return added;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add modules to database", e);
        }
    }

    public boolean addAssessmentDB(int modID, String learningPath, String content) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO assessments (associated_mod, learning_path, content) VALUES (?, ?, ?)")) {
            ps.setInt(1, modID);
            ps.setString(2, learningPath);
            ps.setString(3, content);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to create assessment contact your admin.", e);
        }
    }

    public boolean addJobProgram(JobProgram job) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO jobs (employer_id, learning_path, mod_req, assessment_req, description, job_type) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, job.getEmployerID());
            ps.setString(2, job.getPreferredLearningPath());
            ps.setInt(3, job.getModRequired());
            ps.setInt(4, job.getAssessmentRequired());
            ps.setString(5, job.getDescription());
            ps.setString(6, job.getJobType());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add jobs to database", e);
        }
    }

    public boolean updateModuleDB(LearningModule mod) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE modules SET learning_path = ?, content = ?, subject = ? WHERE mod_id=?")) {
            ps.setString(1, mod.getLearningPath());
            ps.setString(2, mod.getContent());
            ps.setString(3, mod.getSubject());
            ps.setInt(4, mod.getModuleID());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add modules to database", e);
        }
    }

    public boolean updateModuleProgress(int studentID, int modID, int progress) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE module_progress SET mod_progress = ? WHERE student_id = ? AND mod_id=?")) {
            ps.setInt(1, progress);
            ps.setInt(2, studentID);
            ps.setInt(3, modID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add modules to database", e);
        }
    }

    public boolean updateAssessmentGrade(int studentID, int assessmentID, int grade) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE assessment_progress SET grade = ? WHERE student_id = ? AND assessment_id=?")) {
            ps.setInt(1, grade);
            ps.setInt(2, studentID);
            ps.setInt(3, assessmentID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add modules to database", e);
        }
    }

    public boolean updateAssessmentCompletion(int studentId, int assessmentId, boolean completed) {
        return updateAssessmentCompletion(studentId, assessmentId, completed, null);
    }

    public boolean updateAssessmentCompletion(int studentId, int assessmentId, boolean completed, String submissionJson) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE assessment_progress SET completed = ?, submission = ? WHERE student_id = ? AND assessment_id = ?")) {
                ps.setBoolean(1, completed);
                ps.setString(2, submissionJson);
                ps.setInt(3, studentId);
                ps.setInt(4, assessmentId);
                if (ps.executeUpdate() > 0) return true;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO assessment_progress (student_id, assessment_id, grade, completed, submission) VALUES (?, ?, ?, ?, ?)")) {
                ps.setInt(1, studentId);
                ps.setInt(2, assessmentId);
                ps.setInt(3, -1);
                ps.setBoolean(4, completed);
                ps.setString(5, submissionJson);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update assessment completion state", e);
        }
    }

    private static final String ASSESSMENT_SUBMISSION_SELECT = """
            SELECT
                ap.student_id,
                COALESCE(s.first_name, '') AS first_name,
                COALESCE(s.last_name, '') AS last_name,
                ap.assessment_id,
                a.associated_mod,
                a.learning_path,
                m.subject AS module_subject,
                ap.submission AS submission_content,
                ap.grade,
                ap.completed
            FROM assessment_progress ap
            JOIN assessments a ON ap.assessment_id = a.assessment_id
            JOIN modules m ON a.associated_mod = m.mod_id
            LEFT JOIN accounts s ON s.user_id = ap.student_id
            WHERE m.educator_id = ?
            """;

    public List<Assessment> getPendingAssessmentSubmissionsForEducator(int educatorId) {
        List<Assessment> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(ASSESSMENT_SUBMISSION_SELECT +
                     "AND ap.completed = TRUE AND (ap.grade IS NULL OR ap.grade < 0) ORDER BY ap.assessment_id, ap.student_id")) {
            ps.setInt(1, educatorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(submissionFromRow(rs));
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch pending assessment submissions", e);
        }
    }

    public List<Assessment> getGradedAssessmentSubmissionsForEducator(int educatorId) {
        List<Assessment> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(ASSESSMENT_SUBMISSION_SELECT +
                     "AND ap.completed = TRUE AND ap.grade >= 0 ORDER BY ap.assessment_id DESC, ap.student_id")) {
            ps.setInt(1, educatorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(submissionFromRow(rs));
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch graded assessment submissions", e);
        }
    }

    public boolean gradeAssessmentSubmission(int educatorId, int studentId, int assessmentId, int grade) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     UPDATE assessment_progress ap
                     JOIN assessments a ON ap.assessment_id = a.assessment_id
                     JOIN modules m ON a.associated_mod = m.mod_id
                     SET ap.grade = ?, ap.completed = TRUE
                     WHERE ap.student_id = ? AND ap.assessment_id = ? AND m.educator_id = ?
                     """)) {
            ps.setInt(1, grade);
            ps.setInt(2, studentId);
            ps.setInt(3, assessmentId);
            ps.setInt(4, educatorId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to grade assessment submission", e);
        }
    }

    public boolean updateAssessmentContent(int assessmentID, String content) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE assessments SET content = ? WHERE assessment_id = ?")) {
            ps.setString(1, content);
            ps.setInt(2, assessmentID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update assessment content", e);
        }
    }

    public boolean updateJobProgram(JobProgram job) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE jobs SET learning_path = ?, mod_req = ?, assessment_req = ?, description = ?, job_type = ? WHERE job_id=?")) {
            ps.setString(1, job.getPreferredLearningPath());
            ps.setInt(2, job.getModRequired());
            ps.setInt(3, job.getAssessmentRequired());
            ps.setString(4, job.getDescription());
            ps.setString(5, job.getJobType());
            ps.setInt(6, job.getJobID());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add jobs to database", e);
        }
    }

    public List<Assessment> searchAssessmentDB(int id, String userType) {
        List<Assessment> assessments = new ArrayList<>();
        String sql = userType.equals("Student") ? """
                SELECT p.assessment_id, p.grade, p.completed, a.learning_path, a.associated_mod,
                       m.subject AS module_subject, a.content
                FROM assessment_progress p
                JOIN assessments a ON p.assessment_id = a.assessment_id
                JOIN modules m ON a.associated_mod = m.mod_id
                WHERE p.student_id = ?;
                """ : """
                SELECT a.assessment_id, -1 AS grade, FALSE AS completed, a.learning_path, a.associated_mod,
                       m.subject AS module_subject, a.content
                FROM assessments a
                JOIN modules m ON a.associated_mod = m.mod_id
                WHERE a.associated_mod = ?;
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                assessments.add(new Assessment(
                        rs.getInt("assessment_id"), rs.getInt("associated_mod"),
                        rs.getInt("grade"), rs.getString("learning_path"),
                        rs.getString("module_subject"), rs.getString("content"),
                        rs.getBoolean("completed")
                ));
            }
            return assessments;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve modules from database", e);
        }
    }

    public Integer findAvailableCounselor() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     SELECT c.user_id
                     FROM accounts c
                     LEFT JOIN counselor_students cs ON c.user_id = cs.counselor_id
                     WHERE c.acct_type = 'Counselor'
                     GROUP BY c.user_id
                     HAVING COUNT(cs.student_id) < 10
                     ORDER BY COUNT(cs.student_id)
                     LIMIT 1;
                     """)) {
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("user_id") : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve user from database", e);
        }
    }

    public boolean assignStudent(int student_id, int counselor_id) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO counselor_students (counselor_id, student_id) VALUES (?, ?)")) {
                    ps.setInt(1, counselor_id);
                    ps.setInt(2, student_id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE accounts SET assigned_counselor = ? WHERE user_id = ?")) {
                    ps.setInt(1, counselor_id);
                    ps.setInt(2, student_id);
                    ps.executeUpdate();
                }
                appendContactId(conn, student_id, counselor_id);
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw new DataAccessException("Failed to retrieve user from database", e);
            }
        } catch (DataAccessException e) {
            throw e;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Integer> searchStudentsCounselorDB(int counselorID) {
        List<Integer> students = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT cs.student_id FROM counselor_students cs " +
                     "JOIN accounts a ON cs.student_id = a.user_id " +
                     "WHERE cs.counselor_id = ? AND a.acct_type = 'Student'")) {
            ps.setInt(1, counselorID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) students.add(rs.getInt("student_id"));
            return students;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve assigned students from database", e);
        }
    }

    public List<JobProgram> searchJobProgramsDB(int employerID) {
        List<JobProgram> programs = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT job_id, employer_id, learning_path, mod_req, assessment_req, description, job_type, is_available FROM jobs WHERE employer_id = ?")) {
            ps.setInt(1, employerID);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                programs.add(new JobProgram(
                        employerID, rs.getInt("job_id"), rs.getBoolean("is_available"),
                        rs.getInt("mod_req"), rs.getInt("assessment_req"),
                        rs.getString("learning_path"), rs.getString("description"), rs.getString("job_type")
                ));
            }
            return programs;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve job programs from database", e);
        }
    }

    public List<Integer> searchGuardedStudentsDB(int parentID) {
        return queryIntList("SELECT user_id FROM accounts WHERE associated_id = ?", parentID,
                "Failed to retrieve guarded students from database");
    }

    public List<Integer> searchEnrolledStudentsDB(int universityID) {
        return queryIntList(
                "SELECT user_id FROM accounts WHERE acct_type = 'Student' AND university = (SELECT university FROM accounts WHERE user_id = ?)",
                universityID, "Failed to retrieve enrolled students from database");
    }

    public boolean updateStudentUniversity(int studentId, String university) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE accounts SET university = ? WHERE user_id = ? AND acct_type = 'Student'")) {
            ps.setString(1, university == null ? null : university.trim());
            ps.setInt(2, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update student university", e);
        }
    }

    private List<Integer> queryIntList(String sql, int param, String errMsg) {
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
            return ids;
        } catch (SQLException e) {
            throw new DataAccessException(errMsg, e);
        }
    }

    public boolean updateStudentLearningPath(int studentID, String learningPath) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE accounts SET learning_path = ? WHERE user_id = ?")) {
            ps.setString(1, learningPath);
            ps.setInt(2, studentID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update student learning path in database", e);
        }
    }

    /** Executes a contact-query SQL (expects columns: user_id, first_name, last_name, acct_type, contact_info). */
    private List<HashMap<String, Object>> queryContacts(Connection conn, String sql, Integer param) throws SQLException {
        List<HashMap<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(contactFromRow(rs));
        }
        return list;
    }

    public List<HashMap<String, Object>> getStudentContacts(int studentID) {
        return getContactsFromContactList(studentID);
    }

    public List<HashMap<String, Object>> getParentContacts(int parentID) {
        return getContactsFromContactList(parentID);
    }

    public List<HashMap<String, Object>> getCounselorContacts(int counselorID) {
        return getContactsFromContactList(counselorID);
    }

    public List<HashMap<String, Object>> getEmployerContacts(int employerID) {
        return getContactsFromContactList(employerID);
    }

    public List<HashMap<String, Object>> getUniversityContacts(int universityID) {
        return getContactsFromContactList(universityID);
    }

    public List<HashMap<String, Object>> getEducatorContacts(int educatorID) {
        return getContactsFromContactList(educatorID);
    }

    public List<LearningModule> searchAllModulesDB() {
        return searchModulesDB(-1, "All");
    }

    public List<LearningModule> searchModulesByLearningPathDB(String learningPath) {
        List<LearningModule> modules = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM modules WHERE learning_path = ?")) {
            ps.setString(1, learningPath);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modules.add(new LearningModule(
                        rs.getInt("mod_id"), -1, rs.getInt("educator_id"),
                        rs.getString("learning_path"), rs.getString("content"), rs.getString("subject")
                ));
            }
            return modules;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve filtered modules from database", e);
        }
    }

    public List<HashMap<String, Object>> searchAllJobProgramsDB(String learningPath, String query) {
        List<HashMap<String, Object>> jobs = new ArrayList<>();
        try (Connection conn = getConnection()) {
            StringBuilder sql = new StringBuilder("""
                    SELECT j.job_id, j.employer_id, j.learning_path, j.mod_req, j.assessment_req, j.description, j.job_type,
                           a.first_name, a.last_name, a.company
                    FROM jobs j
                    JOIN accounts a ON a.user_id = j.employer_id
                    WHERE 1=1
                    """);
            List<String> params = new ArrayList<>();
            if (learningPath != null && !learningPath.isBlank()) {
                sql.append(" AND j.learning_path = ?");
                params.add(learningPath.trim());
            }
            if (query != null && !query.isBlank()) {
                sql.append(" AND (j.job_type LIKE ? OR j.description LIKE ? OR a.company LIKE ?)");
                String q = "%" + query.trim() + "%";
                params.add(q); params.add(q); params.add(q);
            }
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) ps.setString(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                HashMap<String, Object> row = new HashMap<>();
                row.put("jobId",        rs.getInt("job_id"));
                row.put("employerId",   rs.getInt("employer_id"));
                row.put("learningPath", rs.getString("learning_path"));
                row.put("modReq",       rs.getInt("mod_req"));
                row.put("assessmentReq",rs.getInt("assessment_req"));
                row.put("description",  rs.getString("description"));
                row.put("jobType",      rs.getString("job_type"));
                row.put("employerName", rs.getString("first_name") + " " + rs.getString("last_name"));
                row.put("company",      rs.getString("company"));
                jobs.add(row);
            }
            return jobs;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve job programs from database", e);
        }
    }

    public boolean updateContactInfo(int userId, String email, String phone, String address) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                JsonObject contact = new JsonObject();
                contact.addProperty("email",   email   == null ? "" : email.trim());
                contact.addProperty("phone",   phone   == null ? "" : phone.trim());
                contact.addProperty("address", address == null ? "" : address.trim());

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE accounts SET contact_info = ? WHERE user_id = ?")) {
                    ps.setString(1, gson.toJson(contact));
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }
                if (email != null && !email.isBlank()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE users SET email = ? WHERE user_id = ?")) {
                        ps.setString(1, email.trim());
                        ps.setInt(2, userId);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw new DataAccessException("Failed to update contact information", e);
            }
        } catch (DataAccessException e) {
            throw e;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean notifyCounselorOfModuleRequest(int studentId, String requestedPath, String details) {
        Integer counselorId = findCounselorForStudent(studentId);
        if (counselorId == null || counselorId == 0)
            throw new EntityNotFoundException("No counselor assigned to student #" + studentId);
        return addMessage(new Message(studentId, counselorId,
                "Module Request - Student #" + studentId + " requested a module for learning path: " + requestedPath + "\nDetails: " + details));
    }

    public boolean notifyCounselorOfJobProgramRequest(int studentId, String details) {
        Integer counselorId = findCounselorForStudent(studentId);
        if (counselorId == null || counselorId == 0)
            throw new EntityNotFoundException("No counselor assigned to student #" + studentId);
        return addMessage(new Message(studentId, counselorId,
                "Job Program Request - Student #" + studentId + " requested job support.\nDetails: " + details));
    }

    public Integer findCounselorForStudent(int studentId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT assigned_counselor FROM accounts WHERE user_id = ?")) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("assigned_counselor") : null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find assigned counselor", e);
        }
    }

    public Integer findEducatorForModule(int moduleId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT educator_id FROM modules WHERE mod_id = ?")) {
            ps.setInt(1, moduleId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("educator_id") : null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find educator for module", e);
        }
    }

    public Integer findAvailableCounselorExcluding(int excludedCounselorId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     SELECT c.user_id
                     FROM accounts c
                     LEFT JOIN counselor_students cs ON c.user_id = cs.counselor_id
                     WHERE c.acct_type = 'Counselor' AND c.user_id <> ?
                     GROUP BY c.user_id
                     ORDER BY COUNT(cs.student_id)
                     LIMIT 1
                     """)) {
            ps.setInt(1, excludedCounselorId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("user_id") : null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find replacement counselor", e);
        }
    }

    public List<HashMap<String, Object>> listAllUsersSummary() {
        List<HashMap<String, Object>> users = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT user_id, first_name, last_name, acct_type, contact_info, company FROM accounts ORDER BY user_id")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                HashMap<String, Object> row = new HashMap<>();
                int uid = rs.getInt("user_id");
                String type = rs.getString("acct_type");
                String ci   = rs.getString("contact_info");
                row.put("userId",      uid);  row.put("user_id",      uid);
                row.put("name",        rs.getString("first_name") + " " + rs.getString("last_name"));
                row.put("acctType",    type); row.put("acct_type",    type);
                row.put("company",     rs.getString("company"));
                row.put("contactInfo", ci);   row.put("contact_info", ci);
                users.add(row);
            }
            return users;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve users", e);
        }
    }

    public List<HashMap<String, Object>> listAllModulesSummary() {
        List<HashMap<String, Object>> modules = new ArrayList<>();
        for (LearningModule m : searchAllModulesDB()) {
            HashMap<String, Object> row = new HashMap<>();
            row.put("modId",       m.getModuleID());
            row.put("subject",     m.getSubject());
            row.put("learningPath",m.getLearningPath());
            row.put("educatorId",  m.getEducatorID());
            modules.add(row);
        }
        return modules;
    }

    public List<HashMap<String, Object>> listAllJobProgramsSummary() {
        return searchAllJobProgramsDB(null, null);
    }

    public boolean deleteModuleById(int moduleId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM modules WHERE mod_id = ?")) {
            ps.setInt(1, moduleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete module", e);
        }
    }

    public boolean deleteJobProgramById(int jobId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM jobs WHERE job_id = ?")) {
            ps.setInt(1, jobId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete job program", e);
        }
    }

    public boolean deleteUserCascade(int userId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                String acctType;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT acct_type FROM accounts WHERE user_id = ?")) {
                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) throw new EntityNotFoundException("User #" + userId + " was not found");
                    acctType = rs.getString("acct_type");
                }

                if ("Employer".equals(acctType)) {
                    exec(conn, "DELETE FROM jobs WHERE employer_id = ?", userId);
                }

                if ("Educator".equals(acctType)) {
                    exec(conn, "DELETE FROM module_progress WHERE mod_id IN (SELECT mod_id FROM modules WHERE educator_id = ?)", userId);
                    exec(conn, "DELETE FROM modules WHERE educator_id = ?", userId);
                }

                if ("Counselor".equals(acctType)) {
                    List<Integer> students = new ArrayList<>();
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT student_id FROM counselor_students WHERE counselor_id = ?")) {
                        ps.setInt(1, userId);
                        ResultSet rs = ps.executeQuery();
                        while (rs.next()) students.add(rs.getInt("student_id"));
                    }
                    exec(conn, "DELETE FROM counselor_students WHERE counselor_id = ?", userId);

                    for (int studentId : students) {
                        Integer replacement = findAvailableCounselorExcluding(userId);
                        if (replacement == null)
                            throw new ReassignmentException("No replacement counselor available for student #" + studentId);
                        exec(conn, "INSERT INTO counselor_students (counselor_id, student_id) VALUES (?, ?)", replacement, studentId);
                        exec(conn, "UPDATE accounts SET assigned_counselor = ? WHERE user_id = ?", replacement, studentId);
                        appendContactId(conn, studentId, replacement);
                    }
                }

                exec(conn, "DELETE FROM accounts WHERE user_id = ?", userId);
                exec(conn, "DELETE FROM users WHERE user_id = ?", userId);
                conn.commit();
                return true;
            } catch (SQLException | EntityNotFoundException | ReassignmentException e) {
                conn.rollback();
                if (e instanceof SQLException) throw new DataAccessException("Failed to delete user", (SQLException) e);
                throw e;
            }
        } catch (DataAccessException | EntityNotFoundException | ReassignmentException e) {
            throw e;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Executes a simple DML statement with one or two int parameters. */
    private void exec(Connection conn, String sql, int p1) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p1);
            ps.executeUpdate();
        }
    }

    private void exec(Connection conn, String sql, int p1, int p2) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p1);
            ps.setInt(2, p2);
            ps.executeUpdate();
        }
    }
}
