package nckd.yanye.scm.plugin.operate;

import java.util.*;
import java.util.stream.Collectors;

import kd.bd.master.util.GroupStandardUtils;
import kd.bd.master.util.MasterDataUtil;
import kd.bd.master.vo.GroupStandMap;
import kd.bd.master.vo.GroupVo;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.DeleteServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.util.CollectionUtils;
import nckd.yanye.scm.common.utils.MaterialAttributeInformationUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author husheng
 * @date 2024-08-23 16:34
 * @description 物料申请单审核后创建物料维护单  nckd_materialrequest
 */
public class MaterialrequestAuditOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);

        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.addAll(this.billEntityType.getAllFields().keySet());
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);

        Arrays.stream(e.getDataEntities()).forEach(t -> {
            if (t.getBoolean("nckd_isgenerate")) {
                throw new KDBizException("物料申请单编号：" + t.getString("billno") + "已生成物料维护单，不允许重复生成");
            }
            t.set("nckd_isgenerate", true);
            //物料分录
            DynamicObjectCollection dynamicObjectCollection = t.getDynamicObjectCollection("nckd_materialentries");
            List<String> errorMsg = new ArrayList<>();
            for (DynamicObject dynamicObject : dynamicObjectCollection) {
                DynamicObject materialObject = setMaterialInfo(dynamicObject, t, errorMsg);
                dynamicObject.set("nckd_materialnumber", materialObject.getString("number"));
                /**
                 * 物料类型nckd_materialtype(1:物资、7:费用、8:资产)
                 * 物料属性nckd_materialattribute(1:自制、2：外购)
                 * 自制物料类型nckd_selfmaterialtype(1：产成品、2：半成品)
                 */
//                if ("1".equals(dynamicObject.getString("nckd_materialtype"))
//                        && "1".equals(dynamicObject.getString("nckd_materialattribute"))
//                        && "1".equals(dynamicObject.getString("nckd_selfmaterialtype"))) {
//                    getDynamicObject(dynamicObject, MaterialAttributeInformationUtils.finishedGoodsList, t, errorMsg, materialObject);
//                } else if ("1".equals(dynamicObject.getString("nckd_materialtype"))
//                        && "1".equals(dynamicObject.getString("nckd_materialattribute"))
//                        && "2".equals(dynamicObject.getString("nckd_selfmaterialtype"))) {
//                    getDynamicObject(dynamicObject, MaterialAttributeInformationUtils.semiFinishedList, t, errorMsg, materialObject);
//                } else if (Arrays.asList("1", "8").contains(dynamicObject.getString("nckd_materialtype"))
//                        && "2".equals(dynamicObject.getString("nckd_materialattribute"))) {
//                    getDynamicObject(dynamicObject, MaterialAttributeInformationUtils.outsourcingList, t, errorMsg, materialObject);
//                } else if ("7".equals(dynamicObject.getString("nckd_materialtype"))
//                        && "2".equals(dynamicObject.getString("nckd_materialattribute"))) {
//                    getDynamicObject(dynamicObject, MaterialAttributeInformationUtils.feeOutsourcingList, t, errorMsg, materialObject);
//                }
                getDynamicObject(dynamicObject, MaterialAttributeInformationUtils.list, t, errorMsg, materialObject);

                // 申请组织
                DynamicObject org = t.getDynamicObject("org");
                // 物料
                DynamicObject material = BusinessDataServiceHelper.loadSingle("bd_material", new QFilter[]{new QFilter("number", QCP.equals, dynamicObject.getString("nckd_materialnumber"))});

                // 生成物料属性信息
//                if ("1".equals(dynamicObject.getString("nckd_materialattribute"))
//                        && "1".equals(dynamicObject.getString("nckd_selfmaterialtype"))
//                        && "113".equals(org.getString("number"))) {
//                    //【物料属性】为‘自制’+【自制物料类型】‘产成品’+【申请组织】‘江西盐业包装有限公司’
//                    MaterialAttributeInformationUtils.defaultPurchaseInfo(org, material);// 采购基本信息
//                } else if ("1".equals(dynamicObject.getString("nckd_materialattribute"))
//                        && "2".equals(dynamicObject.getString("nckd_selfmaterialtype"))) {
//                    //【物料属性】为‘自制’+【自制物料类型】“半成品”
//                    MaterialAttributeInformationUtils.defaultPurchaseInfo(org, material);// 采购基本信息
//                    MaterialAttributeInformationUtils.defaultMarketInfo(org, material);// 销售基本信息
//                } else if ("2".equals(dynamicObject.getString("nckd_materialattribute"))) {
//                    //【物料属性】为‘外购’
//                    MaterialAttributeInformationUtils.defaultMarketInfo(org, material);// 销售基本信息
//                }

                // 核算信息设置存货类别并提交审核
                MaterialAttributeInformationUtils.setCheckInfoMaterialcategory(material, org);

                // 单位信息
                DynamicObjectCollection unitentryentity = dynamicObject.getDynamicObjectCollection("nckd_unitentryentity");
                unitentryentity.stream().forEach(d -> {
                    MaterialAttributeInformationUtils.saveBdMultimeasureunit(material, d);
                });

                // 物料分类
                DynamicObjectCollection groupstandard = material.getDynamicObjectCollection("entry_groupstandard");
//                GroupStandardHelper.saveGroupStandard(material, groupstandard, "bd_material");
                this.saveGroupStandard(material, groupstandard, "bd_material", dynamicObject.getDynamicObject("nckd_materialclassify"));
            }
            if (CollectionUtils.isNotEmpty(errorMsg)) {
                throw new KDBizException(errorMsg.stream().collect(Collectors.joining(",")));
            }
            SaveServiceHelper.update(t);
        });
    }

    /*
        保存物料分类
     */
    public void saveGroupStandard(DynamicObject dataEntity, DynamicObjectCollection groupstandards, String type, DynamicObject nckdMaterialclassify) {
        long masterId = (Long) dataEntity.getPkValue();
        List<DynamicObject> entityGroupDetails = new ArrayList();
        Set<Long> deleteIds = new HashSet();
        Long defGroupid = 0L;
        Iterator<DynamicObject> it = groupstandards.iterator();
        Map<Long, DynamicObject> newDynamicObject = new HashMap();
        HashSet standardSet = new HashSet();

        DynamicObject entityGroupDetail;
        Long defGroupOrgId;
        DynamicObject groupdetail;
        while (it.hasNext()) {
            entityGroupDetail = it.next();
            Long standard = GroupStandardUtils.getDataByType(entityGroupDetail.get("standardid"));
            standardSet.add(standard);
            defGroupOrgId = GroupStandardUtils.getDataByType(entityGroupDetail.get("groupid"));
            if (defGroupOrgId != 0L) {
                if ((GroupStandMap.getGroupstandmap().get(type)).getJbflbz().equals(standard)) {
                    defGroupid = defGroupOrgId;
                }

                Long groupCreatOrgId = MasterDataUtil.getDataPkByType(entityGroupDetail.getDynamicObject("groupid").get("createorg"));
                groupdetail = BusinessDataServiceHelper.newDynamicObject((GroupStandMap.getGroupstandmap().get(type)).getGroupdetailType());
                groupdetail.set("standard", standard);
                groupdetail.set("group", defGroupOrgId);
                groupdetail.set("createorg", groupCreatOrgId);
                groupdetail.set((GroupStandMap.getGroupstandmap().get(type)).getName(), masterId);
                newDynamicObject.put(standard, groupdetail);
            }
        }

        if (defGroupid != 0L) {
            dataEntity.set("group", defGroupid);
        }

        if (newDynamicObject == null || !newDynamicObject.containsKey((GroupStandMap.getGroupstandmap().get(type)).getJbflbz())) {
            entityGroupDetail = BusinessDataServiceHelper.newDynamicObject(((GroupVo) GroupStandMap.getGroupmap().get(type)).getGroupDetail());
            standardSet.add((GroupStandMap.getGroupstandmap().get(type)).getJbflbz());
            entityGroupDetail.set("standard", (GroupStandMap.getGroupstandmap().get(type)).getJbflbz());
            entityGroupDetail.set("group", nckdMaterialclassify);
            DynamicObject defGroup = BusinessDataServiceHelper.loadSingleFromCache(1L, type + "group", "createorg");
            defGroupOrgId = GroupStandardUtils.getDataByType(defGroup.get("createorg"));
            entityGroupDetail.set("createorg", defGroupOrgId);
            entityGroupDetail.set((GroupStandMap.getGroupstandmap().get(type)).getName(), dataEntity.getPkValue());
            newDynamicObject.put((GroupStandMap.getGroupstandmap().get(type)).getJbflbz(), entityGroupDetail);
        }

        DynamicObject[] load = BusinessDataServiceHelper.load((GroupStandMap.getGroupstandmap().get(type)).getGroupdetailType(), "id,standard,group," + (GroupStandMap.getGroupstandmap().get(type)).getName(), new QFilter[]{new QFilter((GroupStandMap.getGroupstandmap().get(type)).getName(), "=", dataEntity.getPkValue())});
        DynamicObject[] var19 = load;
        int var20 = load.length;

        for (int var21 = 0; var21 < var20; ++var21) {
            groupdetail = var19[var21];
            Long standard = GroupStandardUtils.getDataByType(groupdetail.get("standard"));
            if (standardSet != null && standardSet.contains(standard)) {
                deleteIds.add(groupdetail.getLong("id"));
            }
        }

        entityGroupDetails.addAll(newDynamicObject.values());
        SaveServiceHelper.update(dataEntity);
        if (deleteIds.size() > 0) {
            DeleteServiceHelper.delete((GroupStandMap.getGroupstandmap().get(type)).getGroupdetailType(), new QFilter[]{new QFilter("id", "in", deleteIds)});
        }

        if (entityGroupDetails.size() > 0) {
            SaveServiceHelper.save(entityGroupDetails.toArray(new DynamicObject[0]));
        }
    }

    /**
     * @param dynamicObject 物料分录
     * @param list          单据类型集合
     * @return
     */
    private void getDynamicObject(DynamicObject dynamicObject, List<String> list, DynamicObject object, List<String> errorMsg, DynamicObject materialdynamicObject) {
        if (CollectionUtils.isNotEmpty(errorMsg)) {
            return;
        }
        for (String billType : list) {
            DynamicObject materialmaintenanObject = BusinessDataServiceHelper.newDynamicObject("nckd_materialmaintenan");
            /**制单信息**/
            materialmaintenanObject.set("nckd_sourceid", object.getLong("id"));//源单id
            materialmaintenanObject.set("nckd_sourcenumber", object.getString("billno"));//源单编码
            materialmaintenanObject.set("creator", RequestContext.get().getCurrUserId());
            materialmaintenanObject.set("createtime", new Date());
            materialmaintenanObject.set("modifier", RequestContext.get().getCurrUserId());
            materialmaintenanObject.set("modifytime", new Date());
            materialmaintenanObject.set("billstatus", "A");//单据状态
            materialmaintenanObject.set("org", object.getDynamicObject("org"));//申请组织
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

            if ("1".equals(billType)) {
                // 生产基本信息
                materialmaintenanObject.set("nckd_mftunit", dynamicObject.getDynamicObject("nckd_baseunit"));//生产计量单位
                materialmaintenanObject.set("nckd_supplyorgunitid", object.getDynamicObject("org"));//供货库存组织
                String materialattri = null;
                if ("1".equals(dynamicObject.getString("nckd_materialattribute"))) {
                    materialattri = "10030";
                } else if ("2".equals(dynamicObject.getString("nckd_materialattribute"))) {
                    materialattri = "10040";
                }
                materialmaintenanObject.set("nckd_materialattri", materialattri);//物料属性
                materialmaintenanObject.set("nckd_bomversionrule", MaterialAttributeInformationUtils.getDefaultBOMRuleVer());//BOM版本规则
                materialmaintenanObject.set("nckd_issuemode", "11010");//领送料方式
                materialmaintenanObject.set("nckd_isbackflush", "A");//倒冲

                // 组织范围内属性页签
                DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle("nckd_orgpropertytab", new QFilter[]{new QFilter("nckd_entryentity.nckd_org", QCP.equals, object.getDynamicObject("org").getPkValue())});
                if (loadSingle != null) {
                    List<DynamicObject> collect = loadSingle.getDynamicObjectCollection("nckd_entryentity").stream().filter(d -> d.getDynamicObject("nckd_org").getPkValue() == object.getDynamicObject("org").getPkValue()).collect(Collectors.toList());
                    List<String> materialproperty = Arrays.stream(collect.get(0).getString("nckd_materialproperty").split(",")).filter(s -> StringUtils.isNotEmpty(s)).collect(Collectors.toList());
                    if (materialproperty.contains("2")) {
                        // 计划基本信息
                        materialmaintenanObject.set("nckd_createorg", object.getDynamicObject("org"));//计划信息创建组织
                        materialmaintenanObject.set("nckd_materialattr", materialattri);//物料属性
                        materialmaintenanObject.set("nckd_planmode", "D");//计划方式
                    }
                }
            } else if ("2".equals(billType)) {
                // 库存基本信息
                materialmaintenanObject.set("nckd_inventoryunit", dynamicObject.getDynamicObject("nckd_baseunit"));//库存单位
            } else if ("4".equals(billType)) {
                // 销售基本信息
                materialmaintenanObject.set("nckd_salesunit", dynamicObject.getDynamicObject("nckd_baseunit"));//销售单位
            } else if ("5".equals(billType)) {
                // 采购基本信息
                materialmaintenanObject.set("nckd_purchaseunit", dynamicObject.getDynamicObject("nckd_baseunit"));//采购单位
            }

            OperationResult operationResult = OperationServiceHelper.executeOperate("save", "nckd_materialmaintenan", new DynamicObject[]{materialmaintenanObject}, OperateOption.create());
            if (!operationResult.isSuccess()) {
                errorMsg.add("物料名称：" + dynamicObject.getString("nckd_materialname") + "新增物料维护单失败");
            } else {
                if(!"3".equals(billType)){
                    // 提交
                    OperationResult submitOperate = OperationServiceHelper.executeOperate("submit", "nckd_materialmaintenan", new DynamicObject[]{materialmaintenanObject}, OperateOption.create());
                    if (!submitOperate.isSuccess()) {
                        errorMsg.add("物料名称：" + dynamicObject.getString("nckd_materialname") + "提交物料维护单失败");
                    }
                }
            }
        }
    }

    /**
     * 生成物料信息
     *
     * @param dynamicObject     物料申请单-物料信息分录
     * @param mainDynamicObject 物料申请单-单据头
     */
    public DynamicObject setMaterialInfo(DynamicObject dynamicObject, DynamicObject mainDynamicObject, List<String> errorMsg) {
        if (CollectionUtils.isNotEmpty(errorMsg)) {
            return null;
        }
        DynamicObject materialObject = BusinessDataServiceHelper.newDynamicObject("bd_material");
        materialObject.set("createorg", mainDynamicObject.get("nckd_createorg"));//创建组织
        materialObject.set("name", dynamicObject.get("nckd_materialname"));//物料名称
        materialObject.set("modelnum", dynamicObject.get("nckd_specifications"));//规格
        materialObject.set("nckd_model", dynamicObject.get("nckd_model"));//型号
        materialObject.set("baseunit", dynamicObject.get("nckd_baseunit"));//基本单位
        materialObject.set("creator", RequestContext.get().getCurrUserId());//创建人
        materialObject.set("modifier", RequestContext.get().getCurrUserId());//修改人
        materialObject.set("createtime", new Date());//创建时间
        materialObject.set("modifytime", new Date());//修改时间
        materialObject.set("ctrlstrategy", "5");//控制策略 5:全局共享
        materialObject.set("status", "A");//状态 1:暂存
        materialObject.set("enable", "1");//使用状态 1:可用

        materialObject.set("group", dynamicObject.get("nckd_materialclassify"));//物料分组
        materialObject.set("useorg", mainDynamicObject.get("org"));//业务组织
        materialObject.set("materialtype", dynamicObject.get("nckd_materialtype"));//物料类型
        materialObject.set("isdisposable", false);//品类物料
        materialObject.set("helpcode", dynamicObject.get("nckd_mnemoniccode"));//助记码
        materialObject.set("oldnumber", dynamicObject.get("nckd_oldmaterialnumber"));//旧物料编码
        materialObject.set("description", dynamicObject.get("nckd_remark"));//描述

        materialObject.set("hazardous", dynamicObject.get("nckd_materialrisk"));//物料危险性
        materialObject.set("enableoutsource", dynamicObject.get("nckd_outsourcing"));//可委外

        materialObject.set("enablepur", false);//可采购
        materialObject.set("enablesale", false);//可销售
        materialObject.set("enableinv", false);//可库存
        materialObject.set("enableproduct", false);//可生产
        materialObject.set("enableinspect", false);//可质检
        materialObject.set("enableasset", false);//可资产
        materialObject.set("enabletrustee", false);//可受托
        materialObject.set("enableconsign", false);//可寄售
        materialObject.set("enablevmi", false);//可VMI
        materialObject.set("suite", false);//套件
        materialObject.set("completetag", false);//整机标识
        materialObject.set("piecemanage", false);//单件管理
        materialObject.set("sparepart", false);//备件
        materialObject.set("isuseauxpty", false);//启用辅助属性

        materialObject.set("farmproducts", false);//农产品
        materialObject.set("enablelot", false);//启用批号管理
        materialObject.set("enablelotsatinf", false);//批号启用附属信息
        materialObject.set("enableserial", false);//启用序列号管理
        materialObject.set("enablesersatinf", false);//序列号启用附属信息
        materialObject.set("enablelifemgr", false);//启用寿命管理
        materialObject.set("isoutputrequest", false);//仅出货必录序列号


        /**
         * 物料类型
         * 物资	1  【可采购】、【可销售】、【可库存】、【可生产】、【可质检】字段默认为‘是’；
         * 费用	7  【可采购】、【可销售】字段默认为‘是’；
         * 资产	8  【可采购】、【可销售】、【可库存】、【可生产】、【可质检】、【可资产】字段默认为‘是’。
         */
        switch (dynamicObject.getString("nckd_materialtype")) {
            case "1":
                materialObject.set("enablepur", true);//可采购
                materialObject.set("enablesale", true);//可销售
                materialObject.set("enableinv", true);//可库存
                materialObject.set("enableproduct", true);//可生产
                materialObject.set("enableinspect", true);//可质检
                break;
            case "7":
                materialObject.set("enablepur", true);//可采购
                materialObject.set("enablesale", true);//可销售
                break;
            case "8":
                materialObject.set("enablepur", true);//可采购
                materialObject.set("enablesale", true);//可销售
                materialObject.set("enableinv", true);//可库存
                materialObject.set("enableproduct", true);//可生产
                materialObject.set("enableinspect", true);//可质检
                materialObject.set("enableasset", true);//可资产
                break;
        }
        if (dynamicObject.getBoolean("nckd_producmaterial")) {
            materialObject.set("isuseauxpty", true);//启用辅助属性
            DynamicObjectCollection entryentity = materialObject.getDynamicObjectCollection("auxptyentry");
            //查询辅助属性matscmproapentry
            QFilter qFilter = new QFilter("flexid.formid", QCP.equals, "bd_flexauxprop");
            DynamicObject[] matscmproapentry = BusinessDataServiceHelper.load("bd_auxproperty", "id,number,name,valuetype,valuesource", qFilter.toArray());
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
            if (!submit.isSuccess()) {
                OperationServiceHelper.executeOperate("delete", "bd_material", new DynamicObject[]{materialObject}, OperateOption.create());
                errorMsg.add(materialObject.getString("name") + "对应的物料提交失败");
            } else {
                OperationResult audit = OperationServiceHelper.executeOperate("audit", "bd_material", new DynamicObject[]{materialObject}, OperateOption.create());
                if (!audit.isSuccess()) {
                    //已提交的数据需要先撤销提交再执行删除操作
                    OperationServiceHelper.executeOperate("unsubmit", "bd_material", new DynamicObject[]{materialObject}, OperateOption.create());
                    OperationServiceHelper.executeOperate("delete", "bd_material", new DynamicObject[]{materialObject}, OperateOption.create());
                    errorMsg.add(materialObject.getString("name") + "对应的物料发起审核失败");
                } else {
                    return materialObject;
                }
            }

        }
        return null;
    }
}
