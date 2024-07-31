package nckd.yanye.hr.plugin.form.transfer.web.common;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.IFormView;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.hr.hbp.formplugin.web.HRCoreBaseBillEdit;
import kd.hr.hdm.business.domain.transfer.service.external.PersonExternalService;
import java.util.Collections;


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
        }
    }
}
