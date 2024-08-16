package nckd.yanye.tmc.plugin.form;

import cn.hutool.core.util.ObjectUtil;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/**
 * Module           :财务云-出纳-收款认领
 * Description      :1.认领处理单，系统控制应收金额不能超过关联的销售订单-收款计划明细行的未关联收款金额，系统关联销售订单时，也会按照明细行进行关联，所以按照明细行进行控制就可以；
 *
 * @author : zhujintao
 * @date : 2024/8/6
 */
public class CasClaimbillFormPlugin extends AbstractBillPlugIn {
    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String name = e.getProperty().getName();
        if (StringUtils.equals("e_receivableamt", name)) {
            ChangeData changeData = e.getChangeSet()[0]; //修改值所在行
            DynamicObject dataEntity = changeData.getDataEntity(); //修改值所在行数据
            Object newValue = changeData.getNewValue();//新值
            int rowIndex = changeData.getRowIndex(); //修改行所在行行号
            String billType = dataEntity.getString("e_corebilltype");
            String billNo = dataEntity.getString("e_corebillno");
            int eCorebillentryseq = dataEntity.getInt("e_corebillentryseq");
            BigDecimal eReceivableamt = dataEntity.getBigDecimal("e_receivableamt");
            //必须是销售订单且核心单据编号不为空
            if ("sm_salorder".equals(billType) && ObjectUtil.isNotEmpty(billNo) && ObjectUtil.isNotEmpty(newValue)) {
                QFilter qFilter = new QFilter("billno", QCP.equals, billNo);
                DynamicObject smSalorder = BusinessDataServiceHelper.loadSingle("sm_salorder", "id,recplanentry.seq,recplanentry.r_unremainamount", qFilter.toArray());
                DynamicObjectCollection recplanentryColl = smSalorder.getDynamicObjectCollection("recplanentry");
                DynamicObject recplanentry = recplanentryColl.get(eCorebillentryseq - 1);
                BigDecimal rUnremainamount = recplanentry.getBigDecimal("r_unremainamount");
                if (eReceivableamt.compareTo(rUnremainamount) > 0) {
                    this.getView().showErrorNotification("应收金额不能大于销售订单" + billNo + "收款计划第" + eCorebillentryseq + "行的未关联收款金额");
                    this.getModel().setValue("e_actamt", 0, rowIndex);
                    this.getModel().setValue("e_receivableamt", 0, rowIndex);
                }
            }
        }
    }
}
