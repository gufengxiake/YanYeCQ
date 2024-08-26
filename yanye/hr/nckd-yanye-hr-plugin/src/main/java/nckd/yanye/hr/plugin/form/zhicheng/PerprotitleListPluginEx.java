package nckd.yanye.hr.plugin.form.zhicheng;

import kd.bos.dataentity.resource.ResManager;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.events.HyperLinkClickArgs;
import kd.sdk.hr.hspm.common.dto.InfoClassifyEntityKeyDTO;
import kd.sdk.hr.hspm.common.enums.InfoClassifyFormOperateEnum;
import kd.sdk.hr.hspm.common.utils.InfoClassifyOpenWindowUtil;
import kd.sdk.hr.hspm.formplugin.infoclassify.InfoClassifyListPlugin;

import java.util.List;

/**
 * 基础资料插件
 * 核心人力云->人员信息->分类维护列表 信息批量处理 职称及技能信息
 * 职称信息 nckd_hspm_perprotitl_ext2
 * 需求：修改标题名称
* 2024-07-26
* chengchaohua
 */
public class PerprotitleListPluginEx extends InfoClassifyListPlugin {


    public PerprotitleListPluginEx(){}

    protected void customerVisibleColumnList(List<String> visibleColumnList) {
        visibleColumnList.add("iscompany");
        visibleColumnList.add("unit");
        visibleColumnList.add("office");
        visibleColumnList.add("approvnum");
        visibleColumnList.add("certiicateid");
        visibleColumnList.add("details");
        visibleColumnList.add("firsttime");
        visibleColumnList.add("secondtime");
        visibleColumnList.add("description");
    }

    @Override
    protected void beforeDoOperateForListBtnNew(BeforeDoOperationEventArgs args, InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO) {
        FormShowParameter formShowParameter = InfoClassifyOpenWindowUtil.openWindowForListBtnNew(infoClassifyEntityKeyDTO, ResManager.loadKDString("职称及技能信息", "PerprotitleListPlugin_0erkai", "hr-hspm-formplugin", new Object[0]));
        formShowParameter.setCloseCallBack(new CloseCallBack(this, InfoClassifyFormOperateEnum.FORM_BTN_SAVE.getOperateKey()));
        this.getView().showForm(formShowParameter);
    }

    protected void billListHyperLinkClick(HyperLinkClickArgs args, Long pkId, InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO) {
        FormShowParameter formShowParameter = InfoClassifyOpenWindowUtil.openWindowForListHyperLink(pkId, infoClassifyEntityKeyDTO, ResManager.loadKDString("职称及技能信息", "PerprotitleListPlugin_0erkai", "hr-hspm-formplugin", new Object[0]));
        formShowParameter.setCloseCallBack(new CloseCallBack(this, InfoClassifyFormOperateEnum.FORM_BTN_UPDATE.getOperateKey()));
        this.getView().showForm(formShowParameter);
    }
}
