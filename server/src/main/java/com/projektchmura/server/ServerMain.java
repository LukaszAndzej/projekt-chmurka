package com.projektchmura.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final int PORT = 9000;

    // Mapa: sessionId -> username
    // Używam ConcurrentHashMap, bo może być wielu klientów naraz
    public static final Map<String, String> SESSIONS = new ConcurrentHashMap<>();

    // Dodajemy pule wątków do obsługi zadań zwracających wartość (Callable)
    public static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        System.out.println("Start serwera...");
        try {
            DatabaseManager.init();
            System.out.println("Baza zainicjalizowana.");

            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Serwer nasłuchuje na porcie: " + PORT);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Nowe połączenie od " + clientSocket.getInetAddress());
                    // Tworzymy nowy wątek do obsługi klienta
                    ServerWorker worker = new ServerWorker(clientSocket);
                    worker.start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Błąd SQL podczas inicjalizacji bazy danych.");
            e.printStackTrace();
        } finally {
            // Zamykamy bazę
            DatabaseManager.close();
            // Zamykamy pula wątków
            EXECUTOR.shutdown();
        }
    }

    /**
     * Generuje nowy sessionId i zapisuje do mapy SESSIONS,
     * łącząc go z danym username.
     */
    public static String createSessionForUser(String username) {
        // np. losowy UUID
        String sessionId = UUID.randomUUID().toString();
        SESSIONS.put(sessionId, username);
        return sessionId;
    }

    /**
     * Sprawdza, do kogo należy sessionId (lub zwraca null, jeśli nie ma go w mapie).
     */
    public static String getUserForSession(String sessionId) {
        return SESSIONS.get(sessionId);
    }
}
