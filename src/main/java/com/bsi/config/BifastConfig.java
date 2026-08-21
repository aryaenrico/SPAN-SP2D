package com.bsi.config;

import com.bsi.entity.span.PathPropertiesBifast;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public class BifastConfig {
    private String apiKey;
    private Integer connectTimeoutms;
    private Integer readTimeoutms;
    private String endpointCT;
    private String endpointAe;
    private String endpointTi;
    private PathPropertiesBifast pathPropertiesBifast;
    private int numThread;
    private int flagName;

    public void setendpointTi (String endpointTI){
    this.endpointTi = endpointTI;
    }
    public String getEndpointTi(){
        return this.endpointTi;
    }

    public void setapiKey(String apikey){
        this.apiKey = apikey;
    }
    public String getapiKey(){
        return this.apiKey;
    }

    public void setendpointCT(String endpointCT){
       this.endpointCT = endpointCT;
    }
    public String getEndpointCT(){
        return this.endpointCT;
    }

    public void setendpointAe (String endpointAE){
        this.endpointAe = endpointAE;
    }
    public String getEndpointAe(){
        return this.endpointAe;
    }

    public void setConnectTimeoutms(String timeout){
        this.connectTimeoutms = Integer.parseInt(timeout);
    }
    public Integer getConnectTimeoutMs(){
        return this.connectTimeoutms;
    }

    public void setreadTimeoutms(String timeout){
        this.readTimeoutms = Integer.parseInt(timeout);
    }
    public Integer getreadTimeoutms(){
        return this.readTimeoutms;
    }

    public void setPathPropertiesBifast(PathPropertiesBifast pathPropertiesBifast){
        this.pathPropertiesBifast  = pathPropertiesBifast;
    }

    public PathPropertiesBifast getPathPropertiesBifast(){
        return  this.pathPropertiesBifast;
    }

    private void setNumThread(int numThread){
        this.numThread = numThread;
    }

    public int getNumThread (){
        return  this.numThread;
    }

    private void setFlagName(int num){
     this.flagName = num;
    }
    public int getFlagName(){
        return this.flagName;
    }


    public static BifastConfig fromProperties(String location){
       Properties props = new Properties();
       InputStream in = null;
        try{
        File file = new File(location);
        if (file.isFile()){
            in = new FileInputStream(location);
        }else {
            in = BifastConfig.class.getClassLoader().getResourceAsStream(location);
        }

        if (in == null){
            throw new IllegalStateException("File Properties tidak ditemukan di filesystem maupun classpath"+location);
        }

        props.load(in);

        }catch(IOException e){
        throw new UncheckedIOException("gagal membaca file properties "+location,e);

       }finally{
         if (in != null){
            try {in.close();
            }catch(IOException ignore){}}
       }
       return fromProperties(props);
    }

    public static BifastConfig fromProperties(Properties prop){
         BifastConfig cfg = new BifastConfig();
         cfg.setendpointAe(trimToNull(prop.getProperty("bifast.accountEnquiry.endpointUrl")));
         cfg.setendpointCT(trimToNull(prop.getProperty("bifast.creditTransfer.endpointUrl")));
         cfg.setapiKey(trimToNull(prop.getProperty("bifast.apiKey")));
         cfg.setConnectTimeoutms(trimToNull(prop.getProperty("bifast.connectTimeoutMs")));
         cfg.setreadTimeoutms(trimToNull(prop.getProperty("bifast.readTimeoutMs")));
         cfg.setendpointTi(trimToNull(prop.getProperty("bifast.paymentRequest.endpointUrl")));
         cfg.setNumThread(Integer.parseInt(trimToNull(prop.getProperty("bifast.thread"))));
         cfg.setFlagName(Integer.parseInt(trimToNull(prop.getProperty("bifast.flag.name.check"))));
         return cfg;
    }

    private static String trimToNull(String value){
        if (value==null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
