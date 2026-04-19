package DatabaseController;

import OtherComponents.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import eu.hansolo.toolbox.tuples.Pair;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
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
            FileInputStream fis = new FileInputStream("db.properties");
            props.load(fis);

            this.ip = props.getProperty("db.ip");
            this.user = props.getProperty("db.user");
            this.password = props.getProperty("db.password");

        } catch (Exception e) {
            System.out.println("Failed to fetch DB details");
            e.printStackTrace();
        }
    }



    public boolean addUser(String email, String password, String fname, String lname, String acctType, String company) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;

        try {
            conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password);
            conn.setAutoCommit(false);

            String sql_user = "INSERT INTO users (email, password, acct_type) VALUES (?, ?, ?)";
            ps = conn.prepareStatement(sql_user, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, email);
            ps.setString(2, CryptoUtil.encrypt(password));
            ps.setString(3, acctType);
            ps.executeUpdate();

            int userId;
            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                userId = generatedKeys.getInt(1);
            } else {
                throw new SQLException("Failed to create user account");
            }

            String sql_acct = "INSERT INTO accounts (user_id, first_name, last_name, acct_type, company, contact_info) VALUES (?, ?, ?, ?, ?, ?)";
            ps2 = conn.prepareStatement(sql_acct);
            ps2.setInt(1, userId);
            ps2.setString(2, fname);
            ps2.setString(3, lname);
            ps2.setString(4, acctType);
            ps2.setString(5, company == null ? "" : company.trim());
            ps2.setString(6, "{\"email\":\"" + email + "\",\"phone\":\"\",\"address\":\"\"}");
            ps2.executeUpdate();

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) conn.rollback();
            e.printStackTrace();
            return false;
        } finally {
            if (ps != null) ps.close();
            if (ps2 != null) ps2.close();
            if (conn != null) conn.close();
        }
    }

    public Pair<Integer, String> searchUserDB(String email, String password) {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {

            String sql = "SELECT user_id, user_type FROM users WHERE email=? AND password=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, CryptoUtil.encrypt(password));
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return new Pair<>(-1, "Not Found");
            }

            return new Pair<>(rs.getInt("user_id"), rs.getString("user_type"));
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve user from database", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Message> searchMessagesDB(int receiverID) {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {

                String sql = "SELECT id, conversation_id, sender_id, subject, content FROM messages WHERE receiver_id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, receiverID);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    int msgID = rs.getInt("id");
                    int convoID = rs.getInt("conversation_id");
                    int senderID = rs.getInt("sender_id");
                    String subject = rs.getString("subject");
                    String content = rs.getString("content");

                    Message msg = new Message(senderID, receiverID, subject, content);
                    msg.setMsgID(msgID);
                    msg.setConvoID(convoID);

                    messages.add(msg);
                };

                return messages;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve user from database", e);
        }
    }

    public boolean addMessage(Message msg) {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql;
            PreparedStatement ps = null;

            if (msg.getConvoID() == -1) {
                sql = "INSERT INTO messages (sender_id, receiver_id, subject, content) VALUES (?, ?, ?, ?)";
            } else {
                sql = "INSERT INTO messages (sender_id, receiver_id, subject, content, conversation_id) VALUES (?, ?, ?, ?, ?)";
            }

            ps = conn.prepareStatement(sql);
            ps.setInt(1, msg.getSenderID());
            ps.setInt(2, msg.getReceiverID());
            ps.setString(3, msg.getSubject());
            ps.setString(4, msg.getContent());

            if (msg.getConvoID() != -1) {
                ps.setInt(5, msg.getConvoID());
            }

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve user from database", e);
        }
    }

    public HashMap<String, Object> searchAccountDB(int id, String query) {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {

                String sql = "SELECT " + query + " FROM accounts WHERE user_id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    HashMap<String, Object> data = new HashMap<>();

                    if(query.contains("first_name")) {
                        data.put("name", rs.getString("first_name") + " " + rs.getString("last_name"));
                    }

                    if(query.contains("acct_type")) {
                        data.put("acctType", rs.getString("acct_type"));
                    }

                    if(query.contains("contact_info")) {
                        data.put("contactInfo", rs.getString("contact_info"));
                    }

                    if(query.contains("associated_id")) {
                        data.put("associatedID", rs.getInt("associated_id"));
                    }

                    if(query.contains("university")) {
                        data.put("university", rs.getString("university"));
                    }

                    if(query.contains("learning_path")) {
                        data.put("learningPath", rs.getString("learning_path"));
                    }

                    if(query.contains("assigned_counselor")) {
                        data.put("counselorID", rs.getInt("assigned_counselor"));
                    }

                    if(query.contains("company")) {
                        data.put("company", rs.getString("company"));
                    }

                    return data;
                } else {
                    return null;
                }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve user from database", e);
        }
    }

    public List<LearningModule> searchModulesDB(int id, String userType) {
        List<LearningModule> modules = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql;

            if (userType.equals("Student")) {
                sql = """
                    SELECT
                      p.mod_id,
                      p.mod_progress,
                      m.learning_path,
                      m.educator_id,
                      m.content,
                      m.subject
                    FROM module_progress p
                    JOIN modules m
                      ON p.mod_id = m.mod_id
                    WHERE p.student_id = ?;
                """;
            } else if (userType.equals("Educator")) {
                sql = "SELECT * FROM modules WHERE educator_id=?";
            } else {
                sql = "SELECT * FROM modules";
            }

            PreparedStatement ps = conn.prepareStatement(sql);
            if (userType.equals("Student") || userType.equals("Educator")) {
                ps.setInt(1, id);
            }
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int modID = rs.getInt("mod_id");
                int modProgress = userType.equals("Student") ? rs.getInt("mod_progress") : -1;
                String learningPath = rs.getString("learning_path");
                int educatorID = rs.getInt("educator_id");
                String content = rs.getString("content");
                String subject = rs.getString("subject");

                LearningModule module = new LearningModule(modID, modProgress, educatorID, learningPath, content, subject);

                modules.add(module);

            }

            return modules;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve modules from database", e);
        }
    }

    public boolean addModuleDB(LearningModule mod) {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "INSERT INTO modules (educator_id, learning_path, content, subject) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, mod.getEducatorID());
            ps.setString(2, mod.getLearningPath());
            ps.setString(3, mod.getContent());
            ps.setString(4, mod.getSubject());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    mod.setModuleID(generatedKeys.getInt(1));
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add modules to database", e);
        }
    }

    public boolean addStudentToModule(int studentID, int modID){
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "INSERT INTO module_progress (student_id, mod_id) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, studentID);
            ps.setInt(2, modID);


            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add modules to database", e);
        }
    }

    public boolean addAssessmentDB(int modID, String learningPath, String content){
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "INSERT INTO assessments (associated_mod, learning_path, content) VALUES (?, ?, ?)"; //This has a trigger in the database to automatically assign all students in the associated mod to this assessment
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, modID);
            ps.setString(2, learningPath);
            ps.setString(3, content);


            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add modules to database", e);
        }
    }

    public boolean addJobProgram(JobProgram job){
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "INSERT INTO jobs (employer_id, learning_path, mod_req, assessment_req, description, job_type) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
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
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "UPDATE modules SET learning_path = ?, content = ?, subject = ? WHERE mod_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(4, mod.getModuleID());
            ps.setString(1, mod.getLearningPath());
            ps.setString(2, mod.getContent());
            ps.setString(3, mod.getSubject());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add modules to database", e);
        }
    }

    public boolean updateModuleProgress(int studentID, int modID, int progress) {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "UPDATE module_progress SET progress = ? WHERE student_id = ? AND mod_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, progress);
            ps.setInt(2, studentID);
            ps.setInt(3, modID);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add modules to database", e);
        }
    }

    public boolean updateAssessmentGrade(int studentID, int assessmentID, int grade){
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "UPDATE assessment_progress SET grade = ? WHERE student_id = ? AND assessment_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, grade);
            ps.setInt(2, studentID);
            ps.setInt(3, assessmentID);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add modules to database", e);
        }
    }

    public boolean updateAssessmentContent(int assessmentID, String content) {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "UPDATE assessments SET content = ? WHERE assessment_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, content);
            ps.setInt(2, assessmentID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update assessment content", e);
        }
    }

    public boolean updateJobProgram(JobProgram job){
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "UPDATE jobs SET learning_path = ?, mod_req = ?, assessment_req = ?, description = ?, job_type = ? WHERE job_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(6, job.getJobID());
            ps.setString(1, job.getPreferredLearningPath());
            ps.setInt(2, job.getModRequired());
            ps.setInt(3, job.getAssessmentRequired());
            ps.setString(4, job.getDescription());
            ps.setString(5, job.getJobType());


            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add jobs to database", e);
        }
    }

    public List<Assessment> searchAssessmentDB(int id, String userType) {
        List<Assessment> assessments = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql;

            if (userType.equals("Student")) {
                sql = """
                    SELECT
                      p.assessment_id,
                      p.grade,
                      a.learning_path,
                      a.associated_mod,
                      a.content
                    FROM assessment_progress p
                    JOIN assessments a
                      ON p.assessment_id = a.assessment_id
                    WHERE p.student_id = ?;
                """;
            } else {
                sql = """
                    SELECT
                      a.assessment_id,
                      -1 AS grade,
                      a.learning_path,
                      a.associated_mod,
                      a.content
                    FROM assessments a
                    WHERE a.associated_mod = ?;
                """;
            }
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int assessmentID = rs.getInt("assessment_id");
                int grade = rs.getInt("grade");
                String learningPath = rs.getString("learning_path");
                int modID = rs.getInt("associated_mod");
                String content = rs.getString("content");


                Assessment assessment = new Assessment(assessmentID, modID, grade, learningPath, content);

                assessments.add(assessment);

            }

            return assessments;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve modules from database", e);
        }
    }

    public Integer findAvailableCounselor() {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = """
                SELECT c.user_id
                FROM accounts c
                LEFT JOIN counselor_students cs
                  ON c.user_id = cs.counselor_id
                WHERE c.acct_type = 'Counselor'
                GROUP BY c.user_id
                HAVING COUNT(cs.student_id) < 10
                ORDER BY COUNT(cs.student_id)
                LIMIT 1;
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("user_id");
            } else {
                return 0;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve user from database", e);
        }
    }

    public boolean assignStudent(int student_id, int counselor_id) {
        Connection conn = null;
        PreparedStatement insertStmt = null;
        PreparedStatement updateStmt = null;

        try{
            conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password);
            conn.setAutoCommit(false);

            String insertSql = "INSERT INTO counselor_students (counselor_id, student_id) VALUES (?, ?)";
            String updateSql = "UPDATE accounts SET assigned_counselor = ? WHERE user_id = ?";

            insertStmt = conn.prepareStatement(insertSql);
            updateStmt = conn.prepareStatement(updateSql);

            // Set INSERT variables
            insertStmt.setInt(1, counselor_id);
            insertStmt.setInt(2, student_id);
            insertStmt.executeUpdate();

            // Set UPDATE variables
            updateStmt.setInt(1, counselor_id);
            updateStmt.setInt(2, student_id);
            updateStmt.executeUpdate();

            // Commit transaction
            conn.commit();
            return true;

        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }

            throw new DataAccessException("Failed to retrieve user from database", e);
        } finally {
            try {
                if (insertStmt != null) insertStmt.close();
                if (updateStmt != null) updateStmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<Integer> searchStudentsCounselorDB(int counselorID) {
        List<Integer> students = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "SELECT student_id FROM counselor_students WHERE counselor_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, counselorID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                students.add(rs.getInt("student_id"));
            }

            return students;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve assigned students from database", e);
        }
    }

    public List<JobProgram> searchJobProgramsDB(int employerID) {
        List<JobProgram> programs = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "SELECT job_id, employer_id, learning_path, mod_req, assessment_req, description, job_type, is_available FROM jobs WHERE employer_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, employerID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int jobID = rs.getInt("job_id");
                String learningPath = rs.getString("learning_path");
                int modRequired = rs.getInt("mod_req");
                int assessmentRequired = rs.getInt("assessment_req");
                String description = rs.getString("description");
                String jobType = rs.getString("job_type");
                Boolean isAvailable = rs.getBoolean("is_available");

                JobProgram program = new JobProgram(employerID, jobID, isAvailable, modRequired, assessmentRequired, learningPath, description, jobType);
                programs.add(program);
            }

            return programs;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve job programs from database", e);
        }
    }

    public List<Integer> searchGuardedStudentsDB(int parentID) {
        List<Integer> students = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "SELECT user_id FROM accounts WHERE associated_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, parentID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                students.add(rs.getInt("user_id"));
            }

            return students;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve guarded students from database", e);
        }
    }

    public List<Integer> searchEnrolledStudentsDB(int universityID) {
        List<Integer> students = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "SELECT user_id FROM accounts WHERE university = (SELECT university FROM accounts WHERE user_id = ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, universityID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                students.add(rs.getInt("user_id"));
            }

            return students;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve enrolled students from database", e);
        }
    }

    public boolean updateStudentLearningPath(int studentID, String learningPath) {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "UPDATE accounts SET learning_path = ? WHERE user_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, learningPath);
            ps.setInt(2, studentID);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update student learning path in database", e);
        }
    }

    public List<HashMap<String, Object>> getStudentContacts(int studentID) {
        List<HashMap<String, Object>> contacts = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            // Get educators of modules the student is enrolled in
            String educatorSql = """
                SELECT DISTINCT
                    a.user_id,
                    a.first_name,
                    a.last_name,
                    a.acct_type,
                    a.contact_info
                FROM accounts a
                JOIN modules m ON a.user_id = m.educator_id
                JOIN module_progress mp ON m.mod_id = mp.mod_id
                WHERE mp.student_id = ? AND a.acct_type = 'Educator'
                """;

            PreparedStatement ps1 = conn.prepareStatement(educatorSql);
            ps1.setInt(1, studentID);
            ResultSet rs1 = ps1.executeQuery();

            while (rs1.next()) {
                HashMap<String, Object> contact = new HashMap<>();
                contact.put("user_id", rs1.getInt("user_id"));
                contact.put("name", rs1.getString("first_name") + " " + rs1.getString("last_name"));
                contact.put("acct_type", rs1.getString("acct_type"));
                contact.put("contact_info", rs1.getString("contact_info"));
                contacts.add(contact);
            }

            // Get assigned counselor
            String counselorSql = """
                SELECT
                    a.user_id,
                    a.first_name,
                    a.last_name,
                    a.acct_type,
                    a.contact_info
                FROM accounts a
                WHERE a.user_id = (SELECT assigned_counselor FROM accounts WHERE user_id = ?) AND a.acct_type = 'Counselor'
                """;

            PreparedStatement ps2 = conn.prepareStatement(counselorSql);
            ps2.setInt(1, studentID);
            ResultSet rs2 = ps2.executeQuery();

            if (rs2.next()) {
                HashMap<String, Object> contact = new HashMap<>();
                contact.put("user_id", rs2.getInt("user_id"));
                contact.put("name", rs2.getString("first_name") + " " + rs2.getString("last_name"));
                contact.put("acct_type", rs2.getString("acct_type"));
                contact.put("contact_info", rs2.getString("contact_info"));
                contacts.add(contact);
            }

            return contacts;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve student contacts from database", e);
        }
    }

    public List<HashMap<String, Object>> getParentContacts(int parentID) {
        List<HashMap<String, Object>> contacts = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            // Get associated student
            String studentSql = """
                SELECT
                    a.user_id,
                    a.first_name,
                    a.last_name,
                    a.acct_type,
                    a.contact_info
                FROM accounts a
                WHERE a.associated_id = ? AND a.acct_type = 'Student'
                """;

            PreparedStatement ps1 = conn.prepareStatement(studentSql);
            ps1.setInt(1, parentID);
            ResultSet rs1 = ps1.executeQuery();

            while (rs1.next()) {
                HashMap<String, Object> contact = new HashMap<>();
                contact.put("user_id", rs1.getInt("user_id"));
                contact.put("name", rs1.getString("first_name") + " " + rs1.getString("last_name"));
                contact.put("acct_type", rs1.getString("acct_type"));
                contact.put("contact_info", rs1.getString("contact_info"));
                contacts.add(contact);
            }

            // Get counselor's counselor
            String counselorSql = """
                SELECT DISTINCT
                    a.user_id,
                    a.first_name,
                    a.last_name,
                    a.acct_type,
                    a.contact_info
                FROM accounts a
                WHERE a.user_id = (SELECT assigned_counselor FROM accounts WHERE associated_id = ?) AND a.acct_type = 'Counselor'
                """;

            PreparedStatement ps2 = conn.prepareStatement(counselorSql);
            ps2.setInt(1, parentID);
            ResultSet rs2 = ps2.executeQuery();

            if (rs2.next()) {
                HashMap<String, Object> contact = new HashMap<>();
                contact.put("user_id", rs2.getInt("user_id"));
                contact.put("name", rs2.getString("first_name") + " " + rs2.getString("last_name"));
                contact.put("acct_type", rs2.getString("acct_type"));
                contact.put("contact_info", rs2.getString("contact_info"));
                contacts.add(contact);
            }

            return contacts;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve parent contacts from database", e);
        }
    }

    public List<HashMap<String, Object>> getCounselorContacts(int counselorID) {
        List<HashMap<String, Object>> contacts = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            // Get assigned students
            String studentsSql = """
                SELECT
                    a.user_id,
                    a.first_name,
                    a.last_name,
                    a.acct_type,
                    a.contact_info
                FROM accounts a
                JOIN counselor_students cs ON a.user_id = cs.student_id
                WHERE cs.counselor_id = ? AND a.acct_type = 'Student'
                """;

            PreparedStatement ps1 = conn.prepareStatement(studentsSql);
            ps1.setInt(1, counselorID);
            ResultSet rs1 = ps1.executeQuery();

            while (rs1.next()) {
                HashMap<String, Object> contact = new HashMap<>();
                contact.put("user_id", rs1.getInt("user_id"));
                contact.put("name", rs1.getString("first_name") + " " + rs1.getString("last_name"));
                contact.put("acct_type", rs1.getString("acct_type"));
                contact.put("contact_info", rs1.getString("contact_info"));
                contacts.add(contact);
            }

            // Get all employers
            String employersSql = """
                SELECT
                    a.user_id,
                    a.first_name,
                    a.last_name,
                    a.acct_type,
                    a.contact_info
                FROM accounts a
                WHERE a.acct_type = 'Employer'
                """;

            PreparedStatement ps2 = conn.prepareStatement(employersSql);
            ResultSet rs2 = ps2.executeQuery();

            while (rs2.next()) {
                HashMap<String, Object> contact = new HashMap<>();
                contact.put("user_id", rs2.getInt("user_id"));
                contact.put("name", rs2.getString("first_name") + " " + rs2.getString("last_name"));
                contact.put("acct_type", rs2.getString("acct_type"));
                contact.put("contact_info", rs2.getString("contact_info"));
                contacts.add(contact);
            }

            return contacts;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve counselor contacts from database", e);
        }
    }

    public List<HashMap<String, Object>> getEmployerContacts(int employerID) {
        List<HashMap<String, Object>> contacts = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            // Get all counselors
            String counselorsSql = """
                SELECT
                    a.user_id,
                    a.first_name,
                    a.last_name,
                    a.acct_type,
                    a.contact_info
                FROM accounts a
                WHERE a.acct_type = 'Counselor'
                """;

            PreparedStatement ps = conn.prepareStatement(counselorsSql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                HashMap<String, Object> contact = new HashMap<>();
                contact.put("user_id", rs.getInt("user_id"));
                contact.put("name", rs.getString("first_name") + " " + rs.getString("last_name"));
                contact.put("acct_type", rs.getString("acct_type"));
                contact.put("contact_info", rs.getString("contact_info"));
                contacts.add(contact);
            }

            return contacts;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve employer contacts from database", e);
        }
    }

    public List<HashMap<String, Object>> getUniversityContacts(int universityID) {
        List<HashMap<String, Object>> contacts = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            // Get all admins (assuming there's an admin account type)
            String adminsSql = """
                SELECT
                    a.user_id,
                    a.first_name,
                    a.last_name,
                    a.acct_type,
                    a.contact_info
                FROM accounts a
                WHERE a.acct_type = 'Admin'
                """;

            PreparedStatement ps = conn.prepareStatement(adminsSql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                HashMap<String, Object> contact = new HashMap<>();
                contact.put("user_id", rs.getInt("user_id"));
                contact.put("name", rs.getString("first_name") + " " + rs.getString("last_name"));
                contact.put("acct_type", rs.getString("acct_type"));
                contact.put("contact_info", rs.getString("contact_info"));
                contacts.add(contact);
            }

            return contacts;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve university contacts from database", e);
        }
    }

    public List<HashMap<String, Object>> getEducatorContacts(int educatorID) {
        List<HashMap<String, Object>> contacts = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            // Get all students assigned to educator's modules
            String studentsSql = """
                SELECT DISTINCT
                    a.user_id,
                    a.first_name,
                    a.last_name,
                    a.acct_type,
                    a.contact_info
                FROM accounts a
                JOIN module_progress mp ON a.user_id = mp.student_id
                JOIN modules m ON mp.mod_id = m.mod_id
                WHERE m.educator_id = ? AND a.acct_type = 'Student'
                """;

            PreparedStatement ps = conn.prepareStatement(studentsSql);
            ps.setInt(1, educatorID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                HashMap<String, Object> contact = new HashMap<>();
                contact.put("user_id", rs.getInt("user_id"));
                contact.put("name", rs.getString("first_name") + " " + rs.getString("last_name"));
                contact.put("acct_type", rs.getString("acct_type"));
                contact.put("contact_info", rs.getString("contact_info"));
                contacts.add(contact);
            }

            return contacts;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve educator contacts from database", e);
        }
    }

    public List<LearningModule> searchAllModulesDB() {
        return searchModulesDB(-1, "All");
    }

    public List<LearningModule> searchModulesByLearningPathDB(String learningPath) {
        List<LearningModule> modules = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "SELECT * FROM modules WHERE learning_path = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, learningPath);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                modules.add(new LearningModule(
                        rs.getInt("mod_id"),
                        -1,
                        rs.getInt("educator_id"),
                        rs.getString("learning_path"),
                        rs.getString("content"),
                        rs.getString("subject")
                ));
            }
            return modules;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve filtered modules from database", e);
        }
    }

    public List<HashMap<String, Object>> searchAllJobProgramsDB(String learningPath, String query) {
        List<HashMap<String, Object>> jobs = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            StringBuilder sql = new StringBuilder("""
                    SELECT j.job_id, j.employer_id, j.learning_path, j.mod_req, j.assessment_req, j.description, j.job_type,
                           a.first_name, a.last_name, a.company
                    FROM jobs j
                    JOIN accounts a ON a.user_id = j.employer_id
                    WHERE 1=1
                    """);

            List<String> parameters = new ArrayList<>();
            if (learningPath != null && !learningPath.isBlank()) {
                sql.append(" AND j.learning_path = ?");
                parameters.add(learningPath.trim());
            }
            if (query != null && !query.isBlank()) {
                sql.append(" AND (j.job_type LIKE ? OR j.description LIKE ? OR a.company LIKE ?)");
                String q = "%" + query.trim() + "%";
                parameters.add(q);
                parameters.add(q);
                parameters.add(q);
            }

            PreparedStatement ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < parameters.size(); i++) {
                ps.setString(i + 1, parameters.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                HashMap<String, Object> row = new HashMap<>();
                row.put("jobId", rs.getInt("job_id"));
                row.put("employerId", rs.getInt("employer_id"));
                row.put("learningPath", rs.getString("learning_path"));
                row.put("modReq", rs.getInt("mod_req"));
                row.put("assessmentReq", rs.getInt("assessment_req"));
                row.put("description", rs.getString("description"));
                row.put("jobType", rs.getString("job_type"));
                row.put("employerName", rs.getString("first_name") + " " + rs.getString("last_name"));
                row.put("company", rs.getString("company"));
                jobs.add(row);
            }
            return jobs;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve job programs from database", e);
        }
    }

    public boolean updateContactInfo(int userId, String email, String phone, String address) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password);
            conn.setAutoCommit(false);

            JsonObject contact = new JsonObject();
            contact.addProperty("email", email == null ? "" : email.trim());
            contact.addProperty("phone", phone == null ? "" : phone.trim());
            contact.addProperty("address", address == null ? "" : address.trim());

            String accountSql = "UPDATE accounts SET contact_info = ? WHERE user_id = ?";
            try (PreparedStatement accountPs = conn.prepareStatement(accountSql)) {
                accountPs.setString(1, gson.toJson(contact));
                accountPs.setInt(2, userId);
                accountPs.executeUpdate();
            }

            if (email != null && !email.isBlank()) {
                String userSql = "UPDATE users SET email = ? WHERE user_id = ?";
                try (PreparedStatement userPs = conn.prepareStatement(userSql)) {
                    userPs.setString(1, email.trim());
                    userPs.setInt(2, userId);
                    userPs.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ignored) {
            }
            throw new DataAccessException("Failed to update contact information", e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    public boolean notifyCounselorOfModuleRequest(int studentId, String requestedPath, String details) {
        Integer counselorId = findCounselorForStudent(studentId);
        if (counselorId == null || counselorId == 0) {
            throw new EntityNotFoundException("No counselor assigned to student #" + studentId);
        }
        String subject = "Module Request";
        String content = "Student #" + studentId + " requested a module for learning path: " + requestedPath + "\nDetails: " + details;
        return addMessage(new Message(studentId, counselorId, subject, content));
    }

    public boolean notifyCounselorOfJobProgramRequest(int studentId, int jobId, String details) {
        Integer counselorId = findCounselorForStudent(studentId);
        if (counselorId == null || counselorId == 0) {
            throw new EntityNotFoundException("No counselor assigned to student #" + studentId);
        }
        String subject = "Job Program Request";
        String content = "Student #" + studentId + " requested job program #" + jobId + "\nDetails: " + details;
        return addMessage(new Message(studentId, counselorId, subject, content));
    }

    public Integer findCounselorForStudent(int studentId) {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "SELECT assigned_counselor FROM accounts WHERE user_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("assigned_counselor");
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find assigned counselor", e);
        }
    }

    public Integer findEducatorForModule(int moduleId) {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "SELECT educator_id FROM modules WHERE mod_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, moduleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("educator_id");
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find educator for module", e);
        }
    }

    public Integer findAvailableCounselorExcluding(int excludedCounselorId) {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = """
                SELECT c.user_id
                FROM accounts c
                LEFT JOIN counselor_students cs ON c.user_id = cs.counselor_id
                WHERE c.acct_type = 'Counselor' AND c.user_id <> ?
                GROUP BY c.user_id
                ORDER BY COUNT(cs.student_id)
                LIMIT 1
                """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, excludedCounselorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("user_id");
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find replacement counselor", e);
        }
    }

    public List<HashMap<String, Object>> listAllUsersSummary() {
        List<HashMap<String, Object>> users = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "SELECT user_id, first_name, last_name, acct_type, contact_info, company FROM accounts ORDER BY user_id";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                HashMap<String, Object> row = new HashMap<>();
                row.put("userId", rs.getInt("user_id"));
                row.put("user_id", rs.getInt("user_id"));
                row.put("name", rs.getString("first_name") + " " + rs.getString("last_name"));
                row.put("acctType", rs.getString("acct_type"));
                row.put("acct_type", rs.getString("acct_type"));
                row.put("company", rs.getString("company"));
                row.put("contactInfo", rs.getString("contact_info"));
                row.put("contact_info", rs.getString("contact_info"));
                users.add(row);
            }
            return users;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to retrieve users", e);
        }
    }

    public List<HashMap<String, Object>> listAllModulesSummary() {
        List<HashMap<String, Object>> modules = new ArrayList<>();
        for (LearningModule module : searchAllModulesDB()) {
            HashMap<String, Object> row = new HashMap<>();
            row.put("modId", module.getModuleID());
            row.put("subject", module.getSubject());
            row.put("learningPath", module.getLearningPath());
            row.put("educatorId", module.getEducatorID());
            modules.add(row);
        }
        return modules;
    }

    public List<HashMap<String, Object>> listAllJobProgramsSummary() {
        return searchAllJobProgramsDB(null, null);
    }

    public boolean deleteModuleById(int moduleId) {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "DELETE FROM modules WHERE mod_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, moduleId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete module", e);
        }
    }

    public boolean deleteJobProgramById(int jobId) {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "DELETE FROM jobs WHERE job_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, jobId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete job program", e);
        }
    }

    public boolean deleteUserCascade(int userId) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password);
            conn.setAutoCommit(false);

            String acctType;
            try (PreparedStatement typePs = conn.prepareStatement("SELECT acct_type FROM accounts WHERE user_id = ?")) {
                typePs.setInt(1, userId);
                ResultSet typeRs = typePs.executeQuery();
                if (!typeRs.next()) {
                    throw new EntityNotFoundException("User #" + userId + " was not found");
                }
                acctType = typeRs.getString("acct_type");
            }

            if ("Employer".equals(acctType)) {
                try (PreparedStatement deleteJobs = conn.prepareStatement("DELETE FROM jobs WHERE employer_id = ?")) {
                    deleteJobs.setInt(1, userId);
                    deleteJobs.executeUpdate();
                }
            }

            if ("Educator".equals(acctType)) {
                try (PreparedStatement deleteProgress = conn.prepareStatement("DELETE FROM module_progress WHERE mod_id IN (SELECT mod_id FROM modules WHERE educator_id = ?)")) {
                    deleteProgress.setInt(1, userId);
                    deleteProgress.executeUpdate();
                }
                try (PreparedStatement deleteModules = conn.prepareStatement("DELETE FROM modules WHERE educator_id = ?")) {
                    deleteModules.setInt(1, userId);
                    deleteModules.executeUpdate();
                }
            }

            if ("Counselor".equals(acctType)) {
                List<Integer> studentsToReassign = new ArrayList<>();
                try (PreparedStatement getStudents = conn.prepareStatement("SELECT student_id FROM counselor_students WHERE counselor_id = ?")) {
                    getStudents.setInt(1, userId);
                    ResultSet rs = getStudents.executeQuery();
                    while (rs.next()) {
                        studentsToReassign.add(rs.getInt("student_id"));
                    }
                }

                try (PreparedStatement deleteAssignments = conn.prepareStatement("DELETE FROM counselor_students WHERE counselor_id = ?")) {
                    deleteAssignments.setInt(1, userId);
                    deleteAssignments.executeUpdate();
                }

                for (Integer studentId : studentsToReassign) {
                    Integer replacement = findAvailableCounselorExcluding(userId);
                    if (replacement == null) {
                        throw new ReassignmentException("No replacement counselor available for student #" + studentId);
                    }

                    try (PreparedStatement assignPs = conn.prepareStatement("INSERT INTO counselor_students (counselor_id, student_id) VALUES (?, ?)")) {
                        assignPs.setInt(1, replacement);
                        assignPs.setInt(2, studentId);
                        assignPs.executeUpdate();
                    }

                    try (PreparedStatement updateAccount = conn.prepareStatement("UPDATE accounts SET assigned_counselor = ? WHERE user_id = ?")) {
                        updateAccount.setInt(1, replacement);
                        updateAccount.setInt(2, studentId);
                        updateAccount.executeUpdate();
                    }
                }
            }

            try (PreparedStatement deleteAccount = conn.prepareStatement("DELETE FROM accounts WHERE user_id = ?")) {
                deleteAccount.setInt(1, userId);
                deleteAccount.executeUpdate();
            }

            try (PreparedStatement deleteUser = conn.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
                deleteUser.setInt(1, userId);
                deleteUser.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ignored) {
            }
            throw new DataAccessException("Failed to delete user", e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }
}
