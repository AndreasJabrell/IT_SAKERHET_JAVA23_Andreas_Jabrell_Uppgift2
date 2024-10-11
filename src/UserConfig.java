import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.lang.System.out;

public class UserConfig {


    //hash password
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    //save new timecapsule to Database
    public static boolean saveToDB(String name, String password, String email) {
        String hashedPassword = hashPassword(password);

        String sql = "INSERT INTO timecapsuleUserdata (name, email, password) VALUES (?,?,?)";

        try {
            Connection connection = Database.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, hashedPassword);
            ps.executeUpdate();

            out.println(sql);
            out.println("New user added with info " + name + " " + email + " " + hashedPassword);

        } catch (SQLException e) {
            out.println("Something went wrong" + e.getMessage());
        }
        return true;
    }

    public static boolean loginUser(String email, String password) {
        String hashedPassword = hashPassword(password);  // Hasha det inmatade lösenordet
        String sql = "SELECT * FROM timecapsuleUserdata WHERE email = ?";

        try {
            Connection connection = Database.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);  // Mata in e-post i frågan
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedPasswordHash = rs.getString("password");  // Hämta den hashade lösenordet från databasen
                if (hashedPassword.equals(storedPasswordHash)) {
                    out.println("User logged in successfully.");
                } else {
                    out.println("WRONG PASSWORD, TRY AGAIN");
                    return false;
                }

                return storedPasswordHash.equals(hashedPassword);  // Jämför hasharna
            } else {
                out.println("No user found with this email.");
                return false;
            }

        } catch (SQLException e) {
            out.println("Login failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean saveTimeCapsule(String email, String encryptedMessage) {
        String sql = "INSERT INTO timecapsule (userEmail, message) VALUES (?, ?);";

        try {
            Connection connection = Database.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, encryptedMessage);
            ps.executeUpdate();

            out.println("Success: Time capsule saved for email " + email);
        } catch (SQLException e) {
            out.println("Something went wrong: " + e.getMessage());
            return false;
        }
        return true;
    }

    public static String displayTimecapsule(String email) {
        String sql = "SELECT message FROM timecapsule WHERE userEmail = ?";
        StringBuilder messages = new StringBuilder();

        try {
            Connection connection = Database.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, email);  // Mata in e-post i frågan
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                messages.append(rs.getString("message")).append(";");
            }

            rs.close();
            ps.close();
            connection.close();

        } catch (SQLException e) {
            out.println("We didn't make it!!! " + e.getMessage());
            return null;
        }

        return messages.toString(); // Returnera meddelandena som en sträng
    }
}

