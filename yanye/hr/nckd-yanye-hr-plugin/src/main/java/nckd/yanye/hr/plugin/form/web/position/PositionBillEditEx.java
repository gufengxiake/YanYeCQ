package nckd.yanye.hr.plugin.form.web.position;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.hr.haos.business.servicehelper.OrgBatchBillHelper;
import kd.hr.haos.common.constants.masterdata.AdminOrgConstants;
import kd.hr.hbp.formplugin.web.HRCoreBaseBillEdit;

import java.util.Date;
import java.util.EventObject;

/**
 * 岗位申请单，标识：nckd_homs_positionb_ext
 * author:chengchaohua
 * date:2024-08-16
 */
public class PositionBillEditEx extends HRCoreBaseBillEdit {

    private static Log logger = LogFactory.getLog(PositionBillEditEx.class);

    // 二开，增加一个文本控件查看时显示组织长名称
    public void afterBindData(EventObject eventObject) {
        // 行政组织
        DynamicObject value = (DynamicObject)this.getModel().getValue("adminorg");
        // 取行政组织的长名称
        String orgLongName = OrgBatchBillHelper.getOrgLongName((Long)value.getPkValue(), new Date(), String.valueOf(AdminOrgConstants.ADMINORG_STRUCT));
        this.getModel().setValue("nckd_orglongname", orgLongName);
    }

}
