package client;

import client.args.*;
import com.beust.jcommander.JCommander;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;

public class Main {
    private static final String DATA_DIR = System.getProperty("user.dir") + "/src/client/data/";
    /**
     * An enum for keeping track of what requests exist. This makes it easier to add new args because
     * all you have to do is simply add a new value to this enum containing the request name and the corresponding args object.
     */
    private enum Requests {
        GET("get", new GetArgs()), SET("set", new SetArgs()), DELETE("delete", new DeleteArgs()), EXIT("exit", new ExitArgs());

        private final String request;

        private final Args args;

        Requests(String request, Args args) {
            this.request = request;
            this.args = args;
        }

        public String getRequest() {
            return request;
        }

        public Args getArgs() {
            return args;
        }
    }

    public static void main(String[] args) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", 51433);
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream()))
        {
            System.out.println("Client started!");

            JsonObject request = getRequest(args);
            JsonObject argsJson = parseArgs(request, args);
            output.writeUTF(argsJson.toString());

            System.out.println("Sent: " + argsJson);
            System.out.println("Received: " + input.readUTF());
        }
    }

    /**
     * Returns the request in the args.
     * @param args the args in which the request is to be extracted from
     * @return the request
     */
    private static JsonObject getRequest(String[] args) {
        Gson gson = new Gson();
        RequestArgs requestArg = new RequestArgs();
        JCommander requestJC = JCommander.newBuilder()
                .addObject(requestArg)
                .build();
        // Just the -t and the request
        requestJC.parse(Arrays.copyOfRange(args, 0, 2));
        String stringJson = gson.toJson(requestArg);
        return gson.fromJson(stringJson, JsonObject.class);
    }

    /**
     * Parsed the args into json to send to the server.
     * @param request the specific action for the server to perform
     * @param args the additional args for the request
     * @return the json command
     */
    private static JsonObject parseArgs(JsonObject request, String[] args) throws IOException {
        JsonObject json = new JsonObject();
        Args argsHolder = null;
        if (request.has("request")) {
            String requestType = request.get("request").getAsString();
            json.addProperty("type", requestType);

            for (Requests enumRequest : Requests.values()) {
                if (enumRequest.getRequest().equals(requestType)) {
                    argsHolder = enumRequest.getArgs();
                    break;
                }
            }

            JCommander jc = JCommander.newBuilder()
                    .addObject(argsHolder)
                    .build();
            // Everything after the request
            jc.parse(Arrays.copyOfRange(args, 2, args.length));

            Map<String, String> argsMap = argsHolder.returnAllArgs();
            for (Map.Entry<String, String> entry : argsMap.entrySet()) {
                json.addProperty(entry.getKey(), entry.getValue());
            }
            return json;
        } else if (request.has("filename")) {
            Gson gson = new Gson();
            String filename = request.get("filename").getAsString();
            StringBuilder stringJson = new StringBuilder();
            try (FileReader reader = new FileReader(DATA_DIR + filename)) {
                while (reader.ready()) {
                    stringJson.append((char) reader.read());
                }
            }

            return gson.fromJson(stringJson.toString(), JsonObject.class);
        }
        return null;
    }
}