package nckd.yanye.occ.plugin.operate;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.OperateOptionConst;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.occ.ocbase.common.status.Status;
import kd.occ.ocbase.common.util.CodeRuleUtil;
import kd.occ.ocbase.common.util.StringUtils;

/**
 * 渠道档案审核新增/更新渠道收货地址
 * 表单标识：nckd_im_transdirbill_ext
 * author:吴国强 2024-08-12
 */
public class ChannelAuditOperatePlugIn extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        e.getFieldKeys().add("area");//省市区
        e.getFieldKeys().add("address");//详细地址
        e.getFieldKeys().add("contact");//联系人
        e.getFieldKeys().add("contactphone");//联系人电话
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);
        DynamicObject[] settleRecords = e.getDataEntities();
        if (settleRecords != null) {
            String targetBill = "ocdbd_channel_address";
            for (DynamicObject dataObject : settleRecords) {
                Object pkId = dataObject.getPkValue();

                //省市区
                Object area = dataObject.get("area");
                //详细地址
                String address = dataObject.getString("address");
                //联系人
                String contact = dataObject.getString("contact");
                //联系人电话
                String contactphone = dataObject.getString("contactphone");
                //查找当前渠道是否存在渠道地址
                QFilter filter = new QFilter("orderchannel.id", QCP.equals, pkId).and("isdefault", QCP.equals, "1");
                DynamicObjectCollection channel_address = QueryServiceHelper.query(targetBill, "id", filter.toArray());
                if (!channel_address.isEmpty()) {
                    DynamicObject item = channel_address.get(0);
                    Object id = item.get("id");
                    DynamicObject channelAddress = BusinessDataServiceHelper.loadSingle(id, targetBill);
                    if (channelAddress != null) {
                        channelAddress.set("contactname", contact);
                        channelAddress.set("telephone", contactphone);
                        channelAddress.set("address", area);
                        channelAddress.set("address2", address);
                        channelAddress.set("isdefault", "1");
                        channelAddress.set("detailaddress", this.getAdminDivisionName(area) + address);
                        SaveServiceHelper.update(channelAddress);
                    }
                } else {
                    DynamicObject item = BusinessDataServiceHelper.newDynamicObject(targetBill);
                    item.set("number", CodeRuleUtil.readCodeRule(targetBill));
                    item.set("status", Status.SAVED.toString());
                    item.set("orderchannel",dataObject);
                    item.set("contactname", contact);
                    item.set("telephone", contactphone);
                    item.set("address", area);
                    item.set("address2", address);
                    item.set("isdefault", "1");
                    item.set("detailaddress", this.getAdminDivisionName(area) + address);
                    //保存
                    OperationResult operationResult1 = SaveServiceHelper.saveOperate(targetBill, new DynamicObject[]{item}, OperateOption.create());
                    if (operationResult1.isSuccess()) {
                        OperateOption auditOption = OperateOption.create();
                        auditOption.setVariableValue(OperateOptionConst.ISHASRIGHT, "true");//不验证权限
                        auditOption.setVariableValue(OperateOptionConst.IGNOREWARN, String.valueOf(true)); // 不执行警告级别校验器
                        //提交
                        OperationResult subResult = OperationServiceHelper.executeOperate("submit", targetBill, new DynamicObject[]{item}, auditOption);
                        if (subResult.isSuccess()) {
                            //审核
                            OperationResult auditResult = OperationServiceHelper.executeOperate("audit", targetBill, new DynamicObject[]{item}, auditOption);
                        }
                    }
                }

            }
        }

    }

    private String getAdminDivisionName(Object adminId) {
        if(adminId.toString()==""){
            return "";
        }
        DynamicObject adminDivision = QueryServiceHelper.queryOne("bd_admindivision", "fullname", (new QFilter("id", "=", Long.parseLong(adminId.toString()))).toArray());
        if (adminDivision == null) {
            return "";
        } else {
            String fullName = adminDivision.getString("fullname");
            return StringUtils.isEmpty(fullName) ? "" : fullName.replace("_", "");
        }
    }
}
