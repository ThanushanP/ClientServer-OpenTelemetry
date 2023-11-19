package com.example;

import java.io.*;
import java.net.*;
import java.util.zip.Deflater;
import java.util.zip.CRC32;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

public class client {
        private static int numOfFiles =5; //Max 20

    public client(){
        String serverAddress = "127.0.0.1"; // Replace with the server's IP address or hostname
        int serverPort = 12345; // Replace with the server's port
        Tracer tracer = conTrig.getTracer();
        Span span = tracer.spanBuilder("ClientOperation").startSpan();
        try (Scope ClientScope = span.makeCurrent()) {

            for (int i=0; i<numOfFiles;i++){
            try (Socket socket = new Socket(serverAddress, serverPort)) {
                System.out.println("Connected to the server.");

                // Specify the local folder path and the file name you want to send
                String localFolderPath = "src/main/resources/Input/";
                String filename = "random_file_"+i+".txt";

                File fileToSend = new File(localFolderPath + filename);
                    if (fileToSend.exists()) {
                        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                        outputStream.writeUTF(filename); // Send the filename to the server

                        try (FileInputStream fileInputStream = new FileInputStream(fileToSend)) {
                            byte[] buffer = new byte[fileInputStream.available()];
                            fileInputStream.read(buffer);
                            long checksum = calculateChecksum(buffer);
                            buffer = compress(encrypt(buffer, 3));
                            
                            outputStream.writeLong(checksum);
                            outputStream.write(buffer); // Send file content

                            outputStream.close();
                            
                            System.out.println("File '" + filename + "' sent to the server.");
                        }
                    } else {
                        System.out.println("File not found: " + filename);
                    }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(i+1>=numOfFiles){
                ClientScope.close();
                span.end();
            }   //Closes scope and span of the client
        }   //Loops through all the files
    }
    try {
        Thread.sleep(30000); // Adjust the sleep time as needed
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    // Exit the application
    System.exit(0);
    }
    // Calculate CRC32 checksum for a byte array
    public static long calculateChecksum(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }
    // Ecnypts the file content
    public static byte[] encrypt(byte[] input, int shift) {
        byte[] encrypted = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            encrypted[i] = (byte) (input[i] + shift);
        }
        return encrypted;
    }
    //Compresses the file content
    public static byte[] compress(byte[] input) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(input);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length);
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        return outputStream.toByteArray();
    }
    //Returns the num of files it needs to run through
    public static int getNumOfFiles(){
        return numOfFiles;
    }
    public static void main(String[] args) {new client();}
}


