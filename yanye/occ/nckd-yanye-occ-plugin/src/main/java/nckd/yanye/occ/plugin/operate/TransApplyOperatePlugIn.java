package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;

/*
调拨单申请单保存服务  设置仓库
 */
public class TransApplyOperatePlugIn extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("org");//组织
        e.getFieldKeys().add("billtype");//单据类型
        e.getFieldKeys().add("applydept");//申请部门
        e.getFieldKeys().add("inwarehouse");//调入仓库
        e.getFieldKeys().add("warehouse");//调出仓库
    }

    @Override
    public void beforeExecuteOperationTransaction(BeforeOperationArgs e) {
        super.beforeExecuteOperationTransaction(e);

        DynamicObject[] entities = e.getDataEntities();
        // 逐单处理
        for (DynamicObject dataEntity : entities) {
            DynamicObject org = dataEntity.getDynamicObject("org");
            Object orgId = org.getPkValue();
            DynamicObject billtype = dataEntity.getDynamicObject("billtype");
            String nameq = billtype.getString("name");
            Object id = billtype.getPkValue();
            // 构造QFilter
            QFilter qFilter = new QFilter("nckd_isjh", QCP.equals, "1").and("createorg.id", QCP.equals, orgId);
            // 将选中的id对应的数据从数据库加载出来
            DynamicObjectCollection collections = QueryServiceHelper.query("bd_warehouse",
                    "id", qFilter.toArray(), "");
            DynamicObject stockDyObj = null;
            if (!collections.isEmpty()) {
                DynamicObject stock = collections.get(0);
                String stockId = stock.getString(("id"));
                stockDyObj = BusinessDataServiceHelper.loadSingle(stockId, "bd_warehouse");
            }

            //部门对应仓库
            DynamicObject depStock = null;
            //申请部门
            DynamicObject dept = dataEntity.getDynamicObject("applydept");
            if (dept != null) {
                Object deptId = dept.getPkValue();
                //从部门 仓库设置基础资料中获取对应仓库
                // 构造QFilter
                QFilter depqFilter = new QFilter("createorg", QCP.equals, orgId)
                        .and("status", QCP.equals, "C")
                        .and("nckd_bm", QCP.equals, deptId);

                //查找部门对应仓库
                DynamicObjectCollection depcollections = QueryServiceHelper.query("nckd_bmcksz",
                        "id,nckd_ck.id stockId", depqFilter.toArray(), "modifytime");
                if (!depcollections.isEmpty()) {
                    DynamicObject stockItem = depcollections.get(0);
                    String stockId = stockItem.getString("stockId");
                    depStock = BusinessDataServiceHelper.loadSingle(stockId, "bd_warehouse");
                }
            }
            DynamicObjectCollection entryentity = dataEntity.getDynamicObjectCollection("billentry");
            if (!entryentity.isEmpty()) {
                for (DynamicObject entryRow : entryentity) {
                    if (id.equals("1994937462568258560") || nameq.equalsIgnoreCase("借货归还申请")) {
                        DynamicObject warehouse =entryRow.getDynamicObject("warehouse");
                        if(warehouse==null){
                            //调出仓库
                            entryRow.set("warehouse", stockDyObj);
                        }

                    } else if (id.equals("1994937113375673344") || nameq.equalsIgnoreCase("借货申请")) {
                        DynamicObject inwarehouse =entryRow.getDynamicObject("inwarehouse");
                        if(inwarehouse==null){
                            //调出仓库
                            entryRow.set("inwarehouse", stockDyObj);
                        }
                    }
                    if (!id.equals("1994937462568258560") || !nameq.equalsIgnoreCase("借货归还申请")) {
                        DynamicObject warehouse =entryRow.getDynamicObject("warehouse");
                        if(warehouse==null){
                            //调出仓库
                            entryRow.set("warehouse", depStock);
                        }
                    }
                }
            }
        }
    }
}

