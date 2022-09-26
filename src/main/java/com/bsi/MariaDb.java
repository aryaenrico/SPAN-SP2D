package com.bsi;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.Locale;
import java.util.ResourceBundle;
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

    public MariaDb(String pathProp, String propName) {
        try {
//            File file = new File(System.getProperty("user.dir"));
            File file = new File(pathProp);
            URL[] urls = {file.toURI().toURL()};
            ClassLoader loader = new URLClassLoader(urls);
            rb = ResourceBundle.getBundle(propName, Locale.getDefault(), loader);

            System.setProperty("line.separator", "\r\n");
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
                            "WHERE status = 'UPK-000' and amount < 0 GROUP BY sp2d_number;"
            );
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
                            "INSERT INTO span_void_list (sp2d_number, status, void_flag) VALUES (?, ?, ?)");
                    qInsert.setString(1, rs.getString("sp2d_number"));
                    qInsert.setString(2, "VOD-201");
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
                            "from span_sp2d_stage_in a where status = 'UPK-000' " +
                            "group by a.sp2d_number, a.sp2dcount;"
            );
            MainCHK.tulisLog(qSelect.toString());
            rs = qSelect.executeQuery();
            while (rs.next()) {
                String existingSp2d = rs.getString("cid");
                String allSp2d = rs.getString("sp2dcount");
                if (existingSp2d.equals(allSp2d)) {
                    qUpdate = conn.prepareStatement(
                            "update span_sp2d_stage_in set status = 'UPL-000' " +
                                    "WHERE sp2d_number = ? AND status = 'UPK-000'");
                    qUpdate.setString(1, rs.getString("sp2d_number"));
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
}
