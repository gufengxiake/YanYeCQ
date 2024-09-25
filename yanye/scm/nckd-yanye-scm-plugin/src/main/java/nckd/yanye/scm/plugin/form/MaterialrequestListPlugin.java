package nckd.yanye.scm.plugin.form;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDBizException;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.util.CollectionUtils;
import nckd.yanye.scm.common.utils.MaterialAttributeInformationUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author husheng
 * @date 2024-09-25 14:11
 * @description 物料申请单（nckd_materialrequest）
 */
public class MaterialrequestListPlugin extends AbstractListPlugin {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        this.addItemClickListeners("nckd_materialcontinue");
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);

        String itemKey = evt.getItemKey();
        if (itemKey.equals("nckd_materialcontinue")) {
            ListSelectedRowCollection selectedRows = this.getSelectedRows();
            if (selectedRows.size() == 0) {
                throw new KDBizException("请选择数据!");
            }

            Object[] keyValues = selectedRows.getPrimaryKeyValues();
            QFilter qFilter = new QFilter("id", QCP.in, keyValues)
                    .and("billstatus", QCP.not_equals, "C");
            boolean exists = QueryServiceHelper.exists("nckd_materialrequest", qFilter.toArray());
            if (exists) {
                throw new KDBizException("只能选择已审核的数据!");
            }

            // 获取选择的数据
            DynamicObjectCollection materialrequest = QueryServiceHelper.query("nckd_materialrequest", "org,nckd_materialentries.nckd_materialnumber", new QFilter[]{new QFilter("id", QCP.in, keyValues)});
            List<Long> orgIds = materialrequest.stream().map(t -> t.getLong("org")).distinct().collect(Collectors.toList());
            List<String> materialnumberList = materialrequest.stream().map(t -> t.getString("nckd_materialentries.nckd_materialnumber")).distinct().collect(Collectors.toList());

            FormShowParameter parameter = new FormShowParameter();
            parameter.setFormId("nckd_selectorg");
            parameter.setCloseCallBack(new CloseCallBack(this, "materialrequest"));
            parameter.getOpenStyle().setShowType(ShowType.Modal);
            parameter.setCustomParam("orgIds", orgIds);
            parameter.setCustomParam("materialnumberList", materialnumberList);
            this.getView().showForm(parameter);
        }
    }

    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        super.closedCallBack(closedCallBackEvent);

        Map<String, Object> map = (Map<String, Object>) closedCallBackEvent.getReturnData();
        if (map != null) {
            DynamicObject org = BusinessDataServiceHelper.loadSingle(map.get("orgId"), "bos_org");

            // 列表选择的数据
            ListSelectedRowCollection selectedRows = this.getSelectedRows();
            Object[] keyValues = selectedRows.getPrimaryKeyValues();

            // 生成物料维护单
            Arrays.stream(keyValues).forEach(l -> {
                DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle(l, "nckd_materialrequest");
                DynamicObjectCollection dynamicObjectCollection = loadSingle.getDynamicObjectCollection("nckd_materialentries");
                for (DynamicObject dynamicObject : dynamicObjectCollection) {
                    // 物料
                    DynamicObject material = BusinessDataServiceHelper.loadSingle("bd_material", new QFilter[]{new QFilter("number", QCP.equals, dynamicObject.getString("nckd_materialnumber"))});

                    /**
                     * 物料类型nckd_materialtype(1:物资、7:费用、8:资产)
                     * 物料属性nckd_materialattribute(1:自制、2：外购)
                     * 自制物料类型nckd_selfmaterialtype(1：产成品、2：半成品)
                     */
                    if ("1".equals(dynamicObject.getString("nckd_materialtype"))
                            && "1".equals(dynamicObject.getString("nckd_materialattribute"))
                            && "1".equals(dynamicObject.getString("nckd_selfmaterialtype"))) {
                        getDynamicObject(dynamicObject, MaterialAttributeInformationUtils.finishedGoodsList, loadSingle, org, material);
                    } else if ("1".equals(dynamicObject.getString("nckd_materialtype"))
                            && "1".equals(dynamicObject.getString("nckd_materialattribute"))
                            && "2".equals(dynamicObject.getString("nckd_selfmaterialtype"))) {
                        getDynamicObject(dynamicObject, MaterialAttributeInformationUtils.semiFinishedList, loadSingle, org, material);
                    } else if (Arrays.asList("1", "8").contains(dynamicObject.getString("nckd_materialtype"))
                            && "2".equals(dynamicObject.getString("nckd_materialattribute"))) {
                        getDynamicObject(dynamicObject, MaterialAttributeInformationUtils.outsourcingList, loadSingle, org, material);
                    } else if ("7".equals(dynamicObject.getString("nckd_materialtype"))
                            && "2".equals(dynamicObject.getString("nckd_materialattribute"))) {
                        getDynamicObject(dynamicObject, MaterialAttributeInformationUtils.feeOutsourcingList, loadSingle, org, material);
                    }

                    // 生成物料属性信息
                    if ("1".equals(dynamicObject.getString("nckd_materialattribute"))
                            && "1".equals(dynamicObject.getString("nckd_selfmaterialtype"))
                            && "113".equals(org.getString("number"))) {
                        //【物料属性】为‘自制’+【自制物料类型】‘产成品’+【申请组织】‘江西盐业包装有限公司’
                        MaterialAttributeInformationUtils.defaultPurchaseInfo(org, material);// 采购基本信息
                    } else if ("1".equals(dynamicObject.getString("nckd_materialattribute"))
                            && "2".equals(dynamicObject.getString("nckd_selfmaterialtype"))) {
                        //【物料属性】为‘自制’+【自制物料类型】“半成品”
                        MaterialAttributeInformationUtils.defaultPurchaseInfo(org, material);// 采购基本信息
                        MaterialAttributeInformationUtils.defaultMarketInfo(org, material);// 销售基本信息
                    } else if ("2".equals(dynamicObject.getString("nckd_materialattribute"))) {
                        //【物料属性】为‘外购’
                        MaterialAttributeInformationUtils.defaultMarketInfo(org, material);// 销售基本信息
                    }

                    // 核算信息设置存货类别并提交审核
                    MaterialAttributeInformationUtils.setCheckInfoMaterialcategory(material, org);
                }
            });
        }
    }

    /**
     * @param dynamicObject 物料分录
     * @param list          单据类型集合
     * @return
     */
    private void getDynamicObject(DynamicObject dynamicObject, List<String> list, DynamicObject object, DynamicObject org, DynamicObject materialdynamicObject) {
        for (String billType : list) {
            DynamicObject materialmaintenanObject = BusinessDataServiceHelper.newDynamicObject("nckd_materialmaintenan");
            /**制单信息**/
            materialmaintenanObject.set("creator", RequestContext.get().getCurrUserId());
            materialmaintenanObject.set("createtime", new Date());
            materialmaintenanObject.set("modifier", RequestContext.get().getCurrUserId());
            materialmaintenanObject.set("modifytime", new Date());
            materialmaintenanObject.set("billstatus", "A");//单据状态
            materialmaintenanObject.set("org", org);//申请组织
            materialmaintenanObject.set("nckd_createorganiza", object.getDynamicObject("nckd_createorg"));//创建组织
            materialmaintenanObject.set("nckd_initiatingdepart", RequestContext.get().getOrgId());//发起部门
            materialmaintenanObject.set("nckd_applicant", RequestContext.get().getCurrUserId());//申请人
            materialmaintenanObject.set("nckd_materialmaintunit", "add");//单据维护类型：新增物料属性
            materialmaintenanObject.set("nckd_documenttype", billType);//单据类型：
            materialmaintenanObject.set("nckd_materialclassify", dynamicObject.getDynamicObject("nckd_materialclassify"));//物料分类
            materialmaintenanObject.set("nckd_materialname", dynamicObject.getString("nckd_materialname"));//物料名称
            materialmaintenanObject.set("nckd_specifications", dynamicObject.getString("nckd_specifications"));//规格
            materialmaintenanObject.set("nckd_model", dynamicObject.getString("nckd_model"));//型号
            materialmaintenanObject.set("nckd_baseunit", dynamicObject.getDynamicObject("nckd_baseunit"));//基本单位
            materialmaintenanObject.set("nckd_materialnumber", materialdynamicObject);//物料

            materialmaintenanObject.set("nckd_materialtype", dynamicObject.getString("nckd_materialtype"));//物料类型
            materialmaintenanObject.set("nckd_oldmaterialnumber", dynamicObject.getString("nckd_oldmaterialnumber"));//旧物料编码
            materialmaintenanObject.set("nckd_mnemoniccode", dynamicObject.getString("nckd_mnemoniccode"));//助记码
            materialmaintenanObject.set("nckd_remark", dynamicObject.getString("nckd_remark"));//描述
            materialmaintenanObject.set("nckd_materialrisk", dynamicObject.getString("nckd_materialrisk"));//物料危险性
            materialmaintenanObject.set("nckd_outsourcing", dynamicObject.getBoolean("nckd_outsourcing"));//可委外
            materialmaintenanObject.set("nckd_materialattribute", dynamicObject.getString("nckd_materialattribute"));//物料属性
            materialmaintenanObject.set("nckd_selfmaterialtype", dynamicObject.getString("nckd_selfmaterialtype"));//自制物料类型
            materialmaintenanObject.set("nckd_materialid", dynamicObject.getLong("id"));//物料申请单物料分录id
            OperationResult operationResult = OperationServiceHelper.executeOperate("save", "nckd_materialmaintenan", new DynamicObject[]{materialmaintenanObject}, OperateOption.create());
        }
    }
}
