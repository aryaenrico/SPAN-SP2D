package com.bsi.entity.bifast.transactioninquiry;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymenStatusRequest {
    private String requestId;
    private String channelType;
    private String bicSendSys;
    private String bicRecvSys;
    private String originator;
    private String endToEndId;



    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getChannelType() { return channelType; }
    public void setChannelType(String channelType) { this.channelType = channelType; }

    public String getBicSendSys() { return bicSendSys; }
    public void setBicSendSys(String bicSendSys) { this.bicSendSys = bicSendSys; }

    public String getBicRecvSys() { return bicRecvSys; }
    public void setBicRecvSys(String bicRecvSys) { this.bicRecvSys = bicRecvSys; }

    public String getOriginator() { return originator; }
    public void setOriginator(String originator) { this.originator = originator; }

    public String getEndToEndId() { return endToEndId; }
    public void setEndToEndId(String endToEndId) { this.endToEndId = endToEndId; }

}
