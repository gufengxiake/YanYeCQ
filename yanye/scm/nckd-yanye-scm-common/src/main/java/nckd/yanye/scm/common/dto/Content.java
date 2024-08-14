package nckd.yanye.scm.common.dto;


import kd.bos.openapi.common.custom.annotation.ApiModel;
import kd.bos.openapi.common.custom.annotation.ApiParam;

import java.io.Serializable;


/**
 * 招采平台回调统一消息体-内容
 *
 * @author liuxiao
 */
@ApiModel
public class Content implements Serializable {
    @ApiParam(value = "随机数", required = true)
    private String nonce;

    @ApiParam(value = "时间戳", required = true)
    private String timestamp;

    @ApiParam(value = "签名", required = true)
    private String signature;

    @ApiParam(value = "加密消息体", required = true)
    private String encryptBody;

    public Content() {
    }

    public Content(String nonce, String timestamp, String signature, String encryptBody) {
        this.nonce = nonce;
        this.timestamp = timestamp;
        this.signature = signature;
        this.encryptBody = encryptBody;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getEncryptBody() {
        return encryptBody;
    }

    public void setEncryptBody(String encryptBody) {
        this.encryptBody = encryptBody;
    }
}
