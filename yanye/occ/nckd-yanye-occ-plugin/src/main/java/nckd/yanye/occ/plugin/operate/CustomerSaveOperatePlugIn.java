package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.entity.property.AssistantProp;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;

/*
 * 客户基础资料保存获取分组
 * 表单标识：nckd_bd_customer_ext
 * author:吴国强 2024-07-22
 */

public class CustomerSaveOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("number");
        e.getFieldKeys().add("groupid");
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);
        DynamicObject[] deliverRecords = e.getDataEntities();
        if (deliverRecords != null) {
            for (DynamicObject dataObject : deliverRecords) {
                String number = dataObject.getString("number");
                // 构造QFilter
                QFilter qFilter = new QFilter("number", QCP.equals, number);
                //查找
                DynamicObjectCollection collections = QueryServiceHelper.query("ocdbd_channelreq",
                        "id,group.number number", qFilter.toArray(), "");
                if (!collections.isEmpty()) {
                    DynamicObject operatorItem = collections.get(0);
                    String groupnumber = operatorItem.getString("number");
                    // 构造QFilter
                    QFilter Filter = new QFilter("number", QCP.equals, groupnumber);
                    DynamicObjectCollection groupCol = QueryServiceHelper.query("bd_customergroup",
                            "id", Filter.toArray(), "");
                    if (!groupCol.isEmpty()) {
                        Object id = groupCol.get(0).get("id");
                        DynamicObject customergroup = BusinessDataServiceHelper.loadSingle(id, "bd_customergroup");
                        DynamicObjectCollection entry_groupstandard = dataObject.getDynamicObjectCollection("entry_groupstandard");
                        if (entry_groupstandard.isEmpty()) {
                            DynamicObject entrygroupRow = entry_groupstandard.addNew();
                            entrygroupRow.set("groupid", customergroup);
                        } else {
                            DynamicObject entrygroupRow = entry_groupstandard.get(0);
                            entrygroupRow.set("groupid", customergroup);
                        }

                    }

                }
            }
        }
    }
}
