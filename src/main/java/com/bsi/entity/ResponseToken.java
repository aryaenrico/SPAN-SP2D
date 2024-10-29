package com.bsi.entity;

public class ResponseToken {
    private Body body;
    private String statusCode;
    private int statusCodeValue;

    public static class Body {
        private String access_token;
        private int expires_in;
        private int refresh_expires_in;
        private String token_type;
        private int not_before_policy;
        private String scope;

        // Getters and setters for Body class
        public String getAccessToken() {
            return access_token;
        }

        public void setAccessToken(String access_token) {
            this.access_token = access_token;
        }

        public int getExpiresIn() {
            return expires_in;
        }

        public void setExpiresIn(int expires_in) {
            this.expires_in = expires_in;
        }

        public int getRefreshExpiresIn() {
            return refresh_expires_in;
        }

        public void setRefreshExpiresIn(int refresh_expires_in) {
            this.refresh_expires_in = refresh_expires_in;
        }

        public String getTokenType() {
            return token_type;
        }

        public void setTokenType(String token_type) {
            this.token_type = token_type;
        }

        public int getNotBeforePolicy() {
            return not_before_policy;
        }

        public void setNotBeforePolicy(int not_before_policy) {
            this.not_before_policy = not_before_policy;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCodeValue() {
        return statusCodeValue;
    }

    public void setStatusCodeValue(int statusCodeValue) {
        this.statusCodeValue = statusCodeValue;
    }
}
