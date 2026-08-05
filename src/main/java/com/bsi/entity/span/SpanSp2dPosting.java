package com.bsi.entity.span;

import java.math.BigDecimal;
import java.util.Date;

public class SpanSp2dPosting {
        public Long  id;
        public String  applicationareaSenderIdentifier;
        public String  applicationareaReceiverIdentifier;
        public String  applicationareaDetailSenderIdentifier;
        public String  applicationareaDetailReceiverIdentifier;
        public Date    applicationareaCreationDatetime;
        public Date    datePosting;
        public String  applicationareaMessageIdentifier;
        public String  applicationareaMessageTypeIndicator;
        public String  applicatioanareaMessageVersionText;
        public Date    documentDate;
        public String  documentNumber;
        public String  beneficiaryName;
        public String  beneficiaryBankCode;
        public String  beneficiaryBank;
        public String  beneficiaryAccount;
        public BigDecimal amount;
        public String  currencyTarget;
        public String  description;
        public String  agentBankCode;
        public String  agentBankAccountNumber;
        public String  agentBankAccountName;
        public String  emailAddress;
        public String  swiftCode;
        public String  ibanCode;
        public String  paymentMethod;
        public String  referenceNumber;
        public String  returnCode;
        public String  status;
        public String  rejectStatus;
        public Date     rejectDate;
        public String  sp2dNumber;
        public int     totalBatchCount;
        public BigDecimal totalAmount;
        public int totalCount;
        public int sp2dCount;

}
