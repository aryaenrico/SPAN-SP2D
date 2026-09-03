package com.bsi.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public class T24Config {

    private String endpointUrl = "http://apigw-uat-cbs-corp.bankbsi.co.id:4015/ws/coreR24AllServices/1.0";   
    private String soapAction = "";
    private String apiKey ="";
    private static String DEFAULT_PROPERTIES ="";

    private String userName;
    private String password;
    private String company;
    
    // [FIX][2026-08-13] Header Content-Type dinamis sesuai spesifikasi ESB BSI (Default: soap/xml; charset=UTF-8)
    private String contentType = "soap/xml; charset=UTF-8";

    private int connectTimeoutMs = 10_000;
    private int readTimeoutMs = 60_000;


  public static T24Config fromProperties() {
    return fromProperties(DEFAULT_PROPERTIES);
  }


public static T24Config fromProperties(String location) {
    Properties props = new Properties();
    InputStream in = null;
    try {
        File file = new File (location);
        if (file.isFile()) {
            in = new FileInputStream(file);
        } else {
            in = T24Config.class.getClassLoader().getResourceAsStream(location);
        }
        if (in == null) {
            throw new IllegalStateException(
                    "File properties tidak ditemukan di filesystem maupun classpath: " + location);
        }
        props.load(in);
    } catch (IOException e) {
        throw new UncheckedIOException("Gagal membaca file properties: " + location, e);
    } finally {
        if (in != null) {
            try { in.close(); } catch (IOException ignored) { }
        }
    }
    return fromProperties(props);
}


public static T24Config fromProperties(Properties props) {
    T24Config cfg = new T24Config();

    String endpointUrl = trimToNull(props.getProperty("t24.endpointUrl"));
    if (endpointUrl != null) cfg.setEndpointUrl(endpointUrl);

    String soapAction = props.getProperty("t24.soapAction");
    if (soapAction != null) cfg.setSoapAction(soapAction);

    String contentType = props.getProperty("t24.contentType");
    if (contentType != null) cfg.setContentType(contentType);

    cfg.setUserName(trimToNull(props.getProperty("t24.userName")));
    cfg.setPassword(trimToNull(props.getProperty("t24.password")));
    cfg.setCompany(trimToNull(props.getProperty("t24.company")));
    cfg.setApiKey(trimToNull(props.getProperty("t24.apiKey")));

    String connectTimeout = trimToNull(props.getProperty("t24.connectTimeoutMs"));
    if (connectTimeout != null) cfg.setConnectTimeoutMs(Integer.parseInt(connectTimeout));

    String readTimeout = trimToNull(props.getProperty("t24.readTimeoutMs"));
    if (readTimeout != null) cfg.setReadTimeoutMs(Integer.parseInt(readTimeout));

    return cfg;
}

    private static String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
}

    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }

    public String getSoapAction() { return soapAction; }
    public void setSoapAction(String soapAction) { this.soapAction = soapAction; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    public String getApikey(){
        return this.apiKey;
    }

    public void setApiKey(String apiKey){
        this.apiKey = apiKey;
    }
}
 

