package nckd.yanye.scm.plugin.operate;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 主数据-客户信息维护单提交操作插件
 * 表单标识：nckd_bd_customer_change
 * author：xiaoxiaopeng
 * date：2024-08-30
 */
public class BdCustomerChangeSubmitOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("nckd_maintenancetype");
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();
        StringBuilder stringBuilder = new StringBuilder();
        //单据提交进行将“社会统一信用代码”字段与供应商及客户档案中该字段进行查找，如有重复，则进行报错，提示与XX供应商或XX客户社会统一信用代码重复；
        for (int i = 0; i < dataEntities.length; i++) {
            DynamicObject date = dataEntities[i];
            date = BusinessDataServiceHelper.loadSingle(date.getPkValue(), "nckd_bd_customer_change");
            DynamicObjectCollection entry = date.getDynamicObjectCollection("nckd_entry");
            String maintenanceType = date.getString("nckd_maintenancetype");
            switch (maintenanceType) {
                case "save":
                    for (int t = 0; t < entry.size(); t++) {
                        DynamicObject entity = entry.get(t);
                        //社会统一信用代码
                        String societyCreditCode = entity.getString("nckd_societycreditcode");
                        String addCustomer = entity.getString("nckd_addcustomer");
                        if (StringUtils.isBlank(societyCreditCode)) {
                            stringBuilder.append("提交单据第" + (i + 1) + "中维护客商信息表体第" + (t + 1) + "条数据中社会统一信用代码无效");
                            continue;
                        }
                        //先查客户，供应商无重复再查供应商，都无重复则放行，否则中断操作
                        DynamicObject bdCustomer = BusinessDataServiceHelper.loadSingle("bd_customer", "id,societycreditcode",
                                new QFilter[]{new QFilter("societycreditcode", QCP.equals, societyCreditCode)});
                        if (bdCustomer != null){
                            stringBuilder.append("提交单据第" + (i+1) + "条中维护客户信息表体第" + (t+1) + "条数据中名字为" + addCustomer + "的客户社会统一信用代码重复");
                            continue;
                        }
                        DynamicObject bdSupplier = BusinessDataServiceHelper.loadSingle("bd_supplier", "id,societycreditcode",
                                new QFilter[]{new QFilter("societycreditcode", QCP.equals, societyCreditCode)});
                        if (bdSupplier != null) {
                            if (!addCustomer.equals(bdSupplier.getString("name"))){
                                stringBuilder.append("提交单据第" + (i + 1) + "条中维护客户信息表体第" + (t + 1) + "条数据中名字为" + addCustomer + "的客户与名字为" + bdSupplier.getString("name") +  "的供应商名字不一致");
                            }
                        }
                    }
                    break;
                case "update":
                    List<Object> pkList = new ArrayList<>();
                    for (int t = 0; t < entry.size(); t++) {
                        DynamicObject entity = entry.get(t);
                        String changeAfter = entity.getString("nckd_changeafter");
                        if ("1".equals(changeAfter)) {
                            DynamicObject customerModify = entity.getDynamicObject("nckd_customermodify");
                            pkList.add(customerModify.getPkValue());
                        }else {
                            //社会统一信用代码
                            String societyCreditCode = entity.getString("nckd_societycreditcode");
                            DynamicObject bdCustomer = BusinessDataServiceHelper.loadSingle("bd_customer", "id,societycreditcode",
                                    new QFilter[]{new QFilter("societycreditcode", QCP.equals, societyCreditCode)});
                            if (bdCustomer != null && !pkList.contains(bdCustomer .getPkValue())) {
                                stringBuilder.append("提交单据第" + (i + 1) + "中维护客商信息表体第" + (t + 1) + "条数据中" + bdCustomer.getString("name") + "供应商社会统一信用代码重复");
                            }
                        }
                    }
                    break;
            }

        }
        if (stringBuilder.length() > 0) {
            e.setCancel(true);
            e.setCancelMessage(stringBuilder.toString());
        }
    }
}
