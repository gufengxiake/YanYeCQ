package nckd.yanye.scm.plugin.form;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.EntityType;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.list.BillList;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :制造云-生产任务管理-负库存物料检查单
 * Description      :负库存物料检查单列表插件
 *
 * @author : yaosijie
 * @date : 2024/7/26
 */
public class NegainventoryOrderListPlugin extends AbstractListPlugin {


    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners("nckd_completedreceipt");
    }


    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        super.afterDoOperation(afterDoOperationEventArgs);
        String operateKey = afterDoOperationEventArgs.getOperateKey();
        //生成完工入库单
        if (StringUtils.equals("dreceipt", operateKey)) {
            BillList billlistap = this.getView().getControl("billlistap");
            ListSelectedRowCollection selectedRows = billlistap.getSelectedRows();
            EntityType entityType = billlistap.getEntityType();
            //获取选中行pkid
            Object[] primaryKeyValues = selectedRows.getPrimaryKeyValues();
            //获取完整数据
            DynamicObject[] bussProcessOrderArr = BusinessDataServiceHelper.load(primaryKeyValues, entityType);
            //选中列的所有分录
            DynamicObjectCollection dynamicObjectCollection = new DynamicObjectCollection();
            //构造map, key:负库存物料检查单分录id，value:负库存物料检查单
            Map<Long,DynamicObject> dynamicObjectMap = new HashMap<>();

            Arrays.asList(bussProcessOrderArr).forEach(t->{
                DynamicObjectCollection dynamicObjects = t.getDynamicObjectCollection("nckd_negainentries");
                for (DynamicObject dynamicObject : dynamicObjects){
                    dynamicObjectMap.put(dynamicObject.getLong("id"),t);
                }
                dynamicObjectCollection.addAll(dynamicObjects);
            });
            //查询所有的物料编码
            Set<Object> codeSet = dynamicObjectCollection.stream().map(t->t.getDynamicObject("nckd_materielfield").getPkValue()).collect(Collectors.toSet());
            //构造查询生产工单分录条件
            QFilter qFilter = new QFilter("treeentryentity.material", QCP.in, codeSet)
                    .and("treeentryentity.bizstatus",QCP.not_equals,"C").and("treeentryentity.taskstatus",QCP.not_equals,"C");
            DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("pom_mftorder", "id,billno,entrustdept,treeentryentity,treeentryentity.material,org,entrustdept", new QFilter[]{qFilter});
            //key,物料生产信息，value:生产工单
            Map<Object,DynamicObject> map = new HashMap<>();
            //key,物料生产信息，value:生产工单物料信息分录
//            Map<Object,DynamicObject> masterMap = new HashMap<>();
            for (DynamicObject dynamicObject : Arrays.asList(dynamicObjects)){
                DynamicObjectCollection dynamics = dynamicObject.getDynamicObjectCollection("treeentryentity");
                for (DynamicObject dynami : dynamics){
                    map.put(dynami.getDynamicObject("material").getPkValue(),dynamicObject);
//                    masterMap.put(dynami.getDynamicObject("material").getPkValue(),dynami);
                }
            }
            //构造map, key负库存物料检查单分录id，分录数据
            Map<Long,DynamicObject> dyMap = dynamicObjectCollection.stream().collect(Collectors.toMap(t->t.getLong("id"),t->t));
            for (Map.Entry<Long,DynamicObject> entry : dyMap.entrySet()){
                //负库存物料检查单分录
                DynamicObject dynamicObject = entry.getValue();
                //负库存物料检查单
                DynamicObject inventoryDynamicObject = dynamicObjectMap.get(entry.getKey());

                /**
                 *  生成完工入库单条件
                 * 1、完工入库单+副产品+【对应生产主产品】
                 * 2、完工入库单+联产品+【对应生产主产品】
                 */
                if (("1".equals(dynamicObject.getString("nckd_businessdocument")) && "B".equals(dynamicObject.getString("nckd_sideproduct")) && StringUtils.isNotEmpty(dynamicObject.getString("nckd_mainproduce")))
                        ||("1".equals(dynamicObject.getString("nckd_businessdocument")) && "A".equals(dynamicObject.getString("nckd_sideproduct")) && StringUtils.isNotEmpty(dynamicObject.getString("nckd_mainproduce")))){
                    generateCompletedWarehouseReceipt(dynamicObject,inventoryDynamicObject,map);
                }else if ("1".equals(dynamicObject.getString("nckd_businessdocument")) && StringUtils.isNotEmpty(dynamicObject.getString("nckd_wareorderworkshop"))){
                    //下推生成完工入库单（完工入库单+【入库单对应车间】）
                    pushDownCompletedWarehouseReceipt(dynamicObject,map);

                }else if ("2".equals(dynamicObject.getString("nckd_businessdocument")) && StringUtils.isNotEmpty(dynamicObject.getString("nckd_wareorderworkshop"))){
                    /**
                     *  生成生产入库单条件
                     * 1、生产入库单+【入库单对应车间】
                     */
                    generateProductionReceipt(dynamicObject,inventoryDynamicObject);
                }else if ("4".equals(dynamicObject.getString("nckd_businessdocument")) && StringUtils.isNotEmpty(dynamicObject.getString("nckd_useworkshop"))){
                    /**
                     *  生成领料出库单条件
                     * 1、领料出库单+正库存领用车间
                     */
                    generateMaterialReceipt(dynamicObject,inventoryDynamicObject);
                }else if ("3".equals(dynamicObject.getString("nckd_businessdocument")) && StringUtils.isNotEmpty(dynamicObject.getString("nckd_mainproduce")) && StringUtils.isNotEmpty(dynamicObject.getString("nckd_useworkshop"))){
                    /**
                     *  生成生产领料单条件
                     * 1、生产领料单+对应生产主产品+正库存领用车间
                     */
                    pushDownProductionMaterialReceipt(dynamicObject,map);
                }
            }
        }
    }
    //完工入库单（新增）
    private void generateCompletedWarehouseReceipt(DynamicObject dynamicObject,DynamicObject inventoryDynamicObject,Map<Object,DynamicObject> map){

        //负库存物料检查单分录物料
        Object masterid = dynamicObject.getDynamicObject("nckd_materielfield").getPkValue();
        DynamicObject warDynamicObject = map.get(masterid);
        //新增完工入库单
        DynamicObject invcountscheme = BusinessDataServiceHelper.newDynamicObject("im_mdc_mftmanuinbill");
        if (Objects.isNull(warDynamicObject)){
            this.getView().showErrorNotification(invcountscheme.getString("billno") + "对应的生产工单不存在");
            return;
        }
        //完工入库单物料明细分录
        DynamicObjectCollection invcountschemebillEntryColl = invcountscheme.getDynamicObjectCollection("billentry");

        invcountscheme.set("org",dynamicObject.getDynamicObject("nckd_inventoryorg"));//库存组织
        DynamicObject billtype = BusinessDataServiceHelper.loadSingle(920040553033394176l, "bos_billtype");
        invcountscheme.set("billtype",billtype);//单据类型
        DynamicObject biztype = BusinessDataServiceHelper.loadSingle(688864881516276736l, "bd_biztype");

        invcountscheme.set("biztype",biztype);//业务类型
        DynamicObject invscheme = BusinessDataServiceHelper.loadSingle(767845948062835712l, "im_invscheme");
        invcountscheme.set("invscheme",invscheme);//库存事务
//        invcountscheme.set("biztime",inventoryDynamicObject.getDate("nckd_inventoryclosedate"));//业务日期
//        invcountscheme.set("billstatus","A");//单据状态
        invcountscheme.set("productionorg",warDynamicObject.getDynamicObject("org"));//生产组织()
//        invcountscheme.set("bizorg",RequestContext.get().getOrgId());//业务组织
//        invcountscheme.set("bizorg",RequestContext.get().getDebt());//业务部门
        invcountscheme.set("bizdept",warDynamicObject.getLong("entrustdept"));//业务部门
        invcountscheme.set("shipper",dynamicObject.getDynamicObject("nckd_inventoryorg"));//货主
//        invcountscheme.set("dept",dynamicObject.getDynamicObject("nckd_inventoryorg"));//库管部门
//        invcountscheme.set("settlecurrency",1);//结算币别
//        invcountscheme.set("bookdate",inventoryDynamicObject.getDate("nckd_inventoryclosedate"));//记账日期
//        invcountscheme.set("asyncstatus","B");//异步状态
        getInvcountSchemeDynamicObject(invcountscheme,dynamicObject,"",inventoryDynamicObject);

//        invcountscheme.set("creator", RequestContext.get().getCurrUserId());
//        invcountscheme.set("createtime",new Date());
//        invcountscheme.set("modifier", RequestContext.get().getCurrUserId());
//        invcountscheme.set("modifytime", new Date());

        DynamicObject negainventoryOrderEntry = invcountschemebillEntryColl.addNew();
//        DynamicObject linetype = BusinessDataServiceHelper.loadSingle(1194150915045641216l, "bd_linetype");
//        negainventoryOrderEntry.set("linetype",linetype);//行类型
//        DynamicObject materielObject = dynamicObject.getDynamicObject("nckd_materiel");
//        Object materieNumber = materielObject.getPkValue();
//        QFilter qFilter = new QFilter("masterid",QCP.equals,materieNumber);
//        DynamicObject material = BusinessDataServiceHelper.loadSingle("bd_materialinventoryinfo", qFilter.toArray());
//        negainventoryOrderEntry.set("material",material);//物料编码
//        negainventoryOrderEntry.set("materialname",material.getString("masterid.name"));//物料名称
        negainventoryOrderEntry.set("producttype",dynamicObject.getString("nckd_sideproduct"));//产品类型
        negainventoryOrderEntry.set("qualitystatus","A");//质量状态
//        negainventoryOrderEntry.set("unit",dynamicObject.getDynamicObject("nckd_unitofmeasurement"));//计量单位
//        negainventoryOrderEntry.set("qty",dynamicObject.getBigDecimal("nckd_number"));//数量
//        negainventoryOrderEntry.set("baseunit",dynamicObject.getDynamicObject("nckd_basicunit"));//基本单位
//        negainventoryOrderEntry.set("baseqty",dynamicObject.getInt("nckd_basicunitnumber"));//基本数量
//        negainventoryOrderEntry.set("lotnumber",dynamicObject.getString("nckd_batchnumber"));//批号
//        negainventoryOrderEntry.set("location",dynamicObject.getDynamicObject("nckd_position"));//仓位
//        negainventoryOrderEntry.set("invtype",688884005529250816l);//入库库存类型
//        negainventoryOrderEntry.set("invstatus",691928582720825344l);//入库库存状态
        negainventoryOrderEntry.set("ownertype","bos_org");//入库货主类型
        negainventoryOrderEntry.set("owner",dynamicObject.getDynamicObject("nckd_inventoryorg"));//入库货主
//        negainventoryOrderEntry.set("keepertype","bos_org");//入库保管者类型
//        negainventoryOrderEntry.set("keeper",dynamicObject.getDynamicObject("nckd_inventoryorg"));//入库保管者
        negainventoryOrderEntry.set("entrysettleorg",dynamicObject.getDynamicObject("nckd_inventoryorg"));//结算组织
        negainventoryOrderEntry.set("manubill",warDynamicObject.getString("billno"));//生产工单编号
        negainventoryOrderEntry.set("mainbillnumber",warDynamicObject.getString("billno"));//核心单据编号
        getDynamicObject(negainventoryOrderEntry,dynamicObject,"");
//        DynamicObject nckdWarehouseDy = BusinessDataServiceHelper.loadSingle(dynamicObject.getLong("nckd_warehouse.id"), "bd_warehouse");
//        negainventoryOrderEntry.set("warehouse",nckdWarehouseDy);//仓库
        OperationResult saveOperationResult = OperationServiceHelper.executeOperate("save", "im_mdc_mftmanuinbill", new DynamicObject[]{invcountscheme}, OperateOption.create());
        if (!saveOperationResult.isSuccess()) {
            this.getView().showErrorNotification(invcountscheme.getString("billno") + "对应的完工入库单新增失败");
        }
    }

    //完工入库单（下推）
    private void pushDownCompletedWarehouseReceipt(DynamicObject dynamicObject,Map<Object,DynamicObject> map){
        Object masterid = dynamicObject.getDynamicObject("nckd_materielfield").getPkValue();
        String number = dynamicObject.getString("nckd_materielfield.masterid.number");
        //生产工单
        DynamicObject warDynamicObject = map.get(masterid);
        if (Objects.isNull(warDynamicObject)){
            this.getView().showErrorNotification("物料编码："+number + "对应的生产工单不存在");
            return;
        }
        PushArgs pushArgs = getPushArgs(warDynamicObject,"pom_mftorder","im_mdc_mftmanuinbill");
        ConvertOperationResult result = ConvertServiceHelper.pushAndSave(pushArgs);
        if (!result.isSuccess()) {
            this.getView().showErrorNotification("下推失败：" + result.getMessage());
        }

    }
    //生产入库单（新增）
    private void generateProductionReceipt(DynamicObject dynamicObject,DynamicObject inventoryDynamicObject){
        //新增生产入库单
        DynamicObject invcountscheme = BusinessDataServiceHelper.newDynamicObject("im_productinbill");
        //生产入库单物料明细分录
        DynamicObjectCollection invcountschemebillEntryColl = invcountscheme.getDynamicObjectCollection("billentry");

        invcountscheme.set("org",dynamicObject.getDynamicObject("nckd_inventoryorg"));//库存组织
        DynamicObject billtype = BusinessDataServiceHelper.loadSingle(697678838297318400l, "bos_billtype");
        invcountscheme.set("billtype",billtype);//单据类型
        DynamicObject biztype = BusinessDataServiceHelper.loadSingle(688864881516276736l, "bd_biztype");

        invcountscheme.set("biztype",biztype);//业务类型
        DynamicObject invscheme = BusinessDataServiceHelper.loadSingle(697663348556242944l, "im_invscheme");
        invcountscheme.set("invscheme",invscheme);//库存事务
//        invcountscheme.set("biztime",inventoryDynamicObject.getDate("nckd_inventoryclosedate"));//业务日期
//        invcountscheme.set("billstatus","A");//单据状态
//        invcountscheme.set("bizorg",RequestContext.get().getOrgId());//业务组织
        invcountscheme.set("bizdept",dynamicObject.getDynamicObject("nckd_inventoryorg"));//业务部门
//        invcountscheme.set("dept",dynamicObject.getDynamicObject("nckd_inventoryorg"));//库管部门
//        invcountscheme.set("settlecurrency",1);//结算币别
//        invcountscheme.set("bookdate",inventoryDynamicObject.getDate("nckd_inventoryclosedate"));//记账日期
//        invcountscheme.set("asyncstatus","B");//异步状态
//        invcountscheme.set("creator", RequestContext.get().getCurrUserId());
//        invcountscheme.set("createtime",new Date());
//        invcountscheme.set("modifier", RequestContext.get().getCurrUserId());
//        invcountscheme.set("modifytime", new Date());
        getInvcountSchemeDynamicObject(invcountscheme,dynamicObject,"",inventoryDynamicObject);

        DynamicObject negainventoryOrderEntry = invcountschemebillEntryColl.addNew();
//        DynamicObject linetype = BusinessDataServiceHelper.loadSingle(1194150915045641216l, "bd_linetype");
//        negainventoryOrderEntry.set("linetype",linetype);//行类型
//        DynamicObject materielObject = dynamicObject.getDynamicObject("nckd_materiel");
//        Object materieNumber = materielObject.getPkValue();
//        QFilter qFilter = new QFilter("masterid",QCP.equals,materieNumber);
//        DynamicObject material = BusinessDataServiceHelper.loadSingle("bd_materialinventoryinfo", qFilter.toArray());
//        negainventoryOrderEntry.set("material",material);//物料编码
//        negainventoryOrderEntry.set("materialname",material.getString("masterid.name"));//物料名称
//        negainventoryOrderEntry.set("unit",dynamicObject.getDynamicObject("nckd_unitofmeasurement"));//计量单位
//        negainventoryOrderEntry.set("qty",dynamicObject.getBigDecimal("nckd_number"));//数量
//        negainventoryOrderEntry.set("baseunit",dynamicObject.getDynamicObject("nckd_basicunit"));//基本单位
//        negainventoryOrderEntry.set("baseqty",dynamicObject.getInt("nckd_basicunitnumber"));//基本数量
//        negainventoryOrderEntry.set("lotnumber",dynamicObject.getString("nckd_batchnumber"));//批号
//        DynamicObject nckdWarehouseDy = BusinessDataServiceHelper.loadSingle(dynamicObject.getLong("nckd_warehouse.id"), "bd_warehouse");
//        negainventoryOrderEntry.set("warehouse",nckdWarehouseDy);//仓库
//        negainventoryOrderEntry.set("location",dynamicObject.getDynamicObject("nckd_position"));//仓位
//        negainventoryOrderEntry.set("invtype",688884005529250816l);//入库库存类型
//        negainventoryOrderEntry.set("invstatus",691928582720825344l);//入库库存状态
        negainventoryOrderEntry.set("ownertype","bos_org");//入库货主类型
        negainventoryOrderEntry.set("owner",dynamicObject.getDynamicObject("nckd_inventoryorg"));//入库货主
//        negainventoryOrderEntry.set("keepertype","bos_org");//入库保管者类型
//        negainventoryOrderEntry.set("keeper",dynamicObject.getDynamicObject("nckd_inventoryorg"));//入库保管者
        getDynamicObject(negainventoryOrderEntry,dynamicObject,"");
        OperationResult saveOperationResult = OperationServiceHelper.executeOperate("save", "im_productinbill", new DynamicObject[]{invcountscheme}, OperateOption.create());
        if (!saveOperationResult.isSuccess()) {
            this.getView().showErrorNotification(invcountscheme.getString("billno") + "对应的生产入库单新增失败");
        }
    }
    //领料出库单（新增）
    private void generateMaterialReceipt(DynamicObject dynamicObject,DynamicObject inventoryDynamicObject){
        //新增领料出库单
        DynamicObject invcountscheme = BusinessDataServiceHelper.newDynamicObject("im_materialreqoutbill");
        //领料出库单物料明细分录
        DynamicObjectCollection invcountschemebillEntryColl = invcountscheme.getDynamicObjectCollection("billentry");

        invcountscheme.set("org",dynamicObject.getDynamicObject("nckd_inventoryorg"));//库存组织
        DynamicObject billtype = BusinessDataServiceHelper.loadSingle(698250135813088256l, "bos_billtype");
        invcountscheme.set("billtype",billtype);//单据类型
        DynamicObject biztype = BusinessDataServiceHelper.loadSingle(688863581240093696l, "bd_biztype");

        invcountscheme.set("biztype",biztype);//业务类型
        DynamicObject invscheme = BusinessDataServiceHelper.loadSingle(699728741555085312l, "im_invscheme");
        invcountscheme.set("invscheme",invscheme);//库存事务
//        invcountscheme.set("biztime",inventoryDynamicObject.getDate("nckd_inventoryclosedate"));//业务日期
//        invcountscheme.set("billstatus","A");//单据状态
//        invcountscheme.set("asyncstatus","B");//异步状态
//        invcountscheme.set("bizorg",RequestContext.get().getOrgId());//需求组织
//        invcountscheme.set("bizdept",dynamicObject.getDynamicObject("nckd_inventoryorg"));//业务部门
        invcountscheme.set("supplyownertype","bos_org");//货主类型
//        invcountscheme.set("dept",dynamicObject.getDynamicObject("nckd_inventoryorg"));//库管部门
//        invcountscheme.set("settlecurrency",1);//结算币别
//        invcountscheme.set("bookdate",inventoryDynamicObject.getDate("nckd_inventoryclosedate"));//记账日期
        invcountscheme.set("supplyowner",dynamicObject.getDynamicObject("nckd_inventoryorg"));//供应货主
        invcountscheme.set("settleorg",dynamicObject.getDynamicObject("nckd_inventoryorg"));//核算组织
        invcountscheme.set("billcretype","B");//单据生成类型

//        invcountscheme.set("creator", RequestContext.get().getCurrUserId());
//        invcountscheme.set("createtime",new Date());
//        invcountscheme.set("modifier", RequestContext.get().getCurrUserId());
//        invcountscheme.set("modifytime", new Date());
        getInvcountSchemeDynamicObject(invcountscheme,dynamicObject,"LL",inventoryDynamicObject);
        DynamicObject negainventoryOrderEntry = invcountschemebillEntryColl.addNew();
        getDynamicObject(negainventoryOrderEntry,dynamicObject,"LL");

//        for (DynamicObject dynamicObj : invcountschemebillEntryColl){
//        DynamicObject linetype = BusinessDataServiceHelper.loadSingle(1194150915045641216l, "bd_linetype");
//        negainventoryOrderEntry.set("linetype",linetype);//行类型
//        DynamicObject materielObject = dynamicObject.getDynamicObject("nckd_materiel");
//        Object materieNumber = materielObject.getPkValue();
//        QFilter qFilter = new QFilter("masterid",QCP.equals,materieNumber);
//        DynamicObject material = BusinessDataServiceHelper.loadSingle("bd_materialinventoryinfo", qFilter.toArray());
//        DynamicObject materi = BusinessDataServiceHelper.loadSingle("bd_material", qFilter.toArray());
//        negainventoryOrderEntry.set("material",material);//物料编码
//        negainventoryOrderEntry.set("materialname",material.getString("masterid.name"));//物料名称
//        negainventoryOrderEntry.set("materialmasterid",materi);//主物料
//        negainventoryOrderEntry.set("unit",dynamicObject.getDynamicObject("nckd_unitofmeasurement"));//计量单位
//        negainventoryOrderEntry.set("qty",dynamicObject.getBigDecimal("nckd_number"));//数量
//        negainventoryOrderEntry.set("baseunit",dynamicObject.getDynamicObject("nckd_basicunit"));//基本单位
//        negainventoryOrderEntry.set("baseqty",dynamicObject.getInt("nckd_basicunitnumber"));//基本数量
//        negainventoryOrderEntry.set("lotnumber",dynamicObject.getString("nckd_batchnumber"));//批号
//        DynamicObject nckdWarehouseDy = BusinessDataServiceHelper.loadSingle(dynamicObject.getLong("nckd_warehouse.id"), "bd_warehouse");
//        negainventoryOrderEntry.set("warehouse",nckdWarehouseDy);//仓库
//        negainventoryOrderEntry.set("location",dynamicObject.getDynamicObject("nckd_position"));//仓位
//        negainventoryOrderEntry.set("invtype",688884005529250816l);//入库库存类型
//        negainventoryOrderEntry.set("invstatus",691928582720825344l);//入库库存状态
        negainventoryOrderEntry.set("outinvtype",688884005529250816l);//出库库存类型
        negainventoryOrderEntry.set("outinvstatus",691928582720825344l);//出库库存状态
        negainventoryOrderEntry.set("outownertype","bos_org");//出库货主类型
        negainventoryOrderEntry.set("outowner",dynamicObject.getDynamicObject("nckd_inventoryorg"));//出库货主
        negainventoryOrderEntry.set("outkeepertype","bos_org");//出库保管者类型
        negainventoryOrderEntry.set("outkeeper",dynamicObject.getDynamicObject("nckd_inventoryorg"));//出库保管者
//        negainventoryOrderEntry.set("keepertype","bos_org");//入库保管者类型

//        negainventoryOrderEntry.set("keeper",dynamicObject.getDynamicObject("nckd_inventoryorg"));//入库保管者
        OperationResult saveOperationResult = OperationServiceHelper.executeOperate("save", "im_materialreqoutbill", new DynamicObject[]{invcountscheme}, OperateOption.create());
        if (!saveOperationResult.isSuccess()) {
            this.getView().showErrorNotification(invcountscheme.getString("billno") + "对应的领料出库单新增失败");
        }
    }
    //生产领料单（下推）
    private void pushDownProductionMaterialReceipt(DynamicObject dynamicObject,Map<Object,DynamicObject> map){
        Object masterid = dynamicObject.getDynamicObject("nckd_materielfield").getPkValue();
        String number = dynamicObject.getString("nckd_materielfield.masterid.number");
        //生产工单
        DynamicObject warDynamicObject = map.get(masterid);
        if (Objects.isNull(warDynamicObject)){
            this.getView().showErrorNotification("物料编码："+number + "对应的生产工单不存在");
            return;
        }
        //根据生产工单找到组件清单
        QFilter qFilter = new QFilter("forderid", QCP.in, warDynamicObject.getPkValue());
        DynamicObject pomMftstocks = BusinessDataServiceHelper.loadSingle("pom_mftstock", qFilter.toArray());
        if (Objects.isNull(pomMftstocks)){
            this.getView().showErrorNotification("生产工单编码："+warDynamicObject.getString("billno") + "对应的组件清单不存在");
            return;
        }
        PushArgs pushArgs = getPushArgs(pomMftstocks,"pom_mftstock","im_mdc_mftproorder");
        ConvertOperationResult result = ConvertServiceHelper.pushAndSave(pushArgs);
        if (!result.isSuccess()) {
            this.getView().showErrorNotification("下推失败：" + result.getMessage());
        }

    }

    @NotNull
    private static PushArgs getPushArgs(DynamicObject dynamicObject,String sourceEntityNumber,String targetEntityNumber) {
        List<ListSelectedRow> rows = Arrays.asList(new ListSelectedRow(dynamicObject.getPkValue()));
        // 创建下推参数
        PushArgs pushArgs = new PushArgs();
        pushArgs.setSourceEntityNumber(sourceEntityNumber);
        pushArgs.setTargetEntityNumber(targetEntityNumber);
        //不检查目标单新增权限
        pushArgs.setHasRight(true);
        //下推后默认保存
        pushArgs.setAutoSave(true);
        //是否生成单据转换报告
        pushArgs.setBuildConvReport(false);
        pushArgs.setSelectedRows(rows);
        return pushArgs;
    }

    private static void getDynamicObject(DynamicObject negainventoryOrderEntry,DynamicObject dynamicObject,String type){
        DynamicObject linetype = BusinessDataServiceHelper.loadSingle(1194150915045641216l, "bd_linetype");
        negainventoryOrderEntry.set("linetype",linetype);//行类型
        DynamicObject materielObject = dynamicObject.getDynamicObject("nckd_materiel");
        Object materieNumber = materielObject.getPkValue();
        QFilter qFilter = new QFilter("masterid",QCP.equals,materieNumber);
        DynamicObject material = BusinessDataServiceHelper.loadSingle("bd_materialinventoryinfo", qFilter.toArray());
        negainventoryOrderEntry.set("material",material);//物料编码
        negainventoryOrderEntry.set("materialname",material.getString("masterid.name"));//物料名称
        if ("LL".equals(type)){
            DynamicObject materi = BusinessDataServiceHelper.loadSingle("bd_material", qFilter.toArray());
            negainventoryOrderEntry.set("materialmasterid",materi);//主物料
        }
        negainventoryOrderEntry.set("unit",dynamicObject.getDynamicObject("nckd_unitofmeasurement"));//计量单位
        negainventoryOrderEntry.set("qty",dynamicObject.getBigDecimal("nckd_number"));//数量
        negainventoryOrderEntry.set("baseunit",dynamicObject.getDynamicObject("nckd_basicunit"));//基本单位
        negainventoryOrderEntry.set("baseqty",dynamicObject.getInt("nckd_basicunitnumber"));//基本数量
        negainventoryOrderEntry.set("lotnumber",dynamicObject.getString("nckd_batchnumber"));//批号
        DynamicObject nckdWarehouseDy = BusinessDataServiceHelper.loadSingle(dynamicObject.getLong("nckd_warehouse.id"), "bd_warehouse");
        negainventoryOrderEntry.set("warehouse",nckdWarehouseDy);//仓库
        negainventoryOrderEntry.set("location",dynamicObject.getDynamicObject("nckd_position"));//仓位
        negainventoryOrderEntry.set("invtype",688884005529250816l);//入库库存类型
        negainventoryOrderEntry.set("invstatus",691928582720825344l);//入库库存状态
        negainventoryOrderEntry.set("keepertype","bos_org");//入库保管者类型
        negainventoryOrderEntry.set("keeper",dynamicObject.getDynamicObject("nckd_inventoryorg"));//入库保管者
    }

    private static void getInvcountSchemeDynamicObject(DynamicObject invcountscheme,DynamicObject dynamicObject,String type,DynamicObject inventoryDynamicObject){

        /**制单信息**/
        invcountscheme.set("creator", RequestContext.get().getCurrUserId());
        invcountscheme.set("createtime",new Date());
        invcountscheme.set("modifier", RequestContext.get().getCurrUserId());
        invcountscheme.set("modifytime", new Date());

        invcountscheme.set("bookdate",inventoryDynamicObject.getDate("nckd_inventoryclosedate"));//记账日期
        invcountscheme.set("settlecurrency",1);//结算币别
        invcountscheme.set("dept",dynamicObject.getDynamicObject("nckd_inventoryorg"));//库管部门
        invcountscheme.set("bizorg",RequestContext.get().getOrgId());//需求组织
        invcountscheme.set("asyncstatus","B");//异步状态
        invcountscheme.set("billstatus","A");//单据状态
        invcountscheme.set("biztime",inventoryDynamicObject.getDate("nckd_inventoryclosedate"));//业务日期
    }
}