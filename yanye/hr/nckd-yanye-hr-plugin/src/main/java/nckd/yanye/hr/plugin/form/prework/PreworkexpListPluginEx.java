package nckd.yanye.hr.plugin.form.prework;

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
 *  信息批量维护-》社会工作经历 列表二开插件
 *  单据标识：nckd_hspm_preworkexpl_ext
 *  需求：修改标题名称
 *  author: chengchaohua
 *  date: 2024-08-26
 */
public class PreworkexpListPluginEx extends InfoClassifyListPlugin {
    public PreworkexpListPluginEx(){}

    protected void customerVisibleColumnList(List<String> visibleColumnList) {
        visibleColumnList.add("empcountry.name");
        visibleColumnList.add("jobcityid.name");
        visibleColumnList.add("empaddress");
        visibleColumnList.add("isabroadbackground");
        visibleColumnList.add("trade.name");
        visibleColumnList.add("businesstypeid.name");
        visibleColumnList.add("companyscale.name");
        visibleColumnList.add("department");
        visibleColumnList.add("positiontype");
        visibleColumnList.add("duty");
        visibleColumnList.add("reportposition");
        visibleColumnList.add("subordinates");
        visibleColumnList.add("jobdesc");
        visibleColumnList.add("tenuretime");
        visibleColumnList.add("tenuretimeunit");
        visibleColumnList.add("exitdate");
        visibleColumnList.add("quitreason");
        visibleColumnList.add("witness");
        visibleColumnList.add("witnessphone");
        visibleColumnList.add("description");
    }

    protected void beforeDoOperateForListBtnNew(BeforeDoOperationEventArgs args, InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO) {
        FormShowParameter formShowParameter = InfoClassifyOpenWindowUtil.openWindowForListBtnNew(infoClassifyEntityKeyDTO, ResManager.loadKDString("社会工作经历", "PreworkexpListPlugin_0erkai", "hr-hspm-formplugin", new Object[0]));
        formShowParameter.setCloseCallBack(new CloseCallBack(this, InfoClassifyFormOperateEnum.FORM_BTN_SAVE.getOperateKey()));
        this.getView().showForm(formShowParameter);
    }

    protected void billListHyperLinkClick(HyperLinkClickArgs args, Long pkId, InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO) {
        FormShowParameter formShowParameter = InfoClassifyOpenWindowUtil.openWindowForListHyperLink(pkId, infoClassifyEntityKeyDTO, ResManager.loadKDString("社会工作经历", "PreworkexpListPlugin_0erkai", "hr-hspm-formplugin", new Object[0]));
        formShowParameter.setCloseCallBack(new CloseCallBack(this, InfoClassifyFormOperateEnum.FORM_BTN_UPDATE.getOperateKey()));
        this.getView().showForm(formShowParameter);
    }
}
