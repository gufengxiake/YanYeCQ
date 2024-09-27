package nckd.yanye.hr.plugin.form.web.position;


import kd.bos.form.control.events.BaseDataColumnDependFieldSetEvent;
import kd.bos.form.events.BeforeCreateListColumnsArgs;
import kd.bos.form.events.SetFilterEvent;
import kd.bos.list.plugin.AbstractListPlugin;

/**
 * Module           :组织发展云 -岗位信息维护
 * 岗位维护列表插件，标识：nckd_homs_position_ext
 * @author:guozhiwei
 * @date:2024-09-24
 */


public class PositionBillListplugin extends AbstractListPlugin  {


    @Override
    public void baseDataColumnDependFieldSet(BaseDataColumnDependFieldSetEvent args) {
        super.baseDataColumnDependFieldSet(args);

    }

    @Override
    public void beforeCreateListColumns(BeforeCreateListColumnsArgs args) {
        super.beforeCreateListColumns(args);

    }

    @Override
    public void setFilter(SetFilterEvent e) {
        super.setFilter(e);
        String order = "adminorg.sortcode asc,nckd_sortnum asc,adminorg.number asc,isleader asc";
        //先按单据状态升序，再按日期降序
        e.setOrderBy(order);
    }

}
