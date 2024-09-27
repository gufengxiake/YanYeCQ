package nckd.yanye.hr.plugin.form.web.position;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.control.events.BaseDataColumnDependFieldSetEvent;
import kd.bos.form.events.BeforeCreateListColumnsArgs;
import kd.bos.form.events.BeforeCreateListDataProviderArgs;
import kd.bos.form.events.SetFilterEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.mvc.list.ListDataProvider;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 岗位维护列表插件，标识：nckd_homs_position_ext
 * author:guozhiwei
 * date:2024-09-24
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
        String order = "adminorg.sortcode asc,adminorg.number asc,nckd_sortnum asc,isleader asc";
        //先按单据状态升序，再按日期降序
        e.setOrderBy(order);
    }

}
