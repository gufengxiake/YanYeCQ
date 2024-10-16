package nckd.yanye.occ.plugin.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.occ.ocbase.formplugin.base.OcbaseBillPlugin;

import java.util.EventObject;

/*
经销商门户要货订单表单插件，新增时携带订货渠道的开票信息
表单标识：nckd_ocbsoc_saleorde_ext1
author:wgq
date:2024/08/20
 */
public class OcbSaleOrderDpBillPlugIn extends OcbaseBillPlugin {
      @Override
      public void afterCreateNewData(EventObject e) {
          //super.afterCreateNewData(e);
          //订货渠道
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
