package nckd.yanye.scm.plugin.check;

import java.util.*;

import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.fi.cal.business.datacheck.DataCheckParam;
import kd.fi.cal.business.datacheck.ExceptionObj;
import kd.fi.cal.business.datacheck.item.DataEntityDataCheck;

/**
 * @author husheng
 * @date 2024-07-24 18:02
 * @description 关账-检查项插件-校验存是否在未审核的月末调价单
 */
public class CloseAccountCheck extends DataEntityDataCheck {
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

        // 查询对应组织的会计期间
        QFilter filter = new QFilter("org", QCP.in, ownerIds)
                .and("entry.isenabled", QCP.equals, true);
        DynamicObject sysctrlentity = BusinessDataServiceHelper.loadSingle("cal_sysctrlentity", filter.toArray());
        DynamicObjectCollection sysctrlentityEntry = sysctrlentity.getDynamicObjectCollection("entry");
        DynamicObject currentperiod = sysctrlentityEntry.get(0).getDynamicObject("currentperiod");

        // 查询对应期间未审核的月末调价单
        QFilter qFilter = new QFilter("nckd_adjustaccountsorg", QCP.in, ownerIds)
                .and("nckd_accountingperiod",QCP.equals,currentperiod.getPkValue())
                .and("billstatus",QCP.not_equals,"C");
        DynamicObject[] endpriceadjusts = BusinessDataServiceHelper.load("nckd_endpriceadjust", "id", qFilter.toArray());
        if(endpriceadjusts.length > 0){
            throw new KDBizException("存在未审核的月末调价单");
        }

        return null;
    }

    @Override
    public List<ExceptionObj> collectExceptionObj(DataSet dataSet, DataCheckParam dataCheckParam) {
        return null;
    }
}
