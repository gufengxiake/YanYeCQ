package nckd.yanye.occ.plugin.form;

import com.alibaba.fastjson.JSONArray;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.data.BusinessDataReader;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityType;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.botp.runtime.SourceBillReport;
import kd.bos.entity.datamodel.IRefrencedataProvider;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.operate.OperateOptionConst;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDBizException;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Button;
import kd.bos.form.control.Control;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import scala.math.BigInt;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BatchCreatMaterialBillPlugIn extends AbstractBillPlugIn {
    @Override
    public void afterCreateNewData(EventObject e) {
        // 获取当前页面的FormShowParameter对象
        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        // 获取自定义参数
        //分组ID
        JSONArray matId = formShowParameter.getCustomParam("matId");
        if(matId.size()>1){
            this.getModel().batchCreateNewEntryRow("nckd_entryentity", matId.size());
        }
        for (int i = 0; i < matId.size(); i++) {
            Object matIdBigInteger = matId.getBigInteger(i);
            this.getModel().setItemValueByID("nckd_materiel", matIdBigInteger, i);
        }
    }

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 注册按钮点击监听（注意itemClick和click的区别）
        Button button = this.getControl("btnok");
        button.addClickListener(this);

    }

    @Override
    public void click(EventObject e) {
        super.click(e);
        //
        Control control = (Control) e.getSource();
        if ("btnok".equalsIgnoreCase(control.getKey())) {
            DynamicObjectCollection org = (DynamicObjectCollection) this.getModel().getValue("nckd_org");
            if (org.isEmpty()) {
                this.getView().showErrorNotification("请先选择组织!");
                return;
            }
            Object business = this.getModel().getValue("nckd_business");
            if (business == null || business.toString().trim() == "") {
                this.getView().showErrorNotification("请先选择物料业务!");
                return;
            }
            Set matSet = new HashSet();
            int row = this.getModel().getEntryRowCount("nckd_entryentity");
            for (int i = 0; i < row; i++) {
                DynamicObject mat = (DynamicObject) this.getModel().getValue("nckd_materiel", i);
                if (mat != null) {
                    matSet.add(mat.getPkValue());
                }
            }
            if (matSet.size() == 0) {
                this.getView().showErrorNotification("表体物料为空!");
                return;
            }
            //库存业务信息
            if (business.toString().contains("A")) {
                this.CreateMatInfo(org, matSet, "A");
            }
            //库存业务信息
            if (business.toString().contains("B")) {
                this.CreateMatInfo(org, matSet, "B");
            }
            //库存业务信息
            if (business.toString().contains("C")) {
                this.CreateMatInfo(org, matSet, "C");
            }
            //库存业务信息
            if (business.toString().contains("D")) {
                this.CreateMatInfo(org, matSet, "D");
            }
            //库存业务信息
            if (business.toString().contains("E")) {
                this.CreateMatInfo(org, matSet, "E");
            }
            //库存业务信息
            if (business.toString().contains("F")) {
                this.CreateMatInfo(org, matSet, "F");
            }
            //库存业务信息
            if (business.toString().contains("G")) {
                this.CreateMatInfo(org, matSet, "G");
            }
         this.getView().showSuccessNotification("成功生成物料业务数据!");

        }
    }

    private void CreateMatInfo(DynamicObjectCollection org, Set matSet, String business) {
        String targetBill = "";
        switch (business) {
            case "A":
                targetBill = "bd_materialinventoryinfo";//物料库存信息
                break;
            case "B":
                targetBill = "bd_materialpurchaseinfo";//物料采购信息
                break;
            case "C":
                targetBill = "bd_materialsalinfo";//物料销售信息
                break;
            case "D":
                targetBill = "bd_materialcalinfo";//物料核算信息
                break;
            case "E":
                targetBill = "bd_materialmftinfo";//物料生产信息
                break;
            case "F":
                targetBill = "bd_inspect_cfg";//物料质检信息
                break;
            case "G":
                targetBill = "mpdm_materialplan";//物料计划信息
                break;
        }
        for (DynamicObject orgItem : org) {
            DynamicObject basedataObj = (DynamicObject)orgItem.getDynamicObject("fbasedataid");
            Long basedataId = (Long) basedataObj.getPkValue();
            DynamicObject orgData=BusinessDataServiceHelper.loadSingle(basedataId,"bos_org");
            String sourceBill = "bd_material";//物料
            // 创建下推参数
            PushArgs pushArgs = new PushArgs();
            // 必填，源单标识
            pushArgs.setSourceEntityNumber(sourceBill);
            // 必填，目标单标识
            pushArgs.setTargetEntityNumber(targetBill);
            // 可选，传入true，不检查目标单新增权
            pushArgs.setHasRight(true);
            // 可选，是否输出详细错误报告
            pushArgs.setBuildConvReport(true);
            // 必填，设置需要下推的单据，或分录行
            List<ListSelectedRow> selectedRows = new ArrayList<>();
            for (Object pk : matSet) {
                ListSelectedRow row = new ListSelectedRow();
                //必填，设置源单单据id
                row.setPrimaryKeyValue(pk);
                selectedRows.add(row);

            }

            // 必选，设置需要下推的源单及分录内码
            pushArgs.setSelectedRows(selectedRows);
            // 调用下推引擎，下推目标单
            ConvertOperationResult pushResult = ConvertServiceHelper.push(pushArgs);
            // 判断下推是否成功，如果失败，提取失败消息
            if (!pushResult.isSuccess()) {
                String errMessage = pushResult.getMessage();    // 错误信息
                for (SourceBillReport billReport : pushResult.getBillReports()) {
                    // 提取各单错误报告
                    if (!billReport.isSuccess()) {
                        String billMessage = billReport.getFailMessage();
                    }
                }
                throw new KDBizException("下推失败:" + errMessage);
            }
            // 获取生成的目标单数据包
            MainEntityType targetMainType = EntityMetadataCache.getDataEntityType(targetBill);
            List<DynamicObject> targetBillObjs = pushResult.loadTargetDataObjects(new IRefrencedataProvider() {
                @Override
                public void fillReferenceData(Object[] objs, IDataEntityType dType) {
                    BusinessDataReader.loadRefence(objs, dType);
                }
            }, targetMainType);
            DynamicObject[] saveDynamicObject = targetBillObjs.toArray(new DynamicObject[targetBillObjs.size()]);
            for (DynamicObject data : saveDynamicObject) {
                data.set("createorg", orgData);
                data.set("org", orgData);
                data.set("srccreateorg", orgData);
            }
            //保存
            OperationResult operationResult1 = SaveServiceHelper.saveOperate(targetBill, saveDynamicObject, OperateOption.create());
            OperateOption auditOption = OperateOption.create();
            auditOption.setVariableValue(OperateOptionConst.ISHASRIGHT, "true");//不验证权限
            auditOption.setVariableValue(OperateOptionConst.IGNOREWARN, String.valueOf(true)); // 不执行警告级别校验器
            //提交
            OperationResult subResult = OperationServiceHelper.executeOperate("submit", targetBill, saveDynamicObject, auditOption);
            //审核
            OperationResult auditResult = OperationServiceHelper.executeOperate("audit", targetBill, saveDynamicObject, auditOption);

        }


    }
}
