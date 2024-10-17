package nckd.yanye.tmc.plugin.function;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.formula.ExpressionContext;
import kd.bos.entity.function.BOSUDFunction;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;

/**
 * 资金-自定义拓展函数
 * 表单标识：bos_devp_billfunction
 * author：xiaoxiaopeng
 * date：2024-10-17
 */
public class GetCasRecByCustomerFunction implements BOSUDFunction {
    @Override
    public BOSUDFunction getInstance(ExpressionContext expressionContext) {
        return new GetCasRecByCustomerFunction(expressionContext);
    }

    @Override
    public String getName() {
        return "getCustomer";
    }
    public GetCasRecByCustomerFunction(){}

    public GetCasRecByCustomerFunction(ExpressionContext expressionContext) {
    }

    @Override
    public Boolean call(Object... objects) {
        if (objects.length == 1 && objects[0] instanceof String) {
            Boolean result = this.getCasRecByCustomerFunction((String)objects[0]);
            return result;
        } else {
            throw new KDBizException("Incorrect Input Parameter Format.");
        }
    }

    private Boolean getCasRecByCustomerFunction(String object) {
        DynamicObject customer = BusinessDataServiceHelper.loadSingle("bd_customer", "id,name,nckd_entry_english,nckd_entry_english.nckd_engname",
                new QFilter[]{new QFilter("nckd_entry_english.nckd_engname", QCP.equals, object)});
        if (customer == null) {
           return false;
        }
        return true;
    }
}
