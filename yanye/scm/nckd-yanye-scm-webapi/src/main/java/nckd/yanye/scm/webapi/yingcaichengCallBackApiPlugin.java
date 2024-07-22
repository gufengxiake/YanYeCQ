package nckd.yanye.scm.webapi;

import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.openapi.common.custom.annotation.*;
import kd.bos.openapi.common.result.CustomApiResult;
import nckd.yanye.scm.dto.Content;

import java.io.Serializable;

/**
 * 招采平台回调统一消息体
 *
 * @author liuxiao
 */
@ApiController(value = "yingcaicheng", desc = "招采平台回调")
@ApiMapping("/back")
public class yingcaichengCallBackApiPlugin implements Serializable {
    private static final Log log = LogFactory.getLog(yingcaichengCallBackApiPlugin.class);

    @ApiPostMapping(value = "/post", desc = "招采平台回调")
    public CustomApiResult<@ApiResponseBody("返回参数") String> getCallBack(
            @ApiParam(value = "回调编号", required = true) String callbackCode,
            @ApiParam(value = "应用id", required = true) String openAppKey,
            @ApiParam(value = "业务类型编号", required = true) String businessType,
            @ApiParam(value = "业务节点编号", required = true) String businessNode,
            @ApiParam(value = "内容", required = true) Content content
    ) {
        // 随机数
        String nonce = content.getNonce();
        // 时间戳
        String timestamp = content.getTimestamp();
        // 签名
        String signature = content.getSignature();
        // 加密消息体
        String encryptBody = content.getEncryptBody();


        log.debug("招采平台回调开始!回调编号:{}", callbackCode);
        log.debug("应用id:{}", openAppKey);
        log.debug("业务类型编号:{}", businessType);
        log.debug("业务节点编号:{}", businessNode);
        log.debug("随机数:{}", nonce);
        log.debug("时间戳:{}", timestamp);
        log.debug("签名:{}", signature);
        log.debug("加密消息体:{}", encryptBody);


        // 判断节点编号-非成交业务不做处理
//        if (!"win-release".equals(businessNode)) {
//            return CustomApiResult.success(new ZcApiResult(true, "200", "调用成功", ""));
//        }
//
//        // 签名校验
//        if (ZcEncryptUtil.checkSignature(signature, timestamp, nonce)) {
//            CustomApiResult<ZcApiResult> fail = CustomApiResult.fail("400", "签名校验失败");
//            fail.setData(new ZcApiResult(false, "400", "签名校验失败", ""));
//            return fail;
//        }
//
//        // 解密消息体
//        String decryptMsg = ZcEncryptUtil.decryptBody(encryptBody);
//        JSONObject msgObj = JSON.parseObject(decryptMsg);
//
//        // 采购单id
//        String orderId = msgObj.getString("orderId");
//        // 业主公司 id
//        String ownerCompanyId = msgObj.getJSONObject("tenderInfo").getString("ownerCompanyId");
        // todo 新增公司


        CustomApiResult<String> result = CustomApiResult.success("success");
        return result;
    }
}


























