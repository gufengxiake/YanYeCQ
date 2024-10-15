package nckd.yanye.hr.plugin.form.web.position;

import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.events.BeforeCreateListDataProviderArgs;
import kd.bos.form.events.SetFilterEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.mvc.list.ListDataProvider;


/**
 * Module           :核心人力云 -人员信息-信息批量维护
 * 信息批量维护列表插件，标识：hspm_personinfolist
 * @author:guozhiwei
 * @date:2024-09-24 17：06
 */



public class PersonInfoListPlugin extends AbstractListPlugin {


    @Override
    public void setFilter(SetFilterEvent e) {
        super.setFilter(e);
        // 排序规则 任职经历中，行政组织排序号，岗位排序号，行政组织编号，人员当前岗位的任职开始时间，
//        hrpi_empposorgrel.position.nckd_sortnum asc
        String order = "hrpi_empposorgrel.adminorg.sortcode asc,hrpi_empposorgrel.adminorg.number asc," +
                "hrpi_empposorgrel.position.nckd_sortnum asc,hrpi_empposorgrel.position.number asc,hrpi_empposorgrel.nckd_personindex asc";
        e.setOrderBy(order);

    }

    @Override
    public void beforeCreateListDataProvider(BeforeCreateListDataProviderArgs args) {
        super.beforeCreateListDataProvider(args);
        args.setListDataProvider(new MyListDataProvider());

    }
}

