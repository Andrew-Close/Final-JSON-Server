package client;

import com.beust.jcommander.Parameter;

/**
 * An args class that holds either a request or a filename.
 */
public class RequestArgs {
    @Parameter(
            names = "-t",
            description = "The operation to execute (get, set, delete, etc)."
    )
    private String request;

    @Parameter(
            names = "-in",
            description = "The filename from which to get the json request."
    )
    private String filename;

    String getRequest() {
        return request;
    }
}
