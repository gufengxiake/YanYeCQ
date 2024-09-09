package nckd.yanye.scm.plugin.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.fi.cal.business.datacheck.DataCheckParam;
import kd.fi.cal.business.datacheck.ExceptionObj;
import kd.fi.cal.business.datacheck.item.DataEntityDataCheck;

/**
 * @author husheng
 * @date 2024-09-09 10:18
 * @description 关账-检查项插件-校验是否存在未审核的财务应付单
 */
public class ApFinapbillCheck extends DataEntityDataCheck {
    @Override
    protected String getDataEntityType() {
        return "im_closeaccount";
    }

    @Override
    protected Set<String> getSelectedFields() {
        return null;
    }

    public List<ExceptionObj> collectExceptionObj(DataCheckParam param){
        Set<Long> ownerIds = param.getCalorg();
        List<ExceptionObj> exceptionObjs = new ArrayList(16);

        // 查询对应组织的会计期间
        QFilter filter = new QFilter("org", QCP.in, ownerIds)
                .and("entry.isenabled", QCP.equals, true);
        DynamicObject sysctrlentity = BusinessDataServiceHelper.loadSingle("cal_sysctrlentity", filter.toArray());
        DynamicObjectCollection sysctrlentityEntry = sysctrlentity.getDynamicObjectCollection("entry");
        DynamicObject currentperiod = sysctrlentityEntry.get(0).getDynamicObject("currentperiod");

        // 查询对应期间未审核的月末调价单
        QFilter qFilter = new QFilter("org", QCP.in, ownerIds)
                .and("billstatus",QCP.not_equals,"C")
                .and("bizdate",QCP.large_equals,currentperiod.getDate("begindate"))
                .and("bizdate",QCP.less_equals,currentperiod.getDate("enddate"));
        DynamicObject[] apFinapbills = BusinessDataServiceHelper.load("ap_finapbill", "id,billno", qFilter.toArray());
        if(apFinapbills.length > 0){
            for (DynamicObject dynamicObject : apFinapbills) {
                ExceptionObj exceptionObj = new ExceptionObj((Long) dynamicObject.getPkValue(), this.getDataEntityType());
                exceptionObj.setDescription("存在未审核的财务应付单:" + dynamicObject.getString("billno"));
                exceptionObjs.add(exceptionObj);
            }
        }

        return exceptionObjs;
    }

    @Override
    public List<ExceptionObj> collectExceptionObj(DataSet dataSet, DataCheckParam dataCheckParam) {
        return null;
    }
}
