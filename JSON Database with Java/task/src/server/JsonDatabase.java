package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JsonDatabase {
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Lock readLock = lock.readLock();
    private static final Lock writeLock = lock.writeLock();
    private static final String DATABASE_DIR = System.getProperty("user.dir") + "/src/server/data/db.json";

    JsonObject fromFile() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        readLock.lock();
        String jsonString = readStringContents();
        readLock.unlock();
        return gson.fromJson(jsonString, JsonObject.class);
    }

    private void toFile(JsonObject json) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        writeLock.lock();
        try (FileWriter writer = new FileWriter(DATABASE_DIR)) {
            writer.write(gson.toJson(json));
        }
        writeLock.unlock();
    }

    private String readStringContents() throws IOException {
        try (FileReader reader = new FileReader(DATABASE_DIR)) {
            StringBuilder jsonString = new StringBuilder();
            while (reader.ready()) {
                jsonString.append((char) reader.read());
            }
            return jsonString.toString();
        }
    }

    /**
     * Adds the specified value to the specified key.
     * @param json the json to add a value to
     * @param key the key of the pair to add
     * @param value the value to be added
     */
    private static void addValue(JsonObject json, String key, String value) {
        json.addProperty(key, value);
    }

    /**
     * Adds the value object to the specified key.
     *
     * @param json  the json to add an object to
     * @param key   the key of the pair to add
     * @param value the object to be added
     */
    private static void addValue(JsonObject json, String key, JsonObject value) {
        json.add(key, value);
    }

    /**
     * Creates a path in the provided json object using the provided path. Existing data will not be overwritten.
     * You may choose to place either a nested object or a single value at the end of the path.
     * @param json the json in which the path should be created
     * @param path the path
     * @param endIsNested determines whether the final value is nested or not
     * @param shouldOverwrite determines if the end of the path should be overwritten.
     * @return the final json with the created path
     */
    private static JsonObject createPath(JsonObject json, String[] path, boolean endIsNested, boolean shouldOverwrite) {
        // Break condition
        if (path.length == 1) {
            // Avoid overwriting existing data. If shouldOverwrite is true, then the end of the path will always be overwritten.
            if (json.has(path[0]) && !shouldOverwrite) {
                JsonObject finalPath = new JsonObject();
                // Tries adding nested object
                try {
                    finalPath.add(path[0], json.get(path[0]).getAsJsonObject());
                    // If fails, then it is a value, not an object
                } catch (Exception e) {
                    finalPath.addProperty(path[0], json.get(path[0]).getAsString());
                }
                return finalPath;
            } else {
                // Either adds a nested object at the end or adds a single value
                if (endIsNested) {
                    json.add(path[0], new JsonObject());
                } else {
                    json.addProperty(path[0], "");
                }
                return json;
            }
        } else {
            JsonObject newPath;
            // Avoid overwriting existing data
            if (json.has(path[0])) {
                // Try adding nested object
                try {
                    newPath = json.get(path[0]).getAsJsonObject();
                    // If fails, then it is a value, not an object, and a new nested path should be added instead
                } catch (Exception e) {
                    newPath = new JsonObject();
                }
            } else {
                newPath = new JsonObject();
            }
            json.add(path[0], createPath(newPath, Arrays.copyOfRange(path, 1, path.length), endIsNested, shouldOverwrite));
        }
        return json;
    }

    /**
     * Gets the object at the indicated path. If the path ends at a non-nested object, then the last nested object is returned.
     * @param json the json from which to get the path
     * @param path the path to get
     * @return the retrieved path
     */
    private static Optional<JsonObject> getPath(JsonObject json, String[] path) {
        // Break condition
        if (path.length == 1) {
            // Tries adding nested object
            try {
                JsonObject finalPath = json.getAsJsonObject(path[0]);
                // Null means that the path does not exist
                if (finalPath == null) {
                    return Optional.empty();
                }
                return Optional.of(finalPath);
                // If npe is thrown, then the path doesn't exist
            } catch (NullPointerException e) {
                return Optional.empty();
                // If try block fails, and it's not npe, then is it a single value, not an object
            } catch (Exception e) {
                JsonObject temp = new JsonObject();
                temp.addProperty(path[0], json.getAsJsonObject().get(path[0]).getAsString());
                return Optional.of(temp);
            }
        } else {
            JsonObject selectedPath;
            try {
                selectedPath = json.getAsJsonObject(path[0]);
                // If try block fails, then the path doesn't exist
            } catch (ClassCastException e) {
                return Optional.empty();
            }
            return getPath(selectedPath, Arrays.copyOfRange(path, 1, path.length));
        }
    }

    /**
     * Sets the value of the key at the end of the specified path to the new value specified in the parameters.
     *
     * @param json  the json to use when setting a new value
     * @param path  the path at which a new value should be set
     * @param value the new value to set
     */
    private static void setValueAt(JsonObject json, String[] path, String value) {
        if (getPath(json, path).isEmpty()) {
            createPath(json, path, false, false);
        }
        // Special case
        if (path.length == 1) {
            addValue(json, path[0], value);
            return;
        }

        json = createPath(json, path, false, true);
        // The object at the end of the path gets updated here
        // selectedJson contains the already updates json
        JsonObject selectedJson = getPath(json, path).get();
        addValue(selectedJson, path[path.length - 1], value);

        // Special case
        if (path.length == 2) {
            addValue((getPath(json, new String[]{path[0]}).get()), path[1], value);
        } else {
            // temp is used to construct the new value for selectedJson, hence why it is called temp
            JsonObject temp;
            for (int i = path.length - 2; i >= 0 ; i--) {
                temp = getPath(json, Arrays.copyOfRange(path, 0, i + 1)).get();
                // Tries to use addProperty on the json pair inside the currently selected one.
                try {
                    // This statement exists simply to check if the inner json pair contains a single value or an object.
                    // If it contains an object, then an exception is thrown, which brings the execution to the catch block
                    temp.get(path[i + 1]).getAsString();
                    addValue(temp, path[i + 1], value);
                    // If fails, then the inner json pair contains an object, and so add should be used instead
                } catch (Exception e) {
                    temp.add(path[i + 1], selectedJson);
                }
                // selectedJson is updated
                selectedJson = temp;
            }
        }
    }

    /**
     * Sets the value of the key at the end of the specified path to the new value specified in the parameters.
     *
     * @param json  the json to use when setting a new value
     * @param path  the path at which a new value should be set
     * @param value the new value to set
     */
    private static void setValueAt(JsonObject json, String[] path, JsonObject value) {
        if (getPath(json, path).isEmpty()) {
            createPath(json, path, false, false);
        }
        // Special case
        if (path.length == 1) {
            addValue(json, path[0], value);
            return;
        }

        json = createPath(json, path, false, true);
        // The object at the end of the path gets updated here
        // selectedJson contains the already updates json
        JsonObject selectedJson = getPath(json, path).get();
        addValue(selectedJson, path[path.length - 1], value);

        // Special case
        if (path.length == 2) {
            addValue((getPath(json, new String[]{path[0]}).get()), path[1], value);
        } else {
            // temp is used to construct the new value for selectedJson, hence why it is called temp
            JsonObject temp;
            for (int i = path.length - 2; i >= 0 ; i--) {
                temp = getPath(json, Arrays.copyOfRange(path, 0, i + 1)).get();
                // Tries to use addProperty on the json pair inside the currently selected one.
                try {
                    // This statement exists simply to check if the inner json pair contains a single value or an object.
                    // If it contains an object, then an exception is thrown, which brings the execution to the catch block
                    temp.get(path[i + 1]).getAsString();
                    addValue(temp, path[i + 1], value);
                    // If fails, then the inner json pair contains an object, and so add should be used instead
                } catch (Exception e) {
                    temp.add(path[i + 1], selectedJson);
                }
                // selectedJson is updated
                selectedJson = temp;
            }
        }
    }

    @Deprecated
    boolean databaseIsEmpty() throws IOException {
        String jsonString = readStringContents();
        return jsonString.length() <= 2;
    }

    /**
     * Adds a pair to the database. If the key already exists, it just gets overwritten.
     * @param key the key of the pair
     * @param value the value of the pair
     */
    void addPair(String key, String value) throws IOException {
        Gson gson = new Gson();
        JsonObject data = fromFile();
        try {
            data.add(key, gson.fromJson(value, JsonObject.class));
        } catch (Exception e) {
            data.addProperty(key, value);
        }
        toFile(data);
    }

    /**
     * Adds a pair to the database. If the path already exists, it just gets overwritten.
     * @param key the key of the pair
     * @param value the value of the pair
     */
    void addPair(String[] key, String value) throws IOException {
        JsonObject data = fromFile();
        setValueAt(data, key, value);
        toFile(data);
    }

    /**
     * Adds a pair to the database. If the key already exists, it just gets overwritten.
     * @param key the key of the pair
     * @param value the value of the pair
     */
    void addPair(String key, JsonObject value) throws IOException {
        Gson gson = new Gson();
        JsonObject data = fromFile();
        try {
            data.add(key, gson.fromJson(value, JsonObject.class));
        } catch (Exception e) {
            data.add(key, value);
        }
        toFile(data);
    }

    /**
     * Adds a pair to the database. If the path already exists, it just gets overwritten.
     * @param key the key of the pair
     * @param value the value of the pair
     */
    void addPair(String[] key, JsonObject value) throws IOException {
        JsonObject data = fromFile();
        setValueAt(data, key, value);
        toFile(data);
    }

    /**
     * Retrieves the value of the corresponding key from the database.
     * @param key the key of the value to retrieve
     * @return the value
     */
    String getPair(String key) throws IOException {
        JsonObject data = fromFile();
        try {
            return data.get(key).getAsString();
        } catch (Exception e) {
            return data.getAsJsonObject(key).toString();
        }
    }



    /**
     * Retrieves the value of the corresponding path from the database.
     * @param key the key of the value to retrieve
     * @return the value
     */
    String getPair(String[] key) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject data = fromFile();
        JsonObject selectedJson = getPath(data, key).get();
        if (selectedJson.keySet().size() == 1) {
            try {
                return selectedJson.get(key[key.length - 1]).getAsString();
            } catch (Exception ignored) {
            }
        }
        return gson.toJson(selectedJson);
    }

    /**
     * Deletes the pair of the corresponding key from the database.
     * @param key the key of the pair to delete
     */
    void deletePair(String key) throws IOException {
        JsonObject data = fromFile();
        data.remove(key);
        toFile(data);
    }

    /**
     * Deletes the pair of the corresponding key from the database.
     * @param key the key of the pair to delete
     */
    void deletePair(String[] key) throws IOException {
        JsonObject data = fromFile();
        try {
            JsonObject selectedJson = getPath(data, Arrays.copyOfRange(key, 0, key.length - 1)).get();
            selectedJson.remove(key[key.length - 1]);
        } catch (Exception e) {
            data.remove(key[key.length - 1]);
        }
        toFile(data);
    }

    /**
     * Checks if a pair with the corresponding key exists.
     * @param key the key to check if it exists
     * @return whether or not it exists
     */
    boolean keyExists(String key) throws IOException {
        JsonObject data = fromFile();
        return data.has(key);
    }

    /**
     * Checks if a pair with the corresponding key exists.
     * @param key the key to check if it exists
     * @return whether or not it exists
     */
    boolean keyExists(String[] key) throws IOException {
        JsonObject data = fromFile();
        return getPath(data, key).isPresent();
    }
}
