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

/**
 * 调拨单申请单保存服务  设置仓库
 * 表单标识：nckd_im_transapply_ext
 * author:吴国强 2024-07-12
 */

public class TransApplyOperatePlugIn extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("org");//组织
        e.getFieldKeys().add("billtype");//单据类型
        e.getFieldKeys().add("applydept");//申请部门
        e.getFieldKeys().add("nckd_regiongroup");//销售片区
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
            //部门
            DynamicObject dept = dataEntity.getDynamicObject("applydept");
            if (dept != null) {
                Object deptId = dept.getPkValue();
                //销售片区
                DynamicObject ph=dataEntity.getDynamicObject("nckd_regiongroup");
                //部门对应仓库-借货仓
                DynamicObject stockDyObj = this.getStock(orgId,deptId,ph,"1");
                //部门对应仓库
                DynamicObject depStock = this.getStock(orgId,deptId,ph,"0");
                DynamicObjectCollection entryentity = dataEntity.getDynamicObjectCollection("billentry");
                if (!entryentity.isEmpty()) {
                    for (DynamicObject entryRow : entryentity) {
                        if (id.equals("1994937462568258560") || nameq.equalsIgnoreCase("借货归还申请")) {
                            DynamicObject warehouse = entryRow.getDynamicObject("warehouse");
                            if (warehouse == null) {
                                //调出仓库
                                entryRow.set("warehouse", stockDyObj);
                            }
                            DynamicObject inwarehouse = entryRow.getDynamicObject("inwarehouse");
                            if (inwarehouse == null) {
                                //调入仓库
                                entryRow.set("inwarehouse", depStock);
                            }

                        } else if (id.equals("1994937113375673344") || nameq.equalsIgnoreCase("借货申请")) {
                            DynamicObject inwarehouse = entryRow.getDynamicObject("inwarehouse");
                            if (inwarehouse == null) {
                                //调入仓库
                                entryRow.set("inwarehouse", stockDyObj);
                            }
                            DynamicObject warehouse = entryRow.getDynamicObject("warehouse");
                            if (warehouse == null) {
                                //调出仓库
                                entryRow.set("warehouse", depStock);
                            }
                        }
                        if (!id.toString().equals("1994937462568258560") && !nameq.equalsIgnoreCase("借货归还申请")) {
                            DynamicObject warehouse = entryRow.getDynamicObject("warehouse");
                            if (warehouse == null) {
                                //调出仓库
                                entryRow.set("warehouse", depStock);
                            }
                        }
                    }
                }
            }


        }
    }

    private DynamicObject getStock(Object orgId, Object deptId, DynamicObject pq,String jh) {
        DynamicObject depStock = null;
        //从部门 仓库设置基础资料中获取对应仓库
        // 构造QFilter
        QFilter depqFilter = new QFilter("createorg", QCP.equals, orgId)
                .and("status", QCP.equals, "C")
                .and("nckd_bm", QCP.equals, deptId)
                .and("nckd_isjh", QCP.equals, jh);//借货仓
        boolean pqSelect = false;
        if (pq != null) {
            Object pqPkId = pq.getPkValue();
            depqFilter.and("nckd_regiongroup", QCP.equals, pqPkId);
            pqSelect = true;
        }else {
            depqFilter.and("nckd_regiongroup", QCP.equals, 0L);
        }
        //查找部门对应仓库
        DynamicObjectCollection depcollections = QueryServiceHelper.query("nckd_bmcksz",
                "id,nckd_ck.id stockId", depqFilter.toArray(), "modifytime");
        if (!depcollections.isEmpty()) {
            DynamicObject stockItem = depcollections.get(0);
            String stockId = stockItem.getString("stockId");
            depStock = BusinessDataServiceHelper.loadSingle(stockId, "bd_warehouse");
        }else if(pqSelect){
            // 构造QFilter
            QFilter nFilter = new QFilter("createorg", QCP.equals, orgId)
                    .and("status", QCP.equals, "C")
                    .and("nckd_bm", QCP.equals, deptId)
                    .and("nckd_isjh", QCP.equals, jh)//借货仓
                    .and("nckd_regiongroup", QCP.equals, 0L);
            //查找部门对应仓库
            DynamicObjectCollection query = QueryServiceHelper.query("nckd_bmcksz",
                    "id,nckd_ck.id stockId", nFilter.toArray(), "modifytime");
            if (!query.isEmpty()) {
                DynamicObject stockItem = query.get(0);
                String stockId = stockItem.getString("stockId");
                depStock = BusinessDataServiceHelper.loadSingle(stockId, "bd_warehouse");
            }

        }

        return depStock;
    }
}

