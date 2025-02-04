package com.projektchmura.server;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static Connection conn;

    /**
     * Inicjalizacja połączenia z bazą danych H2.
     */
    public static void init() throws SQLException {
        // Wersja w pamięci:
        // String url = "jdbc:h2:mem:chmura;DB_CLOSE_DELAY=-1";
        Path dbPath = Paths.get(System.getProperty("user.dir"), "database", "db");
        String absolutePath = dbPath.toAbsolutePath().toString();
        String url = "jdbc:h2:file:" + absolutePath;
        conn = DriverManager.getConnection(url, "sa", "");

        System.out.println("Połączono z bazą danych H2.");

        // Wykonaj skrypt schema.sql - tworzenie tabel (jeśli nie istnieją)
        runSchemaScript();
    }

    private static void runSchemaScript() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Tabela users
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                                    "username VARCHAR(50) PRIMARY KEY, " +
                                    "password VARCHAR(255) NOT NULL)";
            stmt.execute(createUsersTable);

            // Tabela files
            String createFilesTable = "CREATE TABLE IF NOT EXISTS files (" +
                                    "file_id IDENTITY PRIMARY KEY, " +
                                    "owner VARCHAR(50) NOT NULL, " +
                                    "filename VARCHAR(255) NOT NULL, " +
                                    "content BLOB NOT NULL)";
            stmt.execute(createFilesTable);
        }
    }

    public static void close() {
        try {
            if (conn != null) {
                conn.close();
                System.out.println("Zamknięto połączenie z bazą danych.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Rejestracja użytkownika w bazie danych (zwraca true, jeśli się uda).
     */
    public static boolean registerUser(String username, String password) {
        if (username == null || password == null) return false;

        // Sprawdź, czy użytkownik już istnieje
        if (userExists(username)) {
            return false;
        }

        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Logowanie użytkownika (zwraca true, jeśli poprawne dane).
     */
    public static boolean loginUser(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    return dbPassword.equals(password);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sprawdza, czy użytkownik o podanej nazwie istnieje w bazie.
     */
    private static boolean userExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // jeśli jest jakikolwiek wynik, to user istnieje
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Zapisuje plik w bazie, powiązany z danym właścicielem.
     */
    public static void saveFile(String owner, String fileName, byte[] data) throws SQLException {
        String sql = "INSERT INTO files (owner, filename, content) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner);
            ps.setString(2, fileName);
            ps.setBytes(3, data);
            ps.executeUpdate();
        }
    }

    /**
     * Zwraca listę nazw plików należących do danego właściciela.
     */
    public static List<String> listFiles(String owner) throws SQLException {
        List<String> result = new ArrayList<>();
        String sql = "SELECT filename FROM files WHERE owner = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("filename"));
                }
            }
        }
        return result;
    }

    /**
     * Zwraca zawartość pliku (bajty) należącego do 'owner' o nazwie 'fileName'.
     * Jeśli nie ma takiego pliku, zwraca null.
     */
    public static byte[] getFileContent(String owner, String fileName) throws SQLException {
        String sql = "SELECT content FROM files WHERE owner = ? AND filename = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner);
            ps.setString(2, fileName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("content");
                }
            }
        }
        return null;
    }
}
