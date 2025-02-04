package com.projektchmura.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class ServerWorker extends Thread {

    private Socket clientSocket;

    public ServerWorker(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())
        ) {
            while (true) {
                Object request = in.readObject();
                if (request instanceof String) {
                    String command = (String) request;

                    switch (command) {
                        case "REGISTER":
                            handleRegister(in, out);
                            break;
                        case "LOGIN":
                            handleLogin(in, out);
                            break;
                        case "UPLOAD":
                            // obsługa upload przez Callable
                            handleUploadCallable(in, out);
                            break;
                        case "DOWNLOAD":
                            handleDownload(in, out);
                            break;
                        case "LIST_FILES":
                            handleListFiles(in, out);
                            break;
                        default:
                            out.writeObject("Nieznana komenda: " + command);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // e.printStackTrace(); // Nie zaśmiecamy logów
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // -------------------- REJESTRACJA ------------------------
    private void handleRegister(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        String username = (String) in.readObject();
        String password = (String) in.readObject();

        boolean success = DatabaseManager.registerUser(username, password);
        if (success) {
            out.writeObject("REGISTER_OK");
        } else {
            out.writeObject("REGISTER_FAIL");
        }
    }

    // -------------------- LOGOWANIE ------------------------
    private void handleLogin(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        String username = (String) in.readObject();
        String password = (String) in.readObject();

        boolean success = DatabaseManager.loginUser(username, password);
        if (success) {
            out.writeObject("LOGIN_OK");

            // Tworzymy sessionId
            String sessionId = ServerMain.createSessionForUser(username);
            // Odsyłamy do klienta (będzie pamiętać sessionId)
            out.writeObject(sessionId);
        } else {
            out.writeObject("LOGIN_FAIL");
        }
    }

    // -------------------- UPLOAD (Callable) ------------------------
    /**
     * Metoda obsługuje upload pliku w osobnym wątku (Callable), zwracając wynik do klienta.
     */
    private void handleUploadCallable(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        // Odbieramy sessionId, nazwę pliku, potem bajty
        String sessionId = (String) in.readObject();
        String fileName = (String) in.readObject();
        byte[] fileData = (byte[]) in.readObject();

        // Sprawdzamy, do jakiego usera należy sesja
        String user = ServerMain.getUserForSession(sessionId);
        if (user == null) {
            out.writeObject("UPLOAD_FAIL - niepoprawna sesja!");
            return;
        }

        // Tworzymy zadanie Callable, które zapisze plik i zwróci "UPLOAD_OK" lub komunikat błędu
        Callable<String> uploadTask = () -> {
            try {
                DatabaseManager.saveFile(user, fileName, fileData);
                return "UPLOAD_OK";
            } catch (SQLException e) {
                e.printStackTrace();
                return "UPLOAD_FAIL - błąd bazy: " + e.getMessage();
            }
        };

        // Zgłaszamy zadanie do puli wątków w ServerMain
        Future<String> future = ServerMain.EXECUTOR.submit(uploadTask);

        // Oczekujemy na wynik (blokująco)
        try {
            String result = future.get();
            out.writeObject(result);
        } catch (Exception e) {
            e.printStackTrace();
            out.writeObject("UPLOAD_FAIL - " + e.getMessage());
        }
    }

    // -------------------- DOWNLOAD ------------------------
    private void handleDownload(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        String sessionId = (String) in.readObject();
        String fileName = (String) in.readObject();

        String user = ServerMain.getUserForSession(sessionId);
        if (user == null) {
            out.writeObject(null); // sygnalizujemy brak sesji
            return;
        }

        try {
            byte[] data = DatabaseManager.getFileContent(user, fileName);
            if (data == null) {
                out.writeObject(null);
            } else {
                out.writeObject(data);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            out.writeObject(null);
        }
    }

    // -------------------- LIST_FILES ------------------------
    private void handleListFiles(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        String sessionId = (String) in.readObject();

        String user = ServerMain.getUserForSession(sessionId);
        if (user == null) {
            out.writeObject(null); // brak sesji
            return;
        }

        try {
            List<String> files = DatabaseManager.listFiles(user);
            out.writeObject(files);
        } catch (SQLException e) {
            e.printStackTrace();
            out.writeObject(null);
        }
    }
}
