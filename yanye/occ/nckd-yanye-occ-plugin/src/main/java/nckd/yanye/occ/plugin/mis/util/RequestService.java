package nckd.yanye.occ.plugin.mis.util;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

public class RequestService {

    private static final String TRUE = "true";
    private static final String FALSE = "false";

    private ProxyConfig proxyConfig;

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public RequestService(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public RequestService() {
    }

    /**
     * http请求连接超时(毫秒)
     */
    private static final int HTTP_CONNTION_TIMEOUT = 15000;
    /**
     * http读数据超时(毫秒)
     */
    private static final int HTTP_READ_TIMEOUT = 180000;

    /**
     * 向指定 URL 发送Json串的POST方法请求；
     * 如果返回4xx~5xx，抛出HttpStatusCodeException（其中statusCode为具体的http status
     *
     * @param url         发送请求的 URL
     * @param ContentType 内容格式，如果为空，默认为“application/json; charset=UTF-8”
     * @param param       请求内容，json串。
     * @return 所代表远程资源的响应结果
     * @throws Exception
     */
    public String sendJsonPost(String url, String ContentType, String param) {
        System.out.println(url);
        if (StringUtils.isEmpty(ContentType)) {
            ContentType = "application/json; charset=UTF-8";
        }
        return sendPost(url, ContentType, null, param);
    }

    /**
     * 向指定 URL 发送POST方法的请求（以x-www-form-urlencoded格式发送，内容为utf-8编码）；
     * 如果返回4xx~5xx，抛出HttpStatusCodeException（其中statusCode为具体的http status
     *
     * @param url         发送请求的 URL
     * @param ContentType 内容格式，需要与param值的格式匹配。如果为空，默认为“text/plain; charset=UTF-8”
     * @param header      请求头部信息(Key为请求头名，Value为值）
     * @param param       请求参数，可以是json串，或以&分隔的名值对，需要与ContentType格式匹配。
     * @return 所代表远程资源的响应结果
     * @throws Exception
     */
    public String sendPost(String url, String ContentType, Map<String, String> header, String param) {
        String result = "";
        try {
            if (StringUtils.isEmpty(ContentType)) {
                ContentType = "text/plain; charset=UTF-8";
            }
            HttpURLConnection conn = getHttpURLConnection(url, ContentType, header);
            if (param != null) {
                byte[] byData = param.getBytes("UTF-8");
                conn.getOutputStream().write(byData);
            }
            conn.connect();

            result = readResponsContent(conn);

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }

    private HttpURLConnection getHttpURLConnection(String url, String ContentType, Map<String, String> header) throws Exception {
        URL realUrl = new URL(url);
        // 打开和URL之间的连接
        HttpURLConnection conn;
        if (this.proxyConfig != null) {
            Proxy proxy = new Proxy(Proxy.Type.DIRECT.HTTP, new InetSocketAddress(this.proxyConfig.getAddress(), this.proxyConfig.getPort()));
            conn = (HttpURLConnection) realUrl.openConnection(proxy);
        } else {
            conn = (HttpURLConnection) realUrl.openConnection();
        }
        // 设置通用的请求属性
        conn.setRequestProperty("accept", "*/*");
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) Chrome/75.0");
        conn.setRequestProperty("Accept-Charset", "utf-8");
        conn.setRequestProperty("Content-Type", ContentType);
        if (header != null) {
            for (Map.Entry<String, String> entry : header.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        // setCookie(conn);
        conn.setReadTimeout(HTTP_READ_TIMEOUT);
        conn.setConnectTimeout(HTTP_CONNTION_TIMEOUT);
        // 发送POST请求必须设置如下两行
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        return conn;
    }
    /**
     * 获取返回内容的编码方式（默认为utf-8）
     *
     * @param conn
     * @return
     */
    private static String getResponseContentType(HttpURLConnection conn) {
        String charset = "UTF-8";
        //类似 text/html; charset=utf-8
        String sTemp = conn.getContentType()==null?"":conn.getContentType().toLowerCase();
        if (sTemp.indexOf("=gbk") > 0 || sTemp.indexOf("=gb2312") > 0) {
            charset = "GBK";
        }
        return charset;
    }

    private static String readResponsContent(HttpURLConnection conn)
            throws Exception {
        String result = "";

        String charset = getResponseContentType(conn);
        int statusCode = conn.getResponseCode();
        // 定义BufferedReader输入流来读取URL的响应
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset));
        char[] cBuf = new char[1024];
        int iLen;
        while ((iLen = in.read(cBuf)) >= 0) {
            sb.append(cBuf, 0, iLen);
        }
        in.close();
        result = sb.toString();
        if (statusCode >= 300) {
            throw new Exception(result);
        }
        return result;
    }

}
