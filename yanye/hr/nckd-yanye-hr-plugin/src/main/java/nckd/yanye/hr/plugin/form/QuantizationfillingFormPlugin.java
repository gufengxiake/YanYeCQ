package nckd.yanye.hr.plugin.form;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.CloseCallBack;
import kd.bos.form.ShowFormHelper;
import kd.bos.form.control.Control;
import kd.bos.form.events.ClosedCallBackEvent;
import kd.bos.form.field.TextEdit;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.list.ListFilterParameter;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.fi.arapcommon.helper.LspWapper;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Module           :目标绩效云-个人绩效考核-绩效考核计划
 * Description      : 选择考核周期 弹框展示周期方案的分录以选择
 * nckd_quantizationfilling
 * @author : yaosijie
 * @date : 2024/9/29
 */
public class QuantizationfillingFormPlugin extends AbstractFormPlugin {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        TextEdit textEdit = this.getView().getControl("nckd_assessment");
        textEdit.addClickListener(this);

    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);
        Control source = (Control)evt.getSource();
        String key = source.getKey();
        if (key.equals("nckd_assessment")){
            DynamicObject cycleprogrammeObject = (DynamicObject)this.getModel().getValue("nckd_cycleprogramme");
            //打开销售合同列表
            ListShowParameter parameter = getAssessmentShowParameter(cycleprogrammeObject);
            //设置回调
            parameter.setCloseCallBack(new CloseCallBack(this, "assessment"));
            getView().showForm(parameter);
        }
    }
    @Override
    public void closedCallBack(ClosedCallBackEvent closedCallBackEvent) {
        super.closedCallBack(closedCallBackEvent);
        ListSelectedRowCollection selectCollections = (ListSelectedRowCollection) closedCallBackEvent.getReturnData();
        if ("assessment".equals(closedCallBackEvent.getActionId()) && null != selectCollections){
            //分录主键id
            Object entryPkObject = selectCollections.get(0).getEntryPrimaryKeyValue();
            //单据id
            DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle(selectCollections.get(0).getPrimaryKeyValue(),"hbss_cyclescheme");
            List<DynamicObject> entryentity = dynamicObject.getDynamicObjectCollection("entryentity").stream().filter(t->t.getPkValue().equals(entryPkObject)).collect(Collectors.toList());
            this.getModel().setValue("nckd_assessment",entryentity.get(0).get("periodflag"));//给选择考核周期赋值
        }
    }

    public ListShowParameter getAssessmentShowParameter(DynamicObject pk) {
        if (null == pk){
            this.getView().showErrorNotification("请先选择周期方案");
        }
        List<String> showFields = new ArrayList();
        showFields.add("periodflag");
        showFields.add("prefperiod");
        ListShowParameter lsp = createDynamicListShowParameter("hbss_cyclescheme", "entryentity", showFields);
        ListFilterParameter lfp = new ListFilterParameter();
        lfp.setFilter(new QFilter("id", "=", pk.getPkValue()));
        lsp.setListFilterParameter(lfp);
        lsp.setCaption(ResManager.loadKDString("考核计划-选择考核周期", "DynamicListHelper_0", "fi-arapcommon", new Object[0]));
        return lsp;
    }

    public static ListShowParameter createDynamicListShowParameter(String entity, String entry, List<String> showFields) {
        ListShowParameter lsp = ShowFormHelper.createShowListForm(entity, false);
        lsp.setCustomParam("entity", entity);
        lsp.setCustomParam("isEntryMain", Boolean.TRUE);
        lsp.setCustomParam("showFields", showFields);
        lsp.setCustomParam("entry", entry);
        lsp.setMultiSelect(false);
        LspWapper lspWapper = new LspWapper(lsp);
        lspWapper.clearPlugins();
        lspWapper.registerScript("kingdee.fi.ap.mainpage.arapdynamiclistscriptplugin");
        lspWapper.setMergeRow(false);
        lsp.setAppId("ap");
        return lsp;
    }
}
