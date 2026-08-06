/*
package com.bsi.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.bsi.MainCHK;
import org.apache.log4j.BasicConfigurator;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.common.CustomHandler;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.handler.RequestData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DigitalSignature {
    String ReceiverProperties;
    String SenderProperties;
    String DigitalSignatureProperties;
    String SetPrefix = "soapenv";
    String UserName = "";
    String Password = "";
    String Alias = "";
    String AcknowledgePut = "";
    String bankCode = "525451000990";
    public static String inputFileName = null;

    public DigitalSignature(String DigitalSignatureProperties) {
        this.DigitalSignatureProperties = DigitalSignatureProperties;
        BasicConfigurator.configure();
        try {
            File file = new File(DigitalSignatureProperties);
            FileInputStream fileInput = new FileInputStream(file);
            Properties properties = new Properties();
            properties.load(fileInput);
            fileInput.close();
            this.SetPrefix = properties.getProperty("ds_set_prefix_soap");
            this.ReceiverProperties = properties.getProperty("ds_receive_properties");
            this.SenderProperties = properties.getProperty("ds_sender_properties");
            this.UserName = properties.getProperty("ds_user_name");
            this.Password = properties.getProperty("ds_password");
            this.Alias = properties.getProperty("ds_alias");
            this.AcknowledgePut = properties.getProperty("ds_acknowledge_put");
            if (properties.getProperty("ds_bank_code") != null && properties.getProperty("ds_bank_code").length() > 0) {
                this.bankCode = properties.getProperty("ds_bank_code");
            }
        } catch (FileNotFoundException var5) {
            var5.printStackTrace();
        } catch (IOException var6) {
        }

    }

    private SOAPMessage loadDocument(String filePath) throws Throwable {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document xmlDoc = docBuilder.parse(new File(filePath));
        SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
        soapMessage.getSOAPBody().addDocument(xmlDoc);
        return soapMessage;
    }

    private Document loadXML(String filePath) throws Throwable {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document xmlDoc = docBuilder.parse(new File(filePath));
        return xmlDoc;
    }

    private Document signSOAPMessage(SOAPMessage soapEnvelope) throws Throwable {
        Source src = soapEnvelope.getSOAPPart().getContent();
        soapEnvelope.getSOAPPart().getEnvelope().setPrefix("soapenv");
        soapEnvelope.getSOAPHeader().setPrefix("soapenv");
        soapEnvelope.getSOAPBody().setPrefix("soapenv");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMResult result = new DOMResult();
        transformer.transform(src, result);
        Document doc = (Document) result.getNode();
        RequestData requestData = new RequestData();
        Map msgContext = new TreeMap();
        msgContext.put("enableSignatureConfirmation", "true");
        msgContext.put("signaturePropFile", this.SenderProperties);
        msgContext.put("signatureKeyIdentifier", "DirectReference");
        msgContext.put("password", this.Password);
        requestData.setMsgContext(msgContext);
        requestData.setUsername(this.Alias);
        List actions = new ArrayList();
        actions.add(new Integer(2));
        CustomHandler handler = new CustomHandler();
        handler.send(2, doc, requestData, actions, true);
        return doc;
    }

    private void persistDocument(Document doc, String file) throws Throwable {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
            DOMSource source = new DOMSource(doc);
            StreamResult rslt = new StreamResult(fos);
            transformer.transform(source, rslt);
        } finally {
            try {
                fos.close();
            } catch (Exception var13) {
                var13.printStackTrace();
            }

        }

    }

    private Boolean checkSignedDoc(Document document) throws Throwable {
        Crypto crypto = null;
        Boolean result = false;

        try {
            crypto = CryptoFactory.getInstance(this.ReceiverProperties);
        } catch (WSSecurityException var11) {
            var11.printStackTrace();
        }

        WSSecurityEngine wsSecurityEngine = new WSSecurityEngine();
        WSSConfig wssConfig = WSSConfig.getNewInstance();
        wssConfig.setWsiBSPCompliant(false);
        wsSecurityEngine.setWssConfig(wssConfig);
        List<WSSecurityEngineResult> wsSecurityEngineResultList = null;

        try {
            wsSecurityEngineResultList = wsSecurityEngine.processSecurityHeader(document, (String) null, (CallbackHandler) null, crypto);
            result = true;
        } catch (WSSecurityException var10) {
            var10.printStackTrace(System.out);
            MainCHK.tulisLog("The signature or decryption was invalid");
            this.createLog(document, this.AcknowledgePut);
        }

        if (wsSecurityEngineResultList != null) {
            Iterator var8 = wsSecurityEngineResultList.iterator();

            while (var8.hasNext()) {
                WSSecurityEngineResult wsSecurityEngineResult = (WSSecurityEngineResult) var8.next();
                if (wsSecurityEngineResult.get("binary-security-token") == null) {
                    result = true;
                    MainCHK.tulisLog(wsSecurityEngineResult.get("binary-security-token"));
                    X509Certificate cert = (X509Certificate) wsSecurityEngineResult.get("x509-certificate");
                    MainCHK.tulisLog(cert.getSubjectDN());
                }
            }

            result = true;
        }

        return result;
    }

    private void createLog(Document docXML, String acknowledgePut) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        LocalDateTime localDateTime = LocalDateTime.now();
        String text = localDateTime.format(formatter);
        NodeList nList1 = docXML.getElementsByTagName("ApplicationAreaCreationDateTime");
        NodeList nList2 = docXML.getElementsByTagName("ApplicationAreaMessageIdentifier");
        String fileName = acknowledgePut + "/" + nList2.item(0).getTextContent().substring(0, 12) + "_SP2D_SA_" + text + ".txt";
        Path path = Paths.get(fileName);
        MainCHK.tulisLog("Writing to file: " + fileName);
        Throwable var10 = null;
        Object var11 = null;

        try {
            BufferedWriter writer = Files.newBufferedWriter(path);

            try {
                writer.write(nList1.item(0).getTextContent().substring(0, 10) + "|SP2D|-|" + nList2.item(0).getTextContent() + "|A005|Digital signature tidak sesuai\n");
                MainCHK.tulisLog(nList1.item(0).getTextContent().substring(0, 10) + "|SP2D|-|" + nList2.item(0).getTextContent() + "|A005|Digital signature tidak sesuai\n");
            } finally {
                if (writer != null) {
                    writer.close();
                }

            }

        } catch (Throwable var18) {
            if (var10 == null) {
                var10 = var18;
            } else if (var10 != var18) {
                var10.addSuppressed(var18);
            }

            var10.printStackTrace(System.out);
//            throw var10;
        }
    }

    public static List<String> getTextValuesByTagName(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        ArrayList<String> list = new ArrayList();

        for (int i = 0; i < nodeList.getLength(); ++i) {
            list.add(getTextValue(nodeList.item(i)));
        }

        return list;
    }

    public static String getTextValue(Node node) {
        StringBuffer textValue = new StringBuffer();
        int length = node.getChildNodes().getLength();

        for (int i = 0; i < length; ++i) {
            Node c = node.getChildNodes().item(i);
            if (c.getNodeType() == 3) {
                textValue.append(c.getNodeValue());
            }
        }

        return textValue.toString().trim();
    }

    public static boolean isBeforeDate(String appDate, String documentDate) {
        boolean result = false;
        int year = Integer.parseInt(appDate.substring(0, 4));
        int month = Integer.parseInt(appDate.substring(5, 7));
        int day = Integer.parseInt(appDate.substring(8, 10));
        LocalDate lcAppDate = LocalDate.of(year, month, day);
        year = Integer.parseInt(documentDate.substring(0, 4));
        month = Integer.parseInt(documentDate.substring(5, 7));
        day = Integer.parseInt(documentDate.substring(8, 10));
        LocalDate lcDocumentDate = LocalDate.of(year, month, day);
        MainCHK.tulisLog(lcAppDate + " " + documentDate);
        if (lcDocumentDate.isBefore(lcAppDate)) {
            result = true;
        }

        return result;
    }

    public boolean checkDocumentBackDate(String filePath, String appDate) {
        Document docXML = null;
        Boolean result = true;

        try {
            docXML = this.loadXML(filePath);
            List<String> stringList = getTextValuesByTagName(docXML.getDocumentElement(), "DocumentDate");
            String docDate = "";
            Iterator var8 = stringList.iterator();

            while (var8.hasNext()) {
                String value = (String) var8.next();
                result = isBeforeDate(appDate, value);
                if (!result) {
                    break;
                }
            }
        } catch (ParserConfigurationException var9) {
            result = false;
            var9.printStackTrace();
        } catch (SAXException var10) {
            result = false;
            var10.printStackTrace();
        } catch (IOException var11) {
            result = false;
            var11.printStackTrace();
        } catch (Throwable var12) {
            result = false;
            var12.printStackTrace();
        }

        return result;
    }

    public boolean createDigitalSignatureFile(String InputFileXML, String OutputFileXML) {
        boolean result = false;
        if (InputFileXML.endsWith("xml")) {
            try {
                SOAPMessage soapMessage = this.loadDocument(InputFileXML);
                Document document = this.signSOAPMessage(soapMessage);
                this.persistDocument(document, OutputFileXML);
                result = true;
            } catch (Throwable var6) {
                var6.printStackTrace(System.out);
            }
        }

        return result;
    }

    public boolean checkDigitalSignatureFile(String InputFileXML) {
        boolean result = false;
        Document docXML = null;

        try {
            docXML = this.loadXML(InputFileXML);
            if (this.checkSignedDoc(docXML)) {
                result = true;
            }
        } catch (ParserConfigurationException var5) {
            var5.printStackTrace(System.out);
            this.createLog(InputFileXML, this.AcknowledgePut);
        } catch (SAXException var6) {
            var6.printStackTrace(System.out);
            this.createLog(InputFileXML, this.AcknowledgePut);
        } catch (IOException var7) {
            var7.printStackTrace(System.out);
            this.createLog(InputFileXML, this.AcknowledgePut);
        } catch (Throwable var8) {
            var8.printStackTrace(System.out);
            this.createLog(InputFileXML, this.AcknowledgePut);
        }

        return result;
    }

    private void createLog(String InputFileXML, String acknowledgePut) {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String text = localDateTime.format(formatter);
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = localDateTime.format(formatter);
        String fileNameXml = InputFileXML;

        try {
            Path path = Paths.get(InputFileXML);
            fileNameXml = path.getFileName().toString();
        } catch (Exception var21) {
            var21.printStackTrace();
        }

        String fileName = acknowledgePut + "/" + this.bankCode + "_SP2D_SA_" + text + ".txt";
        Path path = Paths.get(fileName);
        MainCHK.tulisLog("Writing to file: " + fileName);

        try {
            Throwable var10 = null;
            Object var11 = null;

            try {
                BufferedWriter writer = Files.newBufferedWriter(path);

                try {
                    writer.write(date + "|SP2D|-|" + fileNameXml + "|A005|Digital signature tidak sesuai\n");
                    MainCHK.tulisLog(date + "|SP2D|-|" + fileNameXml + "|A005|Digital signature tidak sesuai\n");
                } finally {
                    if (writer != null) {
                        writer.close();
                    }

                }
            } catch (Throwable var23) {
                if (var10 == null) {
                    var10 = var23;
                } else if (var10 != var23) {
                    var10.addSuppressed(var23);
                }

                var10.printStackTrace(System.out);
//                throw var10;
            }
        } catch (Exception var24) {
            var24.printStackTrace();
        }

    }
}
     */
    