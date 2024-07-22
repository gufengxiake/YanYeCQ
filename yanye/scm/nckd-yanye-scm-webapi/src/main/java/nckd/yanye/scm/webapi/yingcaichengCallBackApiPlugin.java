package nckd.yanye.scm.webapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.openapi.common.custom.annotation.*;
import kd.bos.openapi.common.result.CustomApiResult;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.scm.common.SupplierConst;
import nckd.yanye.scm.common.utils.ZcPlatformApiUtil;
import nckd.yanye.scm.dto.Content;
import nckd.yanye.scm.utils.ZcEncryptUtil;

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


//        // 判断节点编号-非成交业务不做处理
//        if (!"win-release".equals(businessNode)) {
//            return CustomApiResult.success("success");
//        }
//
//        // 签名校验
//        if (ZcEncryptUtil.checkSignature(signature, timestamp, nonce)) {
//            return CustomApiResult.success("success");
//        }
//
//        // 解密消息体
//        String decryptMsg = ZcEncryptUtil.decryptBody(encryptBody);
//        JSONObject msgObj = JSON.parseObject(decryptMsg);
//
//        // 采购单id
//        String orderId = msgObj.getString("orderId");
//        // 业主公司 id
//        String supplierId = msgObj.getJSONObject("tenderInfo").getString("ownerCompanyId");
//
//        // 查询成交公司数据
//        JSONObject companyData = ZcPlatformApiUtil.getCompanyIdById(supplierId);
//        // 成交公司统一社会信用代码
//        String uscc = companyData.getString("socialCreditCode");
//
//        //根据招采平台供应商id查询供应商信息
//        DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load(
//                SupplierConst.FORMBILLID,
//                SupplierConst.ALLPROPERTY,
//                new QFilter[]{new QFilter(SupplierConst.NCKD_PLATFORMSUPID, QCP.equals, supplierId)}
//        );
//
//        // 供应商不存在，则新增
//        if (dynamicObjects.length == 0) {
//            //根据社会统一信用代码再次查询，如果存在则更新已有供应商信息，不存在则新建
//            DynamicObject[] load = BusinessDataServiceHelper.load(
//                    SupplierConst.FORMBILLID,
//                    SupplierConst.ALLPROPERTY,
//                    new QFilter[]{new QFilter(SupplierConst.SOCIETYCREDITCODE, QCP.equals, uscc)}
//            );
//            if (load.length != 0) {
//                DynamicObject supplier = load[0];
//                supplier.set(SupplierConst.NCKD_PLATFORMSUPID, supplierId);
//                supplier.set(SupplierConst.SOCIETYCREDITCODE, uscc);
//                SaveServiceHelper.saveOperate(SupplierConst.FORMBILLID, new DynamicObject[]{supplier});
//            }
//
//            //不存在，则新增保存至金蝶供应商
//            DynamicObject supplier = BusinessDataServiceHelper.newDynamicObject(SupplierConst.FORMBILLID);
//            DynamicObject org = BusinessDataServiceHelper.loadSingle(
//                    "100000",
//                    "bos_org"
//            );
//
//            //名称
//            supplier.set(SupplierConst.NAME, companyData.getString("companyName"));
//            //创建组织
//            supplier.set(SupplierConst.CREATEORG, org);
//            //业务组织
//            supplier.set(SupplierConst.USEORG, org);
//            //统一社会信用代码
//            supplier.set(SupplierConst.SOCIETYCREDITCODE, uscc);
//            //招采平台id
//            supplier.set(SupplierConst.NCKD_PLATFORMSUPID, supplierId);
//            //控制策略：自由分配
//            supplier.set(SupplierConst.CTRLSTRATEGY, "5");
//            //数据状态
//            supplier.set(SupplierConst.STATUS, "C");
//            //使用状态
//            supplier.set(SupplierConst.ENABLE, "1");
//
//            SaveServiceHelper.saveOperate(SupplierConst.FORMBILLID, new DynamicObject[]{supplier});
//        }


        CustomApiResult<String> result = CustomApiResult.success("success");
        return result;
    }
}


























