package nckd.yanye.hr.plugin.form.zhicheng;

import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.api.ApiResult;
import kd.bos.entity.plugin.ImportLogger;
import kd.bos.form.plugin.impt.BatchImportPlugin;
import kd.bos.form.plugin.impt.ImportBillData;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 核心人力云->人员信息->分类维护表单 信息批量处理
 * 职称信息表单 nckd_hspm_perprotitle_ext
 * 引入插件
 *
 * @author ：luxiao
 * @since ：Created in 14:04 2024/9/29
 */
public class EmpZhiChengBatchImportPlugin extends BatchImportPlugin {

    @Override
    protected ApiResult save(List<ImportBillData> rowdatas, ImportLogger logger) {
        DynamicObject[] load = BusinessDataServiceHelper.load(
                "hspm_perprotitle",
                "person,nckd_type,nckd_iszuigao",
                new QFilter[]{new QFilter("nckd_iszuigao", QCP.equals, true)}
        );

        // 单个个人员，相同类型，只允许一个为最高职称
        //<工号, <类型, 数据>>
        Map<String, Map<String, List<ImportBillData>>> groupedData = rowdatas.stream()
                .collect(
                        Collectors.groupingBy(
                                rowdata -> rowdata.getData().get("person.number").toString(),
                                Collectors.groupingBy(rowdata -> rowdata.getData().get("nckd_type").toString())
                        )
                );

        Set<String> repeatPerson = groupedData.entrySet().stream()
                .filter(
                        entry -> entry.getValue().values().stream()
                                .flatMap(List::stream)
                                .filter(rowdata -> (boolean) rowdata.getData().get("nckd_iszuigao"))
                                .count() > 1
                )
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        for (ImportBillData rowdata : rowdatas) {
            JSONObject billData = rowdata.getData();
            // 员工姓名
            String empName = (String) billData.get("person.name");
            // 员工编号
            String empNumber = (String) billData.get("person.number");
            // 类型
            String type = (String) billData.get("nckd_type");
            // 是否最高
            boolean isZuiGao = (boolean) billData.get("nckd_iszuigao");
            if (repeatPerson.contains(empNumber)) {
                // 返回校验信息
                String validMsg = String.format("员工【%s】(%s)已存在多个类型为【%s】的最高职称，请检查", empName, empNumber, type);
                // 有校验提示，校验不通过，记录日志，移除数据
                logger.log(rowdata.getStartIndex(), validMsg).fail();
                rowdatas.remove(rowdata);
            }
        }

        // 调用缺省方法保存合法的数据
        return super.save(rowdatas, logger);
    }
}
