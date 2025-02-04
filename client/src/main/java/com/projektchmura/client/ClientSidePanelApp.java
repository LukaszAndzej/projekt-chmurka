package com.projektchmura.client;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ClientSidePanelApp extends Application {

    private Stage primaryStage;
    private ClientHandler clientHandler;

    // Sceny
    private Scene loginScene;
    private Scene registerScene;
    private Scene mainScene; // z panelem bocznym

    // Lista plików w chmurze:
    private ListView<String> filesListView;

    // Pasek stanu (na dole okna):
    private Label statusBarLabel;
    private static final ExecutorService uploadExecutor = Executors.newFixedThreadPool(4);

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // Tworzymy ClientHandler (komunikacja z serwerem)
        this.clientHandler = new ClientHandler();

        // Inicjujemy 3 sceny
        loginScene = buildLoginScene();
        registerScene = buildRegisterScene();
        mainScene = buildMainScene();

        // Pokazujemy scenę logowania na start
        primaryStage.setTitle("Chmurka - JavaFX z ulepszonym widokiem");
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    // ------------------------------------
    //  Scena logowania
    // ------------------------------------
    private Scene buildLoginScene() {
        Label titleLabel = new Label("Logowanie");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");

        Label userLabel = new Label("Nazwa użytkownika:");
        TextField userField = new TextField();
        userField.setPromptText("Login");

        Label passLabel = new Label("Hasło:");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Hasło");

        Button loginButton = new Button("Zaloguj");
        Button registerButton = new Button("Rejestracja");

        Label infoLabel = new Label();

        // Logika przycisków
        loginButton.setOnAction(e -> {
            String user = userField.getText();
            String pass = passField.getText();
            String result = clientHandler.loginUser(user, pass);
            if ("OK".equals(result)) {
                infoLabel.setText("Zalogowano pomyślnie.");
                primaryStage.setScene(mainScene);
            } else {
                infoLabel.setText("Błąd logowania!");
            }
        });

        registerButton.setOnAction(e -> {
            userField.clear();
            passField.clear();
            infoLabel.setText("");
            primaryStage.setScene(registerScene);
        });

        VBox vbox = new VBox(10,
                titleLabel, userLabel, userField,
                passLabel, passField,
                loginButton, registerButton,
                infoLabel
        );
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-font-family: 'Arial';");

        return new Scene(vbox, 400, 300);
    }

    // ------------------------------------
    //  Scena rejestracji
    // ------------------------------------
    private Scene buildRegisterScene() {
        Label titleLabel = new Label("Rejestracja");
        titleLabel.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");

        Label userLabel = new Label("Nazwa użytkownika:");
        TextField userField = new TextField();
        userField.setPromptText("Twój login");

        Label passLabel = new Label("Hasło:");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Twoje hasło");

        Button registerButton = new Button("Zarejestruj");
        Button backButton = new Button("Powrót");

        Label infoLabel = new Label();

        registerButton.setOnAction(e -> {
            String user = userField.getText();
            String pass = passField.getText();
            String result = clientHandler.registerUser(user, pass);
            if ("OK".equals(result)) {
                infoLabel.setText("Rejestracja udana! Możesz się zalogować.");
            } else {
                infoLabel.setText("Błąd rejestracji.");
            }
        });

        backButton.setOnAction(e -> {
            userField.clear();
            passField.clear();
            infoLabel.setText("");
            primaryStage.setScene(loginScene);
        });

        VBox vbox = new VBox(10,
                titleLabel, userLabel, userField,
                passLabel, passField,
                registerButton, backButton,
                infoLabel
        );
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-font-family: 'Arial';");
        return new Scene(vbox, 400, 300);
    }

    // ------------------------------------
    //  Scena główna z panelem bocznym
    // ------------------------------------
    private Scene buildMainScene() {
        BorderPane root = new BorderPane();

        // --- Panel boczny (VBox) ---
        VBox sidePanel = new VBox(10);
        sidePanel.setPadding(new Insets(10));
        sidePanel.setStyle("-fx-background-color: #f0f0f0;");

        Label menuLabel = new Label("Menu:");
        menuLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Button filesButton = new Button("Moje pliki");
        Button uploadButton = new Button("Wyślij wiele");
        Button downloadButton = new Button("Pobierz plik");
        Button logoutButton = new Button("Wyloguj");

        sidePanel.getChildren().addAll(
            menuLabel, filesButton, uploadButton, downloadButton, logoutButton
        );

        // --- Obszar centralny: ListView (z customową CellFactory)
        filesListView = new ListView<>();
        filesListView.setPlaceholder(new Label("Brak plików do wyświetlenia"));

        // Prosty przykład custom CellFactory -> emoji/ikona + nazwa pliku:
        filesListView.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String fileName, boolean empty) {
                super.updateItem(fileName, empty);
                if (empty || fileName == null) {
                    setText(null);
                } else {
                    // Dla "nowocześniejszego" efektu dodamy np. emoji: 
                    setText("📄 " + fileName);
                }
            }
        });

        // Możesz też dodać double-click do pobrania:
        filesListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                // Podwójne kliknięcie w plik -> pobieranie
                String selectedFile = filesListView.getSelectionModel().getSelectedItem();
                if (selectedFile != null) {
                    downloadFileByName(selectedFile);
                }
            }
        });

        // Wkładamy ListView w "VBox" z etykietą
        Label centerLabel = new Label("Twoje pliki w chmurze:");
        centerLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        VBox centerBox = new VBox(10, centerLabel, filesListView);
        centerBox.setPadding(new Insets(10));

        root.setLeft(sidePanel);
        root.setCenter(centerBox);

        // --- Pasek stanu (na dole)
        statusBarLabel = new Label("Zalogowano jako...");
        statusBarLabel.setPadding(new Insets(5));
        statusBarLabel.setStyle("-fx-background-color: #e0e0e0;");
        root.setBottom(statusBarLabel);

        // --- Obsługa przycisków w panelu ---
        filesButton.setOnAction(e -> handleListFiles());
        uploadButton.setOnAction(e -> handleUploadMultiple());
        downloadButton.setOnAction(e -> handleDownload());
        logoutButton.setOnAction(e -> handleLogout());

        // Konstruujemy scenę
        Scene scene = new Scene(root, 700, 500);
        scene.getStylesheets().add(getClass().getResource("/styles.css") == null ?
                "" : getClass().getResource("/styles.css").toExternalForm());

        return scene;
    }

    private void handleListFiles() {
        List<String> files = clientHandler.listFiles();
        filesListView.getItems().setAll(files);

        setStatus("Pobrano listę plików (" + files.size() + ")");
    }

    private void handleUploadMultiple() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Wybierz pliki do wysłania");

        List<File> files = chooser.showOpenMultipleDialog(primaryStage);
        if (files != null && !files.isEmpty()) {
            // każdy będzie miał wynik typu String
            List<Future<String>> futures = new ArrayList<>();

            for (File file : files) {
                Callable<String> task = () -> {
                    try {
                        byte[] data = Files.readAllBytes(file.toPath());

                        return clientHandler.uploadFile(file.getName(), data);
                    } catch (Exception e) {
                        return "Błąd: " + e.getMessage();
                    }
                };
                // Wrzucamy zadanie do puli
                Future<String> future = uploadExecutor.submit(task);
                futures.add(future);
            }

            // Teraz (opcjonalnie) czekamy na wyniki
            // Możemy to zrobić asynchronicznie albo synchronicznie
            new Thread(() -> {
                // czekamy w osobnym wątku, by nie blokować UI
                StringBuilder results = new StringBuilder();
                for (Future<String> f : futures) {
                    try {
                        String result = f.get(); // blokuje do momentu zakończenia
                        results.append(result).append("\n");
                    } catch (Exception e) {
                        results.append("Błąd uploadu: ").append(e.getMessage()).append("\n");
                    }
                }
                // Po zebraniu wyników -> wyświetlamy w UI:
                // Ale w wątku javaFX musimy wywołać update przez Platform.runLater(...)
                Platform.runLater(() -> {
                    setStatus("Zakończono wgrywanie wielu plików:\n" + results.toString());
                    //! TODO: do sprawdzenia -> odśwież listę
                    handleListFiles();
                });
            }).start();
        }
    }

    private void handleDownload() {
        // Prosimy użytkownika o nazwę pliku w okienku dialogowym
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Pobierz plik");
        dialog.setHeaderText("Podaj nazwę pliku do pobrania:");
        dialog.setContentText("Nazwa pliku:");
        dialog.showAndWait().ifPresent(this::downloadFileByName);
    }

    private void downloadFileByName(String fileName) {
        // realna logika pobierania
        byte[] data = clientHandler.downloadFile(fileName);
        if (data != null) {
            FileChooser saveChooser = new FileChooser();
            saveChooser.setTitle("Zapisz pobrany plik");
            saveChooser.setInitialFileName(fileName);
            File outFile = saveChooser.showSaveDialog(primaryStage);
            if (outFile != null) {
                try {
                    Files.write(outFile.toPath(), data);
                    setStatus("Pobrano i zapisano: " + outFile.getAbsolutePath());
                } catch (Exception ex) {
                    setStatus("Błąd zapisu: " + ex.getMessage());
                }
            }
        } else {
            setStatus("Błąd pobierania (plik nie istnieje?)");
        }
    }

    private void handleLogout() {
        clientHandler.setSessionId(null);
        filesListView.getItems().clear();
        setStatus("Wylogowano.");
        primaryStage.setScene(loginScene);
    }

    private void setStatus(String msg) {
        statusBarLabel.setText(msg);
    }

    // ------------------ MAIN ------------------
    public static void main(String[] args) {
        launch(args);
    }
}
