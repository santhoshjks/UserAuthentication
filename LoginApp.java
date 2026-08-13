import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginApp {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/logsign?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "mysql";

    public static void main(String[] args) throws IOException {
        Database.initialize();
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new HomeHandler());
        server.createContext("/login", new LoginPageHandler());
        server.createContext("/signup", new SignupPageHandler());
        server.createContext("/forgot", new ForgotPageHandler());
        server.createContext("/authenticate", new AuthenticateHandler());
        server.createContext("/register", new RegisterHandler());
        server.createContext("/reset", new ResetPasswordHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server running at http://localhost:8000");
    }

    private static String pageTemplate(String title, String body) {
        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>" + title + "</title>" +
                "<style>body { font-family: 'Inter', system-ui, sans-serif; background: linear-gradient(135deg, #121b2b, #1a2943); color: #eef2ff; margin: 0; padding: 20px; min-height: 100vh; }" +
                " .container { max-width: 420px; margin: 40px auto; background: rgba(18, 27, 43, 0.96); padding: 28px; border-radius: 20px; box-shadow: 0 28px 80px rgba(0,0,0,0.22); border: 1px solid rgba(255,255,255,0.08); transform: translateY(0); animation: floatIn 0.7s ease-out both; }" +
                " input[type=text], input[type=password] { width: 100%; padding: 14px; margin: 10px 0; box-sizing: border-box; border: 1px solid rgba(255,255,255,0.15); background: rgba(255,255,255,0.06); color: #eef2ff; border-radius: 12px; transition: border-color 0.25s ease, box-shadow 0.25s ease, background 0.25s ease; }" +
                " input[type=text]::placeholder, input[type=password]::placeholder { color: rgba(238,242,255,0.55); }" +
                " input[type=text]:focus, input[type=password]:focus { outline: none; border-color: #6c9cff; background: rgba(255,255,255,0.12); box-shadow: 0 0 0 4px rgba(108,156,255,0.16); }" +
                " button { width: 100%; padding: 14px; margin-top: 18px; background: linear-gradient(135deg, #5f7dff, #2b9dff); color: white; border: none; border-radius: 12px; cursor: pointer; font-weight: 600; letter-spacing: 0.02em; transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease; }" +
                " button:hover { opacity: 0.98; transform: translateY(-1px); box-shadow: 0 18px 40px rgba(43,157,255,0.24); }" +
                " button:active { transform: translateY(0); }" +
                " label { display: block; margin-top: 16px; color: rgba(238,242,255,0.85); font-size: 0.96rem; }" +
                " a { color: #7dbbff; text-decoration: none; }" +
                " a:hover { text-decoration: underline; }" +
                " h2 { margin-top: 0; color: #ffffff; animation: fadeIn 0.8s ease both; }" +
                " p, label, input, button { animation: fadeIn 1s ease both; }" +
                " p { color: rgba(238,242,255,0.8); }" +
                " .subtitle { margin-bottom: 24px; color: rgba(238,242,255,0.72); }" +
                " .form-footer { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-top: 20px; flex-wrap: wrap; }" +
                " .forgot-link { color: #a3c9ff; font-size: 0.95rem; }" +
                " .forgot-link:hover { color: #d4e7ff; }" +
                " @keyframes floatIn { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0); } }" +
                " @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }" +
                "</style>" +
                "</head><body>" +
                "<div class=\"container\">" +
                body +
                "</div></body></html>";
    }

    private static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            redirect(exchange, "/login");
        }
    }

    private static class LoginPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = pageTemplate("Login", "<h2>Login</h2>" +
                    "<form method=\"POST\" action=\"/authenticate\">" +
                    "<label>Username</label><input type=\"text\" name=\"username\" required>" +
                    "<label>Password</label><input type=\"password\" name=\"password\" required>" +
                    "<button type=\"submit\">Sign In</button>" +
                    "</form>" +
                    "<div class=\"form-footer\"><span>Don't have an account? <a href=\"/signup\">Sign up</a></span>" +
                    "<a class=\"forgot-link\" href=\"/forgot\">Forgot password?</a></div>");
            sendHtml(exchange, html);
        }
    }

    private static class SignupPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = pageTemplate("Sign Up", "<h2>Sign Up</h2>" +
                    "<form method=\"POST\" action=\"/register\">" +
                    "<label>Username</label><input type=\"text\" name=\"username\" required>" +
                    "<label>Email</label><input type=\"text\" name=\"email\" required>" +
                    "<label>Password</label><input type=\"password\" name=\"password\" required>" +
                    "<label>Confirm Password</label><input type=\"password\" name=\"confirmPassword\" required>" +
                    "<button type=\"submit\">Create Account</button>" +
                    "</form>" +
                    "<p>Already have an account? <a href=\"/login\">Log in</a></p>");
            sendHtml(exchange, html);
        }
    }

    private static class ForgotPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = pageTemplate("Forgot Password", "<h2>Forgot Password</h2>" +
                    "<p class=\"subtitle\">Enter your username and a new password to reset your account.</p>" +
                    "<form method=\"POST\" action=\"/reset\">" +
                    "<label>Username</label><input type=\"text\" name=\"username\" required>" +
                    "<label>New Password</label><input type=\"password\" name=\"password\" required>" +
                    "<label>Confirm Password</label><input type=\"password\" name=\"confirmPassword\" required>" +
                    "<button type=\"submit\">Reset Password</button>" +
                    "</form>" +
                    "<p><a href=\"/login\">Back to login</a></p>");
            sendHtml(exchange, html);
        }
    }

    private static class AuthenticateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                redirect(exchange, "/login");
                return;
            }

            Map<String, String> data = parseForm(exchange);
            String username = data.getOrDefault("username", "").trim();
            String password = data.getOrDefault("password", "").trim();

            if (username.isEmpty() || password.isEmpty()) {
                sendMessage(exchange, "Login failed", "Please enter both username and password.", "/login");
                return;
            }

            User storedUser;
            try {
                storedUser = Database.findUserByUsername(username);
            } catch (SQLException e) {
                sendMessage(exchange, "Login failed", "Unable to access user data. Please try again later.", "/login");
                return;
            }

            if (storedUser != null && storedUser.password.equals(password)) {
                String body = "<h2>Welcome back, " + escape(username) + "!</h2>" +
                        "<p>You are now logged in.</p>" +
                        "<p><a href=\"/login\">Sign in again</a></p>";
                sendHtml(exchange, pageTemplate("Welcome", body));
            } else {
                sendMessage(exchange, "Login failed", "Invalid username or password.", "/login");
            }
        }
    }

    private static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                redirect(exchange, "/signup");
                return;
            }

            Map<String, String> data = parseForm(exchange);
            String username = data.getOrDefault("username", "").trim();
            String email = data.getOrDefault("email", "").trim();
            String password = data.getOrDefault("password", "").trim();
            String confirmPassword = data.getOrDefault("confirmPassword", "").trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                sendMessage(exchange, "Sign Up failed", "Please enter a username, email, password, and confirmation.", "/signup");
                return;
            }

            if (!password.equals(confirmPassword)) {
                sendMessage(exchange, "Sign Up failed", "Passwords do not match. Please try again.", "/signup");
                return;
            }

            try {
                if (Database.findUserByUsername(username) != null) {
                    sendMessage(exchange, "Sign Up failed", "That username is already taken.", "/signup");
                    return;
                }

                Database.insertUser(new User(username, password, email));
                String body = "<h2>Account created</h2>" +
                        "<p>Your account has been created successfully.</p>" +
                        "<p><strong>Email:</strong> " + escape(email) + "</p>" +
                        "<p><a href=\"/login\">Go to login</a></p>";
                sendHtml(exchange, pageTemplate("Success", body));
            } catch (SQLException e) {
                sendMessage(exchange, "Sign Up failed", "Unable to save your account. Please try again later.", "/signup");
            }
        }
    }

    private static class ResetPasswordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                redirect(exchange, "/forgot");
                return;
            }

            Map<String, String> data = parseForm(exchange);
            String username = data.getOrDefault("username", "").trim();
            String password = data.getOrDefault("password", "").trim();
            String confirmPassword = data.getOrDefault("confirmPassword", "").trim();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                sendMessage(exchange, "Reset failed", "Please enter your username, new password, and confirmation.", "/forgot");
                return;
            }

            if (!password.equals(confirmPassword)) {
                sendMessage(exchange, "Reset failed", "Passwords do not match. Please try again.", "/forgot");
                return;
            }

            try {
                User storedUser = Database.findUserByUsername(username);
                if (storedUser != null) {
                    Database.updatePassword(username, password);
                    sendMessage(exchange, "Reset Complete", "Your password has been reset successfully.", "/login");
                } else {
                    sendMessage(exchange, "Reset failed", "No matching account was found for that username.", "/forgot");
                }
            } catch (SQLException e) {
                sendMessage(exchange, "Reset failed", "Unable to update password at this time. Please try again later.", "/forgot");
            }
        }
    }

    private static void sendMessage(HttpExchange exchange, String title, String message, String link) throws IOException {
        String html = pageTemplate(title, "<h2>" + escape(title) + "</h2>" +
                "<p>" + escape(message) + "</p>" +
                "<p><a href=\"" + link + "\">Return</a></p>");
        sendHtml(exchange, html);
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private static void sendHtml(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return HttpUtils.parseForm(raw);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static class Database {
        private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
        private static final String DB_URL_BASE = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        static void initialize() {
            try {
                Class.forName(JDBC_DRIVER);
                try (Connection conn = DriverManager.getConnection(DB_URL_BASE, DB_USER, DB_PASSWORD);
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS logsign CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                }
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                            "username VARCHAR(64) PRIMARY KEY, " +
                            "email VARCHAR(255) NOT NULL, " +
                            "password VARCHAR(255) NOT NULL" +
                            ")");
                }
            } catch (ClassNotFoundException | SQLException e) {
                throw new RuntimeException("Unable to initialize database: " + e.getMessage(), e);
            }
        }

        static User findUserByUsername(String username) throws SQLException {
            String sql = "SELECT username, email, password FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new User(rs.getString("username"), rs.getString("password"), rs.getString("email"));
                    }
                    return null;
                }
            }
        }

        static void insertUser(User user) throws SQLException {
            String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, user.username);
                stmt.setString(2, user.email);
                stmt.setString(3, user.password);
                stmt.executeUpdate();
            }
        }

        static void updatePassword(String username, String password) throws SQLException {
            String sql = "UPDATE users SET password = ? WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, password);
                stmt.setString(2, username);
                stmt.executeUpdate();
            }
        }
    }
}

class User {
    final String username;
    final String password;
    final String email;

    User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
}

class HttpUtils {
    static Map<String, String> parseForm(String raw) {
        Map<String, String> result = new ConcurrentHashMap<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        String[] pairs = raw.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                result.put(urlDecode(parts[0]), urlDecode(parts[1]));
            }
        }
        return result;
    }

    private static String urlDecode(String value) {
        try {
            return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }
}
