package client.args;

import java.util.HashMap;
import java.util.Map;

/**
 * Args configuration for the exit request. There are no args, so it returns an empty map.
 */
public class ExitArgs implements Args {
    @Override
    public Map<String, String> returnAllArgs() {
        return new HashMap<>();
    }
}
