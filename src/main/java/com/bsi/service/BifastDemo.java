package com.bsi.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.bsi.MainCHK;
import com.bsi.config.BifastConfig;
import com.bsi.config.T24Config;
import com.bsi.entity.span.BifastRcMapping;
import com.bsi.entity.span.PathPropertiesBifast;
import com.bsi.entity.t24.AccountDetailsSoapRequest;
import com.bsi.utility.Utillity;


public class BifastDemo {

    public static void main(String[] args) {
        System.out.println("Start");
        //ServiceMariaDb mariaDb = new ServiceMariaDb("C:\\Users\\ven.arya\\Downloads\\Project\\2026\\SPAN\\Custom Handler\\span-custom-handler\\tesDs", "bo2span");
        String absolutPath = "satu/dua/tiga.out";

        Map<String, BifastRcMapping> map = new HashMap<>();



        BifastRcMapping rc = new BifastRcMapping();
        rc.id                  = 2L;
        rc.service_type        = "CREDIT_TRANSFER";
        rc.bifast_rc           = "25";
        rc.bifast_description  = "";
        rc.span_rc             = "AC0001";
        rc.description_state   ="DESKRIPTION";


        String key = rc.bifast_rc;
        System.out.println("Key1 : "+key);
        if (key.equals("25")){
            String esbResponseMessage = Utillity.safe(Utillity.extractBifastDescription(rc.bifast_description));
            key = "25|"+esbResponseMessage;
        }
        map.put(key,rc);


        BifastRcMapping rc2 = new BifastRcMapping();
        rc2.id                  = 2L;
        rc2.service_type        = "CREDIT_TRANSFER";
        rc2.bifast_rc           = "25";
        rc2.bifast_description  = "U999";
        rc2.span_rc             = "AC0001";
        rc2.description_state   ="DESKRIPTION";


      key = rc2.bifast_rc;
        System.out.println("Key 2: "+key);
        if (key.equals("25")){
            String esbResponseMessage = Utillity.safe(Utillity.extractBifastDescription(rc2.bifast_description));
            key = "25|"+esbResponseMessage;
        }
        map.put(key,rc2);



        for (String data : map.keySet()){
            System.out.println(data);
        }


        //File file = new File(absolutPath);
        //System.out.println(file.getName().replaceFirst("\\.out$",".txt"));
        System.exit(0);

     /*
        try{
        PathPropertiesBifast pathPropertiesBifast = new PathPropertiesBifast("C:\\Users\\ven.arya\\Downloads\\Project\\2026\\SPAN\\Custom Handler\\span-custom-handler\\tesDs","bo2span"); 
        String configFilePath = pathPropertiesBifast.getPathProp() + (pathPropertiesBifast.getPathProp().endsWith("/") || pathPropertiesBifast.getPathProp().endsWith("\\") ? "" : File.separator) + pathPropertiesBifast.getPropName() + ".properties";
        System.out.println(configFilePath);
        BifastConfig config = BifastConfig.fromProperties(configFilePath);
       List<SpanSp2dStageIn>dataBifast = mariaDb.getDataBifast("BO2");
       ProcessBifast processBifast = new ProcessBifast(config, pathPropertiesBifast);
       processBifast.paymentBifast(dataBifast);
       MainCHK.tulisLog("prosesTransactionBifast done..");
        }catch (SQLException e ){
            System.out.println(e.getMessage());
        }
             */
             
     
        

       /* 
        MainCHK.tulisLog("Running command proses Retry Retur...");
        List<SpanSp2dStageIn> dataRrs = mariaDb.getDataBifastForSchedulerPurpose(mariaDb.statusForRetryRetur);
        ProcessBifast processBifast = new ProcessBifast();
        processBifast.prosesRetryRetur(dataRrs);
        MainCHK.tulisLog("proses Retry Retur done..");
        */
         
        /* 
         MainCHK.tulisLog("Running scheduler prosesRetryGetstatus...");
         List<SpanSp2dStageIn> dataRgs = mariaDb.getDataBifastForSchedulerPurpose(mariaDb.statusForRetryGetstatus);
         ProcessBifast processBifast = new ProcessBifast();
         processBifast.prosesTimeoutCtBifast(dataRgs);
         MainCHK.tulisLog("prosesRetryGetstatus done..");
         */


        //List<SpanSp2dStageIn> dataBifast = serviceMariaDb.getDataBifast("BO2");
        /*
        PathPropertiesBifast pathPropertiesBifast = new PathPropertiesBifast("C:\\Users\\user\\Downloads\\Prep This year (must done)\\span-custom-handler(2026-08-11)\\span-custom-handler\\tesDs","bo2span");
        String configFilePath = pathPropertiesBifast.getPathProp() + (pathPropertiesBifast.getPathProp().endsWith("/") || pathPropertiesBifast.getPathProp().endsWith("\\") ? "" : File.separator) + pathPropertiesBifast.getPropName() + ".properties";
        BifastConfig config = BifastConfig.fromProperties(configFilePath);
        SpanConfig config1 = SpanConfig.fromProperties(configFilePath);
        config.setPathPropertiesBifast(pathPropertiesBifast);
        ProcessBifast processBifast = new ProcessBifast(config, pathPropertiesBifast);
        System.out.println("Data : "+config1.getPathSpanAcknowledgePut());
        System.out.println(processBifast.configT24.getUserName());
         */
        try{

                  /* 
                    MainCHK.tulisLog("Start proses posting transaction...");
                    List<SpanSp2dStageIn> dataRgs = mariaDb.getDataBifastForSchedulerPurpose(mariaDb.statusForRetryGetstatus);

                    PathPropertiesBifast pathPropertiesBifast = new PathPropertiesBifast("C:\\Users\\ven.arya\\Downloads\\Project\\2026\\SPAN\\Custom Handler\\span-custom-handler\\tesDs","bo2span");
                    String configFilePath = pathPropertiesBifast.getPathProp() + (pathPropertiesBifast.getPathProp().endsWith("/") || pathPropertiesBifast.getPathProp().endsWith("\\") ? "" : File.separator) + pathPropertiesBifast.getPropName() + ".properties";
                    MainCHK.tulisLog("[CONFIG] Loading BifastConfig from: " + configFilePath);

                    BifastConfig config = BifastConfig.fromProperties(configFilePath);

                    ProcessBifast processBifast = new ProcessBifast(config, pathPropertiesBifast);
                    processBifast.prosesTimeoutCtBifast(dataRgs);

                    MainCHK.tulisLog("Proses posting transaction done..");
                     */

                    //mariaDb.excludeOutOfBalanceBO1();
                    MainCHK.tulisLog("Start proses posting transaction...");
                    //List<SpanSp2dStageIn> dataBifast = mariaDb.getDataBifast("BO1");
                    //List<SpanSp2dStageIn> dataRrs = mariaDb.getDataBifastForSchedulerPurpose(mariaDb.statusForManualRetur);
                    //List<SpanSp2dStageIn> dataRgs = mariaDb.getDataBifastForSchedulerPurpose(mariaDb.statusForRetryGetstatus);
                    //PathPropertiesBifast pathPropertiesBifast = new PathPropertiesBifast("C:\\Users\\ven.arya\\Downloads\\Project\\2026\\SPAN\\Custom Handler\\span-custom-handler\\tesDs","bo2span");
                    PathPropertiesBifast pathPropertiesBifast = new PathPropertiesBifast("C:\\Users\\user\\Downloads\\span-custom-handler(2026-09-04)\\span-custom-handler\\tesDs","bo2span");
                    String configFilePath = pathPropertiesBifast.getPathProp() + (pathPropertiesBifast.getPathProp().endsWith("/") || pathPropertiesBifast.getPathProp().endsWith("\\") ? "" : File.separator) + pathPropertiesBifast.getPropName() + ".properties";
                    MainCHK.tulisLog("[CONFIG] Loading BifastConfig from: " + configFilePath);

                    BifastConfig config = BifastConfig.fromProperties(configFilePath);

                    ProcessBifast processBifast = new ProcessBifast(config, pathPropertiesBifast);
                    AccountDetailsSoapRequest req = new AccountDetailsSoapRequest();
                     req.idiAccountCmsType.enquiryInputCollection.add(
                     new AccountDetailsSoapRequest.EnquiryInput("ID", "7927927298", "EQ"));
                     T24Client t24Client = new T24Client(T24Config.fromProperties(configFilePath));
                     t24Client.accountDetails(req);

                    //processBifast.prosesTimeoutCtBifast(dataRgs);
                    //processBifast.paymentBifast(dataBifast);
                    //mariaDb.includeOutOfBalanceBO1Bifast();
                    MainCHK.tulisLog("Proses posting transaction done..");
            }
                catch(Exception e){
                System.out.println(e.getMessage());
            }
 }     
}
