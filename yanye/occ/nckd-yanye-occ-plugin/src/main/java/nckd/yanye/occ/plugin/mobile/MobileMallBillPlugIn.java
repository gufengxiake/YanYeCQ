package nckd.yanye.occ.plugin.mobile;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.occ.ocbase.common.util.DynamicObjectUtils;

import java.math.BigDecimal;
import java.util.*;

/*
 * 商城显示即时库存数量
 * 表单标识：nckd_ocdma_mall_ext
 * author:吴国强 2024-10-21
 */
public class MobileMallBillPlugIn extends AbstractMobFormPlugin {
    private final static String IM_INV_REALBALAN= "im_inv_realbalance";//即时库存余额表
    private final static String TQSD_IM_INV_REALBALAN_EXT_SEQ = "org,warehouse,location,ownertype,owner,invstatus,invtype,material,auxpty,lotnum,project,baseunit,unit,unit2nd,baseqty,"
            + "qty,qty2nd";

    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
        Long orgId = RequestContext.get().getOrgId();
        //当前人员信息
        DynamicObject currentUser = UserServiceHelper.getCurrentUser("id");
        Object userId = currentUser.get("id");
        DynamicObject user = BusinessDataServiceHelper.loadSingle(userId, "bos_user");
        DynamicObjectCollection usreEntry = user.getDynamicObjectCollection("entryentity");
        Object deptId = null;
        for (DynamicObject entryRow : usreEntry) {
            //是否主岗
            boolean ispartjob = entryRow.getBoolean("ispartjob");
            if (!ispartjob) {
                //部门
                DynamicObject dept = entryRow.getDynamicObject("dpt");
                deptId=dept.getPkValue();
            }
        }
        //获取部门仓库
        DynamicObject depStock = this.getStock(orgId,deptId,null,"0");
        if(depStock!=null){
            Long depStockId= (Long) depStock.getPkValue();
            DynamicObjectCollection itemListCollection = this.getModel().getEntryEntity("itemlist");
            Iterator var25 = itemListCollection.iterator();
            Set matList=new HashSet();
            while (var25.hasNext()) {
                DynamicObject itemEntry = (DynamicObject) var25.next();
                DynamicObject material = DynamicObjectUtils.getDynamicObject(itemEntry, "material");
                Long matId= (Long) material.getPkValue();
                matList.add(matId);
            }
            if(matList.size()>0){
                //获取物料的即时库存
                Map<Object,BigDecimal> matQtyMap=this.lotnumberQuery(matList,depStockId,orgId);
                for(DynamicObject itemEntry:itemListCollection){
                    DynamicObject material = DynamicObjectUtils.getDynamicObject(itemEntry, "material");
                    Long matId= (Long) material.getPkValue();
                    itemEntry.set("nckd_stock",depStock);
                    BigDecimal qty=matQtyMap.get(matId);
                    itemEntry.set("nckd_qty",qty);
                }
            }

            this.getModel().updateEntryCache(itemListCollection);

        }



    }

    //获取部门对应仓库
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

    //获取物料的即时库存
    public Map<Object,BigDecimal> lotnumberQuery(Set<Long> matId,Long stockid,Long orgid) {
        Map<Object,BigDecimal> matQtyMap=new HashMap<>();
        QFilter filter4 = new QFilter("org.id", QCP.equals, orgid);//库存组织
        QFilter filter5 = new QFilter("warehouse.id", QCP.equals, stockid);//仓库
        QFilter filter = new QFilter("material.id", QCP.in, matId);// 物料
        QFilter filter1 = new QFilter("invtype.number", QCP.equals, "110");//库存类型为普通
        QFilter filter2 = new QFilter("baseqty", QCP.large_than, 0);//数量大于0
        QFilter filter3 = new QFilter("invstatus.number", QCP.equals, "110");//库存状态为可用
        filter.and(filter1).and(filter3).and(filter2).and(filter4).and(filter5);
        DataSet orderCollections = QueryServiceHelper.queryDataSet("DATE", IM_INV_REALBALAN,
                TQSD_IM_INV_REALBALAN_EXT_SEQ, filter.toArray(), null);
        //根据物料合计数量
        DataSet sumSql = orderCollections.groupBy(new String[]{"material"}).sum("qty").finish();
        while (sumSql.hasNext()) {
            Row sumItem = sumSql.next();
            Object material=sumItem.get("material");
            BigDecimal qty=sumItem.getBigDecimal("qty");
            matQtyMap.put(material,qty);
        }
        return matQtyMap;

    }
}
