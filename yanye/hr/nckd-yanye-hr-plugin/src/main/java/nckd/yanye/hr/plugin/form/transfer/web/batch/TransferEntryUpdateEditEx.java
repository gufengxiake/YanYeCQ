package nckd.yanye.hr.plugin.form.transfer.web.batch;

import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.hr.hdm.formplugin.transfer.web.batch.TransferEntryUpdateEdit;

/**
 * 批量调动， 标识 nckd_hdm_transferbatc_ext
 * 二开插件-子页面返回职级及干部类型
 * 批量处理
 * author: tangyuxuan
 * date:2024-07-30
 */
public class TransferEntryUpdateEditEx extends TransferEntryUpdateEdit {

    private static final Log Logger = LogFactory.getLog(TransferEntryUpdateEditEx.class);

    static {
        DYNAMICOBJECT_COVERT_FIELDLIST.add("nckd_zhiji");
        DYNAMICOBJECT_COVERT_FIELDLIST.add("nckd_ganbutype");
        DYNAMICOBJECT_COVERT_FIELDLIST.add("nckd_oldzhiji");
        DYNAMICOBJECT_COVERT_FIELDLIST.add("nckd_oldganbutype");
    }
}
