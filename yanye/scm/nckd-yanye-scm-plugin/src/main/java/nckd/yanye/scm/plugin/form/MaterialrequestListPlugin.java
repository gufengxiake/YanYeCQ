package nckd.yanye.scm.plugin.form;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDBizException;
import kd.bos.form.*;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.bos.form.operate.FormOperate;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.scm.common.utils.MaterialAttributeInformationUtils;
import org.apache.commons.lang.StringUtils;

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
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        super.beforeDoOperation(args);

        FormOperate formOperate = (FormOperate) args.getSource();
        String operateKey = formOperate.getOperateKey();
        if ("submit".equals(operateKey)) {
            boolean flag = this.saveOrSubmitVerify();
            if (flag) {
                this.showMessage(args, operateKey);
            }
        }
    }

    private Boolean saveOrSubmitVerify() {
        ListSelectedRowCollection selectedRows = this.getSelectedRows();
        DynamicObjectCollection materialrequest = QueryServiceHelper.query("nckd_materialrequest", "id,nckd_materialentries.nckd_materialname,nckd_materialentries.nckd_specifications,nckd_materialentries.nckd_model", new QFilter[]{new QFilter("nckd_materialentries.id", QCP.in, selectedRows.getEntryPrimaryKeyValues())});

        final Boolean[] flag = {false};
        materialrequest.stream().forEach(m -> {
            // 校验物料名称、规格、型号，组合唯一性
            // 物料
            QFilter qFilter1 = new QFilter("name", QCP.equals, m.getString("nckd_materialentries.nckd_materialname"))
                    .and("modelnum", QCP.equals, m.getString("nckd_materialentries.nckd_specifications"))
                    .and("nckd_model", QCP.equals, m.getString("nckd_materialentries.nckd_model"));
            boolean exists1 = QueryServiceHelper.exists("bd_material", qFilter1.toArray());

            // 物料申请单
            QFilter qFilter2 = new QFilter("nckd_materialentries.nckd_materialname", QCP.equals, m.getString("nckd_materialentries.nckd_materialname"))
                    .and("nckd_materialentries.nckd_specifications", QCP.equals, m.getString("nckd_materialentries.nckd_specifications"))
                    .and("nckd_materialentries.nckd_model", QCP.equals, m.getString("nckd_materialentries.nckd_model"));
            int size1 = QueryServiceHelper.query("nckd_materialrequest", "id", qFilter2.toArray()).size();

            // 物料维护单
            QFilter qFilter3 = new QFilter("nckd_altermaterialname", QCP.equals, m.getString("nckd_materialentries.nckd_materialname"))
                    .and("nckd_alterspecificat", QCP.equals, m.getString("nckd_materialentries.nckd_specifications"))
                    .and("nckd_altermodel", QCP.equals, m.getString("nckd_materialentries.nckd_model"))
                    .and("nckd_materialmaintunit", QCP.equals, "updateinfo");
            boolean exists2 = QueryServiceHelper.exists("nckd_materialmaintenan", qFilter3.toArray());

            // 物料名称、规格、型号，需要组合校验唯一性
            if (exists1 || size1 > 1 || exists2) {
                throw new KDBizException("物料名称、规格、型号，组合需唯一!");
            }

            // 校验物料名称唯一性
            // 物料
            QFilter qFilter4 = new QFilter("name", QCP.equals, m.getString("nckd_materialentries.nckd_materialname"));
            boolean exists3 = QueryServiceHelper.exists("bd_material", qFilter4.toArray());

            // 物料申请单
            QFilter qFilter5 = new QFilter("nckd_materialentries.nckd_materialname", QCP.equals, m.getString("nckd_materialentries.nckd_materialname"));
            int size2 = QueryServiceHelper.query("nckd_materialrequest", "id", qFilter5.toArray()).size();

            // 物料维护单
            QFilter qFilter6 = new QFilter("nckd_altermaterialname", QCP.equals, m.getString("nckd_materialentries.nckd_materialname"))
                    .and("nckd_materialmaintunit", QCP.equals, "updateinfo");
            boolean exists4 = QueryServiceHelper.exists("nckd_materialmaintenan", qFilter6.toArray());

            // 物料名称、规格、型号，需要组合校验唯一性
            if (exists3 || size2 > 1 || exists4) {
                flag[0] = true;
            }
        });

        return flag[0];
    }

    private void showMessage(BeforeDoOperationEventArgs args, String operateKey) {
        // 判断是否处理过
        String isDealed = this.getView().getPageCache().get("isDealed");
        if (!"true".equals(isDealed)) {
            // 取消原来的操作
            args.setCancel(true);
            // 在用户点击确认框上的按钮后，系统会调用confirmCallBack方法
            ConfirmCallBackListener confirmCallBackListener = new ConfirmCallBackListener(operateKey, this);
            // 设置页面确认框，参数为：标题，选项框类型，回调监听
            this.getView().showConfirm("物料名称重复！", MessageBoxOptions.YesNo, confirmCallBackListener);
            // 只执行一次
            this.getView().getPageCache().put("isDealed", "true");
        }
    }

    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        super.confirmCallBack(messageBoxClosedEvent);

        String callBackId = messageBoxClosedEvent.getCallBackId();
        //判断回调参数id
        if ("submit".equals(callBackId)) {
            if (MessageBoxResult.Yes.equals(messageBoxClosedEvent.getResult())) {
                this.getView().invokeOperation(callBackId);
            } else if (MessageBoxResult.No.equals(messageBoxClosedEvent.getResult())) {
                // 点击否也清除
                this.getView().getPageCache().remove("isDealed");
            }
        }
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);

        String itemKey = evt.getItemKey();
        if (itemKey.equals("nckd_materialcontinue")) {
            ListSelectedRowCollection selectedRows = this.getSelectedRows();
//            if (selectedRows.size() == 0) {
//                throw new KDBizException("请选择数据!");
//            }

            Object[] keyValues = selectedRows.getPrimaryKeyValues();
            QFilter qFilter = new QFilter("id", QCP.in, keyValues)
                    .and("billstatus", QCP.not_equals, "C");
            boolean exists = QueryServiceHelper.exists("nckd_materialrequest", qFilter.toArray());
            if (exists) {
                throw new KDBizException("只能选择已审核的数据!");
            }

            // 获取选择的数据
            DynamicObjectCollection materialrequest = QueryServiceHelper.query("nckd_materialrequest", "org,nckd_materialentries.nckd_materialnumber", new QFilter[]{new QFilter("nckd_materialentries.id", QCP.in, selectedRows.getEntryPrimaryKeyValues())});
//            List<Long> orgIds = null;
            List<String> materialnumberList = null;
            if (materialrequest.size() > 0) {
//                orgIds = materialrequest.stream().map(t -> t.getLong("org")).distinct().collect(Collectors.toList());
                materialnumberList = materialrequest.stream().map(t -> t.getString("nckd_materialentries.nckd_materialnumber")).distinct().collect(Collectors.toList());
            }

            FormShowParameter parameter = new FormShowParameter();
            parameter.setFormId("nckd_selectorg");
            parameter.setCloseCallBack(new CloseCallBack(this, "materialrequest"));
            parameter.getOpenStyle().setShowType(ShowType.Modal);
//            parameter.setCustomParam("orgIds", orgIds);
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
            DynamicObject department = (DynamicObject) map.get("department");
            DynamicObjectCollection entryentity = (DynamicObjectCollection) map.get("entryentity");

            try {
                entryentity.stream().forEach(entity -> {
                    DynamicObject bdMaterial = BusinessDataServiceHelper.loadSingle(entity.getDynamicObject("nckd_material").getLong("id"), "bd_material");

                    // 判断是不是通过物料申请单生成的物料
//                QFilter filter = new QFilter("nckd_materialentries.nckd_materialnumber", QCP.equals, bdMaterial.getString("number"));
//                DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("nckd_materialrequest", filter.toArray());
//                if (dynamicObject != null) {
//                    DynamicObject object = dynamicObject.getDynamicObjectCollection("nckd_materialentries").stream()
//                            .filter(t -> t.getString("nckd_materialnumber").equals(bdMaterial.getString("number")))
//                            .findFirst().orElse(null);
//
//                    getDynamicObject(object, MaterialAttributeInformationUtils.list, dynamicObject, org, bdMaterial, entity);

                    // 生成物料属性信息
//                    if ("1".equals(object.getString("nckd_materialattribute"))
//                            && "1".equals(object.getString("nckd_selfmaterialtype"))
//                            && "113".equals(org.getString("number"))) {
//                        //【物料属性】为‘自制’+【自制物料类型】‘产成品’+【申请组织】‘江西盐业包装有限公司’
//                        MaterialAttributeInformationUtils.defaultPurchaseInfo(org, bdMaterial);// 采购基本信息
//                    } else if ("1".equals(object.getString("nckd_materialattribute"))
//                            && "2".equals(object.getString("nckd_selfmaterialtype"))) {
//                        //【物料属性】为‘自制’+【自制物料类型】“半成品”
//                        MaterialAttributeInformationUtils.defaultPurchaseInfo(org, bdMaterial);// 采购基本信息
//                        MaterialAttributeInformationUtils.defaultMarketInfo(org, bdMaterial);// 销售基本信息
//                    } else if ("2".equals(object.getString("nckd_materialattribute"))) {
//                        //【物料属性】为‘外购’
//                        MaterialAttributeInformationUtils.defaultMarketInfo(org, bdMaterial);// 销售基本信息
//                    }
//                } else {
                    // 手动新增的物料
                    getDynamicObjectForBdMaterial(MaterialAttributeInformationUtils.list, org, bdMaterial, department, entity);
//                }

                    // 核算信息设置存货类别并提交审核
                    MaterialAttributeInformationUtils.setCheckInfoMaterialcategory(bdMaterial, org);
                });

                this.getView().showSuccessNotification("物料维护单生成成功！");
            } catch (Exception e) {
                rollback(org, entryentity);
                throw new KDBizException(e.getLocalizedMessage());
            }
        }
    }

    private void rollback(DynamicObject org, DynamicObjectCollection entryentity) {
        entryentity.stream().forEach(entity -> {
            // 获取物料维护单
            QFilter qFilter = new QFilter("org", QCP.equals, org.getLong("id"))
                    .and("nckd_materialnumber", QCP.equals, entity.getDynamicObject("nckd_material").getLong("id"));
            DynamicObject[] materialmaintenan = BusinessDataServiceHelper.load("nckd_materialmaintenan", "id,billno,billstatus", qFilter.toArray());
            Arrays.stream(materialmaintenan).forEach(n -> {
                QFilter filter = new QFilter("masterid", QCP.equals, entity.getDynamicObject("nckd_material").getLong("id"))
                        .and("createorg", QCP.equals, org.getLong("id"));
                // 物料核算信息
                DynamicObject materialcalinfo = BusinessDataServiceHelper.loadSingle("bd_materialcalinfo", filter.toArray());
                if (materialcalinfo != null) {
                    MaterialAttributeInformationUtils.deleteEntity(materialcalinfo, materialcalinfo.getString("status"));
                }

                MaterialAttributeInformationUtils.deleteEntity(n, n.getString("billstatus"));
            });
        });
    }

    /**
     * @param dynamicObject 物料分录
     * @param list          单据类型集合
     * @return
     */
    private void getDynamicObject(DynamicObject dynamicObject, List<String> list, DynamicObject object, DynamicObject org, DynamicObject materialdynamicObject, DynamicObject entity) {
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

            materialmaintenanObject.set("nckd_sales", entity.get("nckd_sales"));//销售
            materialmaintenanObject.set("nckd_selfmade", entity.get("nckd_selfmade"));//自制
            materialmaintenanObject.set("nckd_purchase", entity.get("nckd_purchase"));//采购
            materialmaintenanObject.set("nckd_enablelot", entity.get("nckd_enablelot"));//启用批号管理
            materialmaintenanObject.set("nckd_lotcoderule", entity.get("nckd_lotcoderule"));//批号规则

            if ("1".equals(billType)) {
                // 生产基本信息
                materialmaintenanObject.set("nckd_mftunit", dynamicObject.getDynamicObject("nckd_baseunit"));//生产计量单位
                materialmaintenanObject.set("nckd_supplyorgunitid", org);//供货库存组织
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
                DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle("nckd_orgpropertytab", new QFilter[]{new QFilter("nckd_entryentity.nckd_org", QCP.equals, org.getPkValue())});
                if (loadSingle != null) {
                    List<DynamicObject> collect = loadSingle.getDynamicObjectCollection("nckd_entryentity").stream().filter(d -> Objects.equals(d.getDynamicObject("nckd_org").getPkValue(), org.getPkValue())).collect(Collectors.toList());
                    List<String> materialproperty = Arrays.stream(collect.get(0).getString("nckd_materialproperty").split(",")).filter(s -> StringUtils.isNotEmpty(s)).collect(Collectors.toList());
                    if (materialproperty.contains("2")) {
                        // 计划基本信息
                        materialmaintenanObject.set("nckd_createorg", org);//计划信息创建组织
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

//            OperationResult operationResult = SaveServiceHelper.saveOperate("nckd_materialmaintenan", new DynamicObject[]{materialmaintenanObject}, OperateOption.create());
////            OperationResult operationResult = OperationServiceHelper.executeOperate("save", "nckd_materialmaintenan", new DynamicObject[]{materialmaintenanObject}, OperateOption.create());
//            if (operationResult.isSuccess()) {
                if (!"3".equals(billType)) {
//                    List<Object> successPkIds = operationResult.getSuccessPkIds();
//                    DynamicObject single = BusinessDataServiceHelper.loadSingle("nckd_materialmaintenan", new QFilter[]{new QFilter("id", QCP.equals, successPkIds.get(0))});

                    // 提交
                    OperationResult submitOperate = OperationServiceHelper.executeOperate("submit", "nckd_materialmaintenan", new DynamicObject[]{materialmaintenanObject}, OperateOption.create());
                }else {
                    OperationResult operationResult = SaveServiceHelper.saveOperate("nckd_materialmaintenan", new DynamicObject[]{materialmaintenanObject}, OperateOption.create());
                }
//            }
        }
    }

    private void getDynamicObjectForBdMaterial(List<String> list, DynamicObject org, DynamicObject materialdynamicObject, DynamicObject department, DynamicObject entity) {
        // 江盐集团
        QFilter qFilter = new QFilter("number", QCP.equals, "1");
        DynamicObject bosAdminorg = BusinessDataServiceHelper.loadSingle("bos_adminorg", new QFilter[]{qFilter});

        // 物料分类
        QFilter filter = new QFilter("material", QCP.equals, materialdynamicObject.get("id"));
        DynamicObject materialgroupdetail = BusinessDataServiceHelper.loadSingle("bd_materialgroupdetail", filter.toArray());

        for (String billType : list) {
            DynamicObject materialmaintenanObject = BusinessDataServiceHelper.newDynamicObject("nckd_materialmaintenan");
            /**制单信息**/
            materialmaintenanObject.set("creator", RequestContext.get().getCurrUserId());
            materialmaintenanObject.set("createtime", new Date());
            materialmaintenanObject.set("modifier", RequestContext.get().getCurrUserId());
            materialmaintenanObject.set("modifytime", new Date());
            materialmaintenanObject.set("billstatus", "A");//单据状态
            materialmaintenanObject.set("org", org);//申请组织
            materialmaintenanObject.set("nckd_createorganiza", bosAdminorg.getPkValue());//创建组织
            materialmaintenanObject.set("nckd_initiatingdepart", department);//发起部门
            materialmaintenanObject.set("nckd_applicant", RequestContext.get().getCurrUserId());//申请人
            materialmaintenanObject.set("nckd_materialmaintunit", "add");//单据维护类型：新增物料属性
            materialmaintenanObject.set("nckd_documenttype", billType);//单据类型：
            materialmaintenanObject.set("nckd_materialclassify", materialgroupdetail.getDynamicObject("group"));//物料分类
            materialmaintenanObject.set("nckd_materialname", materialdynamicObject.getString("name"));//物料名称
            materialmaintenanObject.set("nckd_specifications", materialdynamicObject.getString("modelnum"));//规格
            materialmaintenanObject.set("nckd_model", materialdynamicObject.getString("nckd_model"));//型号
            materialmaintenanObject.set("nckd_baseunit", materialdynamicObject.getDynamicObject("baseunit"));//基本单位
            materialmaintenanObject.set("nckd_materialnumber", materialdynamicObject);//物料

            materialmaintenanObject.set("nckd_materialtype", materialdynamicObject.getString("materialtype"));//物料类型
            materialmaintenanObject.set("nckd_oldmaterialnumber", materialdynamicObject.getString("oldnumber"));//旧物料编码
            materialmaintenanObject.set("nckd_mnemoniccode", materialdynamicObject.getString("helpcode"));//助记码
            materialmaintenanObject.set("nckd_remark", materialdynamicObject.getString("description"));//描述
            materialmaintenanObject.set("nckd_materialrisk", materialdynamicObject.getString("hazardous"));//物料危险性
            materialmaintenanObject.set("nckd_outsourcing", materialdynamicObject.getBoolean("enableoutsource"));//可委外
//            materialmaintenanObject.set("nckd_materialattribute", dynamicObject.getString("nckd_materialattribute"));//物料属性
//            materialmaintenanObject.set("nckd_selfmaterialtype", dynamicObject.getString("nckd_selfmaterialtype"));//自制物料类型
//            materialmaintenanObject.set("nckd_materialid", dynamicObject.getLong("id"));//物料申请单物料分录id

            materialmaintenanObject.set("nckd_sales", entity.get("nckd_sales"));//销售
            materialmaintenanObject.set("nckd_selfmade", entity.get("nckd_selfmade"));//自制
            materialmaintenanObject.set("nckd_purchase", entity.get("nckd_purchase"));//采购
            materialmaintenanObject.set("nckd_enablelot", entity.get("nckd_enablelot"));//启用批号管理
            materialmaintenanObject.set("nckd_lotcoderule", entity.get("nckd_lotcoderule"));//批号规则

            if ("1".equals(billType)) {
                // 生产基本信息
                materialmaintenanObject.set("nckd_mftunit", materialdynamicObject.getDynamicObject("baseunit"));//生产计量单位
                materialmaintenanObject.set("nckd_supplyorgunitid", org);//供货库存组织
                materialmaintenanObject.set("nckd_materialattri", "10030");//物料属性
                materialmaintenanObject.set("nckd_bomversionrule", MaterialAttributeInformationUtils.getDefaultBOMRuleVer());//BOM版本规则
                materialmaintenanObject.set("nckd_issuemode", "11010");//领送料方式
                materialmaintenanObject.set("nckd_isbackflush", "A");//倒冲

                // 组织范围内属性页签
                DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle("nckd_orgpropertytab", new QFilter[]{new QFilter("nckd_entryentity.nckd_org", QCP.equals, org.getPkValue())});
                if (loadSingle != null) {
                    List<DynamicObject> collect = loadSingle.getDynamicObjectCollection("nckd_entryentity").stream().filter(d -> Objects.equals(d.getDynamicObject("nckd_org").getPkValue(), org.getPkValue())).collect(Collectors.toList());
                    List<String> materialproperty = Arrays.stream(collect.get(0).getString("nckd_materialproperty").split(",")).filter(s -> StringUtils.isNotEmpty(s)).collect(Collectors.toList());
                    if (materialproperty.contains("2")) {
                        // 计划基本信息
                        materialmaintenanObject.set("nckd_createorg", org);//计划信息创建组织
                        materialmaintenanObject.set("nckd_materialattr", "10030");//物料属性
                        materialmaintenanObject.set("nckd_planmode", "D");//计划方式
                    }
                }
            } else if ("2".equals(billType)) {
                // 库存基本信息
                materialmaintenanObject.set("nckd_inventoryunit", materialdynamicObject.getDynamicObject("baseunit"));//库存单位
            } else if ("4".equals(billType)) {
                // 销售基本信息
                materialmaintenanObject.set("nckd_salesunit", materialdynamicObject.getDynamicObject("baseunit"));//销售单位
            } else if ("5".equals(billType)) {
                // 采购基本信息
                materialmaintenanObject.set("nckd_purchaseunit", materialdynamicObject.getDynamicObject("baseunit"));//采购单位
            }

            OperationResult operationResult = OperationServiceHelper.executeOperate("save", "nckd_materialmaintenan", new DynamicObject[]{materialmaintenanObject}, OperateOption.create());
            if (operationResult.isSuccess()) {
                if (!"3".equals(billType)) {
                    // 提交
                    OperationResult submitOperate = OperationServiceHelper.executeOperate("submit", "nckd_materialmaintenan", new DynamicObject[]{materialmaintenanObject}, OperateOption.create());
                    if (!submitOperate.isSuccess()) {
                        throw new KDBizException(submitOperate.getAllErrorOrValidateInfo() + submitOperate.getMessage());
                    }
                }
            } else {
                throw new KDBizException(operationResult.getAllErrorOrValidateInfo() + operationResult.getMessage());
            }
        }
    }
}
