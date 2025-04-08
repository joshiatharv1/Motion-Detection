/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.motiondetector;
import com.google.gson.*;
import com.opencsv.CSVWriter;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.io.*;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.*;
/**
 *
 * @author Atharv Joshi
 */
public class MotionDetector extends WebSocketServer{

     private CSVWriter csvWriter;
    private Gson gson;
    private final int MOVING_THRESHOLD = 5; // data points to look at
    private final double STD_THRESHOLD = 0.05; // std deviation threshold for idle

    private final List<Double> recentMagnitudes = new LinkedList<>();

    public MotionDetector(int port) throws IOException {
        super(new InetSocketAddress(port));
        gson = new Gson();

        FileWriter fw = new FileWriter("motion_data_java.csv");
        csvWriter = new CSVWriter(fw);
        csvWriter.writeNext(new String[]{"timestamp", "x", "y", "z"});
        csvWriter.flush();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("📲 Client connected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject data = gson.fromJson(message, JsonObject.class);

            double x = data.get("x").getAsDouble();
            double y = data.get("y").getAsDouble();
            double z = data.get("z").getAsDouble();

            double magnitude = Math.sqrt(x * x + y * y + z * z);
            updateRecentMagnitudes(magnitude);

            // Determine motion state
            String state = isIdle() ? "Idle 📴" : "Moving 📱";
            System.out.printf("Received: x=%.2f, y=%.2f, z=%.2f => %s%n", x, y, z, state);

            // Log to CSV
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
            csvWriter.writeNext(new String[]{timestamp, String.valueOf(x), String.valueOf(y), String.valueOf(z)});
            csvWriter.flush();

        } catch (Exception e) {
            System.err.println("Error parsing message: " + message);
            e.printStackTrace();
        }
    }

    private void updateRecentMagnitudes(double magnitude) {
        if (recentMagnitudes.size() >= MOVING_THRESHOLD) {
            recentMagnitudes.remove(0);
        }
        recentMagnitudes.add(magnitude);
    }

    private boolean isIdle() {
        if (recentMagnitudes.size() < MOVING_THRESHOLD) return false;
        double avg = recentMagnitudes.stream().mapToDouble(d -> d).average().orElse(0);
        double std = Math.sqrt(recentMagnitudes.stream().mapToDouble(d -> Math.pow(d - avg, 2)).average().orElse(0));
        return std < STD_THRESHOLD;
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("🔌 Client disconnected.");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("❗ WebSocket Error:");
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("✅ Java WebSocket Server running on port " + getPort());
    }

    public static void main(String[] args) throws IOException {
        MotionDetector server = new MotionDetector(8000);
        server.start();
}
}
