package com.bsi.entity.api;

public class ApiClientException extends RuntimeException {

    private final int httpStatus;
    private final String responseBody;

    public ApiClientException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = -1;
        this.responseBody = null;
    }

    public ApiClientException(String message, int httpStatus, String responseBody) {
        super(message);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getResponseBody() { return responseBody; }

    public boolean isNetworkTimeout() {
    Throwable cause = getCause();
    if (cause instanceof java.net.SocketTimeoutException || cause instanceof java.net.ConnectException) {
        return true;
    }
    if (getMessage() != null) {
        String msg = getMessage().toLowerCase();
        return msg.contains("timeout") || msg.contains("timed out") || msg.contains("read timed out");
    }
    return false;
}

public String getTimeoutCode() {
    if (getCause() instanceof java.net.SocketTimeoutException) {
        return "TO_READ";
    } else if (getCause() instanceof java.net.ConnectException) {
        return "TO_CONN";
    }
    return "TO_NET";
}

}
 
