package com.bsi;

import com.bsi.DigitalSignature;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MainCHK {
    public static final Logger logger = Logger.getLogger("SP2D-CHECK-NEGATIVE-AMOUNT");
    private static final boolean isDebug = true;

    public static void main(String[] args) {
        initLog();
        String propPath = "", propName = "";
        MariaDb mariaDb = null;
        boolean chkDS = Arrays.asList(args).contains("-chkDS");
        if (isDebug) {
            //debug only
            tulisLog("Debugging...");
//            mariaDb = new MariaDb("/Users/choirulrahmadan/BSI/SpanPlay/conf/", "bo2span");
            chkDS("/Users/choirulrahmadan/BSI/SP2D_CHECK_NEGATIVE_AMOUNT/tesDs/bo2span.properties",
                    "/Users/choirulrahmadan/BSI/SP2D_CHECK_NEGATIVE_AMOUNT/tesDs/525451000990_SP2D_O_20220921_130509_073.xml");
            System.exit(0);
        } else if (args.length > 0) {
            tulisLog("param1: " + args[0]);
            propPath = args[0];
            if (args.length > 1) {
                tulisLog("param2: " + args[1]);
                propName = args[1];
            } else {
                tulisLog("Tidak ada param untuk nama file properties!");
                System.exit(0);
            }
            mariaDb = new MariaDb(propPath, propName);
        } else {
            tulisLog("Tidak ada param untuk path file properties!");
            System.exit(0);
        }
        if (chkDS) {
            if (args.length > 3) {
                chkDS(propPath + propName + ".properties", args[4] + "");
            }
        } else {
            try {
                tulisLog("checkVoidList");
                mariaDb.checkVoidList();
                tulisLog("updStageIn");
                mariaDb.updStageIn();
            } catch (Throwable e) {
                tulisLog("Throwable :" + e.getMessage());
                e.printStackTrace(System.out);
            }
            mariaDb.close();
        }
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
        MainCHK.logger.log(Level.INFO, txt);
        System.out.println(txt);
    }

    public static void tulisLog(Object txt) {
        MainCHK.logger.log(Level.INFO, txt + "");
        System.out.println(txt + "");
    }

    public static void chkDS(String DSprop, String fileXml) {
        DigitalSignature digitalSignature = new DigitalSignature(
                DSprop
//                "/Users/choirulrahmadan/BSI/SP2D_CHECK_NEGATIVE_AMOUNT/tesDs/bo2span.properties"
        );
        tulisLog("************************************************ CHECK DS ****************************************************");
        boolean result = digitalSignature.checkDigitalSignatureFile(fileXml);
        tulisLog("result chkDS " + fileXml + " :" + result);
        tulisLog("************************************************ END CHECK DS ***************************************************");
    }

}