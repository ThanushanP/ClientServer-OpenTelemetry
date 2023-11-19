package com.example;

import java.io.*;
import java.util.Random;

public class random {
    public static void main(String[] args) {
        String outputPath = "src/main/resources/Input/";
        int numFiles = 20;
        long minSizeBytes = 5 * 1024; // 5KB
        long maxSizeBytes = 100 * 1024 * 1024; // 100MB

        File outputFolder = new File(outputPath);
        if (!outputFolder.exists() || !outputFolder.isDirectory()) {
            outputFolder.mkdirs();
        }

        Random random = new Random();

        for (int i = 0; i < numFiles; i++) {
            String fileName = "random_file_" + i + ".txt";
            long fileSize = minSizeBytes + (long) (random.nextDouble() * (maxSizeBytes - minSizeBytes));

            createRandomTxtFile(outputPath + fileName, fileSize);
        }
    }

    public static void createRandomTxtFile(String filePath, long sizeBytes) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            Random random = new Random();

            for (long writtenBytes = 0; writtenBytes < sizeBytes; ) {
                int randomNumber = random.nextInt();
                writer.println(randomNumber);
                writtenBytes += Integer.BYTES; // Size of an integer in bytes
            }

            System.out.println("Generated file: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

