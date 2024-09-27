package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {

    /**
     * Handles the interaction of the server and a single client
     * @param socket the client socket
     */
    private record ClientHandler(Socket socket) implements Callable<Void> {
        @Override
        public Void call() {
            try (DataInputStream input = new DataInputStream(socket.getInputStream());
                 DataOutputStream output = new DataOutputStream(socket.getOutputStream()))
            {
                Gson gson = new Gson();
                String command = input.readUTF();
                JsonObject json = gson.fromJson(command, JsonObject.class);
                JsonObject result = executeCommand(json);
                output.writeUTF(gson.toJson(result));
                if (SHOULD_EXIT) {
                    socket.close();
                    SERVER.close();
                    return null;
                }
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    private static ServerSocket SERVER;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final JsonDatabase JSON_DATABASE = new JsonDatabase();
    private static volatile boolean SHOULD_EXIT = false;

    public static void main(String[] args) throws IOException {
        SERVER = new ServerSocket(51433);
        System.out.println("Server started!");
        while (!SHOULD_EXIT) {
            try {
                SERVER.setSoTimeout(0);
                Socket socket = SERVER.accept();
                EXECUTOR.submit(new ClientHandler(socket));
            } catch (SocketException e) {
                if (SHOULD_EXIT) {
                    EXECUTOR.shutdown();
                    return;
                }
            } catch (IOException ignored) {
            }
        }
    }

    /*

















    Work on preventing backslashes from appearing in the database. Check the current database to see what I mean.
















     */

    /**
     * Executes an operation on the database/program according to the passed command.
     * @param command the command to execute
     * @return the server response
     */
    private static JsonObject executeCommand(JsonObject command) throws IOException {
        String operation = command.get("type").getAsString();
        String key;
        String[] keyArray;
        switch (operation) {
            case "set":
                // Text is string
                try {
                    // Key is string
                    try {
                        key = command.get("key").getAsString();
                        String text = command.get("value").getAsString();
                        return setCell(key, text);
                    // Key is array
                    } catch (Exception e) {
                        keyArray = parseStringArray(command.get("key").getAsJsonArray().toString());
                        String text = command.get("value").getAsString();
                        return setCell(keyArray, text);
                    }
                // Text is object
                } catch (Exception e) {
                    // Key is string
                    try {
                        key = command.get("key").getAsString();
                        JsonObject text = command.getAsJsonObject("value");
                        return setCell(key, text);
                    // Key is array
                    } catch (Exception ex) {
                        keyArray = parseStringArray(command.get("key").getAsJsonArray().toString());
                        JsonObject text = command.getAsJsonObject("value");
                        return setCell(keyArray, text);
                    }
                }
            case "get":
                try {
                    key = command.get("key").getAsString();
                    return getCell(key);
                } catch (Exception e) {
                    keyArray = parseStringArray(command.get("key").getAsJsonArray().toString());
                    return getCell(keyArray);
                }
            case "delete":
                try {
                    key = command.get("key").getAsString();
                    return deleteCell(key);
                } catch (Exception e) {
                    keyArray = parseStringArray(command.get("key").getAsJsonArray().toString());
                    return deleteCell(keyArray);
                }
            case "exit":
                return signalExit();
            default:
                return returnError();
        }
    }

    /**
     * Sets the cell at the passed key to the passed value.
     * @param key the key to which the passed value should be assigned
     * @param value the value which should be added to the database
     * @return the server response
     */
    private static JsonObject setCell(String key, String value) throws IOException {
            JSON_DATABASE.addPair(key, value);
        ServerResponse response = new ServerResponse.ServerResponseBuilder()
                .addResponse("OK")
                .build();
        return response.getResponse();
    }

    /**
     * Sets the cell at the passed key to the passed value.
     * @param key the key to which the passed value should be assigned
     * @param value the value which should be added to the database
     * @return the server response
     */
    private static JsonObject setCell(String[] key, String value) throws IOException {
        JSON_DATABASE.addPair(key, value);
        ServerResponse response = new ServerResponse.ServerResponseBuilder()
                .addResponse("OK")
                .build();
        return response.getResponse();
    }

    /**
     * Sets the cell at the passed key to the passed value.
     * @param key the key to which the passed value should be assigned
     * @param value the value which should be added to the database
     * @return the server response
     */
    private static JsonObject setCell(String key, JsonObject value) throws IOException {
        JSON_DATABASE.addPair(key, value);
        ServerResponse response = new ServerResponse.ServerResponseBuilder()
                .addResponse("OK")
                .build();
        return response.getResponse();
    }

    /**
     * Sets the cell at the passed key to the passed value.
     * @param key the key to which the passed value should be assigned
     * @param value the value which should be added to the database
     * @return the server response
     */
    private static JsonObject setCell(String[] key, JsonObject value) throws IOException {
        JSON_DATABASE.addPair(key, value);
        ServerResponse response = new ServerResponse.ServerResponseBuilder()
                .addResponse("OK")
                .build();
        return response.getResponse();
    }

    /**
     * Gets the value of the cell at the passed key.
     * @param key the key of the value which should be retrieved.
     * @return the server response
     */
    private static JsonObject getCell(String key) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ServerResponse.ServerResponseBuilder responseBuilder = new ServerResponse.ServerResponseBuilder();
        if (JSON_DATABASE.keyExists(key)) {
            try {
                responseBuilder.addResponse("OK")
                        .addValue(gson.fromJson(JSON_DATABASE.getPair(key), JsonObject.class));
            } catch (Exception e) {
                responseBuilder.addResponse("OK")
                        .addValue(gson.toJson(JSON_DATABASE.getPair(key)));
            }
        } else {
            responseBuilder.addResponse("ERROR")
                    .addErrorReason("No such key");
        }
        ServerResponse response = responseBuilder.build();
        return response.getResponse();
    }

    /**
     * Gets the value of the cell at the passed key.
     * @param key the key of the value which should be retrieved.
     * @return the server response
     */
    private static JsonObject getCell(String[] key) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ServerResponse.ServerResponseBuilder responseBuilder = new ServerResponse.ServerResponseBuilder();
        if (JSON_DATABASE.keyExists(key)) {
            responseBuilder.addResponse("OK")
                    .addValue(gson.toJson(JSON_DATABASE.getPair(key)));
        } else {
            responseBuilder.addResponse("ERROR")
                    .addErrorReason("No such key");
        }
        ServerResponse response = responseBuilder.build();
        return response.getResponse();
    }

    /**
     * Removes the corresponding key-value pair based on the passed key.
     * @param key the key of the pair which should be deleted.
     * @return the server response
     */
    private static JsonObject deleteCell(String key) throws IOException {
        ServerResponse.ServerResponseBuilder responseBuilder = new ServerResponse.ServerResponseBuilder();
        if (JSON_DATABASE.keyExists(key)) {
            JSON_DATABASE.deletePair(key);
            responseBuilder.addResponse("OK");
        } else {
            responseBuilder.addResponse("ERROR")
                    .addErrorReason("No such key");
        }
        ServerResponse response = responseBuilder.build();
        return response.getResponse();
    }

    /**
     * Removes the corresponding key-value pair based on the passed key.
     * @param key the key of the pair which should be deleted.
     * @return the server response
     */
    private static JsonObject deleteCell(String[] key) throws IOException {
        ServerResponse.ServerResponseBuilder responseBuilder = new ServerResponse.ServerResponseBuilder();
        if (JSON_DATABASE.keyExists(key)) {
            JSON_DATABASE.deletePair(key);
            responseBuilder.addResponse("OK");
        } else {
            responseBuilder.addResponse("ERROR")
                    .addErrorReason("No such key");
        }
        ServerResponse response = responseBuilder.build();
        return response.getResponse();
    }

    /**
     * Signals to the server that it should shut down.
     * The method itself doesn't actually exit the program, it just sends a signal that the program should stop.
     * @return the server response
     */
    private static JsonObject signalExit() {
        SHOULD_EXIT = true;
        ServerResponse response = new ServerResponse.ServerResponseBuilder()
                .addResponse("OK")
                .build();
        return response.getResponse();
    }

    /**
     * Returns a default error message. Simply has a response "ERROR" and nothing else.
     * @return the error message
     */
    private static JsonObject returnError() {
        ServerResponse response = new ServerResponse.ServerResponseBuilder()
                .addResponse("ERROR")
                .build();
        return response.getResponse();
    }

    /**
     * Converts a string representation of an array into a string array. Must use brackets and colons.
     * Spacing between elements is optional.
     * @param string the string to be converted into an array
     * @return the converted string array
     */
    private static String[] parseStringArray(String string) {
        string = string.replaceAll("\"", "");
        List<String> list = new ArrayList<>();
        int startPointer = 1;
        int endPointer = 2;

        while (true) {
            if (string.charAt(endPointer) == ']') {
                list.add(string.substring(startPointer, endPointer));
                return list.toArray(new String[0]);
            } else if (string.charAt(endPointer) == ',') {
                list.add(string.substring(startPointer, endPointer));
                ++endPointer;
                startPointer = endPointer;
            } else if (string.charAt(endPointer - 1) == ' ') {
                ++startPointer;
            }
            ++endPointer;
        }
    }
}
