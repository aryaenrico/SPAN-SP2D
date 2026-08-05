package com.bsi.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.bsi.MainCHK;
import com.bsi.entity.mock.ProcessBifast;
import com.bsi.entity.mock.SpanSp2dStageIn;

public class BifastDemo {

    public static void main(String[] args) {
    ServiceMariaDb mariaDb = new ServiceMariaDb(
            "C:/Users/ven.arya/Downloads/Project/2026/SPAN/Custom Handler/span-custom-handler/tesDs",
            "bo2span");

    List<SpanSp2dStageIn> data = new ArrayList<>();
    
    int maxDataPerprocess = 15;
    int jumlahThread =3;

    ExecutorService executor = Executors.newFixedThreadPool(jumlahThread);
    List<Future<String>> futures = new ArrayList<>();
    
    try {
      mariaDb.excludeOutOfBalanceBO2();
      data = mariaDb.getDataBifast("BO2");
      System.out.println("jumlah transaki yang akan di proses melalui bifast :"+data.size());
      if (!data.isEmpty()){
         for (int i=0 ; i < data.size(); i+=maxDataPerprocess){
            int paramEnd = Math.min(data.size(),i+maxDataPerprocess);
            List<SpanSp2dStageIn> batchData = new ArrayList<>(data.subList(i, paramEnd));
            int nomorBatch =(i/maxDataPerprocess)+1;
            futures.add(
                executor.submit(()->{
                    String status = "SUCCESS";
                    try{
                     ProcessBifast processBifast = new ProcessBifast();
                     status = processBifast.paymentBifast(batchData,"BO2");
                    } catch(Exception e ){
                       status = e.getMessage();
                    }
                    return status;
                })
            );
            System.out.println("Batch yang jalan"+ nomorBatch);
         }    
         executor.shutdown();
         try{
             if (!executor.awaitTermination(30, TimeUnit.MINUTES)){
                 executor.shutdownNow();
             }
         } catch(InterruptedException e){
             executor.shutdownNow();
             Thread.currentThread().interrupt();
             MainCHK.tulisLog("Error Terjadi"+e.getMessage());
         }

         for (Future<String> future : futures) {
             try {
                 System.out.println("Hasil eksekusi: " + future.get());
             } catch (Exception e) {
                 System.err.println("Gagal mengeksekusi task: " + e.getMessage());
                 e.printStackTrace();
             }
         }
      }
    } 
    catch (SQLException e){
      MainCHK.tulisLog(e.getMessage());
    }   
}     
}
