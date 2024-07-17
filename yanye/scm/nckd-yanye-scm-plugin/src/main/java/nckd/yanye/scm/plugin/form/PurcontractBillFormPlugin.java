package nckd.yanye.scm.plugin.form;

import cn.hutool.core.util.ObjectUtil;
import com.kingdee.util.StringUtils;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;

import java.math.BigDecimal;

/**
 * Module           :供应链云-采购管理-采购铬铜
 * Description      :采购铬铜表单插件
 *
 * @author : zhujintao
 * @date : 2024/7/16
 */
public class PurcontractBillFormPlugin extends AbstractBillPlugIn {
    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String name = e.getProperty().getName();
        //采购合同明细输入含税单价，含税单价上下限默认等于含税单价，且上限大于等于含税单价大于等于下限
        if (StringUtils.equals("nckd_priceandtaxup", name)) {
            ChangeData changeData = e.getChangeSet()[0]; //修改值所在行
            DynamicObject dataEntity = changeData.getDataEntity(); //修改值所在行数据
            Object newValue = changeData.getNewValue();//新值
            Object oldValue = changeData.getOldValue();
            if (ObjectUtil.isEmpty(newValue)) {
                this.getView().showErrorNotification("含税单价上限不能为空");
                return;
            }
            //int rowIndex = changeData.getRowIndex(); //修改行所在行行号
            //获取所在行的含税单价下限
            BigDecimal nckdPriceandtaxup = (BigDecimal) newValue;
            BigDecimal nckdPriceandtaxlow = dataEntity.getBigDecimal("nckd_priceandtaxlow");
            if (nckdPriceandtaxup.compareTo(nckdPriceandtaxlow) < 0) {
                dataEntity.set("nckd_priceandtaxup", oldValue);
                this.getView().showErrorNotification("含税单价上限不能小于含税单价下限");
                this.getView().updateView();
                return;
            }
        }
        if (StringUtils.equals("nckd_priceandtaxlow", name)) {
            ChangeData changeData = e.getChangeSet()[0]; //修改值所在行
            DynamicObject dataEntity = changeData.getDataEntity(); //修改值所在行数据
            Object newValue = changeData.getNewValue();//新值
            Object oldValue = changeData.getOldValue();
            if (ObjectUtil.isEmpty(newValue)) {
                this.getView().showErrorNotification("含税单价下限不能为空");
                return;
            }
            //int rowIndex = changeData.getRowIndex(); //修改行所在行行号
            //获取所在行的含税单价上限
            BigDecimal nckdPriceandtaxlow = (BigDecimal) newValue;
            BigDecimal nckdPriceandtaxup = dataEntity.getBigDecimal("nckd_priceandtaxup");
            if (nckdPriceandtaxup.compareTo(nckdPriceandtaxlow) < 0) {
                dataEntity.set("nckd_priceandtaxlow", oldValue);
                this.getView().showErrorNotification("含税单价上限不能小于含税单价下限");
                this.getView().updateView();
                return;
            }
        }
    }
}
