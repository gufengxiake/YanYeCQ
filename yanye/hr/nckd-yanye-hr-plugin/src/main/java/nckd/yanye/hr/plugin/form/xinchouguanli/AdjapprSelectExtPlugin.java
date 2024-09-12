package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.entity.BasedataEntityType;
import kd.sdk.swc.hcdm.business.extpoint.adjapprbill.IDecAdjApprExtPlugin;
import kd.sdk.swc.hcdm.business.extpoint.adjapprbill.event.AfterF7PersonSelectEvent;
import kd.sdk.swc.hcdm.common.stdtab.StdAmountAndSalaryCountQueryResult;
import kd.sdk.swc.hcdm.common.stdtab.StdAmountQueryParam;
import kd.sdk.swc.hcdm.service.spi.SalaryStdQueryService;
import org.apache.commons.compress.utils.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 员工定调薪申请单-业务扩展插件
 * 单据标识：nckd_hcdm_adjapprbill_ext
 * 业务场景编码：kd.sdk.swc.hcdm.business.extpoint.adjapprbill.IDecAdjApprExtPlugin#onAfterF7PersonSelect
 *
 * @author ：luxiao
 * @since ：Created in 17:28 2024/9/11
 */
public class AdjapprSelectExtPlugin implements IDecAdjApprExtPlugin {

    /**
     * 在添加定调薪人员时，选择定调薪人员后，需要二开对新增的字段赋值
     *
     * @param e
     */
    @Override
    public void onAfterF7PersonSelect(AfterF7PersonSelectEvent e) {
        // 非调薪不作处理
        if (!"2".equals(e.getAdjAttributionType())) {
            return;
        }

        List<DynamicObject> adjPersonDyObjList = e.getAdjPersonDyObjList();
        for (DynamicObject obj : adjPersonDyObjList) {
            Map<String, IDataEntityProperty> allFields = ((BasedataEntityType) obj.getDynamicObjectType()).getAllFields();
            DynamicObjectCollection entryentity = obj.getDynamicObjectCollection("entryentity");
            for (DynamicObject entry : entryentity) {
//                ArrayList<StdAmountQueryParam> queryParams = Lists.newArrayList();
//                // 构建查询参数
//                StdAmountQueryParam queryParam = new StdAmountQueryParam();
//                // 对应薪酬标准表id
//                long salaryStdId = entry.getDynamicObject("salarystd").getLong("id");
//                queryParam.setStdTabId(salaryStdId);
//                // 定调薪项目id
//                long standardItemId = entry.getDynamicObject("standarditem").getLong("id");
//                queryParam.setItemId(standardItemId);
//                // 薪等id(对应的薪等信息中的gradeId)，必传
//                queryParam.setGradeId(entry.getDynamicObject("pregrade").getLong("id"));
//                // 薪档id(对应的薪档信息中的rankId)，非必传。如果项目在标准表下未启用薪档，则薪档id可不传
//                queryParam.setRankId(entry.getDynamicObject("prerank").getLong("id"));
//                // UnionId 代表这组参数的唯一标识，通过该参数将入参和返回值对应起来，必传
//                queryParam.setUnionId("11");
//
//                queryParams.add(queryParam);
//                List<StdAmountAndSalaryCountQueryResult> stdAmountAndSalaryCountQueryResults =
//                        SalaryStdQueryService.get().queryAmountAndSalaryCount(queryParams);
//
//                System.out.println(1);
            }
        }
    }
}
