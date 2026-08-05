package com.bsi.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.bsi.MainCHK;
import com.bsi.entity.span.ReturnStatusAck;
import com.bsi.entity.span.SpanSp2dPosting;
import com.bsi.utility.Utillity;

public class GenerateAckDemo {
    public static void main(String[] args) {
        ServiceMariaDb mariaDb = new ServiceMariaDb(
            "C:/Users/ven.arya/Downloads/Project/2026/SPAN/Custom Handler/span-custom-handler/tesDs",
            "bo2span");

        List<SpanSp2dPosting> postedData = new ArrayList<>();

        try{
          postedData =  mariaDb.fetchSp2dPostingForGaji();
          Map<String, ReturnStatusAck> statusAckMap = Utillity.fetchReturnStatusAck(mariaDb.conn);
         if (!postedData.isEmpty()){

         }
        
        } catch(SQLException e ){
            MainCHK.tulisLog("Error saat fect data dari tabel return status ack :"+e.getMessage());
            e.printStackTrace();

        }
        
    }
}
