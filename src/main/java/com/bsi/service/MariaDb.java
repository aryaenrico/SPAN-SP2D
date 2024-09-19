package com.bsi.service;

import com.bsi.MainCHK;
import com.bsi.entity.PostingRequest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.ws.rs.core.Response;
import java.io.File;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MariaDb {
    Connection conn = null;
    Statement st;
    public PreparedStatement ps;
    ResultSet rs, rs2;
    String namaFile, tglSP2D;
    ResourceBundle rb;
    RestClient restClient;
    public long rowCount;

    public final String statusReadyProses = "UPL-000";
    public final String statusRetryProses = "RDY-008";
    public final String statusWaitingUPL = "UPK-000";
    public final String statusWaitingUPV = "UPV-000";
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
    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

    public MariaDb(String pathProp, String propName) {
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

            String baseurl_api_magic = rb.getString("baseurl_api_magic").trim();
            restClient = new RestClient(baseurl_api_magic);
        } catch (ClassNotFoundException e) {
            MainCHK.tulisLog("Error 1. Cek konfigurasi koneksi database");
            e.printStackTrace(System.out);
        } catch (SQLException ex) {
            MainCHK.tulisLog("Error 2. Cek konfigurasi koneksi database");
            ex.printStackTrace(System.out);
        } catch (MalformedURLException ex) {
            Logger.getLogger(MariaDb.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        MariaDb db = new MariaDb("D:/BSI/Span/sp2d_check_negative_amount/tesDs", "bo2span");
        try {
            db.postingDetailAffiliate();
        } catch (SQLException ex) {
            Logger.getLogger(MariaDb.class.getName()).log(Level.SEVERE, null, ex);
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
                                qUpdate.setString(2, prefixStatusVoid + voidAmount);
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
                            qUpdate.setString(2, prefixStatusVoid + voidAlreadyPosted);
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
                            qUpdate.setString(2, prefixStatusVoid + voidExpired);
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
                            qUpdate.setString(2, prefixStatusVoid + voidNotFound);
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

    public void excludeOutOfBalanceBO1() throws SQLException {
        PreparedStatement qSelectSaldo = null, qSelectSP2D = null, qUpdate = null;
        try {
            qSelectSaldo = conn.prepareStatement(
                    "select * " +
                            "from span_account " +
                            "         join span_application_config conf on span_account.account_number = conf.config_value " +
                            "where conf.config_name = ? ;"
            );
            qSelectSaldo.setString(1, "ACCT_RPKBUN_NON_GAJI");
            MainCHK.tulisLog(qSelectSaldo.toString());
            rs = qSelectSaldo.executeQuery();

            BigDecimal amount_balance;
            BigDecimal tempAmount = new BigDecimal(0);
            BigDecimal perLoopAmount = new BigDecimal(0);
            String account_number = "";

            if (rs.next()) {
                ACCT_RPKBUN_NON_GAJI = rs.getString("account_number");
                amount_balance = rs.getBigDecimal("amount_balance");
                account_number = ACCT_RPKBUN_NON_GAJI;
                MainCHK.tulisLog("saldo [" + account_number + "]:[" + amount_balance + "]");


                qSelectSP2D = conn.prepareStatement(
                        "select applicationareamessageidentifier, count(1) count, sum(amount) sumamount " +
                                "from span_sp2d_stage_in " +
                                "WHERE documentdate = (select config_value from span_application_config where config_name = 'APP_DATE') " +
                                "  and agentbankaccountnumber = ? " +
                                "  AND status in ('" + statusReadyProses + "','" + statusRetryProses + "') " +
                                "GROUP BY applicationareamessageidentifier"
                );
                qSelectSP2D.setString(1, account_number);
//                qSelectSP2D.setString(2, statusReadyProses);
                qSelectSP2D.executeQuery();
                rs2 = qSelectSP2D.executeQuery();
                List<String> list = new ArrayList<>(Collections.emptyList());
                while (rs2.next()) {
                    perLoopAmount = tempAmount;
                    tempAmount = perLoopAmount.add(rs2.getBigDecimal("sumamount"));
                    if (amount_balance.compareTo(tempAmount) < 0) {
                        list.add(rs2.getString("applicationareamessageidentifier"));
                    }
                }

                String joinNamaFileSp2d = "'" + String.join("','", list) + "'";

                if (list.size() > 0) {
                    qUpdate = conn.prepareStatement(
                            "update span_sp2d_stage_in " +
                                    "set status = ? " +
                                    "WHERE documentdate = (select config_value from span_application_config where config_name = 'APP_DATE') " +
                                    "  and applicationareamessageidentifier IN (" + joinNamaFileSp2d + ") " +
                                    "  and agentbankaccountnumber = ? " +
                                    "  and status = ?;"
                    );
                    qUpdate.setString(1, statusWaitingDropping);
                    qUpdate.setString(2, account_number);
                    qUpdate.setString(3, statusReadyProses);
                    qUpdate.executeUpdate();
                    MainCHK.tulisLog(qUpdate.toString());
                } else {
                    MainCHK.tulisLog("Saldo Cukup untuk semua SP2D saat ini.");
                }
            } else {
                MainCHK.tulisLog("ACCT_RPKBUN_NON_GAJI tidak ditemukan");
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
            MainCHK.logger.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (qSelectSaldo != null) {
                qSelectSaldo.close();
            }
            if (qUpdate != null) {
                qUpdate.close();
            }
        }
    }

    public void excludeOutOfBalanceBO2() throws SQLException {
        PreparedStatement qSelectSaldo = null, qSelectSP2D = null, qUpdate = null;
        try {
            qSelectSaldo = conn.prepareStatement(
                    "select * " +
                            "from span_account " +
                            "         join span_application_config conf on span_account.account_number = conf.config_value " +
                            "where conf.config_name = ? ;"
            );
            qSelectSaldo.setString(1, "ACCT_RPKBUN_GAJI");
            MainCHK.tulisLog(qSelectSaldo.toString());
            rs = qSelectSaldo.executeQuery();

            BigDecimal amount_balance;
            BigDecimal tempAmount = new BigDecimal(0);
            BigDecimal perLoopAmount = new BigDecimal(0);
            String account_number = "";

            if (rs.next()) {
                ACCT_RPKBUN_GAJI = rs.getString("account_number");
                amount_balance = rs.getBigDecimal("amount_balance");
                account_number = ACCT_RPKBUN_GAJI;
                MainCHK.tulisLog("saldo [" + account_number + "]:[" + amount_balance + "]");

                qSelectSP2D = conn.prepareStatement(
                        "select applicationareamessageidentifier, count(1) count, sum(amount) sumamount " +
                                "from span_sp2d_stage_in " +
                                "WHERE documentdate = (select config_value from span_application_config where config_name = 'APP_DATE') " +
                                "  and agentbankaccountnumber = ? " +
                                "  AND status in ('" + statusReadyProses + "','" + statusRetryProses + "') " +
                                "GROUP BY applicationareamessageidentifier"
                );
                qSelectSP2D.setString(1, account_number);
                qSelectSP2D.executeQuery();
                rs2 = qSelectSP2D.executeQuery();
                List<String> list = new ArrayList<>(Collections.emptyList());
                while (rs2.next()) {
                    perLoopAmount = tempAmount;
                    tempAmount = perLoopAmount.add(rs2.getBigDecimal("sumamount"));
                    if (amount_balance.compareTo(tempAmount) < 0) {
                        list.add(rs2.getString("applicationareamessageidentifier"));
                    }
                }

                String joinNamaFileSp2d = "'" + String.join("','", list) + "'";

                if (list.size() > 0) {
                    qUpdate = conn.prepareStatement(
                            "update span_sp2d_stage_in " +
                                    "set status = ? " +
                                    "WHERE documentdate = (select config_value from span_application_config where config_name = 'APP_DATE') " +
                                    "  and applicationareamessageidentifier IN (" + joinNamaFileSp2d + ") " +
                                    "  and agentbankaccountnumber = ? " +
                                    "  and status = ?;"
                    );
                    qUpdate.setString(1, statusWaitingDropping);
                    qUpdate.setString(2, account_number);
                    qUpdate.setString(3, statusReadyProses);
                    qUpdate.executeUpdate();
                    MainCHK.tulisLog(qUpdate.toString());
                } else {
                    MainCHK.tulisLog("Saldo Cukup untuk semua SP2D saat ini.");
                }
            } else {
                MainCHK.tulisLog("ACCT_RPKBUN_GAJI tidak ditemukan");
            }
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
            MainCHK.logger.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (qSelectSaldo != null) {
                qSelectSaldo.close();
            }
            if (qUpdate != null) {
                qUpdate.close();
            }
        }
    }

    public void includeOutOfBalanceBO1() throws SQLException {
        PreparedStatement qUpdate = null;
        try {
            qUpdate = conn.prepareStatement(
                    "update span_sp2d_stage_in set status = ? WHERE " +
                            "documentdate = (select config_value from span_application_config where config_name = 'APP_DATE') " +
                            "AND agentbankaccountnumber = ? " +
                            "AND status = ?"
            );
            qUpdate.setString(1, statusReadyProses);
            qUpdate.setString(2, ACCT_RPKBUN_NON_GAJI);
            qUpdate.setString(3, statusWaitingDropping);
            qUpdate.executeUpdate();
            MainCHK.tulisLog(qUpdate.toString());
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
            MainCHK.logger.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (qUpdate != null) {
                qUpdate.close();
            }
        }
    }

    public void includeOutOfBalanceBO2() throws SQLException {
        PreparedStatement qUpdate = null;
        try {
            qUpdate = conn.prepareStatement(
                    "update span_sp2d_stage_in set status = ? WHERE " +
                            "documentdate = (select config_value from span_application_config where config_name = 'APP_DATE') " +
                            "AND agentbankaccountnumber = ? " +
                            "AND status = ?"
            );
            qUpdate.setString(1, statusReadyProses);
            qUpdate.setString(2, ACCT_RPKBUN_GAJI);
            qUpdate.setString(3, statusWaitingDropping);
            qUpdate.executeUpdate();
            MainCHK.tulisLog(qUpdate.toString());
        } catch (SQLException ex) {
            ex.printStackTrace(System.out);
            MainCHK.logger.log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            if (qUpdate != null) {
                qUpdate.close();
            }
        }
    }


}
