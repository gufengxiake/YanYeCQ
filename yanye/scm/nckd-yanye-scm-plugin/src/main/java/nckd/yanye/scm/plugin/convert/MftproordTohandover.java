package nckd.yanye.scm.plugin.convert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.botp.plugin.AbstractConvertPlugIn;
import kd.bos.entity.botp.plugin.args.AfterConvertEventArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;

/**
 * 生产领料单下推生成时，系统自动后去当前即时库存数量
 * 表单标识：nckd_im_mdc_mftproord_ext  nckd_im_mdc_handover
 * author:黄文波 2024-10-12
 */
public class MftproordTohandover extends AbstractConvertPlugIn {
    private final static String IM_INV_REALBALAN= "im_inv_realbalance";//即时库存余额表
    private final static String TQSD_IM_INV_REALBALAN_EXT_SEQ = "org,warehouse,location,ownertype,owner,invstatus,invtype,material,auxpty,lotnum,project,baseunit,unit,unit2nd,baseqty,"
            + ",qty,qty2nd";
    @Override
    public void afterConvert(AfterConvertEventArgs e) {
        super.afterConvert(e);

        // 获取目标单
        String name = this.getTgtMainType().getName();
        ExtendedDataEntity[] dataEntities = e.getTargetExtDataEntitySet().FindByEntityKey(name);

        for (ExtendedDataEntity dataEntity : dataEntities) {
            DynamicObject dynamicObject = dataEntity.getDataEntity();
            for (DynamicObject object : dynamicObject.getDynamicObjectCollection("entryentity")) {
                DynamicObject material = object.getDynamicObject("nckd_material");//物料
                DynamicObject warehouse = object.getDynamicObject("nckd_warehouse");//仓库
                DynamicObject org = object.getDynamicObject("org");//组织
                String stocktype = "110";//库存类型
                Long materialid = null;


                materialid=(long) material.getPkValue();
                Long stockid = (long) warehouse.getPkValue();//仓库内码
                Long orgid = (long) org.getPkValue();//库存组织内码


                DynamicObject[] record= new DynamicObject[0];
                try {
                    record = lotnumberQuery(materialid, stocktype,stockid,orgid);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                BigDecimal value = BigDecimal.ZERO;
                int chazhi = 0;
                for (int j = 0; j < record.length; j++) {
                    DynamicObject recordobj=record[j];
                    int lockqty = recordobj.getBigDecimal("lockqty").intValue();
                }

                object.set("nckd_inventoryqty", 1);//库存数量

//                if (group != null) {
//                    object.set("nckd_material_group", group);
//
//                    DynamicObject org = dynamicObject.getDynamicObject("org");
//                    QFilter qFilter1 = new QFilter("nckd_material_class.id", QCP.equals, group.getPkValue());
//                    QFilter qFilter2 = new QFilter("nckd_org.id", QCP.equals, org.getPkValue());
//                    QFilter qFilter3 = new QFilter("status", QCP.equals, "C");
//                    DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle("nckd_im_evaluate_material", "nckd_evaluate_period", new QFilter[]{qFilter1, qFilter2, qFilter3});
//                    if (loadSingle != null) {
//                        Integer evaluatePeriod = (Integer) loadSingle.get("nckd_evaluate_period");
//                        if (evaluatePeriod != null) {
//                            object.set("nckd_evaluate_period", evaluatePeriod);
//
//                            Date biztime = (Date) dynamicObject.get("biztime");
//                            LocalDateTime localDateTime = biztime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//                            object.set("nckd_evaluate_date", Date.from(localDateTime.plusDays(evaluatePeriod).atZone(ZoneId.systemDefault()).toInstant()));
//                            object.set("nckd_evaluate_flag", 1);
//                        }
//                    }
//                }
            }
        }



    }

    public DynamicObject[] lotnumberQuery(Long number,String stocktype,Long stockid,Long orgid) throws Exception {
        QFilter filter4 = new QFilter("org.id", QCP.equals, orgid);//库存组织
        QFilter filter5 = new QFilter("warehouse.id", QCP.equals, stockid);//仓库
        QFilter filter = new QFilter("material.id", QCP.equals, number);// 物料
        QFilter filter1 = new QFilter("invtype.number", QCP.equals, stocktype);//库存类型
        QFilter filter2 = new QFilter("baseqty", QCP.large_than, 0);//数量大于0
        QFilter filter3 = new QFilter("invstatus.number", QCP.equals, "110");//库存状态为可用
        filter.and(filter1).and(filter3).and(filter2).and(filter4).and(filter5);
        DynamicObject[] recordDynamicObjectList = BusinessDataServiceHelper.load(IM_INV_REALBALAN,TQSD_IM_INV_REALBALAN_EXT_SEQ, filter.toArray());// 获取批号集合
        return recordDynamicObjectList;

    }
}
