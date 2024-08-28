package nckd.yanye.occ.plugin.mis.api;

import com.ccb.CCBMisSdk;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import nckd.yanye.occ.plugin.mis.enums.TransRequestEnum;

/**
 * @author: Tan Manguang
 * @date: 2022/3/24
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MisApiHttpVo extends MisApiFather {

    private TransRequestEnum txnCode;

    private String mchtNo;

    private String termNo;

    private String authCode;

    private String publicKey;

    private String key;

    private String termSN;

    private String apiVer;

    public String getApiVer() {
        return apiVer;
    }

    public void setApiVer(String apiVer) {
        this.apiVer = apiVer;
    }

    public String getTermSN() {
        return termSN;
    }

    public void setTermSN(String termSN) {
        this.termSN = termSN;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }


    public MisApiHttpVo() {

    }


    public MisApiHttpVo(TransRequestEnum txnCode, String mchtNo, String termNo, String termSN, String apiVer) {
        this.txnCode = txnCode;
        this.mchtNo = mchtNo;
        this.termNo = termNo;
        this.termSN = termSN;
        this.apiVer = apiVer;
    }

    public String getMchtNo() {
        return mchtNo;
    }

    public void setMchtNo(String mchtNo) {
        this.mchtNo = mchtNo;
    }

    public String getTermNo() {
        return termNo;
    }

    public void setTermNo(String termNo) {
        this.termNo = termNo;
    }

    public void sign(String privateKey, String key) {
        this.setTimeStamp(System.currentTimeMillis());
        this.setAuthCode(CCBMisSdk.CCBMisSdk_KeySign(key + "" + this.getTimeStamp(), privateKey));
    }

    public TransRequestEnum getTxnCode() {
        return txnCode;
    }

    public void setTxnCode(TransRequestEnum txnCode) {
        this.txnCode = txnCode;
    }
}
