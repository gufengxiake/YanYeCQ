package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.RowDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.BeforeOperationArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;

/**
 * 直接调拨单单保存服务  设置仓库
 * 表单标识：nckd_im_transdirbill_ext
 * author:吴国强 2024-08-12
 */
public class TransdirOperatePlugIn extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("lotnumber");//调出批号
        e.getFieldKeys().add("inlotnumber");//调入批号
        e.getFieldKeys().add("org");//组织
        e.getFieldKeys().add("billtype");//单据类型
        e.getFieldKeys().add("outdept");//调出部门
        e.getFieldKeys().add("warehouse");//调入仓库
        e.getFieldKeys().add("outwarehouse");//调出仓库
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
            //调出部门
            DynamicObject dept = dataEntity.getDynamicObject("outdept");
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
                    String inlotnumber = entryRow.getString("inlotnumber");
                    if (inlotnumber.trim().equalsIgnoreCase("")) {
                        String lotnumber = entryRow.getString("lotnumber");
                        entryRow.set("inlotnumber", lotnumber);
                    }
                    if (id.equals("1980435141796826112") || nameq.equalsIgnoreCase("借货归还单")) {
                        DynamicObject outwarehouse=entryRow.getDynamicObject("outwarehouse");
                        if(outwarehouse==null){
                            //调出仓库
                            entryRow.set("outwarehouse", stockDyObj);
                        }
                        DynamicObject warehouse=entryRow.getDynamicObject("warehouse");
                        if(warehouse==null){
                            //调入仓库
                            entryRow.set("warehouse", depStock);
                        }

                    } else if (id.equals("1980435041267748864") || nameq.equalsIgnoreCase("借货单")) {
                        DynamicObject warehouse=entryRow.getDynamicObject("warehouse");
                        if(warehouse==null){
                            //调入仓库
                            entryRow.set("warehouse", stockDyObj);
                        }
                    }

                    if (!id.toString().equals("1980435141796826112") && !nameq.equalsIgnoreCase("借货归还单")) {
                        DynamicObject outwarehouse=entryRow.getDynamicObject("outwarehouse");
                        if(outwarehouse==null){
                            //调出仓库
                            entryRow.set("outwarehouse", depStock);
                        }
                    }
                }
            }
        }
    }
}

