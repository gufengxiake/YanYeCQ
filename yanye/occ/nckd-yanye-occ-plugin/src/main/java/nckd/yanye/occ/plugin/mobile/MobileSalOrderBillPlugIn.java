package nckd.yanye.occ.plugin.mobile;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.FieldEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.list.BillList;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.BFTrackerServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.occ.ocbase.formplugin.base.OcbaseFormMobPlugin;
import kd.occ.ocdma.formplugin.order.SaleOrderBillPlugin;

import java.util.*;

/*
 * 要货订单移动表单插件
 * 表单标识：nckd_ocdma_saleorder_add
 * author:吴国强 2024-07-22
 */
public class MobileSalOrderBillPlugIn extends OcbaseFormMobPlugin implements BeforeF7SelectListener {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        BasedataEdit salerIdEdit = this.getView().getControl("nckd_salerid");
        if (salerIdEdit != null) {
            salerIdEdit.addBeforeF7SelectListener(this);
        }
        BasedataEdit operatorEdit = this.getView().getControl("nckd_operatorgroup");
        if (operatorEdit != null) {
            operatorEdit.addBeforeF7SelectListener(this);
        }
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent evt) {
        String name = evt.getProperty().getName();
        if (name.equalsIgnoreCase("nckd_operatorgroup")) {
            //销售组织
            DynamicObject salOrg = (DynamicObject) this.getModel().getValue("saleorgid", 0);
            if (salOrg == null) {
                this.getView().showErrorNotification("请先选择销售组织！");
                evt.setCancel(true);
                return;
            }
            Object orgId = salOrg.getPkValue();
            ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
            List<QFilter> qFilters = new ArrayList<>();
            qFilters.add(new QFilter("createorg.id", QCP.equals, orgId).and("operatorgrouptype", QCP.equals, "XSZ"));
            formShowParameter.getListFilterParameter().setQFilters(qFilters);

        } else if (name.equalsIgnoreCase("nckd_salerid")) {
            DynamicObject operatorGroup = (DynamicObject) this.getModel().getValue("nckd_operatorgroup", 0);
            if (operatorGroup == null) {
                this.getView().showErrorNotification("请先选择业务组！");
                evt.setCancel(true);
                return;
            }
            Object operatorGroupId = operatorGroup.getPkValue();
            ListShowParameter formShowParameter = (ListShowParameter) evt.getFormShowParameter();
            List<QFilter> qFilters = new ArrayList<>();
            qFilters.add(new QFilter("operatorgrpid", QCP.equals, operatorGroupId));
            formShowParameter.getListFilterParameter().setQFilters(qFilters);
        }
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        //super.afterCreateNewData(e);
        String home = this.getView().getParentView().getParentView().getFormShowParameter().getFormId();
        //父页面为经销商门户主页
        if ("ocdma_homeindex".equals(home)) {
            //单据类型默认
            this.getModel().setItemValueByNumber("nckd_billtype", "ocbsoc_saleorder_BT001");
        }

        DynamicObject orderchannelid = (DynamicObject) this.getModel().getValue("orderchannelid");
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
        //String formName= this.getView().getFormShowParameter().getFormName();

        String formId = this.getView().getFormShowParameter().getFormId();

        if (formId.equalsIgnoreCase("ocdma_saleorder_add")) {
            //给业务员赋值
            Long orgId = RequestContext.get().getOrgId();
            DynamicObject org = (DynamicObject) this.getModel().getValue("saleorgid");
            if (org != null) {
                orgId = (Long) org.getPkValue();
            }
            if (orgId != 0) {
                // 构造QFilter  createorg  创建组织   operatorgrouptype 业务组类型=销售组
                QFilter qFilter = new QFilter("createorg.id", QCP.equals, orgId)
                        .and("operatorgrouptype", QCP.equals, "XSZ");
                //查找业务组
                DynamicObjectCollection collections = QueryServiceHelper.query("bd_operatorgroup",
                        "id", qFilter.toArray(), "");
                if (!collections.isEmpty()) {
                    DynamicObject operatorGroupItem = collections.get(0);
                    long operatorGroupId = (long) operatorGroupItem.get("id");
                    this.getModel().setItemValueByID("nckd_operatorgroup", operatorGroupId);
                    DynamicObject user = UserServiceHelper.getCurrentUser("id,number,name");
                    if (user != null && operatorGroupId != 0) {
                        String number = user.getString("number");
                        // 构造QFilter  operatornumber业务员   operatorgrpid 业务组id
                        QFilter Filter = new QFilter("operatornumber", QCP.equals, number)
                                .and("operatorgrpid", QCP.equals, operatorGroupId);
                        //查找业务员
                        DynamicObjectCollection opreatorColl = QueryServiceHelper.query("bd_operator",
                                "id", Filter.toArray(), "");
                        if (!opreatorColl.isEmpty()) {
                            DynamicObject operatorItem = opreatorColl.get(0);
                            String operatorId = operatorItem.getString("id");
                            this.getModel().setItemValueByID("nckd_salerid", operatorId);
                        }
                    }
                }

            }
        }
    }


    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        String home = this.getView().getParentView().getParentView().getFormShowParameter().getFormId();
        //父页面为经销商门户主页
        if ("ocdma_homeindex".equals(home)) {
            //设置业务员非必录
            this.getControl("lago_attachmentfield1");
            //设置业务员不可见
            BasedataEdit ywy = this.getControl("nckd_salerid");
            ywy.setMustInput(false);
            ywy.setVisible("", false);
        }
    }


    @Override
    public void afterDoOperation(AfterDoOperationEventArgs e) {
        super.afterDoOperation(e);
        //开票信息更新到渠道档案和客户档案
        if (e.getOperateKey().equals("update")) {
            DynamicObject orderchannelid = (DynamicObject) this.getModel().getValue("orderchannelid");
            DynamicObject customer = orderchannelid.getDynamicObject("customer");
            if (orderchannelid != null) {
                orderchannelid.set("nckd_nashuitype", this.getModel().getValue("nckd_nashuitype"));
                orderchannelid.set("nckd_nashuitax", this.getModel().getValue("nckd_nashuitax"));
                orderchannelid.set("nckd_name1", this.getModel().getValue("nckd_name1"));
                orderchannelid.set("nckd_fptype", this.getModel().getValue("nckd_fptype"));
                orderchannelid.set("nckd_addtel", this.getModel().getValue("nckd_addtel"));
                orderchannelid.set("nckd_bank", this.getModel().getValue("nckd_bank"));
                orderchannelid.set("nckd_yhzh", this.getModel().getValue("nckd_yhzh"));
                orderchannelid.set("nckd_phonenumber", this.getModel().getValue("nckd_phonenumber"));
                orderchannelid.set("nckd_mail", this.getModel().getValue("nckd_mail"));
                SaveServiceHelper.update(orderchannelid);
            }
            if (customer != null) {
                Object customerId = customer.getPkValue();
                DynamicObject customerDy = BusinessDataServiceHelper.loadSingle(customerId, "bd_customer");
                customerDy.set("nckd_nashuitype", this.getModel().getValue("nckd_nashuitype"));
                customerDy.set("nckd_nashuitax", this.getModel().getValue("nckd_nashuitax"));
                customerDy.set("nckd_name1", this.getModel().getValue("nckd_name1"));
                customerDy.set("invoicecategory", this.getModel().getValue("nckd_fptype"));
                customerDy.set("nckd_addtel", this.getModel().getValue("nckd_addtel"));
                customerDy.set("nckd_bank", this.getModel().getValue("nckd_bank"));
                customerDy.set("nckd_yhzh", this.getModel().getValue("nckd_yhzh"));
                customerDy.set("nckd_phonenumber", this.getModel().getValue("nckd_phonenumber"));
                customerDy.set("nckd_mail", this.getModel().getValue("nckd_mail"));
                SaveServiceHelper.update(customerDy);
            }
            this.getView().showSuccessNotification("更新完成！");


        }
    }
}
