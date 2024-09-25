package nckd.yanye.scm.plugin.form;

import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.exception.KDBizException;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Control;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

/**
 * @author husheng
 * @date 2024-09-25 14:33
 * @description
 */
public class SelectorgFormPlugin extends AbstractBillPlugIn {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        this.addClickListeners("btnok");
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);

        Control source = (Control) evt.getSource();
        String key = source.getKey();
        if (key.equals("btnok")) {
            Map<String, Object> map = new HashMap<>();
            DynamicObject org = (DynamicObject) this.getModel().getValue("nckd_org");
            if (org != null) {
                FormShowParameter showParameter = this.getView().getFormShowParameter();
                Map<String, Object> customParams = showParameter.getCustomParams();
                List<Long> orgIds = (List<Long>) customParams.get("orgIds");
                if (orgIds.contains(org.getLong("id"))) {
                    throw new KDBizException("属性申请组织重复!");
                }

                List<String> materialnumberList = (List<String>) customParams.get("materialnumberList");
                QFilter qFilter = new QFilter("org", QCP.equals, org.getLong("id"))
                        .and("nckd_materialnumber.number", QCP.in, materialnumberList);
                boolean exists = QueryServiceHelper.exists("nckd_materialmaintenan", qFilter.toArray());
                if (exists) {
                    throw new KDBizException("属性申请组织存在对应物料的物料维护单!");
                }

                map.put("orgId", org.getLong("id"));
                //返回数据
                this.getView().returnDataToParent(map);
                this.getView().close();
            }
        }
    }
}
