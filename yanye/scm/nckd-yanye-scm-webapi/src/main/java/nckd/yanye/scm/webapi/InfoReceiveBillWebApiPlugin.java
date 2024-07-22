package nckd.yanye.scm.webapi;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.openapi.api.plugin.ApiSavePlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.scm.common.SupplierConst;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 信息接收单保存api插件
 * @author liuxiao
 */
public class InfoReceiveBillWebApiPlugin implements ApiSavePlugin {
    @Override
    public List<Map<String, Object>> preHandleRequestData(List<Map<String, Object>> reqData) {
        for (Map<String, Object> data : reqData) {
            String supplierId = (String) data.get("nckd_supplierid");
            String uscc = (String) data.get("nckd_uscc");
            OperationResult result = addSupplier(supplierId, uscc);
            data.put("nckd_failinfo", result.getMessage());
            HashMap<String, String> map = new HashMap<>();
            map.put("number", "CNY");
            data.put("nckd_currency", map);
        }
        return reqData;
    }

    public static OperationResult addSupplier(String supplierId, String uscc) {
        //根据招采平台供应商id查询供应商信息
        DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load(
                SupplierConst.FORMBILLID,
                SupplierConst.ALLPROPERTY,
                new QFilter[]{new QFilter(SupplierConst.NCKD_PLATFORMSUPID, QCP.equals, supplierId)}
        );

        // 供应商不存在，则新增
        if (dynamicObjects.length == 0) {
            //根据社会统一信用代码再次查询，如果存在则更新已有供应商信息，不存在则新建
            DynamicObject[] load = BusinessDataServiceHelper.load(
                    SupplierConst.FORMBILLID,
                    SupplierConst.ALLPROPERTY,
                    new QFilter[]{new QFilter(SupplierConst.SOCIETYCREDITCODE, QCP.equals, uscc)}
            );
            if (load.length != 0) {
                DynamicObject supplier = load[0];
                supplier.set(SupplierConst.NCKD_PLATFORMSUPID, supplierId);
                supplier.set(SupplierConst.SOCIETYCREDITCODE, uscc);
                //todo 保存成功校验
                return SaveServiceHelper.saveOperate(SupplierConst.FORMBILLID, new DynamicObject[]{supplier});
            }


            //不存在，则新增保存至金蝶供应商
            DynamicObject supplier = BusinessDataServiceHelper.newDynamicObject(SupplierConst.FORMBILLID);
            DynamicObject org = BusinessDataServiceHelper.loadSingle(
                    "100000",
                    "bos_org"
            );
            //todo 查询招采平台供应商信息

            //编码
//            supplier.set(SupplierConst.NUMBER, "123");
            //名称
            supplier.set(SupplierConst.NAME, "测试11");
            //创建组织
            supplier.set(SupplierConst.CREATEORG, org);
            //业务组织
            supplier.set(SupplierConst.USEORG, org);
            //统一社会信用代码
            supplier.set(SupplierConst.SOCIETYCREDITCODE, "123123");
            //招采平台id
            supplier.set(SupplierConst.NCKD_PLATFORMSUPID, supplierId);
            //控制策略：自由分配
            supplier.set(SupplierConst.CTRLSTRATEGY, "5");
            //数据状态
            supplier.set(SupplierConst.STATUS, "C");
            //使用状态
            supplier.set(SupplierConst.ENABLE, "1");

            return SaveServiceHelper.saveOperate(SupplierConst.FORMBILLID, new DynamicObject[]{supplier});
        }

        OperationResult result = new OperationResult();
        result.setSuccess(false);
        result.setMessage("系统已存在对应的供应商");
        return result;
    }


}