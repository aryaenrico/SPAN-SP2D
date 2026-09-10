package com.bsi.service;

import com.bsi.MainCHK;
import com.bsi.config.SpanConfig;
import com.bsi.entity.PostingRequest;
import com.bsi.entity.bifast.accountinquiry.AccountInquiryResponse;
import com.bsi.entity.bifast.credittransfer.CreditTransferResponse;
import com.bsi.entity.span.SpanSp2dStageIn;
import com.bsi.entity.span.BifastRcMapping;
import com.bsi.entity.span.GenerateAckOut;
import com.bsi.entity.span.ReturnStatusAck;
import com.bsi.entity.span.SpanSp2dPosting;
import com.bsi.entity.t24.FundsTransferSoapResponse;
import com.bsi.utility.Utillity;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceMariaDb {
    Connection conn = null;
    Statement st;
    public PreparedStatement ps;
    ResultSet rs, rs2;
    String namaFile, tglSP2D;
    ResourceBundle rb;
    RestClient restClient;
    public long rowCount;
    public SpanConfig spanConfig;

    public final String statusReadyProses = "UPL-000";
    public final String statusRetryProses = "RDY-008";
    public final String statusWaitingUPL = "UPK-000";
    public final String statusWaitingUPV = "UPV-000";
    public final String statusReadyPosting = "PST-000";
    public final String paymentMethodAfiliasi = "4";
    public String ACCT_RPKBUN_GAJI;
    public String ACCT_RPKBUN_NON_GAJI;
    public final String statusWaitingDropping = "UPW-000";
    public final String statusVoid = "VOD-201";
    public final String prefixStatusVoid = "VOD-";
    public final String voidAmount = "204";
    public final String voidAlreadyPosted = "206";
    public final String voidExpired = "205";
    public final String voidNotFound = "203";
    public final String statusPosted ="FIN-000";
    
    // bifast status 
    public final String statusReadyProsesBifast ="RPB-000";
    public final String flaggingBifast = "UPB-000";
    public final String statusWaitingDroppingBifast ="UBW-000";
    public final String statusForRetryGetstatus ="RGS-000";
    public final String statusForRetryRetur ="RRS-000";
    public final String statusTimeoutCreditTransferBifast = "TMO-000";
    public final String statusTimeoutCreditTransferCore = "TMO-001";
    public final String statusForManualRetur = "RMR-000";
    public final String statusForApprovedValidationName ="AVN-000";

    private static final String PAYMENT_METHOD_BIFAST = "5";


    //esb response code
    public final String rcForTimeoutCtProcess ="51";
    public final String rcForFailedProcess ="25";
    public final String rcForTimeoutCoreProcess ="57";
    public final String rcForCorePostingFailed ="05";
    public final String rcForTimeoutGetstatus ="68";

    

    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

    public ServiceMariaDb(String pathProp, String propName) {
        try {
            File file = new File(pathProp);
            URL[] urls = {file.toURI().toURL()};
            ClassLoader loader = new URLClassLoader(urls);
            rb = ResourceBundle.getBundle(propName, Locale.getDefault(), loader);

            System.setProperty("line.separator", "\r");
            Class.forName("org.mariadb.jdbc.Driver");
            String serverSpan = rb.getString("db_bo2span_host_name").trim();
            String dbSpan = rb.getString("db_bo2span_database_name").trim();
            String usrSpan = rb.getString("db_bo2span_user_name").trim();
            String pwdSpan = rb.getString("db_bo2span_password").trim();
            this.rowCount = 0;
            String url = "jdbc:mariadb://" + serverSpan + ":3306/" + dbSpan;
            conn = DriverManager.getConnection(url, usrSpan, pwdSpan);
            st = conn.createStatement();
            conn.setAutoCommit(true);
            
            MainCHK.tulisLog("DB Connected:" + url);

            spanConfig = Utillity.loadApplicationConfig(conn);
            spanConfig.setPathBo2spanHome(rb.getString("path_bo2span_home").trim());
            spanConfig.setPathBo2spanConfig(rb.getString("path_bo2span_config").trim());
            spanConfig.setPathSpanAcknowledgePut(rb.getString("path_bo2span_span_acknowledge_put").trim());
            spanConfig.setPathSpanAcknowledgeArchive(rb.getString("path_bo2span_span_acknowledge_archive").trim());

            String baseurl_api_magic = rb.getString("baseurl_api_magic").trim();
            String clientId = rb.getString("magic_clientId").trim();
            String clientSecret = rb.getString("magic_clientSecret").trim();
            restClient = new RestClient(baseurl_api_magic, clientId, clientSecret);

        } catch (ClassNotFoundException e) {
            MainCHK.tulisLog("Error 1. Cek konfigurasi koneksi database");
            e.printStackTrace(System.out);
        } catch (SQLException ex) {
            MainCHK.tulisLog("Error 2. Cek konfigurasi koneksi database");
            ex.printStackTrace(System.out);
        } catch (MalformedURLException ex) {
            Logger.getLogger(ServiceMariaDb.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        ServiceMariaDb db = new ServiceMariaDb("D:/BSI/Span/sp2d_check_negative_amount/tesDs", "bo2span");
        try {
            db.postingDetailAffiliate();
        } catch (SQLException ex) {
            Logger.getLogger(ServiceMariaDb.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public long getRowCount() {
        return this.rowCount;
    }

    public void close() {
        try {
            if (null != rs) {
                rs.close();
            }
            if (null != rs2) {
                rs2.close();
            }
            if (null != st) {
                st.close();
            }
            if (null != conn) {
                conn.close();
            }
            if (null != ps) {
                ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace(System.out);
            MainCHK.tulisLog("eror close db: " + e);
        }

    }

    public void checkVoidList() throws SQLException {
        PreparedStatement qSelect = null, qSelect2 = null, qInsert = null;
        try {
            qSelect = conn.prepareStatement(
                    "SELECT sp2d_number FROM span_sp2d_stage_in " +
                            "WHERE status = ? and amount < 0 GROUP BY sp2d_number;"
            );
            qSelect.setString(1, statusWaitingUPL);
            MainCHK.tulisLog(qSelect.toString());
            rs = qSelect.executeQuery();
            while (rs.next()) {
                String sp2dno = rs.getString("sp2d_number");
                MainCHK.tulisLog("negative amount sp2d_number:" + sp2dno);
                qSelect2 = conn.prepareStatement(
                        "SELECT * from span_void_list where sp2d_number = ?;"
                );
                qSelect2.setString(1, sp2dno);
                rs2 = qSelect2.executeQuery();
                MainCHK.tulisLog(qSelect2.toString());
                if (!rs2.next()) {
                    qInsert = conn.prepareStatement(
                            "INSERT INTO span_void_list (sp2d_number, status) VALUES (?, ?)"
                    );
                    qInsert.setString(1, rs.getString("sp2d_number"));
                    qInsert.setString(2, statusVoid);
                    qInsert.executeUpdate();
                    MainCHK.tulisLog(qInsert.toString());
                }
                if (null != rs2) {
                    rs2.close();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
            MainCHK.logger.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (qSelect != null) {
                qSelect.close();
            }
            if (qSelect2 != null) {
                qSelect.close();
            }
            if (qInsert != null) {
                qInsert.close();
            }
        }
    }

    public void updStageIn() throws SQLException {
        PreparedStatement qSelect = null, qUpdate = null;
        try {
            qSelect = conn.prepareStatement(
                    "select a.sp2d_number sp2d_number, a.sp2dcount sp2dcount, count(id) as cid " +
                            "from span_sp2d_stage_in a where status = ? " +
                            "group by a.sp2d_number, a.sp2dcount;"
            );
            qSelect.setString(1, statusWaitingUPL);
            MainCHK.tulisLog(qSelect.toString());
            rs = qSelect.executeQuery();
            while (rs.next()) {
                String existingSp2d = rs.getString("cid");
                String allSp2d = rs.getString("sp2dcount");
                if (existingSp2d.equals(allSp2d)) {
                    qUpdate = conn.prepareStatement(
                            "update span_sp2d_stage_in set status = ? WHERE sp2d_number = ? AND status = ?"
                    );
                    qUpdate.setString(1, statusReadyProses);
                    qUpdate.setString(2, rs.getString("sp2d_number"));
                    qUpdate.setString(3, statusWaitingUPL);
                    qUpdate.executeUpdate();
                    MainCHK.tulisLog(qUpdate.toString());
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
            MainCHK.logger.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (qSelect != null) {
                qSelect.close();
            }
            if (qUpdate != null) {
                qUpdate.close();
            }
        }
    }

    public void checkPaymentMethod4() throws SQLException {
        PreparedStatement qSelect = null, qUpdate = null, qInsertVoidList = null;
        try {
            qSelect = conn.prepareStatement("select beneficiaryaccount, sp2d_number, amount, documentdate, documentnumber, applicationareamessageidentifier, reference_number, return_code from span_sp2d_stage_in a where status = ? and a.paymentmethod = ? ;");
            qSelect.setString(1, statusWaitingUPV);
            qSelect.setString(2, paymentMethodAfiliasi);
            MainCHK.tulisLog(qSelect.toString());
            rs = qSelect.executeQuery();
            while (rs.next()) {
                String kodeReferal = rs.getString("beneficiaryaccount");
                String documentNumber = rs.getString("documentnumber");
                String sp2d_number = rs.getString("sp2d_number");
                String amount = (Math.round(rs.getDouble("amount"))) + "";

                MainCHK.tulisLog("Checking payment method for documentnumber/beneficiaryaccount(kodeReferal):" + documentNumber + "/" + kodeReferal);
                Response response = restClient.getMasterValidasi(kodeReferal);
                if (response.getStatus() == 401 || response.getStatus() == 403) {
                    response = restClient.getMasterValidasi(kodeReferal);
                }
                String responseString = response.readEntity(String.class);
                JsonObject jsonObject = JsonParser.parseString(responseString).getAsJsonObject();
                MainCHK.tulisLog("response: " + jsonObject.toString());
                String status = jsonObject.get("status").getAsString();
                String message = jsonObject.get("message").getAsString();
                if (response.getStatus() == 200) {
                    String queryVoid = "INSERT INTO span_void_list (sp2d_number, status) VALUES (?, ?)";
                    String queryStageIn = "update span_sp2d_stage_in set return_code = ?, status = ? WHERE documentnumber = ? ";
                    switch (status) {
                        case "00":
                            String totalAmount = jsonObject.get("data").getAsJsonObject().get("totalAmount").getAsString();
                            if (totalAmount.equals(amount)) {
                                MainCHK.tulisLog("amount match: [" + totalAmount + "] == [" + amount + "]");
                                qUpdate = conn.prepareStatement("update span_sp2d_stage_in set status = ? WHERE documentnumber = ? AND status = ?");
                                qUpdate.setString(1, statusReadyProses);
                                qUpdate.setString(2, documentNumber);
                                qUpdate.setString(3, statusWaitingUPV);
                                qUpdate.executeUpdate();
                                MainCHK.tulisLog(qUpdate.toString());
                            } else {
                                qUpdate = conn.prepareStatement(queryStageIn);
                                qUpdate.setString(1, voidAmount);
                                qUpdate.setString(2, statusReadyProses);
                                qUpdate.setString(3, documentNumber);
                                qUpdate.executeUpdate();
                                MainCHK.tulisLog("amount not match: [" + totalAmount + "] != [" + amount + "]");
                                qInsertVoidList = conn.prepareStatement(queryVoid);
                                qInsertVoidList.setString(1, sp2d_number);
                                qInsertVoidList.setString(2, prefixStatusVoid + voidAmount);
                                qInsertVoidList.executeUpdate();
                                MainCHK.tulisLog(qInsertVoidList.toString());
                            }
                            break;
                        case "91": //already posted
                            qUpdate = conn.prepareStatement(queryStageIn);
                            qUpdate.setString(1, voidAlreadyPosted);
                            qUpdate.setString(2, statusReadyProses);
                            qUpdate.setString(3, documentNumber);
                            qUpdate.executeUpdate();
                            MainCHK.tulisLog("response code : [" + status + "] with message [" + message + "]");
                            qInsertVoidList = conn.prepareStatement(queryVoid);
                            qInsertVoidList.setString(1, sp2d_number);
                            qInsertVoidList.setString(2, prefixStatusVoid + voidAlreadyPosted);
                            qInsertVoidList.executeUpdate();
                            MainCHK.tulisLog(qInsertVoidList.toString());
                            break;
                        case "92": //expired
                            qUpdate = conn.prepareStatement(queryStageIn);
                            qUpdate.setString(1, voidExpired);
                            qUpdate.setString(2, statusReadyProses);
                            qUpdate.setString(3, documentNumber);
                            qUpdate.executeUpdate();
                            MainCHK.tulisLog("response code : [" + status + "] with message [" + message + "]");
                            qInsertVoidList = conn.prepareStatement(queryVoid);
                            qInsertVoidList.setString(1, sp2d_number);
                            qInsertVoidList.setString(2, prefixStatusVoid + voidExpired);
                            qInsertVoidList.executeUpdate();
                            MainCHK.tulisLog(qInsertVoidList.toString());
                            break;
                        case "99": //not found
                            qUpdate = conn.prepareStatement(queryStageIn);
                            qUpdate.setString(1, voidNotFound);
                            qUpdate.setString(2, statusReadyProses);
                            qUpdate.setString(3, documentNumber);
                            qUpdate.executeUpdate();
                            MainCHK.tulisLog("response code : [" + status + "] with message [" + message + "]");
                            qInsertVoidList = conn.prepareStatement(queryVoid);
                            qInsertVoidList.setString(1, sp2d_number);
                            qInsertVoidList.setString(2, prefixStatusVoid + voidNotFound);
                            qInsertVoidList.executeUpdate();
                            MainCHK.tulisLog(qInsertVoidList.toString());
                            break;
                    }
                } else {
                    MainCHK.tulisLog("response getStatus: " + response.getStatus());
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
            MainCHK.logger.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (qSelect != null) {
                qSelect.close();
            }
            if (qUpdate != null) {
                qUpdate.close();
            }
        }
    }

    public void postingDetailAffiliate() throws SQLException {
        PreparedStatement qSelect = null, qUpdate = null;
        try {
            qSelect = conn.prepareStatement(
                    "select beneficiaryaccount, agentbankaccountnumber, amount, documentdate, sp2d_number, documentnumber, applicationareamessageidentifier, reference_number, return_code from span_sp2d_posting a " +
                            "where a.documentdate = (select config_value from span_application_config where config_name = 'APP_DATE') " +
                            "and a.paymentmethod = ? and a.flag_ack is  null and reference_number <> '' " +
                            "and agentbankaccountnumber = (select config_value from span_application_config where config_name = 'ACCT_RPKBUN_NON_GAJI');");
            qSelect.setString(1, paymentMethodAfiliasi);
            MainCHK.tulisLog(qSelect.toString());
            rs = qSelect.executeQuery();
            while (rs.next()) {
                String referenceNumber = rs.getString("reference_number");
                String kodeReferal = rs.getString("beneficiaryaccount");
                String documentNumber = rs.getString("documentnumber");
                String amount = rs.getString("amount");
                Date documentdate = rs.getDate("documentdate");
                String agentBankAccountNumber = rs.getString("agentbankaccountnumber");
                String applicationareamessageidentifier = rs.getString("applicationareamessageidentifier");
                MainCHK.tulisLog("postingDetail for documentnumber/beneficiaryaccount(kodeReferal):" + documentNumber + "/" + kodeReferal);
                if (referenceNumber != null || referenceNumber != "") {
                    PostingRequest postingRequest = new PostingRequest();
                    postingRequest.setReferenceNumber(referenceNumber);
                    postingRequest.setDocumentNumber(documentNumber);
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    String dateString = formatter.format(documentdate);
                    postingRequest.setDocumentDate(dateString);
                    postingRequest.setBeneficiaryAccount(kodeReferal);
                    postingRequest.setAmount(amount);
                    postingRequest.setAgentBankAccountNumber(agentBankAccountNumber);
                    postingRequest.setApplicationAreaMessageIdentifier(applicationareamessageidentifier);
                    MainCHK.tulisLog("request: " + postingRequest.toString());
                    restClient.processPosting(postingRequest);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
            MainCHK.logger.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (qSelect != null) {
                qSelect.close();
            }
            if (qUpdate != null) {
                qUpdate.close();
            }
        }
    }

    public SpanConfig getSpanconfig(){
        return this.spanConfig;
    }

    public Map<String , BifastRcMapping> getBifastMappingRcAe() {
        Map<String,BifastRcMapping> result = null;
        try {
            result = Utillity.fetchBifastRcMappingAe(conn);
        } catch (SQLException e){
            MainCHK.tulisLog("Error receive mapping bifast_rc");
        }
        return result;
    }

    public void excludeOutOfBalanceBO1() throws SQLException {
         processExcludeOutOfBalance("ACCT_RPKBUN_NON_GAJI");
    }

    public void excludeOutOfBalanceBO2() throws SQLException {
         processExcludeOutOfBalance("ACCT_RPKBUN_GAJI");
    }

    public void includeOutOfBalanceBO1() throws SQLException {
        String accountNumber = processIncludeOutOfBalance("ACCT_RPKBUN_NON_GAJI");
         executeUpdateStatusForIncludeBalance(accountNumber,statusReadyProses,statusWaitingDropping);
    }

    public void includeOutOfBalanceBO2() throws SQLException {

        String accountNumber = processIncludeOutOfBalance("ACCT_RPKBUN_GAJI");
        executeUpdateStatusForIncludeBalance(accountNumber,statusReadyProses,statusWaitingDropping);
    }

    public void includeOutOfBalanceBO2Bifast() throws SQLException {

        String accountNumber = processIncludeOutOfBalance("ACCT_RPKBUN_GAJI");
        executeUpdateStatusForIncludeBalance(accountNumber,statusReadyProsesBifast,statusWaitingDroppingBifast);
    }
    

       public void includeOutOfBalanceBO1Bifast() throws SQLException {

        String accountNumber = processIncludeOutOfBalance("ACCT_RPKBUN_NON_GAJI");
        executeUpdateStatusForIncludeBalance(accountNumber,statusReadyProsesBifast,statusWaitingDroppingBifast);
    }
    public List<SpanSp2dStageIn> getDataBifast(String activities) throws SQLException{
        
        // default use account non gaji 
         String sourceAccount="";
         String sourceRetur="";
        
        switch (activities.trim().toUpperCase()) {
            case "BO2":
                sourceAccount = spanConfig.getAcctRpkbunGaji();
                sourceRetur =   spanConfig.getAcctRrRpkbunGaji();
                break;
            case "BO1":
                sourceAccount=spanConfig.getAcctRpkbunNonGaji();
                sourceRetur=spanConfig.getAcctRrRpkbunNonGaji();
                break;
            default:
                sourceAccount=spanConfig.getAcctReksusSbsn();
                sourceRetur=spanConfig.getAcctRrReksusSbsn();
        }

        MainCHK.tulisLog(sourceRetur+" And " + sourceAccount);
        List<SpanSp2dStageIn> result = new ArrayList<>();
        String sql = "SELECT " + "id, applicationareasenderidentifier, applicationareareceiveridentifier, " +
                     "applicationareadetailsenderidentifier, applicationareadetailreceiveridentifier, " +
                     "applicationareacreationdatetime, applicationareamessageidentifier, " +
                     "applicationareamessagetypeindicator, applicatioanareamessageversiontext, " +
                     "documentdate, documentnumber, beneficiaryname, beneficiarybankcode, " +
                     "beneficiarybank, beneficiaryaccount, amount, currencytarget, " +
                     "description, agentbankcode, agentbankaccountnumber, agentbankaccountname, " +
                     "emailaddress, swiftcode, ibancode, paymentmethod, " +
                     "sp2dcount, totalcount, totalamount, totalbatchcount, " +
                     "sp2d_number, date_posting, reference_number, return_code, status " +
                     "FROM span_sp2d_stage_in " +
                     "WHERE documentdate = ? "+
                     "AND status = ? " +
                     "AND agentbankaccountnumber IN ('" + sourceAccount + "','" + sourceRetur + "') " +
                     "ORDER BY documentnumber";

        try(PreparedStatement ps = conn.prepareStatement(sql)){
             ps.setString(1, spanConfig.getAppDate());
             ps.setString(2, statusReadyProsesBifast);
            
            try(ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                int id = rs.getInt("id");
                String status = rs.getString("status");
                BigDecimal amount = rs.getBigDecimal("amount");
                if (amount == null){
                    amount = BigDecimal.ZERO;
                }
               String applicationAreaSenderIdentifier = rs.getString("applicationareasenderidentifier");
               String applicationAreaReceiverIdentifier =rs.getString("applicationareareceiveridentifier");
               String applicationAreaDetailSenderIdentifier =rs.getString("applicationareadetailsenderidentifier");
               String applicationAreaDetailReceiverIdentifier =rs.getString("applicationareadetailreceiveridentifier");
               LocalDateTime applicationAreaCreationDateTime =rs.getTimestamp("applicationareacreationdatetime") != null? rs.getTimestamp("applicationareacreationdatetime").toLocalDateTime(): null;
               String applicationAreaMessageIdentifier =rs.getString("applicationareamessageidentifier");
               String applicationAreaMessageTypeIndicator =rs.getString("applicationareamessagetypeindicator");
               String applicationAreaMessageVersionText =rs.getString("applicatioanareamessageversiontext");
               LocalDate documentDate =rs.getDate("documentdate") != null? rs.getDate("documentdate").toLocalDate(): null;
               String documentNumber =rs.getString("documentnumber");
               String beneficiaryName =rs.getString("beneficiaryname");
               String beneficiaryBankCode =rs.getString("beneficiarybankcode");
               String beneficiaryBank =rs.getString("beneficiarybank");
               String beneficiaryAccount =rs.getString("beneficiaryaccount");
               String currencyTarget =rs.getString("currencytarget");
               String description =rs.getString("description");
               String agentBankCode =rs.getString("agentbankcode");
               String agentBankAccountNumber =rs.getString("agentbankaccountnumber");
               String agentBankAccountName =rs.getString("agentbankaccountname");
               String emailAddress =rs.getString("emailaddress");
               String swiftCode =rs.getString("swiftcode");
               String ibanCode =rs.getString("ibancode");
               String paymentMethod =rs.getString("paymentmethod");
               int sp2dCount =rs.getInt("sp2dcount");
               int totalCount =rs.getInt("totalcount");
               BigDecimal totalAmount =rs.getBigDecimal("totalamount");
               Integer totalBatchCount =rs.getInt("totalbatchcount");
               String sp2dNumber =rs.getString("sp2d_number");
               LocalDate datePosting =rs.getDate("date_posting") != null? rs.getDate("date_posting").toLocalDate(): null;
               String referenceNumber =rs.getString("reference_number");
               String returnCode =rs.getString("return_code");
               SpanSp2dStageIn objData = new SpanSp2dStageIn(
                 id, applicationAreaSenderIdentifier,
                 applicationAreaReceiverIdentifier,
                 applicationAreaDetailSenderIdentifier,
                 applicationAreaDetailReceiverIdentifier,
                 applicationAreaCreationDateTime,
                 applicationAreaMessageIdentifier,
                 applicationAreaMessageTypeIndicator,
                 applicationAreaMessageVersionText,
                 documentDate,
                 documentNumber,
                 beneficiaryName,
                 beneficiaryBankCode,
                 beneficiaryBank,
                 beneficiaryAccount,
                 amount,
                 currencyTarget,
                 description,
                 agentBankCode,
                 agentBankAccountNumber,
                 agentBankAccountName,
                 emailAddress,
                 swiftCode,
                 ibanCode,
                 paymentMethod,
                 sp2dCount,
                 totalCount,
                 totalAmount,
                 totalBatchCount,
                 sp2dNumber,
                 datePosting,
                 referenceNumber,
                 returnCode,
                 status);
                 result.add(objData);
            }
          }
        }
        return result;
    }

    public List<SpanSp2dStageIn> getDataBifastForSchedulerPurpose(String targetStatus) throws SQLException {
        List<SpanSp2dStageIn> result = new ArrayList<>();
        String sql = "SELECT id, applicationareasenderidentifier, applicationareareceiveridentifier, " +
                     "applicationareadetailsenderidentifier, applicationareadetailreceiveridentifier, " +
                     "applicationareacreationdatetime, applicationareamessageidentifier, " +
                     "applicationareamessagetypeindicator, applicatioanareamessageversiontext, " +
                     "documentdate, documentnumber, beneficiaryname, beneficiarybankcode, " +
                     "beneficiarybank, beneficiaryaccount, amount, currencytarget, " +
                     "description, agentbankcode, agentbankaccountnumber, agentbankaccountname, " +
                     "emailaddress, swiftcode, ibancode, paymentmethod, " +
                     "sp2dcount, totalcount, totalamount, totalbatchcount, " +
                     "sp2d_number, date_posting, reference_number, return_code, status " +
                     "FROM span_sp2d_stage_in " +
                     "WHERE documentdate = ? "+
                     "AND status = ? " +
                     "AND paymentmethod = '5' " +
                     "ORDER BY documentnumber";

        try(PreparedStatement ps = conn.prepareStatement(sql)){
             ps.setString(1, spanConfig.getAppDate());
             ps.setString(2, targetStatus);
             MainCHK.tulisLog("Query data bifast by status: " + targetStatus);
             MainCHK.tulisLog(ps);
            try(ResultSet rs = ps.executeQuery()){
                while (rs.next()){
                int id = rs.getInt("id");
                String status = rs.getString("status");
                BigDecimal amount = rs.getBigDecimal("amount");
                
                if (amount == null){
                    amount = BigDecimal.ZERO;
                }

               String applicationAreaSenderIdentifier = rs.getString("applicationareasenderidentifier");
               String applicationAreaReceiverIdentifier =rs.getString("applicationareareceiveridentifier");
               String applicationAreaDetailSenderIdentifier =rs.getString("applicationareadetailsenderidentifier");
               String applicationAreaDetailReceiverIdentifier =rs.getString("applicationareadetailreceiveridentifier");
               LocalDateTime applicationAreaCreationDateTime =rs.getTimestamp("applicationareacreationdatetime") != null? rs.getTimestamp("applicationareacreationdatetime").toLocalDateTime(): null;
               String applicationAreaMessageIdentifier =rs.getString("applicationareamessageidentifier");
               String applicationAreaMessageTypeIndicator =rs.getString("applicationareamessagetypeindicator");
               String applicationAreaMessageVersionText =rs.getString("applicatioanareamessageversiontext");
               LocalDate documentDate =rs.getDate("documentdate") != null? rs.getDate("documentdate").toLocalDate(): null;

               String documentNumber =rs.getString("documentnumber");
               String beneficiaryName =rs.getString("beneficiaryname");
               String beneficiaryBankCode =rs.getString("beneficiarybankcode");
               String beneficiaryBank =rs.getString("beneficiarybank");
               String beneficiaryAccount =rs.getString("beneficiaryaccount");
               String currencyTarget =rs.getString("currencytarget");
               String description =rs.getString("description");
               String agentBankCode =rs.getString("agentbankcode");
               String agentBankAccountNumber =rs.getString("agentbankaccountnumber");
               String agentBankAccountName =rs.getString("agentbankaccountname");
               String emailAddress =rs.getString("emailaddress");
               String swiftCode =rs.getString("swiftcode");
               String ibanCode =rs.getString("ibancode");
               String paymentMethod =rs.getString("paymentmethod");
               int sp2dCount =rs.getInt("sp2dcount");
               int totalCount =rs.getInt("totalcount");
               BigDecimal totalAmount =rs.getBigDecimal("totalamount");
               Integer totalBatchCount =rs.getInt("totalbatchcount");
               String sp2dNumber =rs.getString("sp2d_number");
               LocalDate datePosting =rs.getDate("date_posting") != null? rs.getDate("date_posting").toLocalDate(): null;
               String referenceNumber =rs.getString("reference_number");
               String returnCode =rs.getString("return_code");

               SpanSp2dStageIn objData = new SpanSp2dStageIn(
                 id, applicationAreaSenderIdentifier,
                 applicationAreaReceiverIdentifier,
                 applicationAreaDetailSenderIdentifier,
                 applicationAreaDetailReceiverIdentifier,
                 applicationAreaCreationDateTime,
                 applicationAreaMessageIdentifier,
                 applicationAreaMessageTypeIndicator,
                 applicationAreaMessageVersionText,
                 documentDate,
                 documentNumber,
                 beneficiaryName,
                 beneficiaryBankCode,
                 beneficiaryBank,
                 beneficiaryAccount,
                 amount,
                 currencyTarget,
                 description,
                 agentBankCode,
                 agentBankAccountNumber,
                 agentBankAccountName,
                 emailAddress,
                 swiftCode,
                 ibanCode,
                 paymentMethod,
                 sp2dCount,
                 totalCount,
                 totalAmount,
                 totalBatchCount,
                 sp2dNumber,
                 datePosting,
                 referenceNumber,
                 returnCode,
                 status);
                 result.add(objData);
            }
          }
        }
       
        return result;
    }
    
    public void fallbackToSkn(SpanSp2dStageIn stageIn) throws SQLException {
       String sql =  "UPDATE span_sp2d_stage_in " +
                     "SET paymentmethod = ? ," +
                     "status = ? " +
                     "WHERE documentnumber = ?";
        try(PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, "2");
            ps.setString(2, statusReadyProses);
            ps.setString(3, stageIn.getDocumentNumber());
            ps.executeUpdate();
     }
    }


    public void postingMessageAfterCt(SpanSp2dStageIn stageIn, CreditTransferResponse response) throws SQLException {
     try{
        
        Map<String,ReturnStatusAck> map_status_code = Utillity.fetchReturnStatusAck(conn);
        ReturnStatusAck ack = map_status_code.get(response.getResponseCode());

        SpanSp2dPosting rec = constructDataForposting(stageIn, ack);

        // ft number from response credit transfer
        rec.referenceNumber = response.getReferenceId();

        rec.returnCode = response.getResponseCode();
        
        insertPostingRecords(rec);
        
        if (!rec.returnCode.equals(response.isSuccess())){
            updateErrorRecords(rec);
         }else{
           finalizeSuccessRecords(stageIn);
         }
    } catch (SQLException e){
        System.out.println(e.getMessage());
    } 
    }

        public void postingMessageAfterSuccessGetStatus(SpanSp2dStageIn stageIn, CreditTransferResponse response) throws SQLException {
     try{
        
        Map<String,ReturnStatusAck> map_status_code = Utillity.fetchReturnStatusAck(conn);
        ReturnStatusAck ack = map_status_code.get(response.getResponseCode());

        SpanSp2dPosting rec = constructDataForposting(stageIn, ack);

        // ft number from response credit transfer
        rec.referenceNumber = response.getReferenceId();

        rec.returnCode = response.getResponseCode();
        
        insertPostingRecords(rec);
        MainCHK.tulisLog("setelah insert data ke database");
        if (!rec.returnCode.equals("000")){
            updateErrorRecords(rec);
         }else{
            MainCHK.tulisLog("update harusnya disni");
           finalizeSuccessRecordsAfterGetStatus(stageIn);
         }
    } catch (SQLException e){
        System.out.println(e.getMessage());
    } 
    }

    public SpanSp2dPosting constructDataForposting(SpanSp2dStageIn stageIn ,ReturnStatusAck ack){

        String transactionType = Utillity.getTransactionType(stageIn, this.spanConfig);
        String beneficiaryAccount = "";

        switch (transactionType.trim().toUpperCase()) {
            case "BO2":
                beneficiaryAccount = this.spanConfig.getAcctRrRpkbunGaji();
                break;
            case "BO1":
                beneficiaryAccount = this.spanConfig.getAcctRrRpkbunNonGaji();
                break;
            default:
                beneficiaryAccount = this.spanConfig.getAcctRrReksusSbsn();
        }
        boolean benefciaryAccountisRR = stageIn.getBeneficiaryAccount().equals(beneficiaryAccount);

        SpanSp2dPosting rec = Utillity.builderSpanSp2dPosting(stageIn);

        if (benefciaryAccountisRR){
           switch (transactionType) {
               case "BO2":
                   rec.beneficiaryName= "ACCT_RR_RPKBUN_GAJI_BSM";
                   break;
               case "BO1":
                   rec.beneficiaryName= "ACCT_RR_RPKBUN_NON_GAJI_BSM";
                   break;
               default:
                   rec.beneficiaryName= "ACCT_RR_REKSUS_BSM";
           }
       }

        if (ack != null && "Reject".equals(ack.status)) {
            rec.status = "RDY-008";
            rec.rejectStatus = "N";
            rec.rejectDate = rec.datePosting;
        } else {
            rec.status = "FIN-000";
            rec.rejectStatus = null;
            rec.rejectDate = null; 
        }
      return rec;
}     
    
    public void insertPostingAeFailure(SpanSp2dStageIn stageIn, String rcSpan ) throws SQLException{
      
        Map<String,ReturnStatusAck> map_status_code = Utillity.fetchReturnStatusAck(conn);
        ReturnStatusAck ack = map_status_code.get(rcSpan);
        SpanSp2dPosting rec = constructDataForposting(stageIn, ack);
        rec.returnCode = rcSpan;
        insertPostingRecords(rec);
    }

    public void insertPostingCtFailure(SpanSp2dStageIn stageIn ) throws SQLException{
        Map<String,ReturnStatusAck> map_status_code = Utillity.fetchReturnStatusAck(conn);
        ReturnStatusAck ack = map_status_code.get(stageIn.getReturnCode());

        if (ack == null){
            MainCHK.tulisLog("Mapping ack belum tersedia saat proses gagal ct untuk response esb :"+stageIn.getReturnCode());
            return;
        }

        SpanSp2dPosting rec = constructDataForposting(stageIn,ack);
        rec.referenceNumber =stageIn.getReferenceNumber();
        rec.returnCode = stageIn.getReturnCode();

        insertPostingRecords(rec);
    }
   
    public void insertPostingRecords(SpanSp2dPosting r) throws SQLException {
        if (r == null) return;

        String sql = "INSERT INTO span_sp2d_posting (" +
        "applicationareasenderidentifier, applicationareareceiveridentifier, " +
        "applicationareadetailsenderidentifier, applicationareadetailreceiveridentifier, " +
        "applicationareacreationdatetime, applicationareamessageidentifier, " +
        "applicationareamessagetypeindicator, applicatioanareamessageversiontext, " +
        "documentdate, documentnumber, beneficiaryname, beneficiaryaccount, " +
        "amount, currencytarget, description, agentbankcode, " +
        "agentbankaccountnumber, agentbankaccountname, emailaddress, swiftcode, ibancode, " +
        "paymentmethod, date_posting, reference_number, return_code, status, " +
        "reject_status, reject_date, sp2dcount, totalcount, totalamount, totalbatchcount, sp2d_number , flag_ack ," +
        "beneficiarybankcode , beneficiarybank"+
        ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            MainCHK.tulisLog("Proses insert tabel posting ");
            ps.setString(1,  Utillity.safe(r.applicationareaSenderIdentifier));
            ps.setString(2,  Utillity.safe(r.applicationareaReceiverIdentifier));
            ps.setString(3,  Utillity.safe(r.applicationareaDetailSenderIdentifier));
            ps.setString(4,  Utillity.safe(r.applicationareaDetailReceiverIdentifier));
            ps.setTimestamp(5, r.applicationareaCreationDatetime != null ? new Timestamp(r.applicationareaCreationDatetime.getTime()) : null);
            ps.setString(6,  Utillity.safe(r.applicationareaMessageIdentifier));
            ps.setString(7,  Utillity.safe(r.applicationareaMessageTypeIndicator));
            ps.setString(8,  Utillity.safe(r.applicatioanareaMessageVersionText));
            ps.setTimestamp(9, r.documentDate != null ? new Timestamp(r.documentDate.getTime()) : null);
            ps.setString(10, Utillity.safe(r.documentNumber));
            ps.setString(11, Utillity.safe(r.beneficiaryName));
            ps.setString(12, Utillity.safe(r.beneficiaryAccount));
            ps.setObject(13, r.amount);
            ps.setString(14, Utillity.safe(r.currencyTarget));
            ps.setString(15, Utillity.safe(r.description));
            ps.setString(16, Utillity.safe(r.agentBankCode));
            ps.setString(17, Utillity.safe(r.agentBankAccountNumber));
            ps.setString(18, Utillity.safe(r.agentBankAccountName));
            ps.setString(19, Utillity.safe(r.emailAddress));
            ps.setString(20, Utillity.safe(r.swiftCode));
            ps.setString(21, Utillity.safe(r.ibanCode));
            ps.setString(22, Utillity.safe(r.paymentMethod));
            ps.setTimestamp(23, r.datePosting != null ? new Timestamp(r.datePosting.getTime()) : null);
            ps.setString(24, Utillity.safe(r.referenceNumber));
            ps.setString(25, Utillity.safe(r.returnCode));
            ps.setString(26, Utillity.safe(r.status));
            ps.setString(27, r.rejectStatus);
            ps.setTimestamp(28, r.rejectDate != null ? new Timestamp(r.rejectDate.getTime()) : null);
            ps.setObject(29, r.sp2dCount);
            ps.setObject(30, r.totalCount);
            ps.setObject(31, r.totalAmount);
            ps.setObject(32, r.totalBatchCount);
            ps.setString(33, Utillity.safe(r.sp2dNumber));
            ps.setString(34, r.flagAck);
            ps.setString(35, r.beneficiaryBankCode);
            ps.setString(36, r.beneficiaryBank);
    
            int row =ps.executeUpdate();
         
            if (row == 0){
                MainCHK.tulisLog("error saat insert data posting untuk no sp2dnumber "+r.documentNumber);
            }
    }
}
    
    public void updateErrorRecords(SpanSp2dPosting records) throws SQLException {
      String sql =  "UPDATE span_sp2d_stage_in SET status = ?, date_posting = ?, " +
                    "reference_number = ?, return_code = ? WHERE documentnumber = ? "+
                    "AND paymentmethod ='5'";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if ("000".equals(records.returnCode)) return; // skip sukses
            ps.setString(1, records.status);
            ps.setTimestamp(2, records.datePosting != null ? new Timestamp(records.datePosting.getTime()) : null);
            ps.setString(3, Utillity.safe(records.referenceNumber));
            ps.setString(4, Utillity.safe(records.returnCode));
            ps.setString(5, Utillity.safe(records.documentNumber));
            int row = ps.executeUpdate();

            if (row > 0){
             MainCHK.tulisLog("update data stagein berhasil untuk documentNumber"+records.documentNumber);
            }else{
              MainCHK.tulisLog("update data stagein gagal untuk documentNumber"+records.documentNumber);
            }
    }
}

    public void updateErrorDataForReturProcess(SpanSp2dStageIn records) throws SQLException {
        String sql =  "UPDATE span_sp2d_stage_in SET  " +
                "reference_number = ?, return_code = ? WHERE documentnumber = ? "+
                "AND paymentmethod ='5'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, records.getReferenceNumber());
            ps.setString(2, records.getReturnCode());
            ps.setString(3, records.getDocumentNumber());

            int row = ps.executeUpdate();

            if (row > 0){
                MainCHK.tulisLog("update data pada table stagein untuk proses retur berhasil untuk documentNumber"+records.getDocumentNumber());
            }else{
                MainCHK.tulisLog("update data pada table stagein untuk proses retur gagal untuk documentNumber"+records.getDocumentNumber());
            }
        }
    }

      public void updateRetrunCodeForReturProcess(SpanSp2dStageIn records) throws SQLException {
        String sql =  "UPDATE span_sp2d_stage_in SET  " +
                      "return_code = ? WHERE documentnumber = ? "+
                      "AND paymentmethod ='5'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, records.getReturnCode());
            ps.setString(2, records.getDocumentNumber());

            int row = ps.executeUpdate();

            if (row > 0){
                MainCHK.tulisLog("update data pada table stagein untuk proses retur berhasil untuk documentNumber"+records.getDocumentNumber());
            }else{
                MainCHK.tulisLog("update data pada table stagein untuk proses retur gagal untuk documentNumber"+records.getDocumentNumber());
            }
        }
    }




    public void finalizeSuccessRecords(SpanSp2dStageIn stagein) throws SQLException {

        String transactionType = Utillity.safe(Utillity.getTransactionType(stagein, this.spanConfig));

        String sourceAccount="";
        String returAccount="";

        switch (transactionType.trim().toUpperCase()) {
            case "BO2":
                sourceAccount = spanConfig.getAcctRpkbunGaji();
                returAccount  = spanConfig.getAcctRrRpkbunGaji();
                break;
            case "BO1":
                sourceAccount =spanConfig.getAcctRpkbunNonGaji();
                returAccount  =spanConfig.getAcctRrRpkbunNonGaji();
                break;
            default:
                sourceAccount =spanConfig.getAcctReksusSbsn();
                returAccount  =spanConfig.getAcctRrReksusSbsn();
        }

       String sql = Utillity.finalizeSuccesRecord(sourceAccount,returAccount);
       try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setString(2,statusReadyPosting);
            ps.setString(3,stagein.getDocumentNumber());
            MainCHK.tulisLog("Query Fin : "+ps);
            int updated = ps.executeUpdate();
        System.out.println("Finalized " + updated + " record(s): PST-000 → FIN-000");
    }
  } 

  

    public void finalizeSuccessRecordsAfterRetyRetur(SpanSp2dStageIn stagein) throws SQLException {

        String transactionType = Utillity.getTransactionType(stagein, this.spanConfig);

        String sourceAccount="";
        String returAccount="";

        switch (transactionType.trim().toUpperCase()) {
            case "BO2":
                sourceAccount = spanConfig.getAcctRpkbunGaji();
                returAccount  = spanConfig.getAcctRrRpkbunGaji();
                break;
            case "BO1":
                sourceAccount =spanConfig.getAcctRpkbunNonGaji();
                returAccount  =spanConfig.getAcctRrRpkbunNonGaji();
                break;
            default:
                sourceAccount =spanConfig.getAcctReksusSbsn();
                returAccount  =spanConfig.getAcctRrReksusSbsn();
        }
        String sql = Utillity.finalizeSuccesRecord(sourceAccount,returAccount);
       try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setString(2, statusForRetryRetur);
            ps.setString(3,stagein.getDocumentNumber());
            int updated = ps.executeUpdate();
        System.out.println("Finalized " + updated + " record(s): RRS-000 → FIN-000");
    }
  } 

      public void finalizeSuccessRecordsAfterReturManual(SpanSp2dStageIn stagein) throws SQLException {
          String transactionType = Utillity.getTransactionType(stagein, this.spanConfig);

          String sourceAccount="";
          String returAccount="";

          switch (transactionType.trim().toUpperCase()) {
              case "BO2":
                  sourceAccount = spanConfig.getAcctRpkbunGaji();
                  returAccount  = spanConfig.getAcctRrRpkbunGaji();
                  break;
              case "BO1":
                  sourceAccount =spanConfig.getAcctRpkbunNonGaji();
                  returAccount  =spanConfig.getAcctRrRpkbunNonGaji();
                  break;
              default:
                  sourceAccount =spanConfig.getAcctReksusSbsn();
                  returAccount  =spanConfig.getAcctRrReksusSbsn();
          }

        String sql = Utillity.finalizeSuccesRecord(sourceAccount,returAccount);
       try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setString(2, statusForManualRetur);
            ps.setString(3,stagein.getDocumentNumber());
            MainCHK.tulisLog(ps);
            int updated = ps.executeUpdate();
        System.out.println("Finalized " + updated + " record(s): RRM-000 → FIN-000");
    }
  } 

   public void finalizeSuccessRecordsAfterGetStatus(SpanSp2dStageIn stagein) throws SQLException {

       String transactionType = Utillity.getTransactionType(stagein, this.spanConfig);

       String sourceAccount="";
       String returAccount="";

       switch (transactionType.trim().toUpperCase()) {
           case "BO2":
               sourceAccount = spanConfig.getAcctRpkbunGaji();
               returAccount  = spanConfig.getAcctRrRpkbunGaji();
               break;
           case "BO1":
               sourceAccount =spanConfig.getAcctRpkbunNonGaji();
               returAccount  =spanConfig.getAcctRrRpkbunNonGaji();
               break;
           default:
               sourceAccount =spanConfig.getAcctReksusSbsn();
               returAccount  =spanConfig.getAcctRrReksusSbsn();
       }
        String sql = Utillity.finalizeSuccesRecordAfterGetStatus(sourceAccount,returAccount);
       try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setString(2,statusForRetryGetstatus);
            ps.setString(3,statusTimeoutCreditTransferBifast);
            ps.setString(4,stagein.getDocumentNumber());
            MainCHK.tulisLog(ps);

            int updated = ps.executeUpdate();
        System.out.println("Finalized " + updated + " record(s): RGS-000 → FIN-000");
    }
  } 


  public String getDataSp2dBifast(String documentNumber) throws SQLException{
    String result =null;

    String sql = Utillity.getDataBifastByDocumentNumber();
    try(PreparedStatement ps = conn.prepareStatement(sql)){
        ps.setString(1, documentNumber);
        try(ResultSet rs = ps.executeQuery()){
           if (rs.next()){
            result = rs.getString("document_number");
           }
        }
    }

    return result;
  }

   public String getDataEndToEndId(String documentNumber) throws SQLException{
    String result =null;

    String sql = Utillity.getDataBifastByDocumentNumber();
    try(PreparedStatement ps = conn.prepareStatement(sql)){
        ps.setString(1, documentNumber);
        try(ResultSet rs = ps.executeQuery()){
           if (rs.next()){
            result = rs.getString("end_to_end_id");
           }
        }
    }

    return result;
  }

   public void insertDataSp2dBfast(SpanSp2dStageIn item)throws SQLException{
    String sql  = Utillity.insertDataSp2dBifast();
    try(PreparedStatement ps = conn.prepareStatement(sql)){
        ps.setString(1, item.getDocumentNumber());
        int updated = ps.executeUpdate();
         if (updated > 0) {
                MainCHK.tulisLog("insert data sp2dBifast berhasil pada span_sp2d_stage_in untuk doc: " + item.getDocumentNumber());
            } else {
                MainCHK.tulisLog("insert data sp2dBifast gagal pada span_sp2d_stage_in untuk doc: " + item.getDocumentNumber());
            }
    }
   }

     public void insertDataSp2dBfastCtTimeout(SpanSp2dStageIn item , String endToEndId)throws SQLException{
    String sql  = Utillity.insertDataSp2dBifastCtTimeout();
    try(PreparedStatement ps = conn.prepareStatement(sql)){
        ps.setString(1, item.getDocumentNumber());
        ps.setString(2, endToEndId);
        int updated = ps.executeUpdate();
         if (updated > 0) {
                MainCHK.tulisLog("insert data sp2dBifast berhasil pada span_sp2d_stage_in untuk doc: " + item.getDocumentNumber());
            } else {
                MainCHK.tulisLog("insert data sp2dBifast gagal pada span_sp2d_stage_in untuk doc: " + item.getDocumentNumber());
            }
    }
   }

  public void handleAccountInquiryError(SpanSp2dStageIn item, String responseCode, Map<String,BifastRcMapping> mappingRcAe) throws SQLException{
        Map<String,BifastRcMapping> map = mappingRcAe;
        String rcBifast = map != null ? Utillity.safe(map.get(responseCode).span_rc) : "";

        String sql ="UPDATE span_sp2d_bifast_data " +
                    "SET bifast_response_code = ? "+
                    "WHERE document_number = ? ";
       
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, rcBifast);
            ps.setString(2, item.getDocumentNumber());
            int updated = ps.executeUpdate();
            if (updated > 0) {
                MainCHK.tulisLog("Updated bifast_response_code = '" + responseCode + "' pada span_sp2d_stage_in untuk doc: " + item.getDocumentNumber());
            } else {
                MainCHK.tulisLog("Gagal update bifast_response_code untuk doc: " + item.getDocumentNumber());
            }
        }             
  }

  public void handleCreditTransferError(SpanSp2dStageIn item, String responseCode) throws SQLException{
        Map<String,BifastRcMapping> map = Utillity.fetchBifastRcMappingCt(conn);
        
        String rcBifast = map != null ? Utillity.safe(map.get(responseCode).span_rc) : "";
        MainCHK.tulisLog(rcBifast);

        String sql ="UPDATE span_sp2d_bifast_data " +
                    "SET bifast_response_code = ? "+
                    "WHERE document_number = ? ";
       
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, rcBifast);
            ps.setString(2, item.getDocumentNumber());
            int updated = ps.executeUpdate();
            if (updated > 0) {
                MainCHK.tulisLog("Updated bifast_response_code = '" + responseCode + "' pada span_sp2d_stage_in untuk doc: " + item.getDocumentNumber());
            } else {
                MainCHK.tulisLog("Gagal update bifast_response_code untuk doc: " + item.getDocumentNumber());
            }
        }             
  }

  public void handleAccountInquiryErrorRcSpan(SpanSp2dStageIn item, String rcSpan) throws SQLException{
        
        String sql ="UPDATE span_sp2d_bifast_data " +
                    "SET bifast_response_code = ? "+
                    "WHERE document_number = ? ";
       
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, rcSpan);
            ps.setString(2, item.getDocumentNumber());
            int updated = ps.executeUpdate();
            if (updated > 0) {
                MainCHK.tulisLog("Updated bifast_response_code  pada span_sp2d_stage_in untuk doc: " + item.getDocumentNumber());
            } else {
                MainCHK.tulisLog("Gagal update bifast_response_code untuk doc: " + item.getDocumentNumber());
            }
        }             
  }

  
    
    public void failedProcessBifast(SpanSp2dStageIn item,String responseCode) throws SQLException{
        
        Map<String,BifastRcMapping> map = Utillity.fetchBifastRcMappingCt(conn);
        String rcBifast = map != null ? Utillity.safe(map.get(responseCode).span_rc) : "";

        String sql ="UPDATE span_sp2d_bifast_data " +
                    "SET bifast_response_code = ? "+
                    "WHERE document_number = ? ";
       
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, rcBifast);
            ps.setString(2, item.getDocumentNumber());
            int updated = ps.executeUpdate();
            if (updated > 0) {
                MainCHK.tulisLog("Updated bifast_response_code = '" + responseCode + "' pada span_sp2d_stage_in untuk doc: " + item.getDocumentNumber());
            } else {
                MainCHK.tulisLog("Gagal update bifast_response_code untuk doc: " + item.getDocumentNumber());
            }
        }             
    }


    public void failedProcessRetur(SpanSp2dStageIn item,String responseCode) throws SQLException{
        String sql = "UPDATE span_sp2d_stage_in " +
                "SET bifast_response_code = ? " +
                "WHERE documentnumber = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, responseCode);
            ps.setString(2, item.getDocumentNumber());
            int updated = ps.executeUpdate();
            if (updated > 0) {
                MainCHK.tulisLog("Updated esb_response_code failed retur = '" + responseCode + "' pada span_sp2d_stage_in untuk doc: " + item.getDocumentNumber());
            } else {
                MainCHK.tulisLog("Gagal update esb_response_code untuk doc: " + item.getDocumentNumber());
            }
        }
    }


    public void updateBifastResponseCode(SpanSp2dStageIn item, String responseCode) throws SQLException {
        failedProcessBifast(item, responseCode);
    }

    public void handleTimeoutCoreProcess(SpanSp2dStageIn item) throws SQLException {
        MainCHK.tulisLog("Processing Core Banking FT Timeout (RC 57) update untuk doc: " + item.getDocumentNumber());
        updateBifastResponseCode(item, rcForTimeoutCoreProcess);
    }

    public void handleCorePostingFailed(SpanSp2dStageIn item) throws SQLException {
        MainCHK.tulisLog("Processing Core Banking Posting Failed / Error Validasi Core (RC 05) update untuk doc: " + item.getDocumentNumber());
         String documentNumberOnTableSp2dBifast =getDataSp2dBifast(item.getDocumentNumber());
            if (documentNumberOnTableSp2dBifast == null){
                insertDataSp2dBfast(item);
                }
            updateBifastResponseCode(item, rcForCorePostingFailed);
    }


     // 2026-08-06
    public void handleResponseTimeoutFromCi(SpanSp2dStageIn item , CreditTransferResponse cResponse) throws SQLException{

       MainCHK.tulisLog("Processing Credit Transfer Posting Timeout From Ci-Connector untuk doc: " + item.getDocumentNumber());
         String documentNumberOnTableSp2dBifast =getDataSp2dBifast(item.getDocumentNumber());
            if (documentNumberOnTableSp2dBifast == null){
                insertDataSp2dBfastCtTimeout(item, cResponse.getEndToEndId());
                }
            updateBifastResponseCode(item, rcForTimeoutCtProcess);

        
        String sql ="UPDATE span_sp2d_stage_in " +
                    "SET status = ? , return_code = ? , reference_number= ? " +
                    "WHERE documentnumber = ? " +
                    "AND status = ? ";

        try (PreparedStatement ps  = conn.prepareStatement(sql)) {
            ps.setString(1, statusTimeoutCreditTransferBifast);
            ps.setString(2, rcForTimeoutCtProcess);
            ps.setString(3,item.getReferenceNumber());
            ps.setString(4, item.getDocumentNumber());
            ps.setString(5, statusReadyPosting);
            int updated = ps.executeUpdate();

            if (updated > 0) {
            MainCHK.tulisLog("Status diubah ke TMO-000 (timeout) untuk id: " + item.getId()
                           + " doc: " + item.getDocumentNumber());
            } else {
            MainCHK.tulisLog("Gagal update status timeout untuk id: " + item.getId());
        }
       }
    }

    public void updateSp2dstagein(SpanSp2dStageIn item) throws SQLException{

        String transactionType = Utillity.getTransactionType(item, this.spanConfig);
        String sourceAccount;
        String sourceRetur;

        switch (transactionType.trim().toUpperCase()) {
            case "BO2":
                sourceAccount = spanConfig.getAcctRpkbunGaji();
                sourceRetur =   spanConfig.getAcctRrRpkbunGaji();
                break;
            case "BO1":
                sourceAccount = spanConfig.getAcctRpkbunNonGaji();
                sourceRetur =   spanConfig.getAcctRrRpkbunNonGaji();
                break;
            default:
                sourceAccount = spanConfig.getAcctReksusSbsn();
                sourceRetur =   spanConfig.getAcctRrReksusSbsn();
        }


        String sql = Utillity.updateSp2dStageIn(2);

        try(PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, statusReadyPosting);
            ps.setString(2, spanConfig.getAppDate());
            ps.setString(3, statusReadyProsesBifast);
            ps.setString(4, statusForApprovedValidationName);
            ps.setString(5, item.getDocumentNumber());
            ps.setString(6, sourceAccount);
            ps.setString(7, sourceRetur);
            MainCHK.tulisLog(ps);
            int rowUpdate = ps.executeUpdate();
            if (rowUpdate > 0){
               MainCHK.tulisLog("terdapat "+rowUpdate+" Data yang terupdate menjadi "+statusReadyPosting);
            }else {
               MainCHK.tulisLog("Error update status PST pada document number : "+item.getDocumentNumber());
            }
        }
    }

    public void updateSp2dstageinAEerror(SpanSp2dStageIn item ,String returnCode) throws SQLException{

        String transactionType = Utillity.getTransactionType(item, this.spanConfig);
        String sourceAccount;
        String sourceRetur;

        switch (transactionType.trim().toUpperCase()) {
            case "BO2":
                sourceAccount = spanConfig.getAcctRpkbunGaji();
                sourceRetur =   spanConfig.getAcctRrRpkbunGaji();
                break;
            case "BO1":
                sourceAccount = spanConfig.getAcctRpkbunNonGaji();
                sourceRetur =   spanConfig.getAcctRrRpkbunNonGaji();
                break;
            default:
                sourceAccount = spanConfig.getAcctReksusSbsn();
                sourceRetur =   spanConfig.getAcctRrReksusSbsn();
        }

        String sql = "UPDATE span_sp2d_stage_in SET status = ? , " +
                     "return_code = ? WHERE documentdate = ? " +
                     "AND status = ? " +
                     "AND agentbankaccountnumber  IN ('" + sourceAccount + "','" + sourceRetur + "') "+
                     "AND paymentmethod ='5' "+
                     "AND documentnumber = ? ";

        try(PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, statusPosted);
            ps.setString(2, returnCode);
            ps.setString(3, spanConfig.getAppDate());
            ps.setString(4,statusReadyPosting);
            ps.setString(5, item.getDocumentNumber());

            int rowUpdate = ps.executeUpdate();
            if (rowUpdate == 0){
                MainCHK.tulisLog("Error update status PST pada id "+item.getId());
            }else {
                MainCHK.tulisLog("terdapat "+rowUpdate+" Data yang terupdate menjadi "+statusReadyPosting);
            }
        }
    }

    // 2026-08-06
    public void initiateRetryCheckStatus(SpanSp2dStageIn item) throws SQLException{
    
        String sql = "UPDATE span_sp2d_stage_in " +
                     "SET status = ? , reference_number = ? " +
                     "WHERE documentnumber = ? "+
                     "AND paymentmethod = '5'";
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, statusForRetryGetstatus);
             ps.setString(2, item.getReferenceNumber());
            ps.setString(3, item.getDocumentNumber());
            ps.executeUpdate();
        }
        
        updateBifastResponseCode(item, rcForTimeoutGetstatus);
    }

   
    public int getNumRetryStatus(SpanSp2dStageIn item) throws SQLException{
        String sql = "SELECT retry_bifast_status FROM span_sp2d_bifast_data WHERE document_number = ? ";
        int result=0;                     
        try (PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, item.getDocumentNumber());
         try(ResultSet rs = ps.executeQuery()){
             if (rs.next()){
               result = rs.getInt("retry_bifast_status"); 
             }
         }
        return result;
    }
   }
    public void increaseCounterCheckstatusBifast(SpanSp2dStageIn item , int counter ) throws SQLException{
        
        String sqlUpdate = "UPDATE span_sp2d_bifast_data set retry_bifast_status = ? " +
                           "WHERE document_number = ? ";        
         
        try(PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)){
               psUpdate.setInt(1, counter);
               psUpdate.setString(2,item.getDocumentNumber());
               psUpdate.executeUpdate();
         }
    }

    public void updateSp2dUploaded(SpanSp2dStageIn item) throws SQLException {

        String sql = "UPDATE span_sp2d_uploaded SET status = ? " +
                     "WHERE date_format(date(uploadeddate),'%Y-%m-%d') = ? " +
                     "AND status = ? ";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusReadyPosting);
            ps.setString(2, this.spanConfig.getAppDate());
            ps.setString(3, statusReadyProsesBifast);
            int updated = ps.executeUpdate();
            MainCHK.tulisLog("Updated span_sp2d_uploaded: " + updated + " row(s) → PST-000");
        }
    }
    
    public List<SpanSp2dPosting> fetchSp2dPostingForAckRetur(String documentNumber) throws SQLException {
          String sql  = Utillity.getDataPostingForGenerateAckRetur();
          List<SpanSp2dPosting> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, documentNumber);
            try (ResultSet rs = ps.executeQuery()) {
                 while(rs.next()) {
                    SpanSp2dPosting row = new SpanSp2dPosting();                                    
                    row.id = rs.getLong("id");
                    row.applicationareaMessageTypeIndicator     = rs.getString("applicationareamessagetypeindicator");
                    row.applicationareaSenderIdentifier         = rs.getString("applicationareasenderidentifier");
                    row.applicationareaMessageIdentifier        = rs.getString("applicationareamessageidentifier");
                    row.amount                                  = rs.getBigDecimal("amount");
                    row.returnCode                              = rs.getString("return_code");
                    row.documentNumber                          = rs.getString("documentnumber");
                    row.paymentMethod                           = rs.getString("paymentmethod");
                    result.add(row);
                }
            }
        }
        return result;
    }
    

    public List<SpanSp2dPosting> fetchsp2dPostingForAckBatch() throws SQLException{
      List<SpanSp2dPosting> result = new ArrayList<>();
      String sql = Utillity.getDataForProsesAckBatch();
      try(PreparedStatement ps = conn.prepareStatement(sql)){
        try(ResultSet rs = ps.executeQuery()){
            while (rs.next()) {
                 SpanSp2dPosting row = new SpanSp2dPosting();
                    row.applicationareaMessageTypeIndicator     = rs.getString("applicationareamessagetypeindicator");
                    row.applicationareaSenderIdentifier         = rs.getString("applicationareasenderidentifier");
                    row.applicationareaMessageIdentifier        = rs.getString("applicationareamessageidentifier");
                    row.amount                                  = rs.getBigDecimal("amount");
                    row.returnCode                              = rs.getString("return_code");
                    row.documentNumber                          = rs.getString("documentnumber");
                    row.paymentMethod                           = rs.getString("paymentmethod");
                    result.add(row);
            }
        }
      }
      return result;
    }

    public  List<SpanSp2dPosting> fetchSp2dPostingForGaji(String documentNumber) throws SQLException {
        String sql =Utillity.getDataPostingForGenerateAck();
        List<SpanSp2dPosting> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, spanConfig.getAcctRrRpkbunGaji());
            ps.setString(2, spanConfig.getAcctRrReksusSbsn());
            ps.setString(3, spanConfig.getAcctRpkbunGaji());
            ps.setString(4, spanConfig.getAcctRrRpkbunGaji());
            ps.setString(5, documentNumber);
            try (ResultSet rs = ps.executeQuery()) {
                 while(rs.next()) {
                    SpanSp2dPosting row = new SpanSp2dPosting();                                    
                    row.id = rs.getLong("id");
                    row.applicationareaMessageTypeIndicator     = rs.getString("applicationareamessagetypeindicator");
                    row.applicationareaSenderIdentifier         = rs.getString("applicationareasenderidentifier");
                    row.applicationareaMessageIdentifier        = rs.getString("applicationareamessageidentifier");
                    row.amount                                  = rs.getBigDecimal("amount");
                    row.returnCode                              = rs.getString("return_code");
                    row.documentNumber                          = rs.getString("documentnumber");
                    row.paymentMethod                           = rs.getString("paymentmethod");
                    result.add(row);
                }
            }
        }
        return result;
    }

    public  List<SpanSp2dPosting> fetchSp2dPostingForNonGaji(String documentNumber) throws SQLException {
        String sql =Utillity.getDataPostingForGenerateAck();
        List<SpanSp2dPosting> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, spanConfig.getAcctRpkbunNonGaji());
            ps.setString(2, spanConfig.getAcctRrReksusSbsn());
            ps.setString(3, spanConfig.getAcctRpkbunNonGaji());
            ps.setString(4, spanConfig.getAcctRrRpkbunNonGaji());
            ps.setString(5, documentNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    SpanSp2dPosting row = new SpanSp2dPosting();
                    row.id = rs.getLong("id");
                    row.applicationareaMessageTypeIndicator     = rs.getString("applicationareamessagetypeindicator");
                    row.applicationareaSenderIdentifier         = rs.getString("applicationareasenderidentifier");
                    row.applicationareaMessageIdentifier        = rs.getString("applicationareamessageidentifier");
                    row.amount                                  = rs.getBigDecimal("amount");
                    row.returnCode                              = rs.getString("return_code");
                    row.documentNumber                          = rs.getString("documentnumber");
                    row.paymentMethod                           = rs.getString("paymentmethod");
                    result.add(row);
                }
            }
        }
        return result;
    }


    public SpanSp2dStageIn getStageinByStatus(String documentNumber) throws SQLException{
        SpanSp2dStageIn row = new SpanSp2dStageIn();
        String sql = "SELECT id, applicationareasenderidentifier, applicationareareceiveridentifier, " +
                     "applicationareadetailsenderidentifier, applicationareadetailreceiveridentifier, " +
                     "applicationareacreationdatetime, applicationareamessageidentifier, " +
                     "applicationareamessagetypeindicator, applicatioanareamessageversiontext, " +
                     "documentdate, documentnumber, beneficiaryname, beneficiarybankcode, " +
                     "beneficiarybank, beneficiaryaccount, amount, currencytarget, description, " +
                     "agentbankcode, agentbankaccountnumber, agentbankaccountname, " +
                     "emailaddress, swiftcode, ibancode, paymentmethod, " +
                     "sp2dcount, totalcount, totalamount, totalbatchcount, " +
                     "sp2d_number, date_posting, reference_number, return_code, status " +
                     "FROM span_sp2d_stage_in " +
                     "WHERE status LIKE 'PST-%' " +
                     "AND agentbankaccountnumber IN ('" + spanConfig.getAcctRpkbunGaji() + "','" + spanConfig.getAcctRrRpkbunGaji() + "') " +
                     "AND documentnumber = ? " +
                     "AND paymentmethod = '5'";
       
                Map<String, SpanSp2dStageIn> map = new HashMap<>();
                try(PreparedStatement ps = conn.prepareStatement(sql)){
                    ps.setString(1, documentNumber);
                    try(ResultSet rs = ps.executeQuery()){
                       if (rs.next()){
                         
                         row.setDocumentNumber(documentNumber); 
                         row.setBeneficiaryBankCode(rs.getString("beneficiarybankcode"));
                         row.setBeneficiaryBank(rs.getString("beneficiarybank"));
                         row.setBeneficiaryAccount(rs.getString("beneficiaryaccount"));
                         row.setAmount(BigDecimal.valueOf(rs.getFloat("amount")));
                         row.setCurrencyTarget(rs.getString("currencytarget"));
                         row.setDescription(rs.getString("description"));
                         row.setAgentBankCode(rs.getString("agentbankcode"));
                         row.setAgentBankAccountNumber(rs.getString("agentbankaccountnumber"));
                         row.setAgentBankAccountName(rs.getString("agentbankaccountname"));
                         row.setPaymentMethod(rs.getString("paymentmethod"));
                         row.setStatus(rs.getString("status"));
                         row.setApplicationAreaSenderIdentifier(rs.getString("applicationareasenderidentifier"));
                         row.setApplicationAreaReceiverIdentifier(rs.getString("applicationareareceiveridentifier"));
                         row.setApplicationAreaDetailSenderIdentifier(rs.getString("applicationareadetailsenderidentifier"));
                         row.setApplicationAreaDetailReceiverIdentifier(rs.getString("applicationareadetailreceiveridentifier"));
                   
                         if (rs.getTimestamp("applicationareacreationdatetime") != null) {
                              row.setApplicationAreaCreationDateTime(
                              rs.getTimestamp("applicationareacreationdatetime").toLocalDateTime()
                            );}
                   
                          row.setApplicationAreaMessageIdentifier(
                              rs.getString("applicationareamessageidentifier")
                          );
                          
                          row.setApplicationAreaMessageTypeIndicator(
                              rs.getString("applicationareamessagetypeindicator")
                          );
                          
                          row.setApplicationAreaMessageVersionText(
                              rs.getString("applicatioanareamessageversiontext")
                          );
                          
                          if (rs.getTimestamp("documentdate") != null) {
                              row.setDocumentDate(
                                  rs.getTimestamp("documentdate").toLocalDateTime().toLocalDate()
                              );
                          }
                          
                          row.setSp2dNumber(rs.getString("sp2d_number"));
                          row.setEmailAddress(rs.getString("emailaddress"));
                          row.setSwiftCode(rs.getString("swiftcode"));
                          row.setIbanCode(rs.getString("ibancode"));
                          
                          row.setSp2dCount(rs.getInt("sp2dcount"));
                          row.setTotalCount(rs.getInt("totalcount"));
                          
                          row.setTotalAmount(
                              BigDecimal.valueOf(rs.getDouble("totalamount"))
                          );
                          
                          row.setTotalBatchCount(rs.getInt("totalbatchcount"));   
                        }
                    } 
                     return row;
    }
   }

    public void prosesAckBatch(){
        GenerateAckOut generateAckOut = new GenerateAckOut();
        MainCHK.tulisLog("Start Proses generate Ack secara Batching");
       try{
        List<SpanSp2dPosting> source = fetchsp2dPostingForAckBatch();
        Map<String,ReturnStatusAck> map_status_code = Utillity.fetchReturnStatusAck(conn);
          if (!source.isEmpty() && source != null){
              MainCHK.tulisLog("Jumlah data yang akan di proses : "+ source.size());
              String outputPath = generateAckOut.generateAckFile(conn,spanConfig,source,map_status_code);
              generateAckOut.copyToArchiveIfExists(spanConfig, outputPath, new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            for (SpanSp2dPosting data : source){
              generateAckOut.updateFlagAckBatch(conn, spanConfig, data);
           }
          } else {
            MainCHK.tulisLog("Tidak ada data sp2d yang dapat di proses");
          }
        }
        catch (IOException | SQLException e){
         MainCHK.tulisLog("Error saat generate ack transaksi bifast " + e.getMessage());
         e.printStackTrace();
       }
    }  
   public void prosesAck(SpanSp2dStageIn item){
        GenerateAckOut generateAckOut = new GenerateAckOut();
        MainCHK.tulisLog("Process generate ack untuk document number :" + item.getDocumentNumber());
        List<SpanSp2dPosting> source = null;
       try{
           String transactionType = Utillity.getTransactionType(item, this.spanConfig);
           switch (transactionType.trim().toUpperCase()) {
               case "BO2":
                   source = fetchSp2dPostingForGaji(item.getDocumentNumber());;
                   break;
               case "BO1":
                   source = fetchSp2dPostingForNonGaji(item.getDocumentNumber());
                   break;
           }

        Map<String,ReturnStatusAck> map_status_code = Utillity.fetchReturnStatusAck(conn);

          if (!source.isEmpty() && source != null){
            String outputPath = generateAckOut.generateAckFile(conn,spanConfig,source,map_status_code);
            generateAckOut.copyToArchiveIfExists(spanConfig, outputPath, new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            for (SpanSp2dPosting data : source){
              generateAckOut.updateFlagAck(conn, spanConfig, data);
            }
        }
       } catch (IOException | SQLException e){
         MainCHK.tulisLog("Error saat generate ack transaksi bifast"+e.getMessage());
         e.printStackTrace();
       }
    }

    // [CHANGE][2026-09-08] Overload prosesAck untuk arsitektur dedicated writer per thread.
    // Menggunakan appendAckLines ke writer yang sudah dibuka oleh thread, bukan membuat file baru.
    // copyToArchiveIfExists tidak dipanggil di sini — archive dilakukan sekali saat thread selesai.
    public void prosesAck(SpanSp2dStageIn item, BufferedWriter ackWriter) {
        GenerateAckOut generateAckOut = new GenerateAckOut();
        MainCHK.tulisLog("Process generate ack (dedicated writer) untuk document number: " + item.getDocumentNumber());
        List<SpanSp2dPosting> source = null;
        try {
            String transactionType = Utillity.getTransactionType(item, this.spanConfig);
            switch (transactionType.trim().toUpperCase()) {
                case "BO2":
                    source = fetchSp2dPostingForGaji(item.getDocumentNumber());
                    break;
                case "BO1":
                    source = fetchSp2dPostingForNonGaji(item.getDocumentNumber());
                    break;
            }
            Map<String, ReturnStatusAck> map_status_code = Utillity.fetchReturnStatusAck(conn);
            if (source != null && !source.isEmpty()) {
                generateAckOut.appendAckLines(ackWriter, conn, source, map_status_code);
                for (SpanSp2dPosting data : source) {
                    generateAckOut.updateFlagAck(conn, spanConfig, data);
                }
            }
        } catch (IOException | SQLException e) {
            MainCHK.tulisLog("Error saat append ack transaksi bifast: " + e.getMessage());
            e.printStackTrace();
        }
    }

     public void prosesAckRetur(String documentNumber){
        GenerateAckOut generateAckOut = new GenerateAckOut();
        MainCHK.tulisLog("Proses generate ack retur");
       try{
        List<SpanSp2dPosting> source = fetchSp2dPostingForAckRetur(documentNumber);
        Map<String,ReturnStatusAck> map_status_code = Utillity.fetchReturnStatusAck(conn);

          if (!source.isEmpty()){
            String outputPath = generateAckOut.generateAckFile(conn,spanConfig,source,map_status_code);
            generateAckOut.copyToArchiveIfExists(spanConfig, outputPath, new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            for (SpanSp2dPosting data : source){
              generateAckOut.updateFlagAckRetur(conn, spanConfig, data);
            }
        }
       } catch (IOException | SQLException e){
         MainCHK.tulisLog("Error saat generate ack transaksi bifast"+e.getMessage());
         e.printStackTrace();
       }
    }

    // [CHANGE][2026-09-08] Overload prosesAckRetur untuk arsitektur dedicated writer per thread.
    public void prosesAckRetur(String documentNumber, BufferedWriter ackWriter) {
        GenerateAckOut generateAckOut = new GenerateAckOut();
        MainCHK.tulisLog("Proses generate ack retur (dedicated writer) untuk document number: " + documentNumber);
        try {
            List<SpanSp2dPosting> source = fetchSp2dPostingForAckRetur(documentNumber);
            Map<String, ReturnStatusAck> map_status_code = Utillity.fetchReturnStatusAck(conn);
            if (!source.isEmpty()) {
                generateAckOut.appendAckLines(ackWriter, conn, source, map_status_code);
                for (SpanSp2dPosting data : source) {
                    generateAckOut.updateFlagAckRetur(conn, spanConfig, data);
                }
            }
        } catch (IOException | SQLException e) {
            MainCHK.tulisLog("Error saat append ack retur transaksi bifast: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateReturnCodeReturProses(SpanSp2dStageIn rec) throws SQLException{
        String sql = "UPDATE span_sp2d_posting SET status = ?  , return_code = ? " +
                     "WHERE documentdate = ? " +
                     "AND status = ? " +
                     "AND documentnumber = ? ";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusPosted);
            // hardcode first ,tobe mapping with esb response code
            ps.setString(2, "001");
            ps.setString(3, spanConfig.getAppDate());
            ps.setString(4, statusReadyPosting);
            ps.setString(5, rec.getDocumentNumber());
            MainCHK.tulisLog(ps);
            int updated = ps.executeUpdate();
            MainCHK.tulisLog("Updated return_code: " + updated + " row(s) → 001");
        }
    }

    public void updateStatusBifastData(SpanSp2dStageIn rec , String mappingRc) throws SQLException{
      String sql = Utillity.updateStatusSpanSp2dBifastData(); 
      try (PreparedStatement ps = conn.prepareStatement(sql)){
        ps.setString(1, mappingRc);
        ps.setString(2, rec.getDocumentNumber());
        ps.executeUpdate();

      }

    }
    public void updateStatusForRetryRetur(SpanSp2dStageIn rec , String mappingRc) throws SQLException{
        String sql =Utillity.updateStatusForRetryRetur("span_sp2d_stage_in");
        //Map<String, BifastRcMapping> map = Utillity.fetchBifastRcMappingRetur(conn);

        //String rcBifast = map != null ? Utillity.safe(map.get(mappingRc).bifast_rc)  : "";
        executUpdateForRetryRetur(sql,statusReadyPosting,rec.getDocumentNumber());

       String documentNumberOnTable = getDataSp2dBifast(rec.getDocumentNumber());
       if (documentNumberOnTable == null){
           insertDataSp2dBfast(rec);
       }

       String sqlRetur ="UPDATE span_sp2d_bifast_data " +
               "SET bifast_response_code = ? "+
               "WHERE document_number = ? ";

       try (PreparedStatement ps = conn.prepareStatement(sqlRetur)){
           ps.setString(1, mappingRc);
           ps.setString(2, rec.getDocumentNumber());
           int updated = ps.executeUpdate();
           if (updated > 0) {
               MainCHK.tulisLog("Updated bifast_response_code = '" + mappingRc + "' pada span_sp2d_stage_in untuk doc: " + rec.getDocumentNumber());
           } else {
               MainCHK.tulisLog("Gagal update bifast_response_code untuk doc: " + rec.getDocumentNumber());
           }
       }


   }


   public void insertReturDatainPostingTable(SpanSp2dStageIn stageIn, FundsTransferSoapResponse response) throws SQLException{
    try{

        String transactionType = Utillity.getTransactionType(stageIn, this.spanConfig);
        String beneficiaryAccount = "";

        switch (transactionType) {
            case "BO2":
                beneficiaryAccount = this.spanConfig.getAcctRrRpkbunGaji();
                break;
            case "BO1":
                beneficiaryAccount = this.spanConfig.getAcctRrRpkbunNonGaji();
                break;
            default:
                beneficiaryAccount = this.spanConfig.getAcctRrReksusSbsn();
        }


        boolean benefciaryAccountisRR = stageIn.getBeneficiaryAccount().equals(beneficiaryAccount);
        SpanSp2dPosting rec = Utillity.builderSpanSp2dPosting(stageIn);
        if (benefciaryAccountisRR){
            switch (transactionType) {
                case "BO2":
                    rec.beneficiaryName= "ACCT_RR_RPKBUN_GAJI_BSM";
                    break;
                case "BO1":
                    rec.beneficiaryName= "ACCT_RR_RPKBUN_NON_GAJI_BSM";
                    break;
                default:
                    rec.beneficiaryName= "ACCT_RR_REKSUS_BSM";
            }
        }

        //return code yang awalnya dari core menjadi dari balikan response tws
        if (response.isSuccess()){
          rec.returnCode= "000";
          rec.referenceNumber = response.getTransactionId();
        }else {
          rec.returnCode= response.getTransactionId();
          rec.referenceNumber=null;
        }

        Map<String,ReturnStatusAck> map_status_code = Utillity.fetchReturnStatusAck(conn);
        ReturnStatusAck ack = map_status_code.get(rec.returnCode);
        
        if (ack != null && "Reject".equals(ack.status)) {
            rec.status = "RDY-008";
            rec.rejectStatus = "N";
            rec.rejectDate = rec.datePosting;
        } else {
            rec.status = "FIN-000";
            rec.rejectStatus = null;
            rec.rejectDate = null; 
        }

        rec.flagAck = "Y";

        insertPostingRecords(rec);
        

    } catch (SQLException e){
        System.out.println(e.getMessage());
    } 
    }

   private void processExcludeOutOfBalance(String configAccountKey) throws SQLException {
    String sqlSaldo = Utillity.generateSqlSaldo();
    String account_number = "";
    BigDecimal amount_balance = BigDecimal.ZERO;

    try (PreparedStatement qSelectSaldo = conn.prepareStatement(sqlSaldo)) {
        qSelectSaldo.setString(1, configAccountKey);
        MainCHK.tulisLog(qSelectSaldo.toString());

        try (ResultSet rsSaldo = qSelectSaldo.executeQuery()) {
            if (!rsSaldo.next()) {
                MainCHK.tulisLog(configAccountKey + " tidak ditemukan");
                return;
            }
            account_number = rsSaldo.getString("account_number");
            amount_balance = rsSaldo.getBigDecimal("amount_balance");
            if ("ACCT_RPKBUN_GAJI".equals(configAccountKey)) {
                ACCT_RPKBUN_GAJI = account_number;
            } else {
                ACCT_RPKBUN_NON_GAJI = account_number;
            }
        }
    }

    MainCHK.tulisLog("saldo [" + account_number + "]:[" + amount_balance + "]");

    BigDecimal tempAmount = BigDecimal.ZERO;
    boolean flagSaldoTidakCukup = false;

    List<String> excludeSp2d = new ArrayList<>();
    List<String> includeSp2d = new ArrayList<>();

    try (PreparedStatement qSelectSP2D = conn.prepareStatement(Utillity.getSaldoGroupByMessageIndetifier(3))) {
        qSelectSP2D.setString(1, account_number);
        qSelectSP2D.setString(2, statusReadyProses);
        qSelectSP2D.setString(3, statusRetryProses);
        qSelectSP2D.setString(4, flaggingBifast);

        MainCHK.tulisLog(qSelectSP2D.toString());

        try (ResultSet rsSP2D = qSelectSP2D.executeQuery()) {
            while (rsSP2D.next()) {
                BigDecimal sumAmount = rsSP2D.getBigDecimal("sumamount");
                MainCHK.tulisLog("Nominal Saldo :" +sumAmount);
                if (sumAmount != null) {
                    tempAmount = tempAmount.add(sumAmount);
                   
                }

                String msgId = rsSP2D.getString("applicationareamessageidentifier");

                if (amount_balance.compareTo(tempAmount) < 0) {
                    excludeSp2d.add(msgId);
                } else {
                    includeSp2d.add(msgId);
                }
            }
        }
    }

    if (!excludeSp2d.isEmpty()) {
        flagSaldoTidakCukup = true;
        executeUpdateStatusForExclude(excludeSp2d, statusWaitingDropping, account_number, statusReadyProses);
        executeUpdateStatusForExclude(excludeSp2d, statusWaitingDroppingBifast, account_number, flaggingBifast);
    }

    if (!includeSp2d.isEmpty()) {
        executeUpdateStatusForExclude(includeSp2d, statusReadyProsesBifast, account_number, flaggingBifast);
    }

    if (!flagSaldoTidakCukup) {
        MainCHK.tulisLog("Saldo Cukup untuk semua SP2D saat ini");
    }
}

   private void executeUpdateStatusForExclude(List<String> data, String targetStatus, String accountNumber, String currentStatus) throws SQLException {
    String sql = Utillity.updateStatusForExclude(data.size());
    try (PreparedStatement psUpdate = conn.prepareStatement(sql)) {
        psUpdate.setString(1, targetStatus);
        psUpdate.setString(2, accountNumber);
        psUpdate.setString(3, currentStatus);
        for (int i = 0; i < data.size(); i++) {
            psUpdate.setString(4 + i, data.get(i));
        }
        int rows = psUpdate.executeUpdate();
        if (rows > 0) {
            MainCHK.tulisLog("Update status [" + targetStatus + "] rows => " + rows);
        }
    }
}

    private void executUpdateForRetryRetur(String sql, String targetStatus,String documentNumber) throws SQLException {
    try (PreparedStatement psUpdate = conn.prepareStatement(sql)) {
            psUpdate.setString(1, targetStatus);
            psUpdate.setString(2, spanConfig.getAppDate());
            psUpdate.setString(3, documentNumber);
            MainCHK.tulisLog(psUpdate);
            int rows = psUpdate.executeUpdate();
            if (rows > 0) {
            MainCHK.tulisLog("Update status [" + targetStatus + "] rows => " + rows);
            }
    }
}

   private String processIncludeOutOfBalance(String configAccountKey) throws SQLException {
     String accountNumber = null;
     String sqlSaldo = Utillity.generateSqlSaldo();

      try (PreparedStatement qSelectSaldo = conn.prepareStatement(sqlSaldo)) {
          qSelectSaldo.setString(1, configAccountKey);
          MainCHK.tulisLog(qSelectSaldo.toString());
          try (ResultSet rsSaldo = qSelectSaldo.executeQuery()) {
              if (rsSaldo.next()) {
                  accountNumber = rsSaldo.getString("account_number");
                  if ("ACCT_RPKBUN_GAJI".equals(configAccountKey)) {
                      ACCT_RPKBUN_GAJI = accountNumber;
                  } else {
                      ACCT_RPKBUN_NON_GAJI = accountNumber;
                  }
              } else {
                  MainCHK.tulisLog("Rekening " + configAccountKey + " tidak ditemukan!");
    
              }
          }
   }
    return accountNumber;
 }

    private void executeUpdateStatusForIncludeBalance (String accountNumber ,String targetStatus, String currentStatus) throws  SQLException{
      try (PreparedStatement qUpdate = conn.prepareStatement(Utillity.updateStatusForInclude())) {
        qUpdate.setString(1, targetStatus);
        qUpdate.setString(2, accountNumber);
        qUpdate.setString(3, currentStatus);
        qUpdate.executeUpdate();
     }
   }

   public String getBankCode(String bankCode) throws SQLException{
        String sql = Utillity.getBankCode();
        String result = null;
        try (PreparedStatement ps = conn.prepareStatement(sql) ){
            ps.setString(1,bankCode);
            try(ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    result = rs.getString("participant");
                }
            }
        }
        return result;
   }
   
   public String findDataOnNameChecking (SpanSp2dStageIn item , String inquiryNameResponse) throws SQLException{
    String sql = Utillity.findApproveData();
    String result =null;
    try (PreparedStatement ps = conn.prepareStatement(sql)){
        ps.setString(1, item.getBeneficiaryAccount());
        ps.setString(2, Utillity.normalizeName(item.getBeneficiaryName()));
        ps.setString(3, Utillity.normalizeName(inquiryNameResponse));
     try (ResultSet rs = ps.executeQuery()){
        if (rs.next()){
             result = rs.getString("approval_status");
        }
     }
    }
        return result;
   
   }

   public void insertDataForNameChecking(SpanSp2dStageIn item,String inquiryNameResponse) throws SQLException{
     String sql = " INSERT INTO span_sp2d_bifast_name_checking " +  
                  " (document_number,beneficiary_account,stagein_beneficiary_name,inquiry_beneficiary_name) " +
                  " VALUES (?,?,?,?)";

     try(PreparedStatement ps = conn.prepareStatement(sql) ){
        ps.setString(1, item.getDocumentNumber());
        ps.setString(2, item.getBeneficiaryAccount());
        ps.setString(3, Utillity.normalizeName(item.getBeneficiaryName()));
        ps.setString(4, Utillity.normalizeName(inquiryNameResponse));
       
        
        int row = ps.executeUpdate();
        if (row > 0){
            MainCHK.tulisLog("Proses insert data ke tabel neme checking berhasil untuk norek :" +item.getBeneficiaryAccount() + "untuk document number : "+ item.getDocumentNumber());
        }else{
             MainCHK.tulisLog("Query Insert : "+ps);
        }
     }         

   }

   public int getIdUser() throws SQLException{
    String sql = Utillity.getIdUser();
    int result = 0;
    try(PreparedStatement ps = conn.prepareStatement(sql)){
        ps.setString(1, "SYSTEM");
        ps.setString(2, "SYSTEM");
      try(ResultSet rs = ps.executeQuery()){
        if (rs.next()){
            result = rs.getInt("id");
        }
      }
    }

    return result;

   }


   public void insertAuditTrailFallbackSkn(SpanSp2dStageIn item)throws SQLException{
    String sql  = Utillity.insertAuditTrail();
    int id_user = getIdUser();
    if (id_user != 0){
      try(PreparedStatement ps = conn.prepareStatement(sql)){
        ps.setString(1, item.getDocumentNumber());
        ps.setInt(2, 1);
        ps.setInt(3, id_user);
        ps.setString(4, "status:PST-000|payment_method:5");
        ps.setString(5, "status:UPL-000|payment_method:2");
        ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
        ps.executeUpdate();
    }
   } else{
       MainCHK.tulisLog("terdapat kesalahan dalam get id user system");
   }
}
    
}
