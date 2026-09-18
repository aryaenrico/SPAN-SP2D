package com.bsi.utility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.bsi.config.SpanConfig;
import com.bsi.entity.span.BifastRcMapping;
import com.bsi.entity.span.ReturnStatusAck;
import com.bsi.entity.span.SpanSp2dPosting;
import com.bsi.entity.span.SpanSp2dStageIn;
import com.bsi.entity.span.PaymentMethod;

public class Utillity {
    
    public static String generateSqlSaldo(){
        String sqlSaldo =
            "select account_number, amount_balance " +
            "from span_account " +
            "join span_application_config conf " +
            "  on span_account.account_number = conf.config_value " +
            "where conf.config_name = ? ";
            return sqlSaldo;
    }

    public static String updateStatusForExclude(int paramCount){
        StringBuilder sb = new StringBuilder();  
        for (int i = 0; i < paramCount; i++) {
            sb.append(i == 0 ? "?" : ",?");
        }
       
            return " update span_sp2d_stage_in " +
                    "set status = ? " +
                    "where documentdate = (select config_value from span_application_config where config_name = 'APP_DATE') " +
                    "and agentbankaccountnumber = ? " +
                    "and status = ?" +
                    "and applicationareamessageidentifier IN (" + sb.toString() + ")";
    }

    public static String updateStatusForInclude(){
        return 
                    "update span_sp2d_stage_in set status = ? WHERE " +
                            "documentdate = (select config_value from span_application_config where config_name = 'APP_DATE') " +
                            "AND agentbankaccountnumber = ? " +
                            "AND status = ?"
            ;
    }


    public static SpanConfig loadApplicationConfig(Connection conn) throws SQLException {
    String sql = "SELECT config_name, config_value FROM span_application_config WHERE config_type IN ('D', 'A')";
    SpanConfig config = new SpanConfig();
    try (PreparedStatement ps = conn.prepareStatement(sql);
        
        ResultSet rs = ps.executeQuery()) {
         
        while (rs.next()) {
            String name  = rs.getString("config_name");
        
            String value = rs.getString("config_value");
            
            
            if (name == null || value == null) continue;
            switch (name.trim().toUpperCase()) {
                case "ACCT_RPKBUN_GAJI":    config.setAcctRpkbunGaji(value);  break;
                case "ACCT_RR_RPKBUN_GAJI":  config.setAcctRrRpkbunGaji(value); break;
                case "BANK_CODE":            config.setBankCode(value);        break;
                case "BRANCH_CODE":          config.setBranchCode(value);      break;
                case "RTGS_CODE":            config.setRtgsCode(value);break;
                case "APP_DATE":             config.setAppDate(value);break;
                case "APP_DATE_REALTIME":    config.setAppDateRealtime(value);break;
                case "ACCT_RPKBUN_NON_GAJI" : config.setAcctRpkbunNonGaji(value);break;
                case "ACCT_RR_RPKBUN_NON_GAJI": config.setAcctRrRpkbunNonGaji(value);break;
                case "X_ACCT_RR_REKSUS_SBSN" :config.setAcctRrReksusSbsn(value);break;
                case "X_ACCT_REKSUS_SBSN" :config.setAcctReksusSbsn(value);break;
                case "ACCT_IA_KEWAJIBAN_BIFAST": config.setAcctIaKewajibanBifats(value);break;
            }
        }
        
    }
      return config;
   }

   public static String getSaldoGroupByMessageIndetifier(int paramCount){
     StringBuilder sb = new StringBuilder();  
        for (int i = 0; i < paramCount; i++) {
            sb.append(i == 0 ? "?" : ",?");
        }
     return "select applicationareamessageidentifier, count(1) count, sum(amount) sumamount " +
                        "from span_sp2d_stage_in " +
                        "where documentdate = (" +
                        "    select config_value " +
                        "    from span_application_config " +
                        "    where config_name = 'APP_DATE'" +
                        ") " +
                        "and agentbankaccountnumber = ? " +
                        "and status in ("+sb.toString()+") " +
                        "group by applicationareamessageidentifier ";
   }
   
   public static String updateSp2dUploaded(){
        return "";
   }

   public static Map<String, ReturnStatusAck> fetchReturnStatusAck(Connection conn) throws SQLException {
        String sql = "SELECT id, code, description, status, mapping_code FROM return_status_ack";
        Map<String, ReturnStatusAck> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ReturnStatusAck ack = new ReturnStatusAck();
                ack.id          = rs.getLong("id");
                ack.code        = rs.getString("code");
                ack.description = rs.getString("description");
                ack.status      = rs.getString("status");
                ack.mappingCode = rs.getString("mapping_code");
                if (ack.code != null) {
                    map.put(ack.mappingCode, ack);
                }
            }
        }
        return map;
    }

     public static Map<String, BifastRcMapping> fetchBifastRcMappingPSR(Connection conn) throws SQLException {
        String sql = "SELECT id, service_type, bifast_rc, bifast_description,span_rc , description_state from bifast_response_mapping " +
                     "WHERE service_type = 'PAYMENT_STATUS_REQUEST' ";
        Map<String, BifastRcMapping> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BifastRcMapping rc = new BifastRcMapping();
                rc.id          = rs.getLong("id");
                rc.service_type        = rs.getString("service_type");
                rc.bifast_rc = rs.getString("bifast_rc");
                rc.bifast_description      = rs.getString("bifast_description");
                rc.span_rc = rs.getString("span_rc");
                rc.description_state =rs.getString("description_state");

                map.put(rc.bifast_rc, rc);
            }
        }
        return map;
    }

    public static Map<String, BifastRcMapping> fetchBifastRcMappingAe(Connection conn) throws SQLException {
        String sql = "SELECT id, service_type, bifast_rc, bifast_description,span_rc , description_state from bifast_response_mapping " +
                     "WHERE service_type = 'ACCOUNT_INQUIRY' ";
        Map<String, BifastRcMapping> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BifastRcMapping rc = new BifastRcMapping();
                rc.id          = rs.getLong("id");
                rc.service_type        = rs.getString("service_type");
                rc.bifast_rc = rs.getString("bifast_rc");
                rc.bifast_description      = rs.getString("bifast_description");
                rc.span_rc = rs.getString("span_rc");
                rc.description_state =rs.getString("description_state");

                // handle duplicate rc 25
                String key = rc.bifast_rc;
                if (key.equals("25")){
                    String esbResponseMessage = safe(extractBifastDescription(rc.bifast_description));
                    key = key+"|"+esbResponseMessage;
                }
                map.put(key, rc);
            }
        }
        return map;
    }



public static String extractBifastDescription (String bifastDescription) {
    if (bifastDescription == null) {
        return null;
     }
    Pattern pattern = Pattern.compile("([A-Z]?\\d+[A-Z]?)");
    Matcher matcher = pattern.matcher(bifastDescription);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
}

  public static Map<String, BifastRcMapping> fetchBifastRcMappingCt(Connection conn) throws SQLException {
        String sql = "SELECT id, service_type, bifast_rc, bifast_description,span_rc , description_state from bifast_response_mapping " +
                     "WHERE service_type = 'CREDIT_TRANSFER' ";
        Map<String, BifastRcMapping> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BifastRcMapping rc = new BifastRcMapping();
                rc.id                  = rs.getLong("id");
                rc.service_type        = rs.getString("service_type");
                rc.bifast_rc           = rs.getString("bifast_rc");
                rc.bifast_description  = rs.getString("bifast_description");
                rc.span_rc             = rs.getString("span_rc");
                rc.description_state   =rs.getString("description_state");
                map.put(rc.bifast_rc, rc);

                String key = rc.bifast_rc;
                if (key.equals("25")){
                    String esbResponseMessage = safe(extractBifastDescription(rc.bifast_description));
                    key = key+"|"+esbResponseMessage;
                }
                map.put(key, rc);
            }
        }
        return map;
    }

   
   public static Map<String, BifastRcMapping> fetchBifastRcMappingAll(Connection conn) throws SQLException {
        String sql = "SELECT id, service_type, bifast_rc, bifast_description,span_rc , description_state from bifast_response_mapping";
        Map<String, BifastRcMapping> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BifastRcMapping rc         = new BifastRcMapping();
                rc.id                      = rs.getLong("id");
                rc.service_type            = rs.getString("service_type");
                rc.bifast_rc               = rs.getString("bifast_rc");
                rc.bifast_description      = rs.getString("bifast_description");
                rc.span_rc                 = rs.getString("span_rc");
                rc.description_state       = rs.getString("description_state");

                String key = rc.span_rc;
                map.put(key, rc);
            }
        }
        return map;
    }
   

    public static Map<String, BifastRcMapping> fetchBifastRcMappingRetur(Connection conn) throws SQLException {
        String sql = "SELECT id, service_type, bifast_rc, bifast_description,span_rc , description_state from bifast_response_mapping " +
                     "WHERE service_type = 'RETUR_T24' ";
        Map<String, BifastRcMapping> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BifastRcMapping rc = new BifastRcMapping();
                rc.id          = rs.getLong("id");
                rc.service_type        = rs.getString("service_type");
                rc.bifast_rc = rs.getString("bifast_rc");
                rc.bifast_description      = rs.getString("bifast_description");
                rc.span_rc = rs.getString("span_rc");
                rc.description_state =rs.getString("description_state");

                String key = rc.span_rc;
                while (map.containsKey(key)) {
                    key = key + "U";
                }
                map.put(key, rc);
            }
        }
        return map;
    }
   
   
   public static String safe(String s) { return s != null ? s : ""; }

   public static Map<String,PaymentMethod> fetchPaymentMethod (Connection conn) throws SQLException{
     String sql = "SELECT code, description FROM payment_method";
        Map<String, PaymentMethod> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PaymentMethod pm = new PaymentMethod();
                
                  pm.setCode(rs.getString("code"));
                  pm.setDescription(rs.getString("description"));
                if (pm.getCode() != null) {
                    map.put(pm.getCode(), pm);
                }
            }
        }
        return map;
   }
   
   public static SpanSp2dPosting builderSpanSp2dPosting (SpanSp2dStageIn stageIn){
      SpanSp2dPosting rec = new SpanSp2dPosting();
       rec.datePosting=new Date();
       rec.applicationareaSenderIdentifier = stageIn.getApplicationAreaSenderIdentifier();
       rec.applicationareaReceiverIdentifier = stageIn.getApplicationAreaReceiverIdentifier();
       rec.applicationareaDetailSenderIdentifier = stageIn.getApplicationAreaDetailSenderIdentifier();
       rec.applicationareaDetailReceiverIdentifier = stageIn.getApplicationAreaDetailReceiverIdentifier();
       rec.applicationareaCreationDatetime = stageIn.getApplicationAreaCreationDateTime() != null ? java.sql.Date.valueOf(stageIn.getApplicationAreaCreationDateTime().toLocalDate()) : null;
       rec.applicationareaMessageIdentifier = stageIn.getApplicationAreaMessageIdentifier();
       rec.applicationareaMessageTypeIndicator = stageIn.getApplicationAreaMessageTypeIndicator();
       rec.applicatioanareaMessageVersionText = stageIn.getApplicationAreaMessageVersionText();
       rec.documentDate =  java.sql.Date.valueOf(stageIn.getDocumentDate());
       rec.documentNumber =stageIn.getDocumentNumber();
       rec.beneficiaryName = stageIn.getBeneficiaryName();
       rec.beneficiaryAccount = stageIn.getBeneficiaryAccount();
       rec.amount = stageIn.getAmount();
       rec.currencyTarget = stageIn.getCurrencyTarget();
       rec.description = stageIn.getDescription();
       rec.agentBankCode = stageIn.getAgentBankCode();
       rec.agentBankAccountNumber = stageIn.getAgentBankAccountNumber();
       rec.agentBankAccountName = stageIn.getAgentBankAccountName();
       rec.emailAddress = stageIn.getEmailAddress();
       rec.swiftCode = stageIn.getSwiftCode();
       rec.ibanCode = stageIn.getIbanCode();
       rec.sp2dCount = stageIn.getSp2dCount();
       rec.totalCount = stageIn.getTotalCount();
       rec.totalAmount = stageIn.getTotalAmount();
       rec.totalBatchCount = stageIn.getTotalBatchCount();
       rec.sp2dNumber = stageIn.getSp2dNumber();
       rec.paymentMethod = stageIn.getPaymentMethod();
       rec.beneficiaryBank = stageIn.getBeneficiaryBank();
       rec.beneficiaryBankCode = stageIn.getAgentBankCode();
      return rec; 
   }
   
   public static String getDataPostingForGenerateAckRetur(){
      return" SELECT " +
            "  id, applicationareasenderidentifier, applicationareareceiveridentifier," +
            "  applicationareadetailsenderidentifier, applicationareadetailreceiveridentifier," +
            "  applicationareacreationdatetime, applicationareamessageidentifier," +
            "  applicationareamessagetypeindicator, applicatioanareamessageversiontext," +
            "  documentdate, documentnumber, beneficiaryname, beneficiarybankcode," +
            "  beneficiarybank, beneficiaryaccount, amount, currencytarget," +
            "  description, agentbankcode, agentbankaccountnumber, agentbankaccountname," +
            "  emailaddress, swiftcode, ibancode, paymentmethod," +
            "  reference_number, return_code " +
            "  FROM span_sp2d_posting " +
            "  WHERE flag_ack IS NULL " +
            "  AND documentnumber = ?"; 
   }


   public static String getDataForProsesAckBatch (){
     return "SELECT " +
            "posting.applicationareamessagetypeindicator, " +
            "posting.applicationareasenderidentifier, " +
            "posting.applicationareamessageidentifier, " +
            "posting.amount, " +
            "posting.return_code, " +
            "posting.documentnumber, " +
            "posting.paymentmethod " +
            "FROM span_sp2d_posting posting " +
            "INNER JOIN span_sp2d_stage_in stagein " +
            "ON posting.documentnumber = stagein.documentnumber " +
            "WHERE posting.flag_ack IS NULL " +
            "AND posting.paymentmethod = '5' " +
            "AND stagein.flag_ack IS NULL " +
            "AND posting.status = 'FIN-000' AND stagein.status = 'FIN-000'";
   }
   public static String getDataPostingForGenerateAck(){
       return  " SELECT " +
            "  id, applicationareasenderidentifier, applicationareareceiveridentifier," +
            "  applicationareadetailsenderidentifier, applicationareadetailreceiveridentifier," +
            "  applicationareacreationdatetime, applicationareamessageidentifier," +
            "  applicationareamessagetypeindicator, applicatioanareamessageversiontext," +
            "  documentdate, documentnumber, beneficiaryname, beneficiarybankcode," +
            "  beneficiarybank, beneficiaryaccount, amount, currencytarget," +
            "  description, agentbankcode, agentbankaccountnumber, agentbankaccountname," +
            "  emailaddress, swiftcode, ibancode, paymentmethod," +
            "  reference_number, return_code " +
            "  FROM span_sp2d_posting " +
            "  WHERE flag_ack IS NULL " +
            "  AND beneficiaryaccount <> ? " +  
            "  AND beneficiaryaccount <> ? " +  
            "  AND agentbankaccountnumber IN (?, ?) " +
            "  AND documentnumber = ?"; 
   }

   public static String updateFlagAckTablePosting(){
     return "UPDATE span_sp2d_posting SET flag_ack = 'Y' " +
            "WHERE flag_ack IS NULL " +
            " AND beneficiaryaccount <> ? " +   
            " AND beneficiaryaccount <> ? " +   
            " AND agentbankaccountnumber IN (?, ?)" +
            " AND documentnumber = ? " +
            " AND paymentmethod ='5' "; 
   }

   public static String updateFlagAckDoubeTable(String nameTable){
      String tablename  ="";

      switch (nameTable) {
             case "span_sp2d_stage_in":
              tablename = "span_sp2d_stage_in";
             break;
           default:
              tablename = "span_sp2d_posting";
      }
     return " UPDATE "+ tablename + " SET flag_ack = 'Y' " +
            " WHERE flag_ack IS NULL " +
            " AND documentnumber = ? " +
            " AND paymentmethod ='5' "; 
     }

   public static String updateSp2dStageIn(int paramCount ){
        StringBuilder sb = new StringBuilder();  
        for (int i = 0; i < paramCount; i++) {
            sb.append(i == 0 ? "?" : ",?");
        }
       return "UPDATE span_sp2d_stage_in SET status = ? " +
                     "WHERE documentdate = ? " +
                     "AND status = ? OR status = ? " +
                     "AND paymentmethod ='5' " +
                     "AND documentnumber = ? " + 
                     "AND agentbankaccountnumber IN ("+sb.toString()+") ";
     }

   public static String updateStatusForRetryRetur(String nameTable){
        String tablename  ="";
        switch (nameTable) {
             case "span_sp2d_stage_in":
              tablename = "span_sp2d_stage_in";
             break;
           default:
              tablename = "span_sp2d_posting";
      }
        return "UPDATE " + tablename + " SET status = ? " +
                "WHERE documentdate = ? " +
                "AND documentnumber = ? ";
     }

   public static String getTransactionType(SpanSp2dStageIn spanSp2dStageIn , SpanConfig config){
         String debitAccount = spanSp2dStageIn.getAgentBankAccountNumber();
         String result="";
         if (safe(debitAccount).equals(config.getAcctRpkbunGaji())){
             result = "BO2";
         } else if (safe(debitAccount).equals(config.getAcctRpkbunNonGaji())){
             result = "BO1";
         } else {
             result = "REKSUS";
         }
         return result;
     }

    public static String getTransactionTypeForAck(SpanSp2dPosting spanSp2dStageIn , SpanConfig config){
        String debitAccount = spanSp2dStageIn.agentBankAccountNumber;
        String result="";
        if (safe(debitAccount).equals(config.getAcctRpkbunGaji())){
            result = "BO2";
        } else if (safe(debitAccount).equals(config.getAcctRpkbunNonGaji())){
            result = "BO1";
        } else {
            result = "REKSUS";
        }
        return result.trim().toUpperCase();
    }

   public static String finalizeSuccesRecord(String sourceAccount , String sourceRetur ){
    
      return  "UPDATE span_sp2d_stage_in SET date_posting = ?, status = 'FIN-000' " +
                 "WHERE status = ? " +
                 "AND agentbankaccountnumber IN ('" + sourceAccount + "','" + sourceRetur + "') " + 
                 "AND paymentmethod = '5' " +
                 "AND documentnumber = ? ";
   } 

   public static String finalizeSuccesRecordAfterGetStatus(String sourceAccount , String sourceRetur ){
    
      return  "UPDATE span_sp2d_stage_in SET date_posting = ?, status = 'FIN-000' , return_code = '000' " +
                 "WHERE status = ?  OR status = ? " +
                 "AND agentbankaccountnumber IN ('" + sourceAccount + "','" + sourceRetur + "') " + 
                 "AND paymentmethod = '5' " +
                 "AND documentnumber = ? ";
   } 


   public static String getBankCode(){
        return "SELECT participant FROM bank_code_mapping WHERE account_code = ? AND status = 'Active' ";
   }

   public static String findApproveData(){
    return " SELECT beneficiary_account,stagein_beneficiary_name,inquiry_beneficiary_name,approval_status " +
           " FROM span_sp2d_bifast_name_checking WHERE beneficiary_account = ? " +  
           " AND stagein_beneficiary_name = ? AND inquiry_beneficiary_name = ? LIMIT 1";
   }

   public static String normalizeName(String name) {
     if (name == null) {
        return "";
      }
      return name.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    public static String insertAuditTrail(){
        return "INSERT INTO span_sp2d_bifast_audit_trail "+
               "(document_number,activity_id,user_id,old_value,new_value,created_at) "+
               " VALUES (?,?,?,?,?,?)";
    }

    public static String getIdUser(){
        return "SELECT id FROM users  WHERE email = ? and name = ? ";
    }

    public static String getDataBifastByDocumentNumber(){
        return "SELECT document_number , end_to_end_id from span_sp2d_bifast_data "+
               " where document_number = ? ";
    }

    public static String insertDataSp2dBifast(){
        return "INSERT INTO span_sp2d_bifast_data "+
               "(document_number) "+
               " VALUES (?)";
    }

     public static String insertDataSp2dBifastDocEndToEndId(){
        return "INSERT INTO span_sp2d_bifast_data "+
               "(document_number,end_to_end_id) "+
               " VALUES (?,?)";
    }

     public static String sanitizePaymentDetail(String paymentDetails){
        String result ="";
        if (paymentDetails == null){
            return "";
        }
        result = paymentDetails.toUpperCase().replaceAll("[^A-Z0-9\\- ]", "");
        if (result.length() > 64){
             result = result.substring(0, 64);
        }
        return result;
     }

     public static String constructPaymentInformationBifast(SpanSp2dStageIn item){
        String result="";
        if (item.getDescription() == null){
            return "SPAN-"+item.getDocumentNumber()+"-";
        }
         result ="SPAN-"+item.getDocumentNumber()+"-"+sanitizePaymentDetail(item.getDescription());

         return result;
     }

     public static String updateStatusSpanSp2dBifastData(){
        return "UPDATE span_sp2d_bifast_data set bifast_response_code = ? " +
               "WHERE document_number = ? ";
     }
}
