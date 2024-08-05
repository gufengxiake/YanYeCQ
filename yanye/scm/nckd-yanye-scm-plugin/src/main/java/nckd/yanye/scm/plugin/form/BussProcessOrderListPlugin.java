package nckd.yanye.scm.plugin.form;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.EntityType;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.list.BillList;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :制造云-生产任务管理-物料-业务处理对应单
 * Description      :物料-业务处理对应单列表插件
 *
 * @author : zhujintao
 * @date : 2024/7/23
 */
public class BussProcessOrderListPlugin extends AbstractListPlugin {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners("nckd_generate");
        this.addItemClickListeners("nckd_generateinventory");
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        String itemKey = evt.getItemKey();
        if ("nckd_generate".equals(itemKey)) {
            //巴拉巴拉
            String operationKey = evt.getOperationKey();
        }
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        super.afterDoOperation(afterDoOperationEventArgs);
        String operateKey = afterDoOperationEventArgs.getOperateKey();
        //生成库存数据
        if (StringUtils.equals("generateinventory", operateKey)) {
            //生成盘点方案-审核后最终生成盘点表
            createInvcountScheme();
        }
        //生成负库存物料检查单
        if (StringUtils.equals("generate", operateKey)) {
            //生成盘点方案-审核后最终生成盘点表
            //有了盘点表就有了库存实时数据 生成负库存物料检查单
            createNegainventoryOrder();
        }
    }

    /**
     * 生成盘点方案-审核后最终生成盘点表
     */
    private void createInvcountScheme() {
        BillList billlistap = this.getView().getControl("billlistap");
        ListSelectedRowCollection selectedRows = billlistap.getSelectedRows();
        EntityType entityType = billlistap.getEntityType();
        //获取选中行pkid
        Object[] primaryKeyValues = selectedRows.getPrimaryKeyValues();
        //获取完整数据
        DynamicObject[] bussProcessOrderArr = BusinessDataServiceHelper.load(primaryKeyValues, entityType);
        //判断单据是不是都是审核状态，必须全部为审核状态才可以进行此操作
        Set<Object> billstatusSet = Arrays.stream(bussProcessOrderArr).map(e -> e.getString("billstatus")).collect(Collectors.toSet());
        if (!(billstatusSet.size() == 1 && billstatusSet.toArray()[0].equals("C"))) {
            this.getView().showErrorNotification("所选单据必须全部为审核状态才可以进行此操作");
            return;
        }
        //判断所选数据是不是处于对应的业务期间
        Set<Object> orgSet = Arrays.stream(bussProcessOrderArr).map(e -> e.getDynamicObject("org").getPkValue()).collect(Collectors.toSet());
        //拼装业务期间查询QFilter
        QFilter qFilter = new QFilter("org", QCP.in, orgSet).and("entry.isenabled", QCP.equals, true);
        DynamicObject[] sysctrlentityArr = BusinessDataServiceHelper.load("cal_sysctrlentity", "id,org,entry.periodtype,entry.currentperiod", qFilter.toArray());
        //转化为Map结构
        Map<Object, DynamicObject> sysctrlentityMap = Arrays.stream(sysctrlentityArr).collect(Collectors.toMap(k -> k.getDynamicObject("org").getPkValue(), v -> v));
        for (DynamicObject bussProcessOrder : bussProcessOrderArr) {
            String billno = bussProcessOrder.getString("billno");
            Object pkValue = bussProcessOrder.getDynamicObject("org").getPkValue();
            DynamicObject sysctrlentity = sysctrlentityMap.get(pkValue);
            if (ObjectUtil.isEmpty(sysctrlentity)) {
                this.getView().showErrorNotification(billno + "对应的核算期间设置未找到");
                break;
            }
        }
        //遍历设置盘点方案,生成盘点表
        for (DynamicObject bussProcessOrder : bussProcessOrderArr) {
            String billno = bussProcessOrder.getString("billno");
            //组织
            DynamicObject orgDy = bussProcessOrder.getDynamicObject("org");
            DynamicObjectCollection bussProcessOrderEntry = bussProcessOrder.getDynamicObjectCollection("nckd_bussinessentries");
            DynamicObject sysctrlentity = sysctrlentityMap.get(orgDy.getPkValue());
            DynamicObjectCollection sysctrlentityEntryColl = sysctrlentity.getDynamicObjectCollection("entry");
            if (sysctrlentityEntryColl.size() == 0) {
                this.getView().showErrorNotification(billno + "对应的盘点方案新增失败，对应的核算期间设置分录为空");
                break;
            }
            DynamicObject sysctrlentityEntry = sysctrlentityEntryColl.get(0);
            //截至日期
            Date enddate = sysctrlentityEntry.getDynamicObject("currentperiod").getDate("enddate");
            //新增盘点方案
            DynamicObject invcountscheme = BusinessDataServiceHelper.newDynamicObject("im_invcountscheme");
            //DynamicObjectCollection dynamicObjectCollection = invcountscheme.getDynamicObjectCollection("");
            //DynamicObject invcountschemeEntry = dynamicObjectCollection.addNew();
            invcountscheme.set("org", orgDy);
            Date date = new Date();
            String yyyyMMdd = DateUtil.format(date, "yyyyMMdd");
            int i = RandomUtil.randomInt(10000);
            String code = String.format("%04d", ++i);
            String invcountschemebillno = "PDFA-" + yyyyMMdd + "-" + code;
            invcountscheme.set("billno", invcountschemebillno);
            invcountscheme.set("schemename", invcountschemebillno);
            invcountscheme.set("counttype", "A");
            invcountscheme.set("backupcondition", "enddateinvacc");
            invcountscheme.set("enddate", enddate);
            invcountscheme.set("accessnode", "end");
            invcountscheme.set("excludeenddate", false);
            invcountscheme.set("completestatus", "A");
            invcountscheme.set("gencountstatus", "A");
            invcountscheme.set("billstatus", "A");
            invcountscheme.set("comment", "");
            //countzeroinv  freezeoutin  enablecheck   nogenotherinout
            invcountscheme.set("defaultvalue", "B");
            //获取新建单据的多选基础资料并赋值
            Set<DynamicObject> inventoryOrgSet = bussProcessOrderEntry.stream().map(e -> e.getDynamicObject("nckd_inventoryorg")).collect(Collectors.toSet());
            DynamicObjectCollection mulorgColl = invcountscheme.getDynamicObjectCollection("mulorg");
            for (DynamicObject dy : inventoryOrgSet) {
                DynamicObject mulorg = new DynamicObject(mulorgColl.getDynamicObjectType());
                mulorg.set("fbasedataId", dy);
                mulorgColl.add(mulorg);
            }
            //多选基础资料字段赋值
            invcountscheme.set("mulorg", mulorgColl);
            invcountscheme.set("warehouse", null);
            invcountscheme.set("location", null);
            Set<DynamicObject> materialSet = bussProcessOrderEntry.stream().map(e -> e.getDynamicObject("nckd_materielfield").getDynamicObject("masterid")).collect(Collectors.toSet());
            //获取新建单据的多选基础资料并赋值
            DynamicObjectCollection materialColl = invcountscheme.getDynamicObjectCollection("material");
            for (DynamicObject dy : materialSet) {
                DynamicObject material = new DynamicObject(materialColl.getDynamicObjectType());
                material.set("fbasedataId", dy);
                materialColl.add(material);
            }
            //多选基础资料字段赋值
            invcountscheme.set("material", materialColl);
            long currUserId = RequestContext.get().getCurrUserId();
            invcountscheme.set("creator", currUserId);
            invcountscheme.set("createtime", date);
            invcountscheme.set("modifier", currUserId);
            invcountscheme.set("lastupdateuser", currUserId);
            invcountscheme.set("modifytime", date);
            invcountscheme.set("lastupdatetime", date);
            //调用保存操作
            OperationResult saveOperationResult = OperationServiceHelper.executeOperate("save", "im_invcountscheme", new DynamicObject[]{invcountscheme}, OperateOption.create());
            if (!saveOperationResult.isSuccess()) {
                this.getView().showErrorNotification(billno + "对应的盘点方案新增失败");
                break;
            } else {
                this.getView().showSuccessNotification(billno + "对应的盘点方案新增成功");
            }
            //生成了暂存的盘点方案，然后提交
            OperationResult submitOperationResult = OperationServiceHelper.executeOperate("submit", "im_invcountscheme", new DynamicObject[]{invcountscheme}, OperateOption.create());
            if (!submitOperationResult.isSuccess()) {
                this.getView().showErrorNotification(billno + "对应的盘点方案提交失败");
                break;
            } else {
                this.getView().showSuccessNotification(billno + "对应的盘点方案提交成功");
            }
            //再审核
            OperationResult auditOperationResult = OperationServiceHelper.executeOperate("audit", "im_invcountscheme", new DynamicObject[]{invcountscheme}, OperateOption.create());
            if (!auditOperationResult.isSuccess()) {
                this.getView().showErrorNotification(billno + "对应的盘点方案审核失败");
                break;
            } else {
                this.getView().showSuccessNotification(billno + "对应的盘点方案审核成功");
            }
            //保存库存截止时间直接带到负库存物料检查单
            bussProcessOrder.set("nckd_inventoryclosedate", enddate);
            //盘点方案编码直接用于查询盘点表
            bussProcessOrder.set("nckd_invcountschemeno", invcountschemebillno);
        }
        SaveServiceHelper.update(bussProcessOrderArr);
    }

    /**
     * 有了盘点表就有了库存实时数据 生成负库存物料检查单
     */
    private void createNegainventoryOrder() {
        BillList billlistap = this.getView().getControl("billlistap");
        ListSelectedRowCollection selectedRows = billlistap.getSelectedRows();
        EntityType entityType = billlistap.getEntityType();
        //获取选中行pkid
        Object[] primaryKeyValues = selectedRows.getPrimaryKeyValues();
        //获取完整数据
        DynamicObject[] bussProcessOrderArr = BusinessDataServiceHelper.load(primaryKeyValues, entityType);
        //判断单据是不是都是审核状态，必须全部为审核状态才可以进行此操作
        Set<Object> billstatusSet = Arrays.stream(bussProcessOrderArr).map(e -> e.getString("billstatus")).collect(Collectors.toSet());
        if (!(billstatusSet.size() == 1 && billstatusSet.toArray()[0].equals("C"))) {
            this.getView().showErrorNotification("所选单据必须全部为审核状态才可以进行此操作");
            return;
        }
        //获取盘点方案编码用于查询盘点表
        Set<Object> invcountschemebillnoSet = Arrays.stream(bussProcessOrderArr).map(e -> e.getString("nckd_invcountschemeno")).collect(Collectors.toSet());
        //拼装业务期间查询QFilter
        QFilter qFilter = new QFilter("schemenumber", QCP.in, invcountschemebillnoSet);
        //查询盘点方案对应的盘点表，可能存在多个
        DynamicObject[] invcountbillArr = BusinessDataServiceHelper.load("im_invcountbill",
                "id,org,schemenumber,billentry.material,billentry.unit,billentry.baseunit,billentry.warehouse,billentry.location,billentry.lotnumber,billentry.qtyacc,billentry.baseqtyacc", qFilter.toArray());
        //判断盘点表是不是都生成了
        //获取物料-业务处理对应单上的盘点方案编码
        Set<Object> invcountschemeNoSet = Arrays.stream(bussProcessOrderArr).map(e -> e.getString("nckd_invcountschemeno")).collect(Collectors.toSet());
        //获取盘点表上的盘点方案编码
        Set<Object> schemenumberSet = Arrays.stream(invcountbillArr).map(e -> e.getString("schemenumber")).collect(Collectors.toSet());
        //两边数量应该一致，粗略的判断一下
        if (invcountschemeNoSet.size() != schemenumberSet.size()) {
            this.getView().showErrorNotification("有盘点方案存在未生成的盘点表,请检查盘点方案是否一一生成盘点表");
            return;
        }
        //分组，根据盘点方案编号分组
        Map<String, List<DynamicObject>> invcountbillMap = Arrays.stream(invcountbillArr).collect(Collectors.groupingBy(e -> e.getString("schemenumber")));
        //遍历选中数据生成盘点表
        for (DynamicObject bussProcessOrder : bussProcessOrderArr) {
            //获取到对应的盘点方案编码
            String schemenumber = bussProcessOrder.getString("nckd_invcountschemeno");
            Date inventoryclosedate = bussProcessOrder.getDate("nckd_inventoryclosedate");
            //通过对应的盘点方案编码 来获取 盘点表集合
            List<DynamicObject> invcountbillList = invcountbillMap.get(schemenumber);
            //将盘点表转为map结构，key为库存组织 value为对应盘点表
            Map<Object, DynamicObject> invcountbillListMap = invcountbillList.stream().collect(Collectors.toMap(k -> k.getDynamicObject("org").getPkValue(), v -> v));
            //新增负库存物料检查单
            DynamicObject negainventoryOrder = BusinessDataServiceHelper.newDynamicObject("nckd_negainventoryorder");
            Date date = new Date();
            String yyyyMMdd = DateUtil.format(date, "yyyyMMdd");
            int i = RandomUtil.randomInt(10000);
            String code = String.format("%04d", ++i);
            negainventoryOrder.set("billno", "FKCWLJCD-" + yyyyMMdd + "-" + code);
            int month = DateUtil.month(inventoryclosedate) + 1;
            negainventoryOrder.set("nckd_periodnumber", month);
            negainventoryOrder.set("nckd_inventoryclosedate", inventoryclosedate);
            negainventoryOrder.set("nckd_invcountschemeno", schemenumber);
            negainventoryOrder.set("billstatus", "A");
            long currUserId = RequestContext.get().getCurrUserId();
            negainventoryOrder.set("creator", currUserId);
            negainventoryOrder.set("createtime", date);
            negainventoryOrder.set("modifier", currUserId);
            negainventoryOrder.set("modifytime", date);
            //分录赋值
            DynamicObjectCollection negainventoryOrderEntryColl = negainventoryOrder.getDynamicObjectCollection("nckd_negainentries");
            DynamicObjectCollection bussProcessOrderEntryColl = bussProcessOrder.getDynamicObjectCollection("nckd_bussinessentries");
            bussProcessOrderEntryColl.forEach(e -> {
                DynamicObject negainventoryOrderEntry = negainventoryOrderEntryColl.addNew();
                //这一部分直接由物料-业务处理对应单 分录带过来
                DynamicObject nckdMaterielfield = e.getDynamicObject("nckd_materielfield");
                negainventoryOrderEntry.set("nckd_materielfield", nckdMaterielfield);
                negainventoryOrderEntry.set("nckd_materiel", nckdMaterielfield.getDynamicObject("masterid"));
                negainventoryOrderEntry.set("nckd_isinventory", e.getBoolean("nckd_isinventory"));
                negainventoryOrderEntry.set("nckd_inventoryorg", e.getDynamicObject("nckd_inventoryorg"));
                DynamicObject nckdWarehouse = e.getDynamicObject("nckd_warehouse");
                negainventoryOrderEntry.set("nckd_warehouse", nckdWarehouse);
                negainventoryOrderEntry.set("nckd_businessdocument", e.getString("nckd_businessdocument"));
                negainventoryOrderEntry.set("nckd_sideproduct", e.getString("nckd_sideproduct"));
                negainventoryOrderEntry.set("nckd_mainproduce", e.getDynamicObject("nckd_mainproduce"));
                negainventoryOrderEntry.set("nckd_useworkshop", e.getDynamicObject("nckd_useworkshop"));
                negainventoryOrderEntry.set("nckd_wareorderworkshop", e.getDynamicObject("nckd_wareorderworkshop"));
                negainventoryOrderEntry.set("nckd_illustrate", e.getString("nckd_illustrate"));
                //这部分是通过盘点表赋值
                DynamicObject nckdInventoryorg = e.getDynamicObject("nckd_inventoryorg");
                //获取到对应库存组织的盘点表，还需要根据仓库和物料获取分录数据
                DynamicObject invcountbill = invcountbillListMap.get(nckdInventoryorg.getPkValue());
                DynamicObjectCollection invcountbillEntryColl = invcountbill.getDynamicObjectCollection("billentry");
                for (DynamicObject invcountbillEntry : invcountbillEntryColl) {
                    if (invcountbillEntry.getDynamicObject("material").getDynamicObject("masterid").getPkValue().equals(nckdMaterielfield.getDynamicObject("masterid").getPkValue())
                            && invcountbillEntry.getDynamicObject("warehouse").getPkValue().equals(nckdWarehouse.getPkValue())) {
                        negainventoryOrderEntry.set("nckd_number", invcountbillEntry.getBigDecimal("qtyacc"));
                        negainventoryOrderEntry.set("nckd_unitofmeasurement", invcountbillEntry.getDynamicObject("unit").getPkValue());
                        negainventoryOrderEntry.set("nckd_basicunitnumber", invcountbillEntry.getBigDecimal("baseqtyacc"));
                        negainventoryOrderEntry.set("nckd_basicunit", invcountbillEntry.getDynamicObject("baseunit").getPkValue());
                        negainventoryOrderEntry.set("nckd_basewarehouse", invcountbillEntry.getDynamicObject("warehouse").getPkValue());
                        negainventoryOrderEntry.set("nckd_position", invcountbillEntry.getDynamicObject("location") != null ? invcountbillEntry.getDynamicObject("location").getPkValue() : null);
                        negainventoryOrderEntry.set("nckd_batchnumber", invcountbillEntry.getString("lotnumber"));
                        break;
                    }
                }
                //negainventoryOrderEntryColl.add(negainventoryOrderEntry);
            });
            //调用保存操作
            OperationResult saveOperationResult = OperationServiceHelper.executeOperate("save", "nckd_negainventoryorder", new DynamicObject[]{negainventoryOrder}, OperateOption.create());
            if (!saveOperationResult.isSuccess()) {
                this.getView().showErrorNotification(bussProcessOrder.getString("billno") + "对应的负库存物料检查单新增失败");
            } else {
                this.getView().showSuccessNotification(bussProcessOrder.getString("billno") + "对应的负库存物料检查单新增成功");
            }
        }
    }
}
