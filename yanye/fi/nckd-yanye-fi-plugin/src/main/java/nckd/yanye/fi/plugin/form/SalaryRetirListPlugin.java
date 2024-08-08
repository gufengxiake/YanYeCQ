package nckd.yanye.fi.plugin.form;

import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import nckd.yanye.fi.plugin.form.SalaryRetirSettleOpPlugin;

import java.util.EventObject;

public class SalaryRetirListPlugin extends AbstractListPlugin {



    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        this.addItemClickListeners("nckd_baritemap");
//        this.addItemClickListeners("nckd_baritemap");
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);

        if (evt.getItemKey().equals("rentsettlebutton")) {
            //暂估批量调价 下推新增月末调价单
//            this.batchAdjustPrice();
            new SalaryRetirSettleOpPlugin();
        }
    }
}
