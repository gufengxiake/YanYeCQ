package nckd.yanye.occ.plugin.mobile;

import kd.bos.dataentity.entity.DynamicObject;
import kd.occ.ocdma.formplugin.order.SaleOrderBillPlugin;

import java.util.EventObject;

/*
 * 要货订单移动表单插件
 * 表单标识：nckd_ocdma_saleorder_add
 * author:吴国强 2024-07-22
 */
public class MobileSalOrderBillPlugIn extends SaleOrderBillPlugin {
    @Override
    public void afterCreateNewData(EventObject e) {
        //super.afterCreateNewData(e);
        DynamicObject orderchannelid= (DynamicObject) this.getModel().getValue("orderchannelid");
        if(orderchannelid!=null){
            //纳税人类型
            DynamicObject nsType=orderchannelid.getDynamicObject("nckd_nashuitype");
            //纳税人识别号
            String nxrNum=orderchannelid.getString("nckd_nashuitax");
            //购方名称
            String name=orderchannelid.getString("nckd_name1");
            //发票类型
            DynamicObject fp=orderchannelid.getDynamicObject("nckd_fptype");
            //地址
            Object dz=orderchannelid.get("nckd_addtel");
            //开户行
            DynamicObject bank=orderchannelid.getDynamicObject("nckd_bank");
            //银行账号
            String bankZh=orderchannelid.getString("nckd_yhzh");
            //手机号
            String phonenumber=orderchannelid.getString("nckd_phonenumber");
            //邮箱
            String mail=orderchannelid.getString("nckd_mail");

            this.getModel().setValue("nckd_nashuitype",nsType);
            this.getModel().setValue("nckd_nashuitax",nxrNum);
            this.getModel().setValue("nckd_name1",name);
            this.getModel().setValue("nckd_fptype",fp);
            this.getModel().setValue("nckd_addtel",dz);
            this.getModel().setValue("nckd_bank",bank);
            this.getModel().setValue("nckd_yhzh",bankZh);
            this.getModel().setValue("nckd_phonenumber",phonenumber);
            this.getModel().setValue("nckd_mail",mail);


        }

    }
}
