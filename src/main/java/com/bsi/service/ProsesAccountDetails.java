package com.bsi.service;

import com.bsi.config.T24Config;
import com.bsi.entity.t24.AccountDetailsSoapRequest;
import com.bsi.entity.t24.AccountDetailsSoapResponse;

public class ProsesAccountDetails {

        T24Config configT24;
        T24Client client;

        public ProsesAccountDetails(T24Config t24config){
            this.configT24 = t24config;
            this.client = new T24Client(this.configT24);
        }

        public AccountDetailsSoapResponse getCocode(String debitAcct){
            AccountDetailsSoapRequest req = new AccountDetailsSoapRequest();
            req.idiAccountCmsType.enquiryInputCollection.add(
                    new AccountDetailsSoapRequest.EnquiryInput("ID", debitAcct, "EQ"));
            return client.accountDetails(req);
        }







}
