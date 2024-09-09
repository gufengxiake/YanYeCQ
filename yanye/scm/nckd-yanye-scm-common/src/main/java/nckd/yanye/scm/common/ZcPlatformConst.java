package nckd.yanye.scm.common;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import nckd.base.common.utils.capp.CappConfig;

public class ZcPlatformConst {
    private String clientId;
    private String clientSecret;
    private String msgSecret;
    private boolean isExist;


    public ZcPlatformConst() {
    }

    public ZcPlatformConst(Long orgId) {
        // 查询对应招采平台参数
        DynamicObject zcConfig = BusinessDataServiceHelper.loadSingle(
                "nckd_zcconfig",
                new QFilter[]{new QFilter("org.id", QCP.equals, orgId)}
        );

        if (zcConfig == null) {
            this.clientId = null;
            this.clientSecret = null;
            this.msgSecret = null;
            this.isExist = false;
        } else {
            this.clientId = zcConfig.getString("client_id");
            this.clientSecret = zcConfig.getString("client_secret");
            this.msgSecret = zcConfig.getString("msg_secret");
            this.isExist = true;
        }
    }

    public ZcPlatformConst(String clientId) {
        // 查询对应招采平台参数
        DynamicObject zcConfig = BusinessDataServiceHelper.loadSingle(
                "nckd_zcconfig",
                new QFilter[]{new QFilter("client_id", QCP.equals, clientId)}
        );

        this.clientId = clientId;
        this.clientSecret = zcConfig.getString("client_secret");
        this.msgSecret = zcConfig.getString("msg_secret");
        this.isExist = true;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getMsgSecret() {
        return msgSecret;
    }


    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setMsgSecret(String msgSecret) {
        this.msgSecret = msgSecret;
    }

    public boolean isExist() {
        return isExist;
    }

    /**
     * 招采平台接口授权类型，固定值: client_credentials
     */
    public static final String GRANT_TYPE = "client_credentials";

    /**
     * 招采平台-测试环境url
     */
    public static final String ZC_URL = CappConfig.getConfigValue("zc_url");

    /**
     * 招采平台-测试环境访问指定页面url
     */
    public static final String ZC_PASSPORTURL = CappConfig.getConfigValue("zc_passporturl");
}
