package client.delegates;

import com.beust.jcommander.Parameter;

/**
 * Delegate for the request parameter.
 */
public class RequestDelegate {
    @Parameter(
            names = "-t",
            description = "The operation to execute (get, set, delete, etc)."
    )
    private String request;

    public String getRequest() {
        return request;
    }
}
