package client.args;

import java.util.Map;

public interface Args {
    /**
     * Returns all args of the specific args configuration in a map to be parsed into json.
     * @return the map of args
     */
    Map<String, String> returnAllArgs();
}
