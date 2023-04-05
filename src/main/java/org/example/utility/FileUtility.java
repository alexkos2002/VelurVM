package org.example.utility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileUtility {

    public static String readStringFromFile(String baseCodeFilePath) {
        String baseCode = null;
        try {
            baseCode = Files.readString(Path.of(baseCodeFilePath));
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return baseCode;
    }

    public static void writeStringToFile(String baseCodeFilePath, String data) {
        String baseCode = null;
        try {
            Files.writeString(Path.of(baseCodeFilePath), data, StandardOpenOption.WRITE);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


}
