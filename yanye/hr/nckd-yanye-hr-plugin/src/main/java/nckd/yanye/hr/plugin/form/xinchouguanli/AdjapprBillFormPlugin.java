package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.IFormView;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.sdk.swc.hcdm.service.spi.SalaryStdQueryService;

/**
 * 员工定调薪申请单-表单插件
 * 单据标识：nckd_hcdm_adjapprbill_ext
 *
 * @author ：luxiao
 * @since ：Created in 14:38 2024/9/12
 */
public class AdjapprBillFormPlugin extends AbstractFormPlugin {

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        IDataModel model = this.getModel();
        IFormView view = this.getView();
        // 非调动调薪不作处理
        DynamicObject salaryadjrsn = (DynamicObject) model.getValue("salaryadjrsn");
        if (salaryadjrsn == null) {
            return;
        }
        String salaryadjrsnName = salaryadjrsn.getString("name");
        if (!"调动调薪".equals(salaryadjrsnName)) {
            return;
        }

        // 本次调薪信息：薪等变化，执行
        String propertyName = e.getProperty().getName();
        if ("dy_grade".equals(propertyName)) {
            DynamicObjectCollection entryentity = model.getEntryEntity("adjapprdetailentry");
            for (DynamicObject entry : entryentity) {
                // 本次调薪信息：薪等
                DynamicObject grade = entry.getDynamicObject("dy_grade");
                if (grade == null) {
                    continue;
                }
                // 上次薪酬信息：薪等
                DynamicObject pregrade = entry.getDynamicObject("dy_pregrade");
                if (pregrade == null) {
                    continue;
                }
                // 薪等相同，则薪档为原薪档。
                int gradeIndex = grade.getInt("gradeindex");
                int preGradeIndex = pregrade.getInt("gradeindex");
                if (gradeIndex == preGradeIndex) {
                    entry.set("dy_rank", entry.getDynamicObject("dy_prerank"));
                    view.updateView("adjapprdetailentry");
                    return;
                }

                // 获取此薪档下2档金额和1档金额
                // 本次薪酬标准表
                DynamicObject salarystd = entry.getDynamicObject("dy_salarystd");
                // SPI接口2：获取标准表的薪档信息
//                SalaryStdQueryService.get().getRankInfo();

                // 晋升：取调动后岗位的薪等，取（此薪等下2档金额-1档金额）*2+原金额，得出调整后金额，
                // 再按照就高原则匹配到对应的薪档，系统会根据薪档得到本次调薪后的金额
                if (gradeIndex > preGradeIndex) {

                }

                // 下降：取（原薪等下2档金额-1档金额）*（-2）+原金额，得出调整后金额，
                // 再按照就低原则匹配到对应的薪档，系统会根据薪档得到本次调薪后的金额。
                if (gradeIndex < preGradeIndex) {

                }


            }
        }
    }
}
