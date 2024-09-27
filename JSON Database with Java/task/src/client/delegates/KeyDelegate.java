package client.delegates;

import com.beust.jcommander.Parameter;

/**
 * Delegate for the key parameter.
 */
public class KeyDelegate {
    @Parameter(
            names = "-k",
            description = "The key which is paired with the value to be used in the operation.",
            required = true
    )
    private String key;

    public String getKey() {
        return key;
    }
}
