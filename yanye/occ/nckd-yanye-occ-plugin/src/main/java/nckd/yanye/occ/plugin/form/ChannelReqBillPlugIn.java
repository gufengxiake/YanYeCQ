package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;

import java.util.EventObject;

public class ChannelReqBillPlugIn extends AbstractBillPlugIn {
    @Override
    public void afterCreateNewData(EventObject e) {
        Long orgId = RequestContext.get().getOrgId();
        if (orgId != 0) {
            //销售组织
           this.getModel().setItemValueByID("saleorg",orgId,0);

            //设置销售片区--开始
            DynamicObject user = UserServiceHelper.getCurrentUser("id,number,name");
            Object userId=user.get("id");
            // 构造QFilter  createorg  创建组织   operatorgrouptype 业务组类型=销售组
            QFilter qFilter = new QFilter("createorg.id", QCP.equals, orgId)
                    .and("operatorgrouptype", QCP.equals, "XSZ")
                    .and("entryentity.operator.id",QCP.equals,userId);
            //查找业务组
            DynamicObjectCollection collections = QueryServiceHelper.query("bd_operatorgroup",
                    "id,entryentity.nckd_regiongroup as group", qFilter.toArray(), "");
            if (!collections.isEmpty()) {
                DynamicObject operatorGroupItem = collections.get(0);
                Object group=operatorGroupItem.get("group");
                this.getModel().setValue("nckd_regiongroup",group,0);
            }
            //设置销售片区--结束
        }
        Object[] ids=new Object[]{768696787199340544L,769522191178951680L};
        //渠道职能
        this.getModel().setValue("channelfunctions",ids,0);
    }
}
