package com.bsi.entity.t24;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Status")
public class Status {
    @XmlElement(name = "transactionId")    public String transactionId;
    @XmlElement(name = "messageId")        public String messageId;
    @XmlElement(name = "successIndicator") public String successIndicator;
    @XmlElement(name = "application")      public String application;

    /** Diisi server bila gagal (umumnya berisi pesan error OFS) */
    @XmlElement(name = "messages")
    public List<String> messages = new ArrayList<String>();

    @Override
    public String toString() {
        return "Status{transactionId='" + transactionId + "', messageId='" + messageId
                + "', successIndicator='" + successIndicator + "', application='" + application
                + (messages.isEmpty() ? "" : "', messages=" + messages) + "'}";
    }
}
