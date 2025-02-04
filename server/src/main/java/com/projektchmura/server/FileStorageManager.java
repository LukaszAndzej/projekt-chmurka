package com.projektchmura.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileStorageManager {

    private static final String STORAGE_DIR = "uploads";

    public static void init() {
        // Tworzymy katalog do przechowywania plików (jeśli nie istnieje)
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("Utworzono katalog: " + dir.getAbsolutePath());
            }
        }
    }

    /**
     * Zapis pliku na dysku
     */
    public static void saveFile(String fileName, byte[] data) throws IOException {
        Files.write(Paths.get(STORAGE_DIR, fileName), data);
        System.out.println("Zapisano plik: " + fileName);
    }

    /**
     * Odczyt pliku z dysku
     */
    public static byte[] readFile(String fileName) throws IOException {
        return Files.readAllBytes(Paths.get(STORAGE_DIR, fileName));
    }
}
