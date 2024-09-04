package nckd.yanye.scm.plugin.convert;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.BillEntityType;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.botp.plugin.AbstractConvertPlugIn;
import kd.bos.entity.botp.plugin.args.AfterConvertEventArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.fi.gl.upgradeservice.BalancesheetUpgradeService;

/**
 * 供应链-收入确认单botp插件
 * 表单标识：nckd_ar_revcfmbill_ext
 * author：xiaoxiaopeng
 * date：2024-09-03
 */
public class FinarBillConvertPlugin extends AbstractConvertPlugIn {

    @Override
    public void afterConvert(AfterConvertEventArgs e) {
        super.afterConvert(e);
        //获取目标单
        String name = this.getTgtMainType().getName();
        ExtendedDataEntity[] dataEntities = e.getTargetExtDataEntitySet().FindByEntityKey(name);
        for (ExtendedDataEntity d : dataEntities) {
            DynamicObject dataEntity = d.getDataEntity();
            DynamicObjectCollection entry = dataEntity.getDynamicObjectCollection("entry");
            for (int i = 0; i < entry.size(); i++) {
                DynamicObject entity = entry.get(i);
                DynamicObject material = entity.getDynamicObject("e_material");
                DynamicObject org = dataEntity.getDynamicObject("org");
                DynamicObject bdMaterialcalinfo = BusinessDataServiceHelper.loadSingleFromCache("bd_materialcalinfo", "id,createorg,masterid,group",
                        new QFilter[]{new QFilter("masterid.id", QCP.equals, material.getPkValue()).and("createorg.id", QCP.equals, org.getPkValue())});
                if (bdMaterialcalinfo == null) {
                    continue;
                }
                DynamicObject group = bdMaterialcalinfo.getDynamicObject("group");
                entity.set("nckd_chlb",group);
            }
        }
    }
}
