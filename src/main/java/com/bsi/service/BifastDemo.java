package com.bsi.service;

import java.io.File;
import java.util.List;
import com.bsi.MainCHK;
import com.bsi.config.BifastConfig;
import com.bsi.entity.mock.ProcessBifast;
import com.bsi.entity.mock.SpanSp2dStageIn;
import com.bsi.entity.span.PathPropertiesBifast;


public class BifastDemo {

    public static void main(String[] args) {
        System.out.println("Start");
        ServiceMariaDb mariaDb = new ServiceMariaDb("C:\\Users\\ven.arya\\Downloads\\Project\\2026\\SPAN\\Custom Handler\\span-custom-handler\\tesDs", "bo2span");
        

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
                    List<SpanSp2dStageIn> dataRgs = mariaDb.getDataBifastForSchedulerPurpose(mariaDb.statusForRetryGetstatus);
                    PathPropertiesBifast pathPropertiesBifast = new PathPropertiesBifast("C:\\Users\\ven.arya\\Downloads\\Project\\2026\\SPAN\\Custom Handler\\span-custom-handler\\tesDs","bo2span");
                    String configFilePath = pathPropertiesBifast.getPathProp() + (pathPropertiesBifast.getPathProp().endsWith("/") || pathPropertiesBifast.getPathProp().endsWith("\\") ? "" : File.separator) + pathPropertiesBifast.getPropName() + ".properties";
                    MainCHK.tulisLog("[CONFIG] Loading BifastConfig from: " + configFilePath);

                    BifastConfig config = BifastConfig.fromProperties(configFilePath);

                    ProcessBifast processBifast = new ProcessBifast(config, pathPropertiesBifast);
                    processBifast.prosesTimeoutCtBifast(dataRgs);
                    //processBifast.paymentBifast(dataBifast);
                    //mariaDb.includeOutOfBalanceBO1Bifast();
                    MainCHK.tulisLog("Proses posting transaction done..");
            }
                catch(Exception e){
                System.out.println(e.getMessage());
            }
 }     
}
