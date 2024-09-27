package nckd.yanye.hr.report.shebao;

import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.ORM;
import kd.hr.hspm.formplugin.web.report.EmpReportListPlugin;


/**
 * Module           :核心人力云 -人员信息-在职员工花名册,报表插件
 * Description      :在职员工花名册报表插件，标识：nckd_hspm_empreport_ext
 *
 * @author guozhiwei
 * @date  2024/9/13 10：40
 * 标识 nckd_hspm_empreport_ext
 *
 */


public class DutyPersonnelRosterReportPlugin extends EmpReportListPlugin {



    public DataSet query(ReportQueryParam reportQueryParam, Object selectedObj) {
        DataSet query = super.query(reportQueryParam, selectedObj);
//        query.orderBy(new String[]{""});

        ORM orm = ORM.create();

        // 设置排序规则


        System.out.println("DutyPersonnelRosterReportPlugin query");
        return query;


    }

}
