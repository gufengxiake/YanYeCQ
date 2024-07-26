package nckd.yanye.hr.plugin.form.zhicheng;

import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.list.IListView;
import kd.bos.list.plugin.AbstractListPlugin;

/**
 * 基础资料插件
 * 核心人力云->人员信息->分类维护列表 信息批量处理
 * 职称信息 nckd_hspm_perprotitl_ext2
* 2024-07-26
* chengchaohua
 */
public class EmpZhiChengHspmListPlugin  extends AbstractListPlugin {


    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        super.closedCallBack(closedCallBackEvent);
        ((IListView)getView()).refresh();
    }
}
