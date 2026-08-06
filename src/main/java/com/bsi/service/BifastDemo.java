package com.bsi.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.bsi.MainCHK;
import com.bsi.entity.mock.ProcessBifast;
import com.bsi.entity.mock.SpanSp2dStageIn;

public class BifastDemo {

    public static void main(String[] args) {
    ServiceMariaDb mariaDb = new ServiceMariaDb(
            "C:/Users/ven.arya/Downloads/Project/2026/SPAN/Custom Handler/span-custom-handler/tesDs",
            "bo2span");

    List<SpanSp2dStageIn> dataBifast = new ArrayList<>();

    MainCHK.tulisLog("[MULTI-THREAD] Running command prosesTransactionBifast...");
    try {
       mariaDb.excludeOutOfBalanceBO2();
       dataBifast = mariaDb.getDataBifast("BO2");
       ProcessBifast processBifast = new ProcessBifast();
       processBifast.paymentBifast(dataBifast, "BO2");
                    MainCHK.tulisLog("prosesTransactionBifast done..");
    } catch (SQLException E){

    }
 }     
}
