package com.projektchmura.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler {

    private String serverHost;
    private int serverPort;

    // Po zalogowaniu przechowujemy sessionId
    private String sessionId;

    public ClientHandler() {
        this.serverHost = "192.168.0.25"; // lub inny IP serwera
        this.serverPort = 9000;
    }

    public String registerUser(String username, String password) {
        try (Socket socket = new Socket(serverHost, serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("REGISTER");
            out.writeObject(username);
            out.writeObject(password);

            Object response = in.readObject();
            if (response instanceof String) {
                String respStr = (String) response;
                if ("REGISTER_OK".equals(respStr)) {
                    return "OK";
                } else {
                    return "FAIL";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ERROR";
    }

    public String loginUser(String username, String password) {
        try (Socket socket = new Socket(serverHost, serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("LOGIN");
            out.writeObject(username);
            out.writeObject(password);

            Object response = in.readObject();
            if (response instanceof String) {
                String respStr = (String) response;
                if ("LOGIN_OK".equals(respStr)) {
                    // Odczytaj kolejny obiekt: sessionId
                    Object sessionObj = in.readObject();
                    if (sessionObj instanceof String) {
                        this.sessionId = (String) sessionObj;
                        System.out.println("Otrzymano sessionId: " + this.sessionId);
                    }
                    return "OK";
                } else {
                    return "FAIL";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ERROR";
    }

    /**
     * Upload pliku z wczytanymi bajtami. 
     * Zwraca np. "UPLOAD_OK" lub komunikat błędu.
     */
    public String uploadFile(String fileName, byte[] fileData) {
        if (sessionId == null) {
            return "Brak sesji!";
        }
        try (Socket socket = new Socket(serverHost, serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("UPLOAD");
            // Wysyłamy najpierw sessionId
            out.writeObject(sessionId);
            // Następnie nazwę pliku i dane
            out.writeObject(fileName);
            out.writeObject(fileData);

            Object response = in.readObject();
            if (response instanceof String) {
                return (String) response;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "UPLOAD_FAIL";
    }

    public List<String> listFiles() {
        List<String> result = new ArrayList<>();
        if (sessionId == null) {
            return result; // pusta lista
        }
        try (Socket socket = new Socket(serverHost, serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("LIST_FILES");
            out.writeObject(sessionId);

            Object response = in.readObject();
            if (response instanceof List<?>) {
                // Bezpieczne rzutowanie
                List<?> list = (List<?>) response;
                for (Object o : list) {
                    if (o instanceof String) {
                        result.add((String) o);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public byte[] downloadFile(String fileName) {
        if (sessionId == null) {
            return null;
        }
        try (Socket socket = new Socket(serverHost, serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("DOWNLOAD");
            out.writeObject(sessionId);
            out.writeObject(fileName);

            Object response = in.readObject();
            if (response instanceof byte[]) {
                return (byte[]) response;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
