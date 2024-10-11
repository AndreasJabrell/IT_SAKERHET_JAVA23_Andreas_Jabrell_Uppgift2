import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class Main {
    private static final int PORT = 1234;
    private static final String SECRET = "mySecretKey";

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servern är igång och lyssnar på port " + PORT);

        //UserConfig.saveToDB("admin", "password", "admin");

        try {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("En klient har anslutit");
                try {
                    handleClient(socket);
                } catch (IOException e) {
                    System.out.println(e);
                    socket.close();
                }
            }
        } finally {
            serverSocket.close();
        }
    }

    private static void handleClient(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // First row from each page in the client to decide where to go
        String requestType = in.readLine();

        if ("login".equalsIgnoreCase(requestType)) {
            handleLogin(in, out);
        } else if ("register".equalsIgnoreCase(requestType)) {
            handleRegister(in, out);
        } else if ("registerTimecapsule".equalsIgnoreCase(requestType)) {
            handleRegisterTimecapsule(in, out);
        } else if ("displayTimecapsule".equalsIgnoreCase(requestType)) {
            handleDisplayTimecapsule(in, out);
        } else {
            out.println("Error Invalid request type!");
        }
        socket.close();
    }
    //handle login, generate a JWT Token
    private static void handleLogin(BufferedReader in, PrintWriter out) throws IOException {
        String user = in.readLine();
        String password = in.readLine();

        if (UserConfig.loginUser(user, password)) {
            String token = generateJWT(user);
            System.out.println("Success " + token + user);

            out.println("Success " + token + " " + user);
        } else {
            out.println("Error Invalid credentials!");
        }
    }
    //handle registration of new users, save info to Database
    private static void handleRegister(BufferedReader in, PrintWriter out) throws IOException {
        String newUser = in.readLine();
        String newPassword = in.readLine();
        String email = in.readLine();

        if (UserConfig.saveToDB(newUser, newPassword, email)) {
            out.println("Success User registered! Welcome " + newUser);
        } else {
            out.println("Error Registration failed!");
        }
    }
    //handling of registration of new timecapsules/messages
    private static void handleRegisterTimecapsule(BufferedReader in, PrintWriter out) throws IOException {
        String token = in.readLine();
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        String email = in.readLine();
        String message = in.readLine();

        System.out.println("Server Received email: " + email); // Logging to make sure not null
        System.out.println("Server Received message: " + message); // Logging to make sure not null

        if (verifyJWT(actualToken) && UserConfig.saveTimeCapsule(email, message)) {
            out.println("Success timecapsule registered to " + email);
        } else {
            out.println("Error timecapsule registration failed for " + email);
        }
    }
    //handling display of saved timecapsules. A bit of problem with this one, lot of help from ChatGPT to almost
    //get it right
    private static void handleDisplayTimecapsule(BufferedReader in, PrintWriter out) throws IOException {
        String token = in.readLine();
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = in.readLine();

        if (verifyJWT(actualToken)) {
            String messages = String.valueOf(UserConfig.displayTimecapsule(email));
            if (messages != null && !messages.isEmpty()) {
                out.println("Success " + messages);
            } else {
                out.println("Error No timecapsules found for this email");
            }
        } else {
            out.println("Error Invalid token");
        }
    }

    //generate JWT Token
    private static String generateJWT(String user) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        return JWT.create()
                .withIssuer("auth0")
                .withClaim("user", user)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600 * 1000)) // 1 timme giltig
                .sign(algorithm);
    }
    //verify Token
    public static boolean verifyJWT(String token) {
        try {
            System.out.println("Token i början: " + token);
            Algorithm algorithm = Algorithm.HMAC256(SECRET);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("auth0")
                    .build();
            DecodedJWT jwt = verifier.verify(token);
            return true;
        } catch (JWTVerificationException exception) {
            System.out.println("JWT Verification failed: " + exception.getMessage());
            System.out.println("Received token: " + token);
            return false;
        }
    }
}
