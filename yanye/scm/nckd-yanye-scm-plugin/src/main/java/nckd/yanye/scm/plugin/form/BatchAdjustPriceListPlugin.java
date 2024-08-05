package nckd.yanye.scm.plugin.form;

import static kd.bos.entity.botp.ConvertOpType.Push;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import kd.bos.bill.BillShowParameter;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.exception.KDBizException;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.metadata.botp.ConvertRuleReader;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.bos.util.CollectionUtils;
import kd.fi.cal.business.account.CloseAccountParamBuilder;
import kd.fi.cal.common.helper.AccountingSysHelper;
import org.apache.commons.lang.StringUtils;

/**
 * @author husheng
 * @date 2024-07-22 17:31
 * @description 暂估应付单-列表插件
 */
public class BatchAdjustPriceListPlugin extends AbstractListPlugin {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        this.addItemClickListeners("nckd_baritemap");
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);

        if (evt.getItemKey().equals("nckd_baritemap")) {
            //暂估批量调价 下推新增月末调价单
            this.batchAdjustPrice();
        }
    }

    /**
     * 暂估批量调价 下推新增月末调价单
     */
    private void batchAdjustPrice() {
        ListSelectedRowCollection selectedRows = this.getSelectedRows();
        if(selectedRows.size() == 0){
            throw new KDBizException("请选择数据!");
        }

        Object[] keyValues = selectedRows.getPrimaryKeyValues();
        QFilter qFilter = new QFilter("id", QCP.in, keyValues);
        DynamicObject[] busbills = BusinessDataServiceHelper.load("ap_busbill", "org,bizdate,entry.id,entry.nckd_supplier,entry.e_material", new QFilter[]{qFilter});

        // 查询对应组织的会计期间
        QFilter filter = new QFilter("org", QCP.equals, busbills[0].getDynamicObject("org").get("id"))
                .and("entry.isenabled", QCP.equals, true);
        DynamicObject sysctrlentity = BusinessDataServiceHelper.loadSingle("cal_sysctrlentity", filter.toArray());
        DynamicObjectCollection sysctrlentityEntry = sysctrlentity.getDynamicObjectCollection("entry");
        DynamicObject currentperiod = sysctrlentityEntry.get(0).getDynamicObject("currentperiod");
        String number = currentperiod.getString("number");

        // 获取对应组织的上次关账日期
        Date closedate = this.loadGrid(busbills[0].getDynamicObject("org").getLong("id"));

        Long orgId = null;
        String period = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyyMM");
        for (DynamicObject dataEntity : busbills) {
            Long id = (Long) dataEntity.getDynamicObject("org").get("id");
            if (Objects.isNull(orgId)) {
                orgId = id;
            } else if (!orgId.equals(id)) {
                throw new KDBizException("所选暂估应付单对应的结算组织必须为同一组织!");
            }

            Date bizdate = dataEntity.getDate("bizdate");
            if (StringUtils.isEmpty(period)) {
                period = format.format(bizdate);
            } else if (!period.equals(format.format(bizdate))) {
                throw new KDBizException("所选暂估应付单必须在同一月份!");
            }

            if (!number.equals(period)) {
                throw new KDBizException("所选暂估应付单的单据日期必须在存货核算模块当前期间!");
            }

            if(closedate != null && bizdate.compareTo(closedate) <= 0){
                throw new KDBizException("所选暂估应付单的当前期间存货核算模块必须未关账!");
            }
        }

        List<ListSelectedRow> list = selectedRows.stream().map(row -> {
            ListSelectedRow listSelectedRow = new ListSelectedRow();
            listSelectedRow.setPrimaryKeyValue(row.getPrimaryKeyValue());
            listSelectedRow.setEntryEntityKey(row.getEntryEntityKey());
            listSelectedRow.setEntryPrimaryKeyValue(row.getEntryPrimaryKeyValue());
            return listSelectedRow;
        }).collect(Collectors.toList());

        if (list.size() > 0) {
            List<Object> collect1 = list.stream().map(row -> {
                return row.getEntryPrimaryKeyValue();
            }).collect(Collectors.toList());
            DynamicObject[] busbillDynamicObject = BusinessDataServiceHelper.load("ap_busbill",
                    "entry.id,entry.nckd_billnumber",
                    new QFilter[]{new QFilter("entry.id",QCP.in,collect1)});

            Arrays.stream(busbillDynamicObject).forEach(dynamicObject -> {
                dynamicObject.getDynamicObjectCollection("entry").forEach(object -> {
                    if(collect1.contains(object.get("id"))){
                        boolean exists = QueryServiceHelper.exists("nckd_endpriceadjust",
                                new QFilter[]{new QFilter("entryentity.nckd_oddnumber", QCP.equals, object.getString("nckd_billnumber"))});
                        if(exists){
                            throw new KDBizException("请勿重复下推!");
                        }
                    }
                });
            });

            // 创建下推参数
            PushArgs pushArgs = new PushArgs();
            //源单单据标识
            String sourceEntityNumber = "ap_busbill";
            //目标单单据标识
            String targetEntityNumber = "nckd_endpriceadjust";
            pushArgs.setSourceEntityNumber(sourceEntityNumber);
            pushArgs.setTargetEntityNumber(targetEntityNumber);
            //不检查目标单新增权限
            pushArgs.setHasRight(true);
            //下推后默认保存
            pushArgs.setAutoSave(true);
            // 是否输出详细错误报告
            pushArgs.setBuildConvReport(true);

            // 单据转换规则id
            ConvertRuleReader reader = new ConvertRuleReader();
            List<String> ruleIds = reader.loadRuleIds(sourceEntityNumber, targetEntityNumber, false);
            if (ruleIds.size() > 0) {
                pushArgs.setRuleId(ruleIds.get(0));
            }

            pushArgs.setSelectedRows(list);

            // 执行下推操作
            ConvertOperationResult result = ConvertServiceHelper.pushAndSave(pushArgs);
            if (!result.isSuccess()) {
                throw new KDBizException("下推失败：" + result.getMessage());
            }

            // 获取下推目标单id
            Set<Object> targetBillIds = result.getTargetBillIds();
            QFilter qf = new QFilter("id", QCP.in, targetBillIds);
            DynamicObject[] load = BusinessDataServiceHelper.load("nckd_endpriceadjust",
                    "id,nckd_businessdate,nckd_accountingperiod,entryentity.nckd_supplier,entryentity.nckd_materialcode,nckd_entryentity.nckd_supplieradjust,nckd_entryentity.nckd_material",
                    qf.toArray());
            for (DynamicObject object : load) {
                object.set("nckd_businessdate",currentperiod.getDate("enddate"));
                object.set("nckd_accountingperiod",currentperiod);

                DynamicObjectCollection entry = object.getDynamicObjectCollection("entryentity");
                Map<String, List<DynamicObject>> collect = entry.stream().collect(Collectors.groupingBy(dynamicObject -> {
                    return dynamicObject.getDynamicObject("nckd_supplier").getPkValue() + "-" + dynamicObject.getDynamicObject("nckd_materialcode").getPkValue();
                }));

                DynamicObjectCollection entryentity = object.getDynamicObjectCollection("nckd_entryentity");
                collect.keySet().stream().forEach(s -> {
                    String[] split = s.split("-");
                    DynamicObject dynamicObject = entryentity.addNew();
                    dynamicObject.set("nckd_supplieradjust", split[0]);
                    dynamicObject.set("nckd_material", split[1]);
                });
            }

            SaveServiceHelper.save(load);

            BillShowParameter billShowParameter = new BillShowParameter();
            billShowParameter.setFormId("nckd_endpriceadjust");
            billShowParameter.setPkId(targetBillIds.toArray()[0]);
            // 打开方式
            billShowParameter.getOpenStyle().setShowType(ShowType.MainNewTabPage);
            this.getView().showForm(billShowParameter);
        }
    }

    /**
     * 获取对应组织的上次关账日期
     * @param orgId
     * @return
     */
    private Date loadGrid(Long orgId) {
        Date closedate = null;
        List<Long> orgList = new ArrayList<>();
        orgList.add(orgId);

        DynamicObjectCollection accSysColl = AccountingSysHelper.getAccountingSysColls(orgList, null);
        if (accSysColl.size() != 0) {
            Set<Long> ownerIdSet = new HashSet();
            Set<Long> calorgSet = new HashSet();
            Iterator var6 = accSysColl.iterator();

            while (var6.hasNext()) {
                DynamicObject accSysInfo = (DynamicObject) var6.next();
                ownerIdSet.add(accSysInfo.getLong("ownerid"));
                calorgSet.add(accSysInfo.getLong("calorgid"));
            }

            Long[] calOrgIds = AccountingSysHelper.getCalOrgIds(calorgSet);
            calorgSet.retainAll(Arrays.asList(calOrgIds));
            Map<Long, Date> calOrgIdCurPeriodMaxEndateMap = CloseAccountParamBuilder.getCalOrgCurPeriodMaxEndDateMap(calorgSet);
            Map<Long, DynamicObject> ownerIdLastCloseAccountDycMap = CloseAccountParamBuilder.getOwnerIdLastCloseAcctDycMap(ownerIdSet);
            List<DynamicObject> hasAccountAccSysDycs = new ArrayList(16);
            Iterator lastInfo = accSysColl.iterator();

            Long calorgid;
            while (lastInfo.hasNext()) {
                DynamicObject accSysDyc = (DynamicObject) lastInfo.next();
                calorgid = accSysDyc.getLong("calorgid");
                if (calorgSet.contains(calorgid)) {
                    hasAccountAccSysDycs.add(accSysDyc);
                }
            }

            accSysColl.clear();
            accSysColl.addAll(hasAccountAccSysDycs);
            if (!accSysColl.isEmpty()) {
                calorgid = accSysColl.get(0).getLong("calorgid");
                if (calorgSet.contains(calorgid)) {
                    Long ownerId = accSysColl.get(0).getLong("ownerid");
                    DynamicObject ownerIdLastClose = ownerIdLastCloseAccountDycMap.get(ownerId);

                    if (ownerIdLastClose != null) {
                        closedate = ownerIdLastClose.getDate("closedate");
                    }
                }
            }
        }

        return closedate;
    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        ListSelectedRowCollection selectedRows = this.getSelectedRows();
        super.beforeItemClick(evt);
        if (evt.getItemKey().equals("nckd_baritemap")){
            Object[] keyValues = selectedRows.getPrimaryKeyValues();
            QFilter qFilter = new QFilter("id", QCP.in, keyValues);
            DynamicObject[] busbills = BusinessDataServiceHelper.load("ap_busbill", "id,sourcebillid,sourcebillno,billno", new QFilter[]{qFilter});
            Set<String> billnos = new HashSet<>();
            //暂估应付单map key:源单编码（采购入库单编码） value:暂估应付单
            Map<String,DynamicObject> billnoMap = new HashMap<>();
            for (DynamicObject dynamicObject : Arrays.asList(busbills)) {
                billnos.add(dynamicObject.getString("sourcebillno"));
                billnoMap.put(dynamicObject.getString("sourcebillno"),dynamicObject);
            }
            QFilter newrecordFilter = new QFilter("cal_feeshare_newrecord.entry.billno",QCP.in,billnos);
            DynamicObject[] newrecordDynamics = BusinessDataServiceHelper.load("cal_feeshare_newrecord", "id,entry.billno", new QFilter[]{newrecordFilter});
            DynamicObjectCollection collectionAll = new DynamicObjectCollection();
            for (DynamicObject dynamicObject : Arrays.asList(newrecordDynamics)){
                collectionAll.addAll(dynamicObject.getDynamicObjectCollection("entry"));
            }
            List<String> msg = new ArrayList<>();
            Map<String,List<DynamicObject>> listMap = collectionAll.stream().collect(Collectors.groupingBy(t->t.getString("billno")));
            for (Map.Entry<String,List<DynamicObject>> entry: listMap.entrySet()){
                if(billnoMap.containsKey(entry.getKey())){
                    msg.add("暂估应付单单据编号:"+billnoMap.get(entry.getKey()).getString("billno")+"对应的采购入库单“单据编号”已做费用分摊，不允许进行调价，请将采购入库单费用反分摊后在进行调价");
                }
            }
            if (CollectionUtils.isNotEmpty(msg)){
                throw new KDBizException(msg.stream().collect(Collectors.joining(",")));
            }
        }
    }
}
