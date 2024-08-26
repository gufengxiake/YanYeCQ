package nckd.yanye.scm.plugin.operate;

import java.util.*;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.fi.cal.business.account.CloseAccountParamBuilder;
import kd.fi.cal.common.helper.AccountingSysHelper;

/**
 * @author husheng
 * @date 2024-08-26 15:35
 * @description 财务应付单(nckd_ap_finapbill_ext) 保存和提交操作添加校验
 */
public class ApFinapbillOpPlugin extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);

        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.addAll(this.billEntityType.getAllFields().keySet());
    }

    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                ExtendedDataEntity[] entities = this.getDataEntities();
                Arrays.asList(entities).forEach(k -> {
                    DynamicObject dynamicObject = k.getDataEntity();
                    // 获取对应组织的上次关账日期
                    Date closedate = loadGrid(dynamicObject.getDynamicObject("org").getLong("id"));
                    if (closedate != null && dynamicObject.getDate("bizdate").compareTo(closedate) <= 0 && dynamicObject.getDate("bookdate").compareTo(closedate) <= 0) {
                        this.addErrorMessage(k, "财务应付单“单据日期”和“记账日期”，要大于存货核算“关账日期”");
                    }
                });
            }
        });
    }

    /**
     * 获取对应组织的上次关账日期
     *
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
