package nckd.yanye.occ.plugin.mis.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author: Tan Manguang
 * @date: 2022/3/24
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransRequest {

    private String address;

    private String gps;

    private TransData transData;

    public TransRequest() {
    }

    public TransRequest(String address, String gps) {
        this.address = address;
        this.gps = gps;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGps() {
        return gps;
    }

    public void setGps(String gps) {
        this.gps = gps;
    }

    public TransData getTransData() {
        return transData;
    }

    public void setTransData(TransData transData) {
        this.transData = transData;
    }
}
