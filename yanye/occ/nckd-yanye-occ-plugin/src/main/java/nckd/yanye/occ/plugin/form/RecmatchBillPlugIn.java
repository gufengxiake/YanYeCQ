package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


/*
匹配预收款表单插件
 */
public class RecmatchBillPlugIn extends AbstractBillPlugIn {
    @Override
    public void afterBindData(EventObject e){
        super.afterBindData(e);
        int row = this.getModel().getEntryRowCount("entry_b");//收款处理单据体
        if(row==0){return;}
        HashSet<String> billNoList = new HashSet<>();
        for (int i = 0; i < row; i++) {
            String billNo = this.getModel().getValue("billno_b", i).toString();
            billNoList.add(billNo);
        }
        //根据单据编号获取摘要
        // 构造QFilter
        QFilter qFilter = new QFilter("billno", QFilter.in, billNoList);
        String number = "cas_recbill";//收款处理单据标识
        String selectFids = "billno,txt_description";
        DynamicObjectCollection collections = QueryServiceHelper.query(number, selectFids, qFilter.toArray(), "");
        if (!collections.isEmpty()) {
            Map<String, String> billDescription = new HashMap<>();
            for (DynamicObject obj : collections) {
                String billno = obj.getString("billno");
                String description = obj.getString("txt_description");
                billDescription.put(billno, description);
            }
            for (int i = 0; i < row; i++) {
                String billNo = this.getModel().getValue("billno_b", i).toString();
                this.getModel().setValue("nckd_description", billDescription.get(billNo), i);
            }
            this.getView().updateView("entry_b");
        }

    }
}
