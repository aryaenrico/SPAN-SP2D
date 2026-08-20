package com.bsi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.bsi.MainCHK;
import com.bsi.config.T24Config;
import com.bsi.entity.api.ApiClientException;
import com.bsi.entity.t24.FundsTransferSoapRequest;
import com.bsi.entity.t24.FundsTransferSoapResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class T24Client {

    private static final Logger log = LoggerFactory.getLogger(T24Client.class);

    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";

    private final T24Config config;
    private final JAXBContext jaxbContext;

    public T24Client(T24Config config) {
        this.config = config;
        try {
            this.jaxbContext = JAXBContext.newInstance(
                    FundsTransferSoapRequest.class,FundsTransferSoapResponse.class);
        } catch (Exception e) {
            throw new ApiClientException("Gagal inisialisasi JAXBContext: " + e.getMessage(), e);
        }
    }

    public FundsTransferSoapResponse fundsTransfer(FundsTransferSoapRequest request) {
        applyCredentials(request);
        try {
            String soapRequest = buildSoapEnvelope(request);
            log.info("T24 SOAP request: {}", soapRequest);
            // [LOG][2026-08-03] Requirement: Logging payload request integrasi Retur T24 via MainCHK.tulisLog
            MainCHK.tulisLog("Payload Request Retur T24: " + soapRequest);

            String soapResponse = postXml(soapRequest);
            log.info("T24 SOAP response: {}", soapResponse);
            // [LOG][2026-08-03] Requirement: Logging payload response integrasi Retur T24 via MainCHK.tulisLog
            MainCHK.tulisLog("Payload Response Retur T24: " + soapResponse);

            return parseResponse(soapResponse);
        } catch (ApiClientException e) {
            // [LOG][2026-08-03] Requirement: Logging payload error integrasi Retur T24 via MainCHK.tulisLog
            MainCHK.tulisLog("Payload Response Retur T24 ERROR: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            // [LOG][2026-08-03] Requirement: Logging payload error integrasi Retur T24 via MainCHK.tulisLog
            MainCHK.tulisLog("Payload Response Retur T24 ERROR: " + e.getMessage());
            throw new ApiClientException("Gagal memanggil T24 FundsTransfer: " + e.getMessage(), e);
        }
    }

    private void applyCredentials(FundsTransferSoapRequest request) {
        FundsTransferSoapRequest.WebRequestCommon wrc = request.webRequestCommon;
        if (isEmpty(wrc.userName) && config.getUserName() != null) wrc.userName = config.getUserName();
        if (isEmpty(wrc.password) && config.getPassword() != null) wrc.password = config.getPassword();
        if (isEmpty(wrc.company)  && config.getCompany()  != null) wrc.company  = config.getCompany();
    }


    private String buildSoapEnvelope(FundsTransferSoapRequest request) throws Exception {
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);

        StringWriter bodyWriter = new StringWriter();
        marshaller.marshal(request, bodyWriter);

        return "<soapenv:Envelope xmlns:soapenv=\"" + SOAP_NS + "\""
                + " xmlns:t24=\"" + FundsTransferSoapRequest.NS + "\">"
                + "<soapenv:Header/>"
                + "<soapenv:Body>"
                + bodyWriter.toString()
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";
    }

    private String postXml(String xml) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(config.getEndpointUrl()).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(config.getConnectTimeoutMs());
            conn.setReadTimeout(config.getReadTimeoutMs());
            conn.setDoOutput(true);
            // [FIX][2026-08-13] Header Content-Type dinamis sesuai spesifikasi Tim ESB BSI (Default: soap/xml; charset=UTF-8)
            //String contentType = config.getContentType() != null ? config.getContentType() : "soap/xml; charset=UTF-8";
            conn.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            conn.setRequestProperty("x-Gateway-APIKey", config.getApikey() != null ? config.getApikey() : "");
            conn.setRequestProperty("SOAPAction",
            config.getSoapAction() != null ? "\"" + config.getSoapAction() + "\"" : "\"\"");

            byte[] payload = xml.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
                os.flush();
            }

            int status = conn.getResponseCode();
            InputStream is = (status >= 200 && status < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            String body = readAll(is);

            // SOAP Fault biasanya dikirim dengan HTTP 500 -- tetap parse body-nya
            if ((status < 200 || status >= 300) && (body == null || !body.contains("Envelope"))) {
                throw new ApiClientException("HTTP error dari T24: " + status, status, body);
            }
            return body;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Ambil elemen pertama di dalam soap:Body lalu unmarshal */
    private FundsTransferSoapResponse parseResponse(String soapXml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        // Hardening terhadap XXE
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setExpandEntityReferences(false);

        Document doc = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(soapXml.getBytes(StandardCharsets.UTF_8)));

        NodeList bodies = doc.getElementsByTagNameNS(SOAP_NS, "Body");
        if (bodies.getLength() == 0) {
            throw new ApiClientException("Response bukan SOAP Envelope yang valid", -1, soapXml);
        }

        Element bodyContent = firstChildElement(bodies.item(0));
        if (bodyContent == null) {
            throw new ApiClientException("SOAP Body kosong", -1, soapXml);
        }

        // Tangani SOAP Fault
        if ("Fault".equals(bodyContent.getLocalName())) {
            throw new ApiClientException("SOAP Fault dari T24: " + nodeToString(bodyContent), -1, soapXml);
        }

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller
                .unmarshal(bodyContent, FundsTransferSoapResponse.class)
                .getValue();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private static Element firstChildElement(Node parent) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) n;
            }
        }
        return null;
    }

    private static String nodeToString(Node node) {
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter sw = new StringWriter();
            t.transform(new DOMSource(node), new StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            return node.getTextContent();
        }
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}