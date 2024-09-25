package nckd.yanye.fi.plugin.report;

import java.util.*;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.form.field.ComboEdit;
import kd.bos.form.field.ComboItem;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.QueryServiceHelper;

/**
 * Module           :财务云-总账-凭证汇总
 * Description      :凭证汇总表报表插件
 * nckd_gl_vouchersummar_ext
 * @author : yaosijie
 * @date : 2024/9/25
 */
public class VoucherSummaryFormRptExt extends AbstractReportFormPlugin {
	@Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        for (DynamicObject row : rowData) {
            if(Objects.nonNull(row.get("number"))){
                row.set("name", row.getString("name")+"小计");
            }else{
                row.set("number", row.get("nckd_accountnumber"));
            }
        }
    }

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        this.initCurrency();
    }

    private void initCurrency() {
        ComboEdit currency = this.getControl("currency");
        currency.setComboItems(this.getCurrencyItems());
    }
    protected List<ComboItem> getCurrencyItems() {
        List<ComboItem> currColl = new ArrayList();
        ComboItem baseCur = new ComboItem();
        baseCur.setValue("basecurrency");
        baseCur.setCaption(getBaseCurrency());
        currColl.add(baseCur);
        ComboItem allCur = new ComboItem();
        allCur.setValue("allcurrency");
        allCur.setCaption(getAllCurrency());
        currColl.add(allCur);
        QFilter qFilter = new QFilter("enable", QCP.equals,"1");
        DynamicObjectCollection coll = QueryServiceHelper.query("bd_currency", "id, name", new QFilter[]{qFilter});
        Iterator var5 = coll.iterator();

        while(var5.hasNext()) {
            DynamicObject obj = (DynamicObject)var5.next();
            ComboItem item = new ComboItem();
            item.setValue(obj.getString("id"));
            item.setCaption(new LocaleString(obj.getString("name")));
            currColl.add(item);
        }

        return currColl;
    }

    private static LocaleString getBaseCurrency() {
        return new LocaleString(ResManager.loadKDString("综合本位币", "GLRptTemplatePlugin_0", "fi-gl-formplugin", new Object[0]));
    }

    protected static LocaleString getAllCurrency() {
        return new LocaleString(ResManager.loadKDString("所有币别", "GLRptTemplatePlugin_1", "fi-gl-formplugin", new Object[0]));
    }
}
