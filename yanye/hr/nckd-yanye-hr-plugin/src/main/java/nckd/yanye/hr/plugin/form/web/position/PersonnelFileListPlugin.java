package nckd.yanye.hr.plugin.form.web.position;

import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.events.BeforeCreateListDataProviderArgs;
import kd.bos.form.events.SetFilterEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.mvc.list.ListDataProvider;


/**
 * Module           :核心人力云 -人员信息-人员档案
 * 人员档案列表插件，标识：nckd_homs_position_ext
 * @author:guozhiwei
 * @date:2024-09-24
 */



public class PersonnelFileListPlugin extends AbstractListPlugin {


    @Override
    public void setFilter(SetFilterEvent e) {
        super.setFilter(e);
        // 排序规则 任职经历中，行政组织排序号，岗位排序号，行政组织编号，人员当前岗位的任职开始时间，
//        hrpi_empposorgrel.position.nckd_sortnum asc
        String order = "hrpi_empposorgrel.adminorg.sortcode asc,hrpi_empposorgrel.adminorg.number asc," +
                "hrpi_empposorgrel.position.nckd_sortnum,hrpi_empposorgrel.position.number,hrpi_empposorgrel.nckd_personindex";
//        String order = "hrpi_empposorgrel.adminorg.sortcode asc";
        e.setOrderBy(order);

    }

    @Override
    public void beforeCreateListDataProvider(BeforeCreateListDataProviderArgs args) {
        super.beforeCreateListDataProvider(args);
        args.setListDataProvider(new MyListDataProvider());

    }
}
class MyListDataProvider extends ListDataProvider {


    /**
     * 加载列表数据
     * @remark: 重写父类方法，如果不使用这个方法，设置的排序有问题
     */
    @Override
    public DynamicObjectCollection getData(int arg0, int arg1) {
        DynamicObjectCollection rows = super.getData(arg0, arg1);
        return rows;
    }
}

