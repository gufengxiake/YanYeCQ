package nckd.yanye.occ.plugin.mis.api;

import com.ccb.CCBMisSdk;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * @author: Tan Manguang
 * @date: 2022/3/24
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MisApiFather implements Serializable {

    private Long timeStamp;

    private String data;

    private String sign;

    private String sysEvtTraceId;

    public void encodeData(String data, String key){
        this.timeStamp = System.currentTimeMillis();
        this.data = CCBMisSdk.CCBMisSdk_DataEncrypt(data, key);
        this.sign = CCBMisSdk.CCBMisSdk_DataSign(""+this.timeStamp+""+this.data, key);
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getSysEvtTraceId() {
        return sysEvtTraceId;
    }

    public void setSysEvtTraceId(String sysEvtTraceId) {
        this.sysEvtTraceId = sysEvtTraceId;
    }

}
