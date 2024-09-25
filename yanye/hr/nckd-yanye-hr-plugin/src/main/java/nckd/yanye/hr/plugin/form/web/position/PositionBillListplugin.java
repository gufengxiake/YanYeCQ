package nckd.yanye.hr.plugin.form.web.position;



import kd.bos.form.events.BeforeCreateListDataProviderArgs;
import kd.bos.form.events.SetFilterEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import java.util.EventObject;

/**
 * 岗位维护列表插件，标识：nckd_homs_position_ext
 * author:guozhiwei
 * date:2024-09-24
 */


public class PositionBillListplugin extends AbstractListPlugin  {




    @Override
    public void setFilter(SetFilterEvent e) {
        super.setFilter(e);

    }

    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
        e.getSource();
    }


    @Override
    public void beforeCreateListDataProvider(BeforeCreateListDataProviderArgs args) {
        super.beforeCreateListDataProvider(args);
    }


}
