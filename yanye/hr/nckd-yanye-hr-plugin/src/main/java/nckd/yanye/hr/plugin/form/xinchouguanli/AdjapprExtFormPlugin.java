package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.entity.BasedataEntityType;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.sdk.swc.hcdm.business.extpoint.adjapprbill.IDecAdjApprExtPlugin;
import kd.sdk.swc.hcdm.business.extpoint.adjapprbill.event.AfterF7PersonSelectEvent;

import java.util.List;
import java.util.Map;

/**
 * 员工定调薪申请单-表单插件
 * 单据标识：nckd_hcdm_adjapprbill_ext
 *
 * @author ：luxiao
 * @since ：Created in 17:28 2024/9/11
 */
public class AdjapprExtFormPlugin extends AbstractFormPlugin implements IDecAdjApprExtPlugin {

    /**
     * 在添加定调薪人员时，选择定调薪人员后，需要二开对新增的字段赋值
     *
     * @param e
     */
    @Override
    public void onAfterF7PersonSelect(AfterF7PersonSelectEvent e) {
        List<DynamicObject> adjPersonDyObjList = e.getAdjPersonDyObjList();
        for (DynamicObject obj : adjPersonDyObjList) {
            Map<String, IDataEntityProperty> allFields = ((BasedataEntityType) obj.getDynamicObjectType()).getAllFields();
            DynamicObjectCollection entryentity = obj.getDynamicObjectCollection("entryentity");
            for (DynamicObject entry : entryentity) {
                if ("1".equals(e.getAdjAttributionType())) {
                    if (allFields.containsKey("nckd_notesinfo")) {
                        obj.set("nckd_notesinfo", "测试测试1111");
                    }
                }
            }
        }
    }
}
