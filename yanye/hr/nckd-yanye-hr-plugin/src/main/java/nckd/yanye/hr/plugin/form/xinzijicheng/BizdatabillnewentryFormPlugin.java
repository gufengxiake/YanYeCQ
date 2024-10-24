package nckd.yanye.hr.plugin.form.xinzijicheng;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.IFormView;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.control.events.CellClickEvent;
import kd.bos.form.control.events.CellClickListener;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.util.StringUtils;
import nckd.yanye.hr.plugin.form.xinchouguanli.AdjapprSelectExtPlugin;

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
public class BizdatabillnewentryFormPlugin extends AbstractFormPlugin implements CellClickListener {
    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        // 锁定列
        String isYear = this.getView().getParentView().getPageCache().get("isJH001");
        if ("true".equals(isYear)) {
            lockField(this.getModel(), this.getView());
        }
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
            if ("jh004".equals(propertyName) || "jh038".equals(propertyName) || "jh037".equals(propertyName)) {
                errMsg = autoGetProportion(model, changeRowIndex);
            }
            if ("bizdate".equals(propertyName)) {
                errMsg = autoGetCardinal(model, changeRowIndex);
            }
            if (errMsg != null) {
                this.getView().showErrorNotification(String.format("注意：“业务数据”第%s行，%s", changeRowIndex + 1, errMsg));
            }
        }

    }

    /**
     * 生成分配比例
     *
     * @param model
     * @param changeRowIndex
     * @return
     */
    public static String autoGetProportion(IDataModel model, int changeRowIndex) {
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
            return null;
        }
        if (ydjx != null && jxgzjds != null) {
            // 当月绩效工资分配比例：计算，=月度绩效/当月绩效工资基数
            BigDecimal jxgzfb = ydjx.divide(jxgzjds, 2, RoundingMode.HALF_UP);
            String errMsg = checkProportion(entry, jxgzfb);
            model.setValue("JH039", jxgzfb, changeRowIndex);
            model.setValue("remark", errMsg, changeRowIndex);
            return errMsg;
        }
        return null;
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
            return null;
        }
        // 获取所有任职经历Map <员工工号, 职级名称>
        Map<String, String> jobExpMap = AdjapprSelectExtPlugin.getJobExpMap();
        // 工号
        DynamicObject empposorgrel = entry.getDynamicObject("empposorgrel");
        String userNumber = empposorgrel.getString("person.number");

        String zhiJi = jobExpMap.get(userNumber);
        if (StringUtils.isEmpty(zhiJi)) {
            return "找不到该人员的职级信息";
        }

        // 查出晶昊分配比例限制
        DynamicObject ygLimit = BusinessDataServiceHelper.loadSingle(
                "nckd_proplimits",
                new QFilter[]{
                        new QFilter("nckd_zhiji.fbasedataid.name", QCP.in, zhiJi),
                        new QFilter("enable", QCP.equals, "1"),
                }
        );

        if (ygLimit == null) {
            return "找不到该人员对应的分配比例限制，请维护！";
        }

        // 找到对应等级的 分配比例限制
        DynamicObjectCollection entryentity = ygLimit.getDynamicObjectCollection("nckd_entryentity");
        DynamicObject result = entryentity.stream()
                .filter(dynamicObject -> level.equals(dynamicObject.getString("nckd_perflevelhr.name")))
                .findFirst()
                .orElse(null);
        if (result == null) {
            return String.format("该人员对应的分配比例限制中找不到绩效等级为“%s”的数据，请维护！", level);
        }
        // 最低限制（大于等于）
        BigDecimal minLimit = result.getBigDecimal("nckd_minlimit");
        // 最大限制（小于）
        BigDecimal maxLimit = result.getBigDecimal("nckd_maxlimit");

        // 比例校验
        if (!(jxgzfb.compareTo(minLimit) >= 0 && jxgzfb.compareTo(maxLimit) < 0)) {
            return "分配比例超限";
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
                "amount,bsed,bsled,salaryadjfile,standarditem,datastatus,iscurrentversion",
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
                        // 生效日期小于业务归属日期
                        new QFilter("bsed", QCP.less_equals, bizDate),
                        // 失效日期大于业务归属日期
                        new QFilter("bsled", QCP.large_equals, bizDate),
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


    public static void lockField(IDataModel model, IFormView view) {
        int rowCount = model.getEntryRowCount("entryentity");
        for (int i = 0; i < rowCount; i++) {
            // 锁定3个字段：当月绩效工资基数、当月绩效考核等级、绩效工资分配比例、备注
            view.setEnable(false, i,
                    "JH038",
//                        "JH037",
                    "JH039",
                    "remark"
            );
        }
    }


    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        EntryGrid entryGrid = this.getView().getControl("entryentity");
        entryGrid.addCellClickListener(this);
    }

    @Override
    public void cellClick(CellClickEvent cellClickEvent) {
        String fieldKey = cellClickEvent.getFieldKey();
        // 锁定列
        String isYear = this.getView().getParentView().getPageCache().get("isJH001");
        if ("true".equals(isYear)) {
            lockField(this.getModel(), this.getView());
        }
    }

    @Override
    public void cellDoubleClick(CellClickEvent cellClickEvent) {

    }
}
