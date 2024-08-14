package nckd.yanye.hr.plugin.form.parttime.web;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.hr.hbp.formplugin.web.HRCoreBaseBillEdit;
import kd.hr.hdm.common.util.HRServiceUtil;

import java.util.Collections;
import java.util.Map;

/**
 * 员工兼职申请单,标识 nckd_hdm_parttimeappl_ext
 * 二开插件-携带职级及干部类型
 * author: tangyuxuan
 * date:2024-07-30
 */
public class ParttimeApplyBillEditEx extends HRCoreBaseBillEdit {

    private static final Log LOGGER = LogFactory.getLog(ParttimeApplyBillEditEx.class);

    public ParttimeApplyBillEditEx (){

    }

    /**
     * 2024-07-29 Tyx
     * 处理选择人员后携带职级及干部类型
     * @param e
     */
    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String propertyName = e.getProperty().getName();
        ChangeData[] changeDataArray = e.getChangeSet();
        switch (propertyName) {
            case "partperson" :
                Map<String, Object> erManFile = this.getErManFile();
                DynamicObject partPerson = (DynamicObject)changeDataArray[0].getNewValue();
                this.setPersonFieldsValue(partPerson, erManFile);
                break;
        }
    }

    private void setPersonFieldsValue(DynamicObject partPerson, Map<String, Object> erManFile) {
        IDataModel model = this.getModel();
        model.setValue("nckd_oldganbutype", null);
        model.setValue("nckd_oldzhiji", null);

        long empposorgrelId = partPerson.getLong("empposrel.id");
        DynamicObject empposrel = BusinessDataServiceHelper.loadSingle(empposorgrelId, "hrpi_empposorgrel");
        model.setValue("nckd_oldzhiji", empposrel.get("nckd_zhiji_id"));
        model.setValue("nckd_oldganbutype", empposrel.get("nckd_ganbutype_id"));
        // 兼职任岗模式设置默认值-cch-20240813
        model.setValue("apositiontype",'1');
    }

    private Map<String, Object> getErManFile() {
        Object partPerson = this.getModel().getValue("partperson");
        if (null == partPerson) {
            return Collections.EMPTY_MAP;
        } else {
            Long ermanFiledId = (Long)((DynamicObject)partPerson).getPkValue();
            return HRServiceUtil.getErManFileById(ermanFiledId);
        }
    }

}
