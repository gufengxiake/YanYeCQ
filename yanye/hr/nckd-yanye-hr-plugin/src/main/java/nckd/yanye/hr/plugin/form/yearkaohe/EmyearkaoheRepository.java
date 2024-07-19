package nckd.yanye.hr.plugin.form.yearkaohe;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QFilter;
import kd.hr.hbp.business.servicehelper.HRBaseServiceHelper;
import kd.hr.hbp.common.util.HRObjectUtils;
import kd.hr.hbp.common.util.HRStringUtils;
import kd.sdk.hr.hspm.common.utils.HrpiServiceOperateParam;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EmyearkaoheRepository {
    // 核心人力云->人员信息->分类维护表单 年度考核信息
    private static final HRBaseServiceHelper SERVICE_HELPER = new HRBaseServiceHelper("nckd_hspm_yearkaohe");
    // HR中台服务云->员工信息中心->人员附表 年度考核信息基础页面
    private static final HRBaseServiceHelper HRPI_EMPPROEXP_SERVICE_HELPER = new HRBaseServiceHelper("nckd_hrpi_yearkaohe");

    private EmyearkaoheRepository() {
    }


    public static EmyearkaoheRepository getInstance() {
        return EmyearkaoheRepository.Holder.INSTANCE;
    }

    public DynamicObject getEmchufeninfo(Long empproexpId, String selectProperties) {
        DynamicObject dbDy = HRStringUtils.isEmpty(selectProperties) ? SERVICE_HELPER.loadSingle(empproexpId) : SERVICE_HELPER.queryOne(selectProperties, empproexpId);
        if (HRObjectUtils.isEmpty(dbDy)) {
            throw new KDBizException(ResManager.loadKDString("数据不存在或已删除", "EmpproexpRepository_0", "hr-hspm-business", new Object[0]));
        } else {
            return dbDy;
        }
    }

    public DynamicObject[] queryByPkIdList(List<Long> pkIdList) {
        return SERVICE_HELPER.query(new QFilter[]{new QFilter("id", "in", pkIdList)});
    }

    public DynamicObject getHrpiEmpproexp(Long empproexpId) {
        return this.getHrpiEmchufeninfo(empproexpId, "");
    }

    public DynamicObject getHrpiEmchufeninfo(Long empproexpId, String selectProperties) {
        DynamicObject dbDy = HRStringUtils.isEmpty(selectProperties) ? HRPI_EMPPROEXP_SERVICE_HELPER.loadSingle(empproexpId) : HRPI_EMPPROEXP_SERVICE_HELPER.queryOne(selectProperties, empproexpId);
        if (HRObjectUtils.isEmpty(dbDy)) {
            throw new KDBizException(ResManager.loadKDString("数据不存在或已删除", "EmpproexpRepository_0", "hr-hspm-business", new Object[0]));
        } else {
            return dbDy;
        }
    }

    public DynamicObject[] queryHrpiEmchufeninfoForPerChg(List<Long> pkIdList) {
        QFilter idFilter = new QFilter("id", "in", pkIdList);
        DynamicObject[] dynamicObjects = HRPI_EMPPROEXP_SERVICE_HELPER.query("id,person,boid,sourcevid", idFilter.toArray());
        if (!HRObjectUtils.isEmpty(dynamicObjects) && dynamicObjects.length != 0) {
            return dynamicObjects;
        } else {
            throw new KDBizException(ResManager.loadKDString("数据不存在或已删除", "EmpproexpRepository_0", "hr-hspm-business", new Object[0]));
        }
    }

    public DynamicObjectCollection getInvokeSaveByEmchufeninfo(DynamicObject dataEntity) {
        return this.getInvokeSaveByEmchufeninfo(new DynamicObject[]{dataEntity});
    }

    public DynamicObjectCollection getInvokeSaveByEmchufeninfo(DynamicObject[] dataEntities) {
        DynamicObjectCollection dynamicObjectCollection = new DynamicObjectCollection();
        DynamicObject[] var3 = dataEntities;
        int var4 = dataEntities.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            DynamicObject dataEntity = var3[var5];
            HrpiServiceOperateParam.getInvokeSave(HRPI_EMPPROEXP_SERVICE_HELPER, dataEntity, dynamicObjectCollection);
        }

        return dynamicObjectCollection;
    }

    public DynamicObjectCollection getInvokeUpdateByEmchufeninfo(Long pkId, DynamicObject dataEntity) {
        Map<Long, DynamicObject> dataEntityMap = new HashMap(16);
        dataEntityMap.put(pkId, dataEntity);
        return this.getInvokeUpdateByEmchufeninfo(dataEntityMap);
    }

    public DynamicObjectCollection getInvokeUpdateByEmchufeninfo(Map<Long, DynamicObject> dataEntityMap) {
        DynamicObjectCollection dynamicObjectCollection = new DynamicObjectCollection();
        Iterator var3 = dataEntityMap.entrySet().iterator();

        while(var3.hasNext()) {
            Map.Entry<Long, DynamicObject> entry = (Map.Entry)var3.next();
            Long pkId = (Long)entry.getKey();
            DynamicObject dataEntity = (DynamicObject)entry.getValue();
            DynamicObject dbDy = this.getHrpiEmpproexp(pkId);
            HrpiServiceOperateParam.getInvokeUpdate(HRPI_EMPPROEXP_SERVICE_HELPER, dataEntity, dbDy, dynamicObjectCollection);
        }

        return dynamicObjectCollection;
    }

    public DynamicObject[] getImportSaveByEmchufeninfo(DynamicObject[] dataEntities) {
        DynamicObject[] importSaveDys = new DynamicObject[dataEntities.length];

        for(int i = 0; i < dataEntities.length; ++i) {
            DynamicObject importSaveDy = HrpiServiceOperateParam.getSaveDy(HRPI_EMPPROEXP_SERVICE_HELPER, dataEntities[i]);
            importSaveDys[i] = importSaveDy;
        }

        return importSaveDys;
    }

    private static class Holder {
        static final EmyearkaoheRepository INSTANCE = new EmyearkaoheRepository();

        private Holder() {
        }
    }
}
