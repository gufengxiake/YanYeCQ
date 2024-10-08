package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.sdk.swc.hcdm.business.extpoint.salarystd.IHcdmContrastPropExtPlugin;
import kd.sdk.swc.hcdm.business.extpoint.salarystd.event.ContrastPropLoadEvent;
import kd.sdk.swc.hcdm.common.stdtab.ContrastPropConfigEntity;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 薪酬标准表-业务扩展插件
 * 单据标识：nckd_hcdm_adjapprbill_ext
 * 业务场景编码：kd.sdk.swc.hcdm.business.extpoint.salarystd.IHcdmContrastPropExtPlugin#loadContrastPropValue
 *
 * @author liuxiao
 * @since 2024/08/19
 */
public class AdjapprBillPropExtPlugin implements IHcdmContrastPropExtPlugin {

    /**
     * 当进行标准表匹配（调用匹配接口）、对照属性取数（自主查询）时，
     * 会进入该方法。可以根据候选人id来加载人员身上的对照属性的值
     *
     * @param e
     */
    @Override
    public void loadContrastPropValue(ContrastPropLoadEvent e) {
        IHcdmContrastPropExtPlugin.super.loadContrastPropValue(e);
        List<Long> fileIds = e.getAdjFileIdList();
        List<ContrastPropConfigEntity> propCfgList = e.getPropCfgList();
        Map<Long, Map<Long, Object>> propValues = e.getPropValues();
        // 全日制最高学历：否
        long notFullTime = BusinessDataServiceHelper.loadSingle(
                "nckd_iffulltime",
                new QFilter[]{new QFilter("name", QCP.equals, "否")}
        ).getLong("id");

        // 全日制最高学历：是
        long fullTime = BusinessDataServiceHelper.loadSingle(
                "nckd_iffulltime",
                new QFilter[]{new QFilter("name", QCP.equals, "是")}
        ).getLong("id");

        // 定调薪档案 <定调薪档案id，定调薪档案object>
        DynamicObject[] adjFiles = BusinessDataServiceHelper.load(
                "hcdm_adjfileinfo",
                "id,name,number,person,position",
                new QFilter[]{}
        );
        Map<Long, DynamicObject> adjFilesMap = Arrays.stream(adjFiles)
                .collect(Collectors.toMap(obj -> obj.getLong("id"), obj -> obj));

        // 教育经历（最高学历） <教育经历person.id，教育经历object>
        DynamicObject[] eduExps = BusinessDataServiceHelper.load(
                "hrpi_pereduexp",
                "id,person,ishighestdegree,nckd_isquanrizuigaoxl",
                new QFilter[]{
                        // 是否最高学历：是
                        new QFilter("ishighestdegree", QCP.equals, "1"),
                        // 数据状态
                        new QFilter("datastatus", QCP.equals, "1"),
                        // 当前版本
                        new QFilter("iscurrentversion", QCP.equals, "1")
                }
        );
        Map<Long, DynamicObject> eduExpsMap = Arrays.stream(eduExps)
                .collect(Collectors.toMap(obj -> obj.getLong("person.id"), obj -> obj));

        // 任职经历 <任职经历person.id，任职经历object>
        DynamicObject[] jobExps = BusinessDataServiceHelper.load(
                "hrpi_empposorgrel",
                "id,person,nckd_zhiji",
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
        Map<Long, DynamicObject> jobExpsMap = Arrays.stream(jobExps)
                .collect(Collectors.toMap(obj -> obj.getLong("person.id"), obj -> obj));

        // hr岗位 <hr岗位id，hr岗位object>
        DynamicObject[] hrPositions = BusinessDataServiceHelper.load(
                "hbpm_positionhr",
                "id,nckd_gangji",
                new QFilter[]{
                        // 数据状态
                        new QFilter("datastatus", QCP.equals, "1"),
                        // 当前版本
                        new QFilter("iscurrentversion", QCP.equals, "1"),
                }
        );
        Map<Long, DynamicObject> hrPositionsMap = Arrays.stream(hrPositions)
                .collect(Collectors.toMap(obj -> obj.getLong("id"), obj -> obj));

        /*
         * 对照属性配置
         */
        for (ContrastPropConfigEntity cfg : propCfgList) {
            switch (cfg.getNumber()) {
                // 是否全日制最高学历
                case "1310_S":
                    for (Long fileId : fileIds) {
                        // 定调薪档案
                        DynamicObject adjFile = adjFilesMap.get(fileId);

                        // 教育经历
                        DynamicObject eduExp = eduExpsMap.get(adjFile.getLong("person.id"));

                        if (eduExp == null) {
                            propValues.get(fileId).putIfAbsent(cfg.getId(), null);
                            continue;
                        }

                        // “是否最高学历”为 true的档案，取“是否全日制最高学历”的值
                        boolean flag = eduExp.getBoolean("nckd_isquanrizuigaoxl");
                        propValues.get(fileId).putIfAbsent(cfg.getId(), flag ? fullTime : notFullTime);
                    }
                    break;
                // 任职经历-职级
                case "1330_S":
                    for (Long fileId : fileIds) {
                        // 定调薪档案
                        DynamicObject adjFile = adjFilesMap.get(fileId);
                        // 任职经历
                        DynamicObject jobExp = jobExpsMap.get(adjFile.getLong("person.id"));

                        if (jobExp == null) {
                            propValues.get(fileId).putIfAbsent(cfg.getId(), null);
                            continue;
                        }

                        DynamicObject zhiJi = jobExp.getDynamicObject("nckd_zhiji");
                        if (zhiJi == null) {
                            propValues.get(fileId).putIfAbsent(cfg.getId(), null);
                            continue;
                        }

                        propValues.get(fileId).putIfAbsent(cfg.getId(), zhiJi.getLong("id"));
                    }
                    break;
                // 定调薪-岗级
                case "1320_S":
                    for (Long fileId : fileIds) {
                        // 定调薪档案
                        DynamicObject adjFile = adjFilesMap.get(fileId);

                        // hr岗位
                        DynamicObject gangWei = hrPositionsMap.get(adjFile.getLong("position.id"));

                        if (gangWei == null) {
                            propValues.get(fileId).putIfAbsent(cfg.getId(), null);
                            continue;
                        }

                        DynamicObject gangJi = gangWei.getDynamicObject("nckd_gangji");
                        if (gangJi == null) {
                            propValues.get(fileId).putIfAbsent(cfg.getId(), null);
                            continue;
                        }

                        propValues.get(fileId).putIfAbsent(cfg.getId(), gangJi.getLong("id"));
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
