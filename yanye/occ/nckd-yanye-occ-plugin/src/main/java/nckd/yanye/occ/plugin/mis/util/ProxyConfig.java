package nckd.yanye.occ.plugin.mis.util;


/**
 * @author: Tan Manguang
 * @date: 2021/9/27
 */
public class ProxyConfig {

    private String address;

    private int port;

    public ProxyConfig() {
    }

    public ProxyConfig(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
