package com.bsi.entity.t24;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "AccountDetailsResponse", namespace = AccountDetailsSoapRequest.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class AccountDetailsSoapResponse {

    @XmlElement(name = "Status")
    public Status status = new Status();

    @XmlElement(name = "IDIACCOUNTCMSType")
    public IdiAccountCmsType idiAccountCmsType;

    /** Sukses jika successIndicator == "Success" */
    public boolean isSuccess() {
        return status != null && "Success".equalsIgnoreCase(status.successIndicator);
    }

    /** Ambil detail record pertama (umumnya hanya 1 account yang dikembalikan) */
    public MIdiAccountCmsDetailType getFirstDetail() {
        if (idiAccountCmsType == null
                || idiAccountCmsType.gIdiAccountCmsDetailType == null
                || idiAccountCmsType.gIdiAccountCmsDetailType.mIdiAccountCmsDetailType.isEmpty()) {
            return null;
        }
        return idiAccountCmsType.gIdiAccountCmsDetailType.mIdiAccountCmsDetailType.get(0);
    }

    // ------------------------------------------------------------------

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class IdiAccountCmsType {
        @XmlElement(name = "gIDIACCOUNTCMSDetailType")
        public GIdiAccountCmsDetailType gIdiAccountCmsDetailType;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GIdiAccountCmsDetailType {
        @XmlElement(name = "mIDIACCOUNTCMSDetailType")
        public List<MIdiAccountCmsDetailType> mIdiAccountCmsDetailType = new ArrayList<MIdiAccountCmsDetailType>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MIdiAccountCmsDetailType {
        @XmlElement(name = "ID")              public String id;
        @XmlElement(name = "CUSTOMER")        public String customer;
        @XmlElement(name = "CURRENCY")        public String currency;
        @XmlElement(name = "ACCOUNTTITLE1")   public String accountTitle1;
        @XmlElement(name = "CATEGORY")        public String category;
        @XmlElement(name = "CATEGORYNAME")    public String categoryName;
        @XmlElement(name = "POSTINGRESTRICT") public String postingRestrict;
        @XmlElement(name = "RESTRICTNAME")    public String restrictName;
        @XmlElement(name = "STATUSDORMANT")   public String statusDormant;
        @XmlElement(name = "COCODE")          public String coCode;
        @XmlElement(name = "BRANCHNAME")      public String branchName;
    }

    @Override
    public String toString() {
        MIdiAccountCmsDetailType detail = getFirstDetail();
        return "AccountDetailsSoapResponse{" + status
                + (detail != null
                    ? ", id='" + detail.id
                      + "', customer='" + detail.customer
                      + "', accountTitle1='" + detail.accountTitle1 + "'"
                    : "")
                + "}";
    }
}
