package com.bsi.entity.t24;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "AccountDetails", namespace = AccountDetailsSoapRequest.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class AccountDetailsSoapRequest {

    public static final String NS = "T24WebServicesImpl";

    @XmlElement(name = "WebRequestCommon")
    public WebRequestCommon webRequestCommon = new WebRequestCommon();

    @XmlElement(name = "IDIACCOUNTCMSType")
    public IdiAccountCmsType idiAccountCmsType = new IdiAccountCmsType();

    // ------------------------------------------------------------------

    @XmlType(name = "IdiAccountCmsTypeReq")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class IdiAccountCmsType {
        @XmlElement(name = "enquiryInputCollection")
        public List<EnquiryInput> enquiryInputCollection = new ArrayList<EnquiryInput>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class EnquiryInput {
        @XmlElement(name = "columnName")    public String columnName;
        @XmlElement(name = "criteriaValue") public String criteriaValue;
        @XmlElement(name = "operand")       public String operand;

        public EnquiryInput() { }

        public EnquiryInput(String columnName, String criteriaValue, String operand) {
            this.columnName = columnName;
            this.criteriaValue = criteriaValue;
            this.operand = operand;
        }
    }
}
