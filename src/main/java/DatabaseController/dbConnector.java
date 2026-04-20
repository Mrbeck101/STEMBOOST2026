package DatabaseController;

import OtherComponents.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import eu.hansolo.toolbox.tuples.Pair;

import java.sql.*;
import java.util.*;
import java.io.FileInputStream;

public class dbConnector {

    private final String dbName = "stemboost_db";
    private String ip;
    private String user;
    private String password;
    private final Gson gson = new Gson();

    public dbConnector() {
        try {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream("db.properties")) {
                props.load(fis);
            }
            this.ip       = props.getProperty("db.ip");
            this.user     = props.getProperty("db.user");
            this.password = props.getProperty("db.password");
        } catch (Exception e) {
            System.out.println("Failed to fetch DB details");
            e.printStackTrace();
        }
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

    /** Maps the current ResultSet row to an AssessmentSubmission. */
    private AssessmentSubmission submissionFromRow(ResultSet rs) throws SQLException {
        int studentId = rs.getInt("student_id");
        String name = (rs.getString("first_name") + " " + rs.getString("last_name")).trim();
        if (name.isEmpty()) name = "Student #" + studentId;
        return new AssessmentSubmission(
                studentId, name,
                rs.getInt("assessment_id"),
                rs.getInt("associated_mod"),
                rs.getString("learning_path"),
                rs.getString("module_subject"),
                rs.getInt("grade"),
                rs.getBoolean("completed"),
                rs.getString("submission_content")
        );
    }

    // ── public methods ───────────────────────────────────────────────────────

    public boolean addUser(String email, String password, String fname, String lname, String acctType, String company) throws SQLException {
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
                        "INSERT INTO accounts (user_id, first_name, last_name, acct_type, company, contact_info) VALUES (?, ?, ?, ?, ?, ?)")) {
                    ps2.setInt(1, userId);
                    ps2.setString(2, fname);
                    ps2.setString(3, lname);
                    ps2.setString(4, acctType);
                    ps2.setString(5, company == null ? "" : company.trim());
                    ps2.setString(6, "{\"email\":\"" + email + "\",\"phone\":\"\",\"address\":\"\"}");
                    ps2.executeUpdate();
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
                     "SELECT user_id, user_type FROM users WHERE email=? AND password=?")) {
            ps.setString(1, email);
            ps.setString(2, CryptoUtil.encrypt(password));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? new Pair<>(rs.getInt("user_id"), rs.getString("user_type"))
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

    public boolean addMessage(Message msg) {
        String sql = msg.getConvoID() == -1
                ? "INSERT INTO messages (sender_id, receiver_id, content) VALUES (?, ?, ?)"
                : "INSERT INTO messages (sender_id, receiver_id, content, conversation_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, msg.getSenderID());
            ps.setInt(2, msg.getReceiverID());
            ps.setString(3, msg.getContent());
            if (msg.getConvoID() != -1) ps.setInt(4, msg.getConvoID());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve user from database", e);
        }
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
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO module_progress (student_id, mod_id) VALUES (?, ?)")) {
            ps.setInt(1, studentID);
            ps.setInt(2, modID);
            return ps.executeUpdate() > 0;
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
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE assessment_progress SET completed = ? WHERE student_id = ? AND assessment_id = ?")) {
                ps.setBoolean(1, completed);
                ps.setInt(2, studentId);
                ps.setInt(3, assessmentId);
                if (ps.executeUpdate() > 0) return true;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO assessment_progress (student_id, assessment_id, grade, completed) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, studentId);
                ps.setInt(2, assessmentId);
                ps.setInt(3, -1);
                ps.setBoolean(4, completed);
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
                msg.content AS submission_content,
                ap.grade,
                ap.completed
            FROM assessment_progress ap
            JOIN assessments a ON ap.assessment_id = a.assessment_id
            JOIN modules m ON a.associated_mod = m.mod_id
            LEFT JOIN accounts s ON s.user_id = ap.student_id
            LEFT JOIN messages msg ON msg.id = (
                SELECT MAX(m2.id) FROM messages m2
                WHERE m2.sender_id = ap.student_id AND m2.receiver_id = m.educator_id
            )
            WHERE m.educator_id = ?
            """;

    public List<AssessmentSubmission> getPendingAssessmentSubmissionsForEducator(int educatorId) {
        List<AssessmentSubmission> list = new ArrayList<>();
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

    public List<AssessmentSubmission> getGradedAssessmentSubmissionsForEducator(int educatorId) {
        List<AssessmentSubmission> list = new ArrayList<>();
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
                     "SELECT student_id FROM counselor_students WHERE counselor_id = ?")) {
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
                "SELECT user_id FROM accounts WHERE university = (SELECT university FROM accounts WHERE user_id = ?)",
                universityID, "Failed to retrieve enrolled students from database");
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
        try (Connection conn = getConnection()) {
            List<HashMap<String, Object>> contacts = queryContacts(conn, """
                    SELECT DISTINCT a.user_id, a.first_name, a.last_name, a.acct_type, a.contact_info
                    FROM accounts a
                    JOIN modules m ON a.user_id = m.educator_id
                    JOIN module_progress mp ON m.mod_id = mp.mod_id
                    WHERE mp.student_id = ? AND a.acct_type = 'Educator'
                    """, studentID);
            contacts.addAll(queryContacts(conn, """
                    SELECT a.user_id, a.first_name, a.last_name, a.acct_type, a.contact_info
                    FROM accounts a
                    WHERE a.user_id = (SELECT assigned_counselor FROM accounts WHERE user_id = ?) AND a.acct_type = 'Counselor'
                    """, studentID));
            return contacts;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve student contacts from database", e);
        }
    }

    public List<HashMap<String, Object>> getParentContacts(int parentID) {
        try (Connection conn = getConnection()) {
            List<HashMap<String, Object>> contacts = queryContacts(conn, """
                    SELECT a.user_id, a.first_name, a.last_name, a.acct_type, a.contact_info
                    FROM accounts a WHERE a.associated_id = ? AND a.acct_type = 'Student'
                    """, parentID);
            contacts.addAll(queryContacts(conn, """
                    SELECT DISTINCT a.user_id, a.first_name, a.last_name, a.acct_type, a.contact_info
                    FROM accounts a
                    WHERE a.user_id = (SELECT assigned_counselor FROM accounts WHERE associated_id = ?) AND a.acct_type = 'Counselor'
                    """, parentID));
            return contacts;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve parent contacts from database", e);
        }
    }

    public List<HashMap<String, Object>> getCounselorContacts(int counselorID) {
        try (Connection conn = getConnection()) {
            List<HashMap<String, Object>> contacts = queryContacts(conn, """
                    SELECT a.user_id, a.first_name, a.last_name, a.acct_type, a.contact_info
                    FROM accounts a
                    JOIN counselor_students cs ON a.user_id = cs.student_id
                    WHERE cs.counselor_id = ? AND a.acct_type = 'Student'
                    """, counselorID);
            contacts.addAll(queryContacts(conn, """
                    SELECT a.user_id, a.first_name, a.last_name, a.acct_type, a.contact_info
                    FROM accounts a WHERE a.acct_type = 'Employer'
                    """, null));
            return contacts;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve counselor contacts from database", e);
        }
    }

    public List<HashMap<String, Object>> getEmployerContacts(int employerID) {
        try (Connection conn = getConnection()) {
            return queryContacts(conn, """
                    SELECT a.user_id, a.first_name, a.last_name, a.acct_type, a.contact_info
                    FROM accounts a WHERE a.acct_type = 'Counselor'
                    """, null);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve employer contacts from database", e);
        }
    }

    public List<HashMap<String, Object>> getUniversityContacts(int universityID) {
        try (Connection conn = getConnection()) {
            return queryContacts(conn, """
                    SELECT a.user_id, a.first_name, a.last_name, a.acct_type, a.contact_info
                    FROM accounts a WHERE a.acct_type = 'Admin'
                    """, null);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve university contacts from database", e);
        }
    }

    public List<HashMap<String, Object>> getEducatorContacts(int educatorID) {
        try (Connection conn = getConnection()) {
            return queryContacts(conn, """
                    SELECT DISTINCT a.user_id, a.first_name, a.last_name, a.acct_type, a.contact_info
                    FROM accounts a
                    JOIN module_progress mp ON a.user_id = mp.student_id
                    JOIN modules m ON mp.mod_id = m.mod_id
                    WHERE m.educator_id = ? AND a.acct_type = 'Student'
                    """, educatorID);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve educator contacts from database", e);
        }
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

    public boolean notifyCounselorOfJobProgramRequest(int studentId, int jobId, String details) {
        Integer counselorId = findCounselorForStudent(studentId);
        if (counselorId == null || counselorId == 0)
            throw new EntityNotFoundException("No counselor assigned to student #" + studentId);
        return addMessage(new Message(studentId, counselorId,
                "Job Program Request - Student #" + studentId + " requested job program #" + jobId + "\nDetails: " + details));
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
