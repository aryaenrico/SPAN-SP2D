package com.bsi;


import com.bsi.entity.mock.ProcessBifast;
import com.bsi.entity.mock.SpanSp2dStageIn;
import com.bsi.service.ServiceMariaDb;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MainCHK {
    public static final Logger logger = Logger.getLogger("SP2D-CHECK-NEGATIVE-AMOUNT");
    private static final boolean isDebug = false;
    private static String pid;

    public static void main(String[] args) {
//        initLog();
        pid = System.nanoTime() + "";
        String propPath = "";//argumen 0
        String propName = "";//argumen 1
        String propTgl = "";//argumen 2
        ServiceMariaDb serviceMariaDb = null;

        if (isDebug) {
            //debug only
            tulisLog("Debugging...");
//            mariaDb = new MariaDb("/Users/choirulrahmadan/BSI/SpanPlay/conf/", "bo2span");
//            chkDS("/Users/choirulrahmadan/BSI/SP2D_CHECK_NEGATIVE_AMOUNT/tesDs/bo2span.properties","/Users/choirulrahmadan/BSI/SP2D_CHECK_NEGATIVE_AMOUNT/tesDs/525451000990_SP2D_O_20220921_130509_073.xml");
            System.exit(0);
        } else if (args.length < 3) {
            tulisLog("Param tidak lengkap!");
            tulisLog("[path] [filename] [command] [tanggal yyyymmdd]");
            tulisLog("Command: [checkNegativeAmount/excludeOutOfBalance/includeOutOfBalance/checkPaymentMethod4/postingDetailAffiliate]");
            System.exit(0);
        } else {
            tulisLog("param1: " + args[0]);
            propPath = args[0];
            tulisLog("param2: " + args[1]);
            propName = args[1];
            tulisLog("param3: " + args[2]);
            serviceMariaDb = new ServiceMariaDb(propPath, propName);
        }

        switch (args[2]) {
            case "checkNegativeAmount":
                try {
                    serviceMariaDb.checkVoidList();
                    tulisLog("checkVoidList done..");
                    serviceMariaDb.updStageIn();
                    tulisLog("updStageIn done..");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break;
            case "excludeOutOfBalanceBO1":
                try {
                    serviceMariaDb.excludeOutOfBalanceBO1();
                    tulisLog("excludeOutOfBalanceBO1 done");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break;
            case "includeOutOfBalanceBO1":
                try {
                    serviceMariaDb.includeOutOfBalanceBO1();
                    tulisLog("includeOutOfBalanceBO1 done");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break;
            case "excludeOutOfBalanceBO2":
                try {
                    serviceMariaDb.excludeOutOfBalanceBO2();
                    tulisLog("excludeOutOfBalanceBO2 done");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break;
            case "includeOutOfBalanceBO2":
                try {
                    serviceMariaDb.includeOutOfBalanceBO2();
                    tulisLog("includeOutOfBalanceBO2 done");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break;
            case "checkPaymentMethod4":
                try {
                    serviceMariaDb.checkPaymentMethod4();
                    tulisLog("checkPaymentMethod4 done");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break;
            case "postingDetailAffiliate":
                try {
                    serviceMariaDb.postingDetailAffiliate();
                    tulisLog("postingDetailAffiliate done");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break;
            case "includeOutOfBalanceBO2Bifast":
                  try {
                    serviceMariaDb.includeOutOfBalanceBifastBo2();
                    tulisLog("includeOutOfBalanceBO2Bifast done");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break; 
            case "excludeOutOfBalanceBO2Bifast":
                  try {
                    serviceMariaDb.excludeOutOfBalanceBifastBo2();;
                    tulisLog("excludeOutOfBalanceBO2Bifast done");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break; 
                case "includeOutOfBalanceBO1Bifast":
                  try {
                    serviceMariaDb.includeOutOfBalanceBifastBo1();
                    tulisLog("includeOutOfBalanceBO1 done");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break; 

                case "excludeOutOfBalanceBO1Bifast":
                  try {
                    serviceMariaDb.excludeOutOfBalanceBifastBo1();
                    tulisLog("excludeOutOfBalanceBifastBo1 done");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break;
            // [NEW][2026-08-03] Implementasi Multi-Threading: Memanggil paymentBifast dengan 3 worker threads
            case "prosesTransactionBifast":
                try {
                    tulisLog("[MULTI-THREAD] Running command prosesTransactionBifast...");
                    List<SpanSp2dStageIn> dataBifast = serviceMariaDb.getDataBifast("BO2");
                    ProcessBifast processBifast = new ProcessBifast();
                    processBifast.paymentBifast(dataBifast, "BO2");
                    tulisLog("prosesTransactionBifast done..");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break; 
            // [NEW][2026-08-03] GAP 7: Scheduler command untuk memproses transaksi BI-FAST yang Timeout (TMO-000)
            case "prosesTimeoutCtBifast":
                try {
                    tulisLog("[GAP7] Running command prosesTimeoutCtBifast...");
                    List<SpanSp2dStageIn> dataTmo = serviceMariaDb.getDataBifastByStatus(serviceMariaDb.statusTimeoutCreditTransferBifast);
                    ProcessBifast processBifast = new ProcessBifast();
                    processBifast.prosesTimeoutCtBifast(dataTmo);
                    tulisLog("prosesTimeoutCtBifast done..");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break;
            // [NEW][2026-08-03] GAP 7: Scheduler command untuk memproses ulang check status BI-FAST (RGS-000)
            case "prosesRetryGetstatus":
                try {
                    tulisLog("[GAP7] Running command prosesRetryGetstatus...");
                    List<SpanSp2dStageIn> dataRgs = serviceMariaDb.getDataBifastByStatus(serviceMariaDb.statusForRetryGetstatus);
                    ProcessBifast processBifast = new ProcessBifast();
                    processBifast.prosesTimeoutCtBifast(dataRgs);
                    tulisLog("prosesRetryGetstatus done..");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break;
            // [NEW][2026-08-03] GAP 7: Scheduler command untuk memproses ulang Retur T24 yang gagal/timeout (RRS-000)
            case "prosesRetryRetur":
                try {
                    tulisLog("[GAP7] Running command prosesRetryRetur...");
                    List<SpanSp2dStageIn> dataRrs = serviceMariaDb.getDataBifastByStatus(serviceMariaDb.statusForRetryRetur);
                    ProcessBifast processBifast = new ProcessBifast();
                    processBifast.prosesRetryRetur(dataRrs);
                    tulisLog("prosesRetryRetur done..");
                } catch (Throwable e) {
                    tulisLog("Throwable :" + e.getMessage());
                    e.printStackTrace(System.out);
                }
                break;
            case "chkDS":
                //chkDS(propPath + propName + ".properties", args[4] + "");
                break;
            default:
                // code block
        }

        serviceMariaDb.close();
        System.exit(0);
    }

    public static void initLog() {
        try {
            String jarDir = System.getProperty("user.dir");
            System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
            FileHandler fh = new FileHandler(jarDir + "/log/SP2D-CHECK-NEGATIVE-AMOUNT.%u.%g.log", 1024 * 10240, 100, true);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            logger.setUseParentHandlers(false);
        } catch (Throwable ex) {
            MainCHK.logger.log(Level.SEVERE, ex.getMessage(), ex);
        }

        // the following statement is used to log any messages
        logger.log(Level.INFO, "initiate logger..{0}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        System.out.println("initiate logger.." + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }

    public static void tulisLog(String txt) {
//        MainCHK.logger.log(Level.INFO, txt);
        System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "]-[" + pid + "] " + txt);
    }

    public static void tulisLog(Object txt) {
        MainCHK.logger.log(Level.INFO, txt + "");
        System.out.println(txt + "");
    }
}
