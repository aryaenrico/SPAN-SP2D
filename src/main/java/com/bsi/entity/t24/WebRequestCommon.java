package com.bsi.entity.t24;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WebRequestCommon")
public class WebRequestCommon {
    @XmlElement(name = "company") private String company;
    @XmlElement(name = "userName") private String userName;
    @XmlElement(name = "password") private String password;

    public String getCompany() {
        return company;
    }

    public void setCompany(String companyId) {
        this.company = companyId;
    }

    // --- Getter & Setter untuk userName ---
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    // --- Getter & Setter untuk password ---
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
