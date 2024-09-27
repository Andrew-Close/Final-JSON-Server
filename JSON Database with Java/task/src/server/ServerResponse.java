package server;

import com.google.gson.JsonObject;

public class ServerResponse {
    private final JsonObject response;

    private ServerResponse(JsonObject response) {
        this.response = response;
    }

    JsonObject getResponse() {
        return response;
    }

    /**
     * A builder for a server response
     */
    static class ServerResponseBuilder {
        private final JsonObject response = new JsonObject();

        ServerResponseBuilder() {}

        /**
         * Adds a response to the json response.
         * @param response the response
         * @return the builder
         */
        ServerResponse.ServerResponseBuilder addResponse(String response) {
            this.response.addProperty("response", response);
            return this;
        }

        /**
         * Adds a value to the json response.
         * @param value the value
         * @return the builder
         */
        ServerResponseBuilder addValue(String value) {
            value = value.replaceAll("\"", "");
            this.response.addProperty("value", value);
            return this;
        }

        /**
         * Adds a value to the json response.
         * @param value the value
         * @return the builder
         */
        ServerResponseBuilder addValue(JsonObject value) {
            this.response.add("value", value);
            return this;
        }

        /**
         * Adds an error reason to the json response.
         * @param reason the error reason
         * @return the builder
         */
        ServerResponseBuilder addErrorReason(String reason) {
            this.response.addProperty("reason", reason);
            return this;
        }

        /**
         * Builds the server response.
         * @return the server response
         */
        ServerResponse build() {
            return new ServerResponse(this.response);
        }
    }
}
