package nckd.yanye.hr.plugin.form.chufen;

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
 * 处分信息-标识：nckd_hspm_chufeninfolist
 */
public class EmpChuFenInfoListPlugin extends InfoClassifyListPlugin {

    public EmpChuFenInfoListPlugin() {

    }

    protected void customerVisibleColumnList(List<String> visibleColumnList) {
//        visibleColumnList.add("nckd_chufenname");
//        visibleColumnList.add("nckd_chufentype.name");
////        visibleColumnList.add("nckd_weijifact");
//        visibleColumnList.add("nckd_chufendate");
//        visibleColumnList.add("nckd_chufenenddate");
//        visibleColumnList.add("nckd_jinzhishixiang");
//        visibleColumnList.add("nckd_chufenpizhun");
//        visibleColumnList.add("nckd_chufenwenhao");
//        visibleColumnList.add("nckd_remark");
    }

    protected void beforeDoOperateForListBtnNew(BeforeDoOperationEventArgs args, InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO) {
        FormShowParameter formShowParameter = InfoClassifyOpenWindowUtil.openWindowForListBtnNew(infoClassifyEntityKeyDTO, ResManager.loadKDString("处分信息", "ChufenListPlugin_0", "hr-hspm-formplugin", new Object[0]));
        formShowParameter.setCloseCallBack(new CloseCallBack(this, InfoClassifyFormOperateEnum.FORM_BTN_SAVE.getOperateKey()));
        this.getView().showForm(formShowParameter);
    }

    protected void billListHyperLinkClick(HyperLinkClickArgs args, Long pkId, InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO) {
        FormShowParameter formShowParameter = InfoClassifyOpenWindowUtil.openWindowForListHyperLink(pkId, infoClassifyEntityKeyDTO, ResManager.loadKDString("处分信息", "ChufenListPlugin_0", "hr-hspm-formplugin", new Object[0]));
        formShowParameter.setCloseCallBack(new CloseCallBack(this, InfoClassifyFormOperateEnum.FORM_BTN_UPDATE.getOperateKey()));
        this.getView().showForm(formShowParameter);
    }

}
