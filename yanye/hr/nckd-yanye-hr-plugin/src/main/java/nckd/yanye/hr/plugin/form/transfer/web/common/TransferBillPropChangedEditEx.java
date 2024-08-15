package nckd.yanye.hr.plugin.form.transfer.web.common;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.BasedataProp;
import kd.bos.entity.property.DateProp;
import kd.bos.form.IFormView;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.DateEdit;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.hr.hbp.formplugin.web.HRCoreBaseBillEdit;
import kd.hr.hdm.business.domain.transfer.service.external.PersonExternalService;
import java.util.Collections;

/**
 * 调入申请，标识 nckd_hdm_transferinbi_ext
 * 批量调动人员申请单，标识 nckd_hdm_transferbat_ext1
 * 二开插件-携带员工职级和干部类型
 * author: tangyuxuan
 * date:2024-07-30
 */
public class TransferBillPropChangedEditEx extends HRCoreBaseBillEdit  {

    private static final Log LOGGER = LogFactory.getLog(TransferBillPropChangedEditEx.class);

    public TransferBillPropChangedEditEx() {

    }

    /**
     * 2024-07-29 Tyx
     * 处理选择人员后，携带任职经历上的职级及干部类型
     * @param e
     */
    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        IFormView view = this.getView();
        IDataModel model = this.getModel();
        ChangeData changeData = e.getChangeSet()[0];
        int rowIndex = changeData.getRowIndex();
        Object newValue = changeData.getNewValue();
        String propertyName = e.getProperty().getName();
        if(propertyName.equals("personfield")) {
            if (null == newValue) {
                view.invokeOperation("refresh");
            } else {
                DynamicObject hrF7 = (DynamicObject) newValue;
                long depempID = hrF7.getLong("id");
                String originator = (String) model.getValue("originator");
                if (!originator.equals("1")) {
                    DynamicObject[] dynamicObjects = PersonExternalService.getInstance().invokeGetErmanFileByDepempId(Collections.singletonList(depempID));
                    if (null != dynamicObjects && dynamicObjects.length != 0) {
                        DynamicObject dynamicObject = dynamicObjects[0];

                        DynamicObject empposrel = dynamicObject.getDynamicObject("empposrel");
                        empposrel = BusinessDataServiceHelper.loadSingle(empposrel.getPkValue(),empposrel.getDataEntityType().getName());
                        model.setValue("nckd_oldzhiji", (Object)null, rowIndex);
                        model.setValue("nckd_oldganbutype", (Object)null, rowIndex);
                        model.setValue("nckd_oldzhiji", empposrel.get("nckd_zhiji_id"), rowIndex);
                        model.setValue("nckd_oldganbutype", empposrel.get("nckd_ganbutype_id"), rowIndex);
                    } else {
                        view.showErrorNotification(ResManager.loadKDString("通过人员档案接口未查询到数据,请联系管理员。", "TransferBillPropChangedEdit_0", "hr-hdm-formplugin", new Object[0]));
                        view.sendFormAction(view);
                    }
                }
            }
            // 选择人员后，对页面部分字段设置默认值
            model.setItemValueByNumber("amanagescope","1"); // 调入信息 所属管理范围：中国区
            model.setItemValueByNumber("nckd_zhiji","1080_S"); // 职级
            model.setItemValueByNumber("nckd_ganbutype","1040_S"); // 人员类型
            // 设置必录符号,调入信息 岗位
            BasedataEdit apiaddressProperty = (BasedataEdit)this.getControl("aposition");
            apiaddressProperty.setMustInput(true);
            BasedataProp prop = (BasedataProp)this.getModel().getDataEntityType().getProperty("aposition");
            prop.setMustInput(true);

        }

        if(propertyName.equals("ermanfile")) {
            if (null == newValue) {
                view.invokeOperation("refresh");
            } else {
                DynamicObject hrF7 = (DynamicObject) newValue;
                DynamicObject empposrel = hrF7.getDynamicObject("empposrel");
                empposrel = BusinessDataServiceHelper.loadSingle(empposrel.getPkValue(),empposrel.getDataEntityType().getName());
                model.setValue("nckd_oldzhiji", (Object)null, rowIndex);
                model.setValue("nckd_oldganbutype", (Object)null, rowIndex);
                model.setValue("nckd_oldzhiji", empposrel.get("nckd_zhiji_id"), rowIndex);
                model.setValue("nckd_oldganbutype", empposrel.get("nckd_ganbutype_id"), rowIndex);
            }
        }

    }
}
