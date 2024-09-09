package nckd.yanye.scm.plugin.form;

import java.util.EventObject;
import java.util.List;
import java.util.stream.Collectors;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.control.events.RowClickEvent;
import kd.bos.form.control.events.RowClickEventListener;

/**
 * @author husheng
 * @date 2024-09-09 13:51
 * @description 检查结果（nckd_cal_datacheck_re_ext）控制修改按钮的可见性
 */
public class CalDatacheckResultFormPlugin extends AbstractBillPlugIn implements RowClickEventListener {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        // 单据体行点击
        EntryGrid entryGrid = this.getView().getControl("entryentity");
        entryGrid.addRowClickListener(this);
    }

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);

        DynamicObjectCollection entryentity = this.getModel().getEntryEntity("entryentity");
        List<DynamicObject> objectList = entryentity.stream().filter(dynamicObject -> "DC-ITEM-46".equals(dynamicObject.getDynamicObject("checkitem").getString("number")))
                .collect(Collectors.toList());
        boolean flag = false;
        if (objectList.size() > 0) {
            String fentrystatus = objectList.get(0).getString("entrystatus");
            DynamicObject checkitem = entryentity.get(0).getDynamicObject("checkitem");
            if (fentrystatus.equals("B") && "DC-ITEM-46".equals(checkitem.getString("number"))) {
                flag = true;
            }
        }
        this.getView().setVisible(flag, "nckd_amend");
    }


    @Override
    public void entryRowClick(RowClickEvent evt) {
        // 获取选中的行
        EntryGrid entryentity = this.getView().getControl("entryentity");
        int[] selectRows = entryentity.getSelectRows();
        boolean flag = false;
        if (selectRows.length > 0) {
            DynamicObject rowEntity = this.getModel().getEntryRowEntity("entryentity", selectRows[0]);
            if ("DC-ITEM-46".equals(rowEntity.getDynamicObject("checkitem").getString("number")) && "B".equals(rowEntity.getString("entrystatus"))) {
                flag = true;
            }
        }
        this.getView().setVisible(flag, "nckd_amend");
    }
}
