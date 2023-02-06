package com.bsi;

import java.io.File;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
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
    public long rowCount;

    public final String statusReadyProses = "UPL-000";
    public final String statusRetryProses = "RDY-008";
    public final String statusWaitingUPL = "UPK-000";
    public String ACCT_RPKBUN_GAJI;
    public String ACCT_RPKBUN_NON_GAJI;
    public final String statusWaitingDropping = "UPW-000";
    public final String statusVoid = "VOD-201";

    public MariaDb(String pathProp, String propName) {
        try {
//            File file = new File(System.getProperty("user.dir"));
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
                            "INSERT INTO span_void_list (sp2d_number, status, void_flag) VALUES (?, ?, ?)"
                    );
                    qInsert.setString(1, rs.getString("sp2d_number"));
                    qInsert.setString(2, statusVoid);
                    qInsert.setInt(3, 1);
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

    public static void main(String[] args) throws SQLException {
        MariaDb mariaDb = new MariaDb("/Users/choirulrahmadan/BSI/SpanPlay/conf/", "bo2span");
//        mariaDb.excludeOutOfBalance();
//        mariaDb.includeOutOfBalance();
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
