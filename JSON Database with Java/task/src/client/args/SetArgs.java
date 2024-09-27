package client.args;

import client.delegates.KeyDelegate;
import client.delegates.ValueDelegate;
import com.beust.jcommander.ParametersDelegate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Args configuration for the set request.
 */
public class SetArgs implements Args {
    @ParametersDelegate
    private final KeyDelegate key = new KeyDelegate();

    @ParametersDelegate
    private final ValueDelegate value = new ValueDelegate();

    public String getKey() {
        return key.getKey();
    }

    String getValue() {
        return value.getValue();
    }

    @Override
    public Map<String, String> returnAllArgs() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key", key.getKey());
        map.put("value", value.getValue());
        return map;
    }
}
