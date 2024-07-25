package nckd.yanye.scm.plugin.form;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.botp.runtime.ConvertOperationResult;
import kd.bos.entity.botp.runtime.PushArgs;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.exception.KDBizException;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.metadata.botp.ConvertRuleReader;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.botp.ConvertServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
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

        List<ListSelectedRow> rows = new ArrayList<>();
        Long orgId = null;
        String period = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyyMM");
        Map<String, List<DynamicObject>> map = new HashMap<>();
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

            if(bizdate.compareTo(closedate) < 0){
                throw new KDBizException("当前期间存货核算模块必须未关账!");
            }

            DynamicObjectCollection entry = dataEntity.getDynamicObjectCollection("entry");
            Map<String, List<DynamicObject>> collect = entry.stream().collect(Collectors.groupingBy(dynamicObject -> {
                return dynamicObject.getDynamicObject("nckd_supplier").getPkValue() + "-" + dynamicObject.getDynamicObject("e_material").getPkValue();
            }));
            map.putAll(collect);

            rows.add(new ListSelectedRow(dataEntity.getPkValue()));
        }

        if (rows.size() > 0) {
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
            //是否生成单据转换报告
            pushArgs.setBuildConvReport(false);

            // 单据转换规则id
            ConvertRuleReader reader = new ConvertRuleReader();
            List<String> ruleIds = reader.loadRuleIds(sourceEntityNumber, targetEntityNumber, false);
            if (ruleIds.size() > 0) {
                pushArgs.setRuleId(ruleIds.get(0));
            }

            pushArgs.setSelectedRows(rows);

            // 执行下推操作
            ConvertOperationResult result = ConvertServiceHelper.pushAndSave(pushArgs);
            if (!result.isSuccess()) {
                throw new KDBizException("下推失败：" + result.getMessage());
            }

            // 获取下推目标单id
            Set<Object> targetBillIds = result.getTargetBillIds();
            QFilter qf = new QFilter("id", QCP.in, targetBillIds);
            DynamicObject[] load = BusinessDataServiceHelper.load("nckd_endpriceadjust","id,nckd_businessdate,nckd_accountingperiod,nckd_entryentity.nckd_supplieradjust,nckd_entryentity.nckd_material",qf.toArray());
            DynamicObject dynamicObject1 = load[0];
            dynamicObject1.set("nckd_businessdate",currentperiod.getDate("enddate"));
            dynamicObject1.set("nckd_accountingperiod",currentperiod);
            DynamicObjectCollection entryentity = dynamicObject1.getDynamicObjectCollection("nckd_entryentity");
            map.keySet().stream().forEach(s -> {
                String[] split = s.split("-");
                DynamicObject dynamicObject = entryentity.addNew();
                dynamicObject.set("nckd_supplieradjust", split[0]);
                dynamicObject.set("nckd_material", split[1]);
            });

            SaveServiceHelper.save(load);
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
}
