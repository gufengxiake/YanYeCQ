package nckd.yanye.hr.plugin.form.empout;

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
 * 信息批量维护：
 * 核心人力云->人员信息->分类维护列表
 * 系统外任职经历 nckd_hspm_emporgreloulist
 */
public class EmpPorgrelOutListPlugin extends InfoClassifyListPlugin {

    public EmpPorgrelOutListPlugin() {

    }

    protected void customerVisibleColumnList(List<String> visibleColumnList) {
//        visibleColumnList.add("nckd_kaoheyear");
//        visibleColumnList.add("nckd_kaoheresult.name");
//        visibleColumnList.add("nckd_pingjiaorg");
//        visibleColumnList.add("nckd_wcjreason");
    }

    protected void beforeDoOperateForListBtnNew(BeforeDoOperationEventArgs args, InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO) {
        FormShowParameter formShowParameter = InfoClassifyOpenWindowUtil.openWindowForListBtnNew(infoClassifyEntityKeyDTO, ResManager.loadKDString("上线前任职经历", "porgreloutListPlugin_0", "hr-hspm-formplugin", new Object[0]));
        formShowParameter.setCloseCallBack(new CloseCallBack(this, InfoClassifyFormOperateEnum.FORM_BTN_SAVE.getOperateKey()));
        this.getView().showForm(formShowParameter);
    }

    protected void billListHyperLinkClick(HyperLinkClickArgs args, Long pkId, InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO) {
        FormShowParameter formShowParameter = InfoClassifyOpenWindowUtil.openWindowForListHyperLink(pkId, infoClassifyEntityKeyDTO, ResManager.loadKDString("上线前任职经历", "porgreloutListPlugin_0", "hr-hspm-formplugin", new Object[0]));
        formShowParameter.setCloseCallBack(new CloseCallBack(this, InfoClassifyFormOperateEnum.FORM_BTN_UPDATE.getOperateKey()));
        this.getView().showForm(formShowParameter);
    }

}
