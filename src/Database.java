import com.mysql.cj.jdbc.MysqlDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class Database {

    //Database config
    static String url = "localhost";
    static int port = 3306;
    static String database = "userdata";
    static String userName = "root";
    static String password = "Andreas1";

    //Private variables
    private static Database db;
    private MysqlDataSource dataSource;


    private Database() {
        initializeDataSource();
    }

    public static Connection getConnection() {
        if (db == null) {
            db = new Database();
            db.initializeDataSource();
        }
        return db.createConnection();
    }

    public static void PrintSQLException(SQLException sqle) {
        PrintSQLException(sqle, false);
    }

    public static void PrintSQLException(SQLException sqle, boolean printStackTrace) {
        while (sqle != null) {
            System.out.println("\n----SQLException Caught-----\n");
            System.out.println("SQLState: " + sqle.getSQLState());
            System.out.println("Errorcode: " + sqle.getErrorCode());
            System.out.println("Message: " + sqle.getMessage());
            if (printStackTrace) sqle.printStackTrace();
            sqle = sqle.getNextException();
        }
    }

    private void initializeDataSource() {
        // try {
        dataSource = new MysqlDataSource();
        dataSource.setUser(userName);
        dataSource.setPassword(password);
        dataSource.setURL("jdbc:mysql://" + url + ":" + port + "/" + database + "?serverTimezone=UTC");
/*        }catch(SQLException ex) {

        }*/
    }

    private Connection createConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException ex) {
            PrintSQLException(ex);
            return null;
        }
    }

}
