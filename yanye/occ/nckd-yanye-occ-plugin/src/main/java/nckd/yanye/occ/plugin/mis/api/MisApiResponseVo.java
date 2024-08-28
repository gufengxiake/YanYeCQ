package nckd.yanye.occ.plugin.mis.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author: Tan Manguang
 * @date: 2022/3/25
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MisApiResponseVo extends MisApiHttpVo {
    private String retCode;
    private String retErrMsg;
    private String traceNo;
    private String retErrCode;
    private String txnTraceNo;


    public MisApiResponseVo(){

    }

    public String getRetCode() {
        return retCode;
    }

    public void setRetCode(String retCode) {
        this.retCode = retCode;
    }

    public String getRetErrMsg() {
        return retErrMsg;
    }

    public void setRetErrMsg(String retErrMsg) {
        this.retErrMsg = retErrMsg;
    }

    public String getTraceNo() {
        return traceNo;
    }

    public void setTraceNo(String traceNo) {
        this.traceNo = traceNo;
    }

    public String getRetErrCode() {
        return retErrCode;
    }

    public void setRetErrCode(String retErrCode) {
        this.retErrCode = retErrCode;
    }

    public String getTxnTraceNo() {
        return txnTraceNo;
    }

    public void setTxnTraceNo(String txnTraceNo) {
        this.txnTraceNo = txnTraceNo;
    }
}
