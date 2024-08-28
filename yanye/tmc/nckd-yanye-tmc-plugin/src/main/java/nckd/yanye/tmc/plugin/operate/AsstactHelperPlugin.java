package nckd.yanye.tmc.plugin.operate;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import org.apache.commons.lang3.ObjectUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * Module           :财务云-应付-付款申请-付款申请单
 * Description      :付款申请单-选择付款方工具类
 *
 *
 * @author guozhiwei
 * @date  2024/8/12 14:20
 * 标识 nckd_ap_payapply_ext
 *
 *
 */

public class AsstactHelperPlugin {

    public AsstactHelperPlugin() {
    }

    public static Map<Object, Object> getaccbebankMap(DynamicObject asstact) {
        Map<Object, Object> map = new HashMap();
        String accountOb = null;
        Long bebankOb = null;
        DynamicObject settlementtypeId = null;
        if (asstact == null) {
            map.put("account", accountOb);
            map.put("bebank", bebankOb);
            map.put("settlementtype", settlementtypeId);
            return map;
        } else {
            String asstactType = asstact.getDataEntityType().getName();
            if ("bos_user".equals(asstactType)) {
                DynamicObject erPayeeInfo = getErPayeeInfo(asstact.getLong("id"));
                if (erPayeeInfo != null) {
                    long bebank = erPayeeInfo.getDynamicObject("payerbank").getLong("id");
                    accountOb = erPayeeInfo.getString("payeraccount");
                    bebankOb = bebank;
                    settlementtypeId = null;
                }
            } else if (!"bd_customer".equals(asstactType) && !"bd_supplier".equals(asstactType)) {
                QFilter filter = new QFilter("company", "=", asstact.getPkValue());
                filter.and(new QFilter("status", "=", "C"));
                filter.and(new QFilter("acctstatus", "=", "normal"));
                DynamicObject account = BusinessDataServiceHelper.loadSingleFromCache("bd_accountbanks", "id,bank.id,bankaccountnumber,bank.bebank", new QFilter[]{filter});
                if (!ObjectUtils.isEmpty(account)) {
                    accountOb = account.getString("bankaccountnumber");
                    if (!ObjectUtils.isEmpty(account.getDynamicObject("bank.bebank"))) {
                        bebankOb = (Long)account.getDynamicObject("bank.bebank").getPkValue();
                    }

                    settlementtypeId = null;
                }
            } else {
                asstact = BusinessDataServiceHelper.loadSingleFromCache(asstact.getPkValue(), asstactType);
                DynamicObjectCollection bankColls = asstact.getDynamicObjectCollection("entry_bank");

                for(int i = 0; i < bankColls.size(); ++i) {
                    DynamicObject bankInfo = (DynamicObject)bankColls.get(i);
                    DynamicObject bank;
                    if (bankInfo.getBoolean("isdefault_bank")) {
                        accountOb = bankInfo.getString("bankaccount");
                        bank = bankInfo.getDynamicObject("bank");
                        if (!ObjectUtils.isEmpty(bank)) {
                            bebankOb = bank.getLong("id");
                        }
                        break;
                    }

                    if (i == 0) {
                        accountOb = bankInfo.getString("bankaccount");
                        bank = bankInfo.getDynamicObject("bank");
                        if (!ObjectUtils.isEmpty(bank)) {
                            bebankOb = bank.getLong("id");
                        }
                    }
                }
                // 是内部供应商
                if("bd_supplier".equals(asstactType) && ObjectUtils.isNotEmpty(asstact.getDynamicObject("internal_company"))){
                    // 更新银行账号和银行信息

                    Map<String,Object> innerSupplier = isInnerSupplier(asstact.getPkValue());
                    if(innerSupplier != null){
                        accountOb = (String) innerSupplier.get("number");
                        bebankOb = (Long) innerSupplier.get("bankid");
                    }

                }

                settlementtypeId = asstact.getDynamicObject("settlementtypeid");
            }

            map.put("account", accountOb);
            map.put("bebank", bebankOb);
            map.put("settlementtypeid", settlementtypeId);
            return map;
        }
    }

    private static DynamicObject getErPayeeInfo(long userId) {
        DynamicObject defaultAccount = null;
        QFilter uFilter = new QFilter("payer", "=", userId);
        uFilter = uFilter.and(new QFilter("status", "=", 'C')).and(new QFilter("enable", "=", Boolean.TRUE));
        QFilter[] qFilters = new QFilter[]{uFilter};
        String selectFields = "id,payerbank,payeraccount";
        Map<Object, DynamicObject> defaultAccountMap = BusinessDataServiceHelper.loadFromCache("er_payeer", selectFields, qFilters, "isdefault desc");
        if (defaultAccountMap != null && defaultAccountMap.size() > 0) {
            defaultAccount = ((DynamicObject[])defaultAccountMap.values().toArray(new DynamicObject[0]))[0];
        }

        return defaultAccount;
    }

    /**
     * 校验是否是内部供应商
     *
     */

    public static Map<String, Object> isInnerSupplier(Object supplierid) {
        Map<String, Object> map = new HashMap<>();

        // 查询是否存在内部公司
        DynamicObject o = (DynamicObject) BusinessDataServiceHelper.loadSingle(supplierid, "bd_supplier").get("internal_company");
        if (ObjectUtils.isNotEmpty(o)) {
            QFilter qFilter = new QFilter("openorg.masterid", "=", o.getPkValue());
            // 查询供应商的银行账户信息
            DynamicObject amAccountbank = BusinessDataServiceHelper.loadSingle("am_accountbank", "bank,bankaccountnumber,currency", new QFilter[]{qFilter});
            if (ObjectUtils.isNotEmpty(amAccountbank)) {
                // 合作金融机构
                map.put("number", amAccountbank.getString("bankaccountnumber"));
                amAccountbank.getLong("bank.id");
                //  查询银行账户是否有对应票据开户行信息，如果有，则设置到e_bebank
                QFilter qFilter2 = new QFilter("account.masterid", "=", amAccountbank.getPkValue());
                // 合作金融机构
                Object cooperationId = null;
                DynamicObject billbank = BusinessDataServiceHelper.loadSingle("am_accountmaintenance","billbank.id",new QFilter[]{qFilter2});
                if (ObjectUtils.isNotEmpty(billbank)) {
                    cooperationId = amAccountbank.get("bank.id");
                }else{
                    cooperationId = billbank.getLong("billbank.id");
                }
                // 合作金融机构信息
                DynamicObject bdFinorginfo = BusinessDataServiceHelper.loadSingle(cooperationId, "bd_finorginfo");
                map.put("bankid",bdFinorginfo.getLong("bebank.id"));
                return map;
            }
        }
        return null;
    }


}
