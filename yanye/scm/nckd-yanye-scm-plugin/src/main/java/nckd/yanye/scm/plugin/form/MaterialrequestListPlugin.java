package nckd.yanye.scm.plugin.form;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.EntityType;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.list.BillList;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.basedata.BaseDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class MaterialrequestListPlugin extends AbstractListPlugin {

    /**
     * 【物料类型】为‘物资’+【物料属性】为‘自制’+【自制物料类型】“产成品”时
     * 集合中的参数代表单据类型
     */
    private List<String> finishedGoodsList = Arrays.asList("1", "2", "3", "4");
    /**
     * 【物料类型】为‘物资’+【物料属性】为‘自制’+【自制物料类型】“半成品”时
     */
    private List<String> semiFinishedList = Arrays.asList("1", "2", "3");
    /**
     * 【物料类型】为‘物资’或‘资产’+【物料属性】为‘外购’时
     */
    private List<String> outsourcingList = Arrays.asList("1", "2", "3", "5");
    /**
     * 【物料类型】为‘费用’+【物料属性】为‘外购’时
     */
    private List<String> feeOutsourcingList = Arrays.asList("3", "5");


    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners("nckd_material");
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        String operateKey = afterDoOperationEventArgs.getOperateKey();
        //生成物料维护单
        if (StringUtils.equals("material", operateKey)) {
            BillList billlistap = this.getView().getControl("billlistap");
            ListSelectedRowCollection selectedRows = billlistap.getSelectedRows();
            EntityType entityType = billlistap.getEntityType();
            //获取选中行pkid
            Object[] primaryKeyValues = selectedRows.getPrimaryKeyValues();
            //构造DynamicObjectCollection 存储所有的物料分录
//            DynamicObjectCollection objects = new DynamicObjectCollection();
            //获取完整数据（物料申请单）
            DynamicObject[] bussProcessOrderArr = BusinessDataServiceHelper.load(primaryKeyValues, entityType);
//            Map<Long, DynamicObject> map = new HashMap<>();
            Arrays.stream(bussProcessOrderArr).forEach(t -> {
                if (t.getBoolean("nckd_isgenerate")) {
                    this.getView().showErrorNotification("物料申请单编号：" + t.getString("billno") + "已生成物料维护单，不允许重复生成");
                    return;
                }
                t.set("nckd_isgenerate", true);
//                map.put(t.getLong("id"), t);
                //物料分录
                DynamicObjectCollection dynamicObjectCollection = t.getDynamicObjectCollection("nckd_materialentries");
//                objects.addAll(dynamicObjectCollection);
                List<String> errorMsg = new ArrayList<>();
                for (DynamicObject dynamicObject : dynamicObjectCollection) {
                    setMaterialInfo(dynamicObject,t,errorMsg);
                    /**
                     * 物料类型nckd_materialtype(1:物资、7:费用、8:资产)
                     * 物料属性nckd_materialattribute(1:自制、2：外购)
                     * 自制物料类型nckd_selfmaterialtype(1：产成品、2：半成品)
                     */
                    if ("1".equals(dynamicObject.getString("nckd_materialtype"))
                            && "1".equals(dynamicObject.getString("nckd_materialattribute"))
                            && "1".equals(dynamicObject.getString("nckd_selfmaterialtype"))) {
                        getDynamicObject(dynamicObject, finishedGoodsList, t,errorMsg);
                    } else if ("1".equals(dynamicObject.getString("nckd_materialtype"))
                            && "1".equals(dynamicObject.getString("nckd_materialattribute"))
                            && "2".equals(dynamicObject.getString("nckd_selfmaterialtype"))) {
                        getDynamicObject(dynamicObject, semiFinishedList, t,errorMsg);
                    } else if (Arrays.asList("1", "8").contains(dynamicObject.getString("nckd_materialtype"))
                            && "2".equals(dynamicObject.getString("nckd_materialattribute"))) {
                        getDynamicObject(dynamicObject, outsourcingList, t,errorMsg);
                    } else if ("7".equals(dynamicObject.getString("nckd_materialtype"))
                            && "2".equals(dynamicObject.getString("nckd_materialattribute"))) {
                        getDynamicObject(dynamicObject, feeOutsourcingList, t,errorMsg);
                    }

                    // 申请组织
                    DynamicObject org = t.getDynamicObject("org");

                    // 生成物料属性信息
                    if ("1".equals(dynamicObject.getString("nckd_materialattribute"))
                            && "1".equals(dynamicObject.getString("nckd_selfmaterialtype"))
                            && "113".equals(org.getString("number"))) {
                        //【物料属性】为‘自制’+【自制物料类型】‘产成品’+【申请组织】‘江西盐业包装有限公司’
                        this.purchaseInfo(dynamicObject, org,errorMsg);// 采购基本信息
                    } else if ("1".equals(dynamicObject.getString("nckd_materialattribute"))
                            && "2".equals(dynamicObject.getString("nckd_selfmaterialtype"))) {
                        //【物料属性】为‘自制’+【自制物料类型】“半成品”
                        this.purchaseInfo(dynamicObject, org,errorMsg);// 采购基本信息
                        this.marketInfo(dynamicObject, org,errorMsg);// 销售基本信息
                    } else if ("2".equals(dynamicObject.getString("nckd_materialattribute"))) {
                        //【物料属性】为‘外购’
                        this.marketInfo(dynamicObject, org,errorMsg);// 销售基本信息
                    }
                }
                if (CollectionUtils.isNotEmpty(errorMsg)){
                    this.getView().showErrorNotification(errorMsg.stream().collect(Collectors.joining(",")));
                    return;
                }
                SaveServiceHelper.update(t);
                this.getView().showSuccessNotification("物料维护单创建成功成功");
            });
        }
    }

    /**
     * 销售基本信息
     */
    private void marketInfo(DynamicObject dynamicObject, DynamicObject org,List<String> errorMsg) {
        if (CollectionUtils.isNotEmpty(errorMsg)){
            return;
        }
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_materialsalinfo");

        DynamicObject material = this.getMaterial(dynamicObject);

        // 物料
        newDynamicObject.set("masterid", material);
        // 物料
        newDynamicObject.set("material", material);
        // 销售信息创建组织
        newDynamicObject.set("createorg", org);
        // 销售单位
        newDynamicObject.set("salesunit", material.getDynamicObject("baseunit"));
        // 销售信息数据状态
        newDynamicObject.set("status", "A");
        // 销售信息控制策略
        newDynamicObject.set("ctrlstrategy", this.getCtrlStrgy(org));
        // 销售信息使用状态
        newDynamicObject.set("enable", "1");
        // 创建人
        newDynamicObject.set("creator", RequestContext.get().getCurrUserId());

        OperationResult operationResult = SaveServiceHelper.saveOperate("bd_materialsalinfo", new DynamicObject[]{newDynamicObject}, OperateOption.create());
        if (!operationResult.isSuccess()){
            errorMsg.add("物料名称："+dynamicObject.getString("nckd_materialname")+"后台默认新增生成采购基本信息失败");
        }
    }

    /**
     * 采购基本信息
     */
    private void purchaseInfo(DynamicObject dynamicObject, DynamicObject org,List<String> errorMsg) {
        if (CollectionUtils.isNotEmpty(errorMsg)){
            return;
        }
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_materialpurchaseinfo");

        DynamicObject material = this.getMaterial(dynamicObject);

        // 物料
        newDynamicObject.set("masterid", material);
        // 物料
        newDynamicObject.set("material", material);
        // 采购信息创建组织
        newDynamicObject.set("createorg", org);
        // 采购单位
        newDynamicObject.set("purchaseunit", material.getDynamicObject("baseunit"));
        // 采购信息数据状态
        newDynamicObject.set("status", "A");
        // 采购信息控制策略
        newDynamicObject.set("ctrlstrategy", this.getCtrlStrgy(org));
        // 采购信息使用状态
        newDynamicObject.set("enable", "1");
        // 创建人
        newDynamicObject.set("creator", RequestContext.get().getCurrUserId());

        OperationResult operationResult = SaveServiceHelper.saveOperate("bd_materialpurchaseinfo", new DynamicObject[]{newDynamicObject}, OperateOption.create());
        if (!operationResult.isSuccess()){
            errorMsg.add("物料名称："+dynamicObject.getString("nckd_materialname")+"后台默认新增生成采购基本信息失败");
        }
    }


    // 获取物料
    private DynamicObject getMaterial(DynamicObject dynamicObject) {
        String nckdMaterialnumber = dynamicObject.getString("nckd_materialnumber");
        DynamicObject bd_material = BusinessDataServiceHelper.loadSingle("bd_material", new QFilter[]{new QFilter("number", QCP.equals, nckdMaterialnumber)});
        return bd_material;
    }

    // 获取控制策略
    private String getCtrlStrgy(DynamicObject org) {
        String ctrlStrgy = BaseDataServiceHelper.getBdCtrlStrgy("bd_materialmftinfo", String.valueOf(org.getPkValue()));
        if (ctrlStrgy != null && ctrlStrgy.length() > 0) {
            String[] ctrlStrgys = ctrlStrgy.split(",");
            if (ctrlStrgys.length > 1) {
                String[] var3 = ctrlStrgys;
                int var4 = ctrlStrgys.length;

                for (int var5 = 0; var5 < var4; ++var5) {
                    String ctr = var3[var5];
                    if (kd.bos.dataentity.utils.StringUtils.isNotEmpty(ctr)) {
                        return ctr;
                    }
                }
            }
        }

        return ctrlStrgy;
    }

    /**
     * @param dynamicObject 物料分录
     * @param list          单据类型集合
     * @return
     */
    private void getDynamicObject(DynamicObject dynamicObject, List<String> list, DynamicObject object,List<String> errorMsg) {
        if (CollectionUtils.isNotEmpty(errorMsg)){
            return;
        }
        for (String billType : list) {
//            DynamicObject object = map.get(dynamicObject.getLong("nckd_fid"));
            DynamicObject materialmaintenanObject = BusinessDataServiceHelper.newDynamicObject("nckd_materialmaintenan");
            /**制单信息**/
            materialmaintenanObject.set("creator", RequestContext.get().getCurrUserId());
            materialmaintenanObject.set("createtime", new Date());
            materialmaintenanObject.set("modifier", RequestContext.get().getCurrUserId());
            materialmaintenanObject.set("modifytime", new Date());
            materialmaintenanObject.set("billstatus", "A");//单据状态
            materialmaintenanObject.set("org", object.getDynamicObject("org"));//申请组织
            materialmaintenanObject.set("nckd_materialmaintunit", "add");//单据维护类型：新增物料属性
            materialmaintenanObject.set("nckd_documenttype", billType);//单据类型：
            materialmaintenanObject.set("nckd_materialclassify", dynamicObject.getDynamicObject("nckd_materialclassify"));//物料分类
            materialmaintenanObject.set("nckd_materialname", dynamicObject.getString("nckd_materialname"));//物料名称
            materialmaintenanObject.set("nckd_specifications", dynamicObject.getString("nckd_specifications"));//规格
            materialmaintenanObject.set("nckd_model", dynamicObject.getString("nckd_model"));//型号
            materialmaintenanObject.set("nckd_baseunit", dynamicObject.getDynamicObject("nckd_baseunit"));//基本单位

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
            if (!operationResult.isSuccess()){
                errorMsg.add("物料名称："+dynamicObject.getString("nckd_materialname")+"新增物料维护单失败");

            }
        }
    }

    /**
     * 生成物料信息
     * @param dynamicObject 物料申请单-物料信息分录
     * @param mainDynamicObject 物料申请单-单据头
     */
    public void setMaterialInfo(DynamicObject dynamicObject,DynamicObject mainDynamicObject,List<String> errorMsg){
        if (CollectionUtils.isNotEmpty(errorMsg)){
            return;
        }
        DynamicObject materialObject = BusinessDataServiceHelper.newDynamicObject("bd_material");
        materialObject.set("createorg",mainDynamicObject.get("nckd_createorg"));//创建组织
        materialObject.set("name",dynamicObject.get("nckd_materialname"));//物料名称
        materialObject.set("modelnum",dynamicObject.get("nckd_specifications"));//规格
        materialObject.set("modelnum",dynamicObject.get("nckd_model"));//型号
        materialObject.set("baseunit",dynamicObject.get("nckd_baseunit"));//基本单位
        materialObject.set("creator",RequestContext.get().getCurrUserId());//创建人
        materialObject.set("modifier",RequestContext.get().getCurrUserId());//修改人
        materialObject.set("createtime",new Date());//创建时间
        materialObject.set("modifytime", new Date());//修改时间
        materialObject.set("ctrlstrategy", "5");//控制策略 5:全局共享
        materialObject.set("status", "A");//状态 1:暂存
        materialObject.set("enable", "1");//使用状态 1:可用

        materialObject.set("group",dynamicObject.get("nckd_materialclassify"));//物料分组
        materialObject.set("useorg",mainDynamicObject.get("org"));//业务组织
        materialObject.set("materialtype",dynamicObject.get("nckd_materialtype"));//物料类型
        materialObject.set("isdisposable",false);//品类物料
        materialObject.set("helpcode",dynamicObject.get("nckd_mnemoniccode"));//助记码
        materialObject.set("oldnumber",dynamicObject.get("nckd_oldmaterialnumber"));//旧物料编码
        materialObject.set("description",dynamicObject.get("nckd_remark"));//描述

        materialObject.set("hazardous",dynamicObject.get("nckd_materialrisk"));//物料危险性
        materialObject.set("enableoutsource",dynamicObject.get("nckd_outsourcing"));//可委外

        materialObject.set("enablepur",false);//可采购
        materialObject.set("enablesale",false);//可销售
        materialObject.set("enableinv",false);//可库存
        materialObject.set("enableproduct",false);//可生产
        materialObject.set("enableinspect",false);//可质检
        materialObject.set("enableasset",false);//可资产
        materialObject.set("enabletrustee",false);//可受托
        materialObject.set("enableconsign",false);//可寄售
        materialObject.set("enablevmi",false);//可VMI
        materialObject.set("suite",false);//套件
        materialObject.set("completetag",false);//整机标识
        materialObject.set("piecemanage",false);//单件管理
        materialObject.set("sparepart",false);//备件
        materialObject.set("isuseauxpty",false);//启用辅助属性

        materialObject.set("farmproducts",false);//农产品
        materialObject.set("enablelot",false);//启用批号管理
        materialObject.set("enablelotsatinf",false);//批号启用附属信息
        materialObject.set("enableserial",false);//启用序列号管理
        materialObject.set("enablesersatinf",false);//序列号启用附属信息
        materialObject.set("enablelifemgr",false);//启用寿命管理
        materialObject.set("isoutputrequest",false);//仅出货必录序列号


        /**
         * 物料类型
         * 物资	1  【可采购】、【可销售】、【可库存】、【可生产】、【可质检】字段默认为‘是’；
         * 费用	7  【可采购】、【可销售】字段默认为‘是’；
         * 资产	8  【可采购】、【可销售】、【可库存】、【可生产】、【可质检】、【可资产】字段默认为‘是’。
         */
        switch (dynamicObject.getString("nckd_materialtype")) {
            case "1":
                materialObject.set("enablepur",true);//可采购
                materialObject.set("enablesale",true);//可销售
                materialObject.set("enableinv",true);//可库存
                materialObject.set("enableproduct",true);//可生产
                materialObject.set("enableinspect",true);//可质检
                break;
            case "7":
                materialObject.set("enablepur",true);//可采购
                materialObject.set("enablesale",true);//可销售
                break;
            case "8":
                materialObject.set("enablepur",true);//可采购
                materialObject.set("enablesale",true);//可销售
                materialObject.set("enableinv",true);//可库存
                materialObject.set("enableproduct",true);//可生产
                materialObject.set("enableinspect",true);//可质检
                materialObject.set("enableasset",true);//可资产
                break;
        }
        if (dynamicObject.getBoolean("nckd_producmaterial")){
            materialObject.set("isuseauxpty",true);//启用辅助属性
            DynamicObjectCollection entryentity = materialObject.getDynamicObjectCollection("auxptyentry");
            //查询辅助属性matscmproapentry
            QFilter qFilter = new QFilter("flexid.formid", QCP.equals,"bd_flexauxprop");
            DynamicObject[] matscmproapentry = BusinessDataServiceHelper.load("bd_auxproperty","id,number,name,valuetype,valuesource", qFilter.toArray());
            Arrays.stream(matscmproapentry).forEach(dynamicObject1 -> {
                DynamicObject addNew = entryentity.addNew();
                // 辅助属性
                addNew.set("auxpty", dynamicObject1);
                // 影响计划
                addNew.set("isaffectplan", false);
            });

        }
        //调用保存操作
        OperationResult saveOperationResult = OperationServiceHelper.executeOperate("save", "bd_material", new DynamicObject[]{materialObject}, OperateOption.create());
        if (!saveOperationResult.isSuccess()) {
            errorMsg.add(materialObject.getString("name") + "对应的物料新增失败");
        } else {
            //提交审批
            OperationResult submit = OperationServiceHelper.executeOperate("submit", "bd_material", new DynamicObject[]{materialObject}, OperateOption.create());
            if(!submit.isSuccess()){
                OperationServiceHelper.executeOperate("delete", "bd_material", new DynamicObject[]{materialObject}, OperateOption.create());
                errorMsg.add(materialObject.getString("name") + "对应的物料提交失败");
            }else {
                OperationResult audit = OperationServiceHelper.executeOperate("audit", "bd_material", new DynamicObject[]{materialObject}, OperateOption.create());
                if(!audit.isSuccess()){
                    //已提交的数据需要先撤销提交再执行删除操作
                    OperationServiceHelper.executeOperate("unsubmit", "bd_material", new DynamicObject[]{materialObject}, OperateOption.create());
                    OperationServiceHelper.executeOperate("delete", "bd_material", new DynamicObject[]{materialObject}, OperateOption.create());
                    errorMsg.add(materialObject.getString("name") + "对应的物料发起审核失败");
                }
            }

        }
    }
}
