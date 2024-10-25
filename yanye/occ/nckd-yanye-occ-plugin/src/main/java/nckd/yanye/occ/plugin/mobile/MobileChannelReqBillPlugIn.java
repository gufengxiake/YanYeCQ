package nckd.yanye.occ.plugin.mobile;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.plugin.AbstractMobFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;

import java.util.EventObject;

/*
 * 渠道申请移动单据
 * 表单标识：nckd_ocsaa_home_ext
 * author:吴国强 2024-08-28
 */
public class MobileChannelReqBillPlugIn extends AbstractMobFormPlugin {
    @Override
    public void afterCreateNewData(EventObject e) {
        Long orgId = RequestContext.get().getOrgId();
        if (orgId != 0) {
            //销售组织
            this.getModel().setItemValueByID("saleorg", orgId, 0);

            //设置销售片区--开始
            DynamicObject user = UserServiceHelper.getCurrentUser("id,number,name");
            Object userId = user.get("id");
            // 构造QFilter  createorg  创建组织   operatorgrouptype 业务组类型=销售组
            QFilter qFilter = new QFilter("createorg.id", QCP.equals, orgId)
                    .and("operatorgrouptype", QCP.equals, "XSZ")
                    .and("entryentity.operator.id", QCP.equals, userId);
            //查找业务组
            DynamicObjectCollection collections = QueryServiceHelper.query("bd_operatorgroup",
                    "id,entryentity.nckd_regiongroup as group", qFilter.toArray(), "");
            if (!collections.isEmpty()) {
                DynamicObject operatorGroupItem = collections.get(0);
                Object group = operatorGroupItem.get("group");
                this.getModel().setValue("nckd_regiongroup", group, 0);
            }
            //设置销售片区--结束
        }
        Object[] ids = new Object[]{768696787199340544L, 769522191178951680L};
        //渠道职能
        this.getModel().setValue("channelfunctions", ids, 0);
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        String propName = e.getProperty().getName();
        //结算渠道
        if ("nckd_settlementchannel".equals(propName)) {
            DynamicObject channel = (DynamicObject) e.getChangeSet()[0].getNewValue();
            Object id = channel.getPkValue();
            DynamicObject orderchannelid = BusinessDataServiceHelper.loadSingle(id, "ocdbd_channel");
            if (orderchannelid != null) {
                //纳税人类型
                DynamicObject nsType = orderchannelid.getDynamicObject("nckd_nashuitype");
                //纳税人识别号
                String nxrNum = orderchannelid.getString("nckd_nashuitax");
                //购方名称
                String name = orderchannelid.getString("nckd_name1");
                //发票类型
                DynamicObject fp = orderchannelid.getDynamicObject("nckd_fptype");
                //地址
                Object dz = orderchannelid.get("nckd_addtel");
                //开户行
                DynamicObject bank = orderchannelid.getDynamicObject("nckd_bank");
                //银行账号
                String bankZh = orderchannelid.getString("nckd_yhzh");
                //手机号
                String phonenumber = orderchannelid.getString("nckd_phonenumber");
                //邮箱
                String mail = orderchannelid.getString("nckd_mail");
                this.getModel().setValue("nckd_nashuitype", nsType);
                this.getModel().setValue("nckd_nashuitax", nxrNum);
                this.getModel().setValue("nckd_name1", name);
                this.getModel().setValue("nckd_fptype", fp);
                this.getModel().setValue("nckd_addtel", dz);
                this.getModel().setValue("nckd_bank", bank);
                this.getModel().setValue("nckd_yhzh", bankZh);
                this.getModel().setValue("nckd_phonenumber", phonenumber);
                this.getModel().setValue("nckd_mail", mail);
            }
        }
    }
}
