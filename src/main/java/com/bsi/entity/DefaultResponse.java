package com.bsi.entity;

public class DefaultResponse {
    private String status;
    private String message;
    private Object data;
    private Object error;
    private String timestamp;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public DefaultResponse(String timestamp, Object error, Object data, String message, String status) {
        this.timestamp = timestamp;
        this.error = error;
        this.data = data;
        this.message = message;
        this.status = status;
    }
}
