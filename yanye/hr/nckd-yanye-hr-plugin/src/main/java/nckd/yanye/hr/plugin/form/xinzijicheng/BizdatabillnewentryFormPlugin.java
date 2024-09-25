package nckd.yanye.hr.plugin.form.xinzijicheng;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.AfterAddRowEventArgs;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.EventObject;
import java.util.Map;
import java.util.stream.Collectors;

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
            lockField();
        }
    }

    @Override
    public void afterAddRow(AfterAddRowEventArgs e) {
        super.afterAddRow(e);
        lockField();
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        lockField();
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        IDataModel model = this.getModel();
        String propertyName = e.getProperty().getName();

        ChangeData changeData = e.getChangeSet()[0];
        int changeRowIndex = changeData.getRowIndex();

        String isYear = this.getView().getParentView().getPageCache().get("isJH001");
        if ("true".equals(isYear)) {
            String errMsg = null;
            if ("jh004".equals(propertyName) || "jh038".equals(propertyName)) {
                autoGetProportion(model, changeRowIndex);
            }
            if ("bizdate".equals(propertyName)) {
                errMsg = autoGetCardinal(model, changeRowIndex);
            }

            if (errMsg != null) {
                this.getView().showErrorNotification("注意：" + errMsg);
            }
        }

    }

    /**
     * 生成分配比例
     *
     * @param model
     * @param changeRowIndex
     */
    public static void autoGetProportion(IDataModel model, int changeRowIndex) {
        DynamicObjectCollection entryentity = model.getEntryEntity("entryentity");
        DynamicObject entry = entryentity.get(changeRowIndex);
        // 月度绩效
        BigDecimal ydjx = null;
        BigDecimal jxgzjds = null;
        try {
            ydjx = entry.getBigDecimal("JH004");
            // 当月绩效工资基数
            jxgzjds = entry.getBigDecimal("JH038");
        } catch (Exception e) {
            return;
        }
        if (ydjx != null && jxgzjds != null) {
            // 当月绩效工资分配比例：计算，=月度绩效/当月绩效工资基数
            BigDecimal jxgzfb = ydjx.divide(jxgzjds, 2, RoundingMode.HALF_UP);
            String errMsg = checkProportion(entry, jxgzfb);
            model.setValue("JH039", jxgzfb, changeRowIndex);
            model.setValue("remark", errMsg, changeRowIndex);
        }

    }

    /**
     * 分配比例校验
     *
     * @param entry
     * @param jxgzfb
     * @return
     */
    private static String checkProportion(DynamicObject entry, BigDecimal jxgzfb) {
        // 当月绩效考核等级
        String level = entry.getString("JH037");
        if (StringUtils.isBlank(level)) {
            return level;
        }
        // 获取所有任职经历Map <员工工号, 职级名称>
        DynamicObject[] jobExpArray = BusinessDataServiceHelper.load(
                "hrpi_empposorgrel",
                "person,nckd_zhiji",
                new QFilter[]{
                        // 是否主任职：是
                        new QFilter("isprimary", QCP.equals, "1"),
                        // 开始日期小于今天
                        new QFilter("startdate", QCP.less_than, new Date()),
                        // 结束日期大于今天
                        new QFilter("enddate", QCP.large_than, new Date()),
                        // 业务状态：生效中
                        new QFilter("businessstatus", QCP.equals, "1"),
                        // 数据状态
                        new QFilter("datastatus", QCP.equals, "1"),
                        // 当前版本
                        new QFilter("iscurrentversion", QCP.equals, "1"),
                }
        );
        Map<String, String> jobExpMap = Arrays.stream(jobExpArray)
                .collect(Collectors.toMap(
                        obj -> obj.getString("person.number"),
                        obj -> obj.getString("nckd_zhiji.name") == null ? "" : obj.getString("nckd_zhiji.name")
                ));
        // 工号
        DynamicObject empposorgrel = entry.getDynamicObject("empposorgrel");
        String userNumber = empposorgrel.getString("person.number");

        String zhiJi = jobExpMap.get(userNumber);
        if (StringUtils.isBlank(zhiJi)) {
            // todo 找不到职级信息
            return level;
        }
        if ("员工".equals(zhiJi)) {
            if ("良好".equals(level)) {
                // 员工：比例必须大于等于1.1小于1.2
                if (!(jxgzfb.compareTo(new BigDecimal("1.1")) >= 0 && jxgzfb.compareTo(new BigDecimal("1.2")) < 0)) {
                    return "分配比例超限";
                }
            }
        }

        if (!"员工".equals(zhiJi) && !"其他".equals(zhiJi)) {
            if ("良好".equals(level)) {
                // 中层：比例必须等于1.1
                if (jxgzfb.compareTo(new BigDecimal("1.1")) != 0) {
                    return "分配比例超限";
                }
            }
        }
        return null;
    }

    /**
     * 生成当月绩效工资基数
     *
     * @param model
     * @param changeRowIndex
     * @return
     */
    public static String autoGetCardinal(IDataModel model, int changeRowIndex) {
        DynamicObjectCollection entryentity = model.getEntryEntity("entryentity");
        DynamicObject entry = entryentity.get(changeRowIndex);

        // 当月绩效工资基数取 人员业务归属日期内的薪酬标准绩效工资基数
        // 工号
        DynamicObject empposorgrel = entry.getDynamicObject("empposorgrel");
        String userNumber = empposorgrel.getString("person.number");
        // 薪酬管理组织
        String orgNumber = empposorgrel.getString("adminorg.number");
        // 业务归属日期
        Date bizDate = entry.getDate("bizdate");
        if (bizDate == null) {
            return null;
        }

        // 查询定调薪信息
        DynamicObject[] records = BusinessDataServiceHelper.load(
                "hcdm_salaryadjrecord",
                "amount,bsed,bsled",
                new QFilter[]{
                        // 数据状态
                        new QFilter("datastatus", QCP.equals, "1"),
                        // 当前版本
                        new QFilter("iscurrentversion", QCP.equals, "1"),
                        // 工号
                        new QFilter("salaryadjfile.person.number", QCP.equals, userNumber),
                        // 行政组织
                        new QFilter("salaryadjfile.adminorg.number", QCP.equals, orgNumber),
                        // 定调薪项目：绩效工资基数
                        new QFilter("standarditem.name", QCP.equals, "绩效工资基数"),
                        // 业务归属日期大于当前日期
                        new QFilter("bsed", QCP.less_than, bizDate),
                        // 业务归属日期小于当前日期
                        new QFilter("bsled", QCP.large_than, bizDate),
                }
        );

        String errMsg = null;
        if (records == null || records.length == 0) {
            errMsg = "未找到该员工的绩效工资基数";
            model.setValue("remark", errMsg, changeRowIndex);
        } else {
            BigDecimal amount = records[0].getBigDecimal("amount").setScale(2, RoundingMode.HALF_UP);
            model.setValue("JH038", amount, changeRowIndex);
            model.setValue("remark", null, changeRowIndex);
        }
        return errMsg;
    }


    private void lockField() {
        int rowCount = this.getModel().getEntryRowCount("entryentity");
        for (int i = 0; i < rowCount; i++) {
            // 锁定3个字段：当月绩效工资基数、当月绩效考核等级、绩效工资分配比例
            this.getView().setEnable(false, i,
                    "JH038",
//                        "JH037",
                    "JH039"
            );
        }
    }
}
