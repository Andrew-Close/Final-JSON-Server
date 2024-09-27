package client.args;

import client.delegates.KeyDelegate;
import com.beust.jcommander.ParametersDelegate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Args configuration for the delete request.
 */
public class DeleteArgs implements Args {
    @ParametersDelegate
    private final KeyDelegate key = new KeyDelegate();

    public String getKey() {
        return key.getKey();
    }

    @Override
    public Map<String, String> returnAllArgs() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key", key.getKey());
        return map;
    }
}
