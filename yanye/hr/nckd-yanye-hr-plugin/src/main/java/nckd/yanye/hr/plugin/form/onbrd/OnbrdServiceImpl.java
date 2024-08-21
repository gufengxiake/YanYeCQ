package nckd.yanye.hr.plugin.form.onbrd;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.ImportLogger;
import kd.bos.form.plugin.impt.ImportBillData;
import kd.hr.hbp.common.util.HRStringUtils;
import kd.hr.hom.common.enums.InitTypeEnum;
import kd.sdk.hr.hom.business.onbrd.IOnbrdService;

import java.util.List;
import java.util.Map;

/**
 * 入职办理单--业务扩展插件
 * 单据标识：nckd_hom_onbrdinfo_ext
 * 2024-08-21
 * chengchaohua
 */
public class OnbrdServiceImpl implements IOnbrdService {
    /**
     * 设置员工工号
     * @param dynamicObject
     * @param initType
     * @return
     */
    @Override
    public String setEmployeeNo(DynamicObject dynamicObject, String initType) {
        if (HRStringUtils.equals(InitTypeEnum.ONBRD_NEW_INTEGERATE_TSC.getValue(), initType)
                || HRStringUtils.equals(InitTypeEnum.ONBRD_NEW_INTEGERATE_THIRDSYS.getValue(), initType)
                || HRStringUtils.equals(InitTypeEnum.ONBRD_NEW_IMPORT.getValue(), initType)) {
            // candidatenumber 候选人编号
            return dynamicObject.getString("candidatenumber") + "auto";
        }
        return null;
    }

    @Override
    public void beforeWrapOriginalJson(List<ImportBillData> list, Map<String, ImportLogger> map, String s) {

    }
}
