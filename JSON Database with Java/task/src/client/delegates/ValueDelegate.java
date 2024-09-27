package client.delegates;

import com.beust.jcommander.Parameter;

/**
 * Delegate for the value parameter.
 */
public class ValueDelegate {
    @Parameter(
            names = "-v",
            description = "The value which should be used in the operation, if it requires a value.",
            required = true
    )
    private String value;

    public String getValue() {
        return value;
    }
}
