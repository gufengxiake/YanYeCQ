package nckd.yanye.scm.plugin.operate;

import java.util.Arrays;
import java.util.Date;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.imsc.dmw.utils.DateUtils;

/**
 * @author husheng
 * @date 2024-09-19 10:39
 * @description 检查结果（nckd_cal_datacheck_re_ext）的修改按钮--》财务应收单的业务日期及记账日期统一调整至下月1号
 */
public class CalDatacheckResultArFinarbillOpPlugin extends AbstractOperationServicePlugIn {
    @Override
    public void endOperationTransaction(EndOperationTransactionArgs e) {
        super.endOperationTransaction(e);
        DynamicObject[] dataEntities = e.getDataEntities();

        Arrays.stream(dataEntities).forEach(dynamicObject -> {
            // 核算组织
            String calorg = dynamicObject.getString("calorg");
            DynamicObject org = BusinessDataServiceHelper.loadSingle("bos_org", new QFilter[]{new QFilter("name", QCP.equals, calorg)});

            QFilter filter = new QFilter("org", QCP.equals, org.getPkValue())
                    .and("entry.isenabled", QCP.equals, true);
            DynamicObject sysctrlentity = BusinessDataServiceHelper.loadSingle("cal_sysctrlentity", filter.toArray());
            DynamicObjectCollection sysctrlentityEntry = sysctrlentity.getDynamicObjectCollection("entry");
            DynamicObject currentperiod = sysctrlentityEntry.get(0).getDynamicObject("currentperiod");

            // 获取校验是否存在未审核的财务应付单的检查项
            DynamicObject object = dynamicObject.getDynamicObjectCollection("entryentity").stream()
                    .filter(d -> "DC-ITEM-47".equals(d.getDynamicObject("checkitem").getString("number")))
                    .findFirst().orElse(null);

            // 获取子单据
            object.getDynamicObjectCollection("subentryentity").stream().forEach(subentry -> {
                String[] objdes = subentry.getString("objdes").split(":");
                // 财务应付单
                DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle("ar_finarbill", new QFilter[]{new QFilter("billno", QCP.equals, objdes[1])});

                loadSingle.set("bizdate", getDate(currentperiod.getDate("enddate")));
                loadSingle.set("bookdate", getDate(currentperiod.getDate("enddate")));
                SaveServiceHelper.update(new DynamicObject[]{loadSingle});
            });
        });
    }

    /**
     * 获取对应日期的下月一号日期
     *
     * @param date
     * @return
     */
    private Date getDate(Date date) {
        String format = DateUtils.format(date, "yyyy-MM-dd");
        String[] split = format.split("-");
        String newDate;
        if ("12".equals(split[1])) {
            Integer year = Integer.parseInt(split[0]) + 1;
            newDate = year.toString() + "-01-01";
        } else {
            Integer month = Integer.parseInt(split[1]) + 1;
            newDate = split[0] + "-" + month.toString() + "-01";
        }

        return DateUtils.parseDate(newDate);
    }
}
