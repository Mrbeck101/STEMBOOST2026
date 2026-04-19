package DatabaseController;

import OtherComponents.*;
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

    public boolean addUser(String email, String password, String fname, String lname, String acctType) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;

        try {
            conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password);
            conn.setAutoCommit(false);

            String sql_user = "INSERT INTO users (email, password) VALUES (?, ?)";
            ps = conn.prepareStatement(sql_user);
            ps.setString(1, email);
            ps.setString(2, CryptoUtil.encrypt(password));
            ps.executeUpdate();

            String sql_acct = "INSERT INTO accounts (first_name, last_name, acct_type, contact_info) VALUES (?, ?, ?, ?)";
            ps2 = conn.prepareStatement(sql_acct);
            ps2.setString(1, fname);
            ps2.setString(2, lname);
            ps2.setString(3, acctType);
            ps2.setString(4, "{\"email\":\"" + email + "\",\"phone\":\"\",\"address\":\"\"}");
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

            if (!rs.next()) new Pair<>(-1, "Not Found");

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
            } else {
                sql = "SELECT * FROM modules WHERE educator_id=?";
            }

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
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
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, mod.getModuleID());
            ps.setString(2, mod.getLearningPath());
            ps.setString(3, mod.getContent());
            ps.setString(4, mod.getSubject());

            return ps.executeUpdate() > 0;
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
            String sql = "INSERT INTO jobs (employer_id, learning_path, mod_req, assessment_req, description, job_type) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, job.getEmployerID());
            ps.setString(2, job.getPreferredLearningPath());
            ps.setInt(3, job.getModRequired());
            ps.setInt(4, job.getAssessmentRequired());
            ps.setString(5, job.getDescription());
            ps.setString(5, job.getJobType());


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
            String sql = "UPDATE assassment_progress SET grade = ? WHERE student_id = ? AND assessment_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, grade);
            ps.setInt(2, studentID);
            ps.setInt(3, assessmentID);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add modules to database", e);
        }
    }

    public boolean updateJobProgram(JobProgram job){
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            String sql = "UPDATE jobs SET learning_path = ?, mod_req = ?, assessment_req = ?, description = ?, job_type = ? WHERE job_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(6, job.getEmployerID());
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
                      p.assessment_id,
                      p.grade,
                      a.learning_path,
                      a.associated_mod,
                      a.content
                    FROM assessment_progress p
                    JOIN assessments a
                      ON p.assessment_id = a.assessment_id
                    WHERE a.associated_mod = ?;
                """;
            }
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int assessmentID = rs.getInt("assessment_id");
                int grade = rs.getInt("assessment_grade");
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
}






