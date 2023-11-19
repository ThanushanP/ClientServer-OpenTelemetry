package com.example;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.zip.Inflater;

import java.util.zip.CRC32;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.Context;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.LongCounter;

//Main class for server, creates parallel threads to save each file content
public class server {
    
    public static void main(String[] args) {
        int port = 12345; // Server port

        Tracer tracer = conTrig.getTracer();
        Meter meter = conTrig.gMeter();

        LongCounter errorCounter = meter.counterBuilder("errors.count")
        .setDescription("Counts the number of errors")
        .setUnit("1")
        .build();

        Span mainThreadSpan = tracer.spanBuilder("main-operation")
            .setAttribute("Main", "mainVal")
            .startSpan();
        try (Scope mainThreadScope = mainThreadSpan.makeCurrent()) {

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Server is listening for incoming connections...");

                int parallelism = Runtime.getRuntime().availableProcessors();
                ForkJoinPool forkJoinPool = new ForkJoinPool(parallelism);

                mainThreadSpan.setStatus(StatusCode.OK,"Main Thread starting");
                int i=0;
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Accepted connection from " + clientSocket.getInetAddress());
                    
                    forkJoinPool.submit(new ClientHandler(clientSocket, forkJoinPool, tracer, mainThreadSpan, errorCounter));
                    i++;
                    if(i>=client.getNumOfFiles()){;
                        mainThreadSpan.setStatus(StatusCode.OK,"Main Thread Completed");
                        mainThreadScope.close();
                        mainThreadSpan.end();
                        break;   
                    }   //Ends the main scope and span
                }
            }
        } catch (IOException e) {
            errorCounter.add(1);
            mainThreadSpan.setStatus(StatusCode.ERROR,"Main Thread Error" +e);
            e.printStackTrace();
        }
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            errorCounter.add(1);
            mainThreadSpan.setStatus(StatusCode.ERROR,"Main Thread Error" +e);
            Thread.currentThread().interrupt();
        }//Gives the program enough time for the last thread to complete and send info to jaegar
    }

}
//Handles the async file saving 
class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ForkJoinPool forkJoinPool;
    private Tracer tracer;
    private Span main;
    private LongCounter errCounter;

    public ClientHandler(Socket clientSocket, ForkJoinPool forkJoinPool, Tracer tracer, Span main, LongCounter errCounter) {
        this.clientSocket = clientSocket;
        this.forkJoinPool = forkJoinPool;
        this.tracer = tracer;
        this.main = main;
        this.errCounter = errCounter;
    }

    @Override
    public void run() {
        Span childThreadSpan = tracer.spanBuilder("child-operation")
        .setParent(Context.current().with(main))
        .startSpan();
        try (Scope childThreadScope = childThreadSpan.makeCurrent()) {
                childThreadSpan.setStatus(StatusCode.OK,"Child Thread starting");

            try (DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream())) {
                //Read the file name and content from the client
                String fileName = "Result"+inputStream.readUTF();
                long checksum = inputStream.readLong();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }//Reads through content sent by the client

                byte[] fileContent = byteArrayOutputStream.toByteArray();

                fileContent = decrypt(decompress(fileContent,errCounter),3);

                boolean isIntegrityIntact = verifyChecksum(fileContent, checksum);

                if (!isIntegrityIntact){
                    System.out.println("The data for "+ fileName+" may not be integral");
                }
                if (Math.random()*100<=5){
                    main.setAttribute("Math.random()*100<=5_121", true);
                    System.out.println("Math.random()*100<=5_121");
                }//This if stmt makes no change to the code but we will use it as a predecade

                childThreadSpan.setAttribute("Child", fileName);

                String outputPath = "src/main/resources/result/" + fileName;

                /*Delibertaely delay the program with 1/10 chance */
                if (Math.random()*100<=10){
                        main.setAttribute("Math.random()*100<=10_131", true);
                    if(true){
                        main.setAttribute("true_133", true);
                        try {
                            Thread.sleep(100000);
                        } catch (InterruptedException e) {
                            childThreadSpan.setStatus(StatusCode.ERROR,"Child Thread Error" +e+ "\nFile Name: "+fileName);
                            Thread.currentThread().interrupt();
                        }
                        System.out.println("Error: "+fileName);
                    }//This if stmt does not change anything, only there to experiment with context(p)
                }

                // Save the file on the server
                forkJoinPool.invoke(new FileSaveTask(fileContent,outputPath,errCounter));

                System.out.println("File received and saved: " + fileName);
            } catch (IOException e) {
                errCounter.add(1);
                childThreadSpan.setStatus(StatusCode.ERROR,"Child Thread Error" +e);
                e.printStackTrace();
            }
        } finally {
            childThreadSpan.setStatus(StatusCode.OK);
            childThreadSpan.end();
        }//Ends the scope of the child span
    }
    //Ensures data integerity using checkSums
    public static boolean verifyChecksum(byte[] data, long checksum) {
        long calculatedChecksum = calculateChecksum(data);
        return calculatedChecksum == checksum;
    }
    // Calculate CRC32 checksum for a byte array
    public static long calculateChecksum(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }
    //Decrypts the file content given by client
    public static byte[] decrypt(byte[] input, int shift) {
        byte[] decrypted = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            decrypted[i] = (byte) (input[i] - shift);
        }
        return decrypted;
    }
    //Decompresses the file content sent by client
    public static byte[] decompress(byte[] compressedData, LongCounter errCounter) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedData.length);
        byte[] buffer = new byte[1024];
        while (!inflater.finished()) {
            try {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            } catch (Exception e) {
                errCounter.add(1);
                break;
            }
        }
        outputStream.close();
        return outputStream.toByteArray();
    }
}
//Saves the file content to result folder
class FileSaveTask extends RecursiveAction {    
    private byte[] data;
    private String filePath;
    private LongCounter errCounter;

    public FileSaveTask(byte[] data, String filePath, LongCounter errCounter) {
        this.data = data;
        this.filePath = filePath;
        this.errCounter=errCounter;
    }

    @Override
    protected void compute() {
        try (OutputStream outputStream = new FileOutputStream(filePath)) {
            outputStream.write(data);

            System.out.println("File saved: " + filePath);
        } catch (IOException e) {
            errCounter.add(1);
            e.printStackTrace();
        }
    }
}

