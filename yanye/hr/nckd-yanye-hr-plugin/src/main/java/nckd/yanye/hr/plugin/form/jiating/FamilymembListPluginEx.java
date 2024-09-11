package nckd.yanye.hr.plugin.form.jiating;

import kd.bos.dataentity.resource.ResManager;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.HyperLinkClickArgs;
import kd.hr.hspm.formplugin.web.infoclassify.familymemb.FamilymembListPlugin;
import kd.sdk.hr.hspm.common.dto.InfoClassifyEntityKeyDTO;
import kd.sdk.hr.hspm.common.enums.InfoClassifyFormOperateEnum;
import kd.sdk.hr.hspm.common.utils.InfoClassifyOpenWindowUtil;

import java.util.List;

/**
 * 基础资料插件
 * 核心人力云->人员信息->分类维护列表-》分类维护列表 家庭成员信息
 * 职称信息 nckd_hspm_familymembl_ext
 * 需求：修改标题名称，把 家庭成员  修改程 家庭成员信息
 * 2024-09-10
 * chengchaohua
 */
public class FamilymembListPluginEx extends FamilymembListPlugin {

    public FamilymembListPluginEx(){}

    @Override
    protected void customerVisibleColumnList(List<String> visibleColumnList) {
        super.customerVisibleColumnList(visibleColumnList);
    }

    // 修改弹框标题名字
    @Override
    protected void beforeDoOperateForListBtnNew(BeforeDoOperationEventArgs args, InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO) {
        FormShowParameter formShowParameter = InfoClassifyOpenWindowUtil.openWindowForListBtnNew(infoClassifyEntityKeyDTO, ResManager.loadKDString("家庭成员信息", "FamilymembListPlugin_erkai", "hr-hspm-formplugin", new Object[0]));
        formShowParameter.setCloseCallBack(new CloseCallBack(this, InfoClassifyFormOperateEnum.FORM_BTN_SAVE.getOperateKey()));
        this.getView().showForm(formShowParameter);
    }

    // 修改弹框标题名字
    @Override
    protected void billListHyperLinkClick(HyperLinkClickArgs args, Long pkId, InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO) {
        FormShowParameter formShowParameter = InfoClassifyOpenWindowUtil.openWindowForListHyperLink(pkId, infoClassifyEntityKeyDTO, ResManager.loadKDString("家庭成员信息", "FamilymembListPlugin_erkai", "hr-hspm-formplugin", new Object[0]));
        formShowParameter.setCloseCallBack(new CloseCallBack(this, InfoClassifyFormOperateEnum.FORM_BTN_UPDATE.getOperateKey()));
        this.getView().showForm(formShowParameter);
    }
}
