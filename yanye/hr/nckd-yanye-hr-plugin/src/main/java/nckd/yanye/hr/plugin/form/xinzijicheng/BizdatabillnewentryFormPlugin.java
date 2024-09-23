package nckd.yanye.hr.plugin.form.xinzijicheng;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.IFormView;
import kd.bos.form.plugin.AbstractFormPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EventObject;

/**
 * Module           :薪酬福利云-薪资数据集成-业务数据提报新增
 * Description      :动态表单插件
 * 单据标识：hpdi_bizdatabillnewentry
 *
 * @author ：luxiao
 * @since ：Created in 14:19 2024/9/23
 */
public class BizdatabillnewentryFormPlugin extends AbstractFormPlugin {
    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        // 判断是否传了参数
        String isYear = this.getView().getParentView().getPageCache().get("isJH001");
        if ("true".equals(isYear)) {
            int rowCount = this.getModel().getEntryRowCount("entryentity");
            for (int i = 0; i < rowCount; i++) {
                // 锁定3个字段：当月绩效工资基数、当月绩效考核等级、绩效工资分配比例
                this.getView().setEnable(false, i, "JH038", "JH037", "JH039");
            }
        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        IDataModel model = this.getModel();
        IFormView view = this.getView();
        String propertyName = e.getProperty().getName();

        if ("jh004".equals(propertyName) || "jh038".equals(propertyName)) {
            ChangeData changeData = e.getChangeSet()[0];
            int changeRowIndex = changeData.getRowIndex();

            String isYear = this.getView().getParentView().getPageCache().get("isJH001");
            if ("true".equals(isYear)) {
                autoGetProportion(model, changeRowIndex);
            }
        }

    }

    public static void autoGetProportion(IDataModel model, int changeRowIndex) {
        DynamicObjectCollection entryentity = model.getEntryEntity("entryentity");
        for (DynamicObject entry : entryentity) {
            int thisRowIndex = entry.getInt("seq") - 1;
            if (changeRowIndex != thisRowIndex) {
                continue;
            }
            // 月度绩效
            BigDecimal ydjx = entry.getBigDecimal("jh004");
            // 当月绩效工资基数
            BigDecimal jxgzjds = entry.getBigDecimal("jh038");
            if (ydjx != null && jxgzjds != null) {
                // 当月绩效工资分配比例：计算，=月度绩效/当月绩效工资基数
                BigDecimal jxgzfb = ydjx.divide(jxgzjds, 2, RoundingMode.HALF_UP);
                model.setValue("jh039", jxgzfb, changeRowIndex);
            }
        }
    }


    public static void autoGetLevel(IDataModel model) {
        DynamicObjectCollection entryentity = model.getEntryEntity("entryentity");
        for (DynamicObject entry : entryentity) {
            // 当月绩效工资基数取 人员业务归属日期内的薪酬标准绩效工资基数


        }
    }


}
