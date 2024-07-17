package nckd.yanye.scm.plugin.form;

import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.list.plugin.AbstractListPlugin;

import java.util.EventObject;

/**
 * 采购申请单-列表插件
 * @author liuxiao
 *
 */
public class PurapplyBillListPlugin extends AbstractListPlugin {
    /**
     * 按钮标识-推送招采平台
     */
    final static String PUSHTOZC = "tblpushtozc";

    /**
     * 按钮标识-公告查看
     */
    final static String ANNOUNCEMENT = "tblannouncement";

    @Override
    public void afterBindData(EventObject e) {
    }


    /**
     * 按钮点击事件
     *
     * @param evt
     */
    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        switch (evt.getItemKey()) {
            // 推送招采平台
            case PUSHTOZC:
                pushToZc();
                break;
            // 公告查看
            case ANNOUNCEMENT:
                announcement();
                break;
            default:
                break;
        }


    }

    /**
     * TODO
     * 点击“推送招采平台”按钮，调取招采平台公告发布接口，进行公告发布，
     * 发布成功，进行消息提醒“公告发布成功”，同步将字段“已成功推送招采平台”自动进行勾选，
     * 如未成功，根据错误原因进行友好提示；
     */
    private void pushToZc() {

    }

    /**
     * TODO
     * 5、点击“公告查看”按钮，自动进行单点登陆，且跳转至相应的公告查看界面，
     * 如未发布公告，则进行提醒“该采购申请单未推送至招采平台”；
     */
    private void announcement() {

    }

}