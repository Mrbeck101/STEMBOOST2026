package DatabaseController;

import OtherComponents.Message;

import java.sql.*;
import java.util.ArrayList;
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

    public String searchUserDB(String data, String queryType) {
        try (Connection conn = DriverManager.getConnection(this.ip + this.dbName, this.user, this.password)) {
            if (queryType.equals("password")) {
                String sql = "SELECT password FROM users WHERE email=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, data);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) return "Not Found";

                return CryptoUtil.decrypt(rs.getString("password"));
            }

            return "Not Found";
        } catch (Exception e) {
            e.printStackTrace();
            return "Not Found";
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

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Empty List Returned");
            return messages;
        }
    }

    public boolean addMessage(Message msg) throws SQLException {
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

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Empty List Returned");
            return false;
        }
    }
}




