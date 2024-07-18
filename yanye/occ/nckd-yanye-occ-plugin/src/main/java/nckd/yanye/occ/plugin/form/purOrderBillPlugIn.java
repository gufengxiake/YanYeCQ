package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.CloseCallBack;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.ItemClickEvent;
import org.apache.kafka.common.serialization.VoidDeserializer;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.EventObject;
import java.util.HashSet;
/**
 * 采购订单按钮查询月度采购统计
 * 表单插件
 * author:吴国强 2024-07-12
 */
public class purOrderBillPlugIn extends AbstractBillPlugIn {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 工具栏注册监听（注意这里是把整个工具栏注册监听，工具栏项是没有运行时控件模型的）
        Toolbar toolbar = this.getControl("tbmainentry");
        // 注意itemClick和click的区别
        toolbar.addItemClickListener(this);
    }

    // 注意itemClick和click的区别
    @Override
    public void itemClick(ItemClickEvent e) {
        super.itemClick(e);

        String itemKey = e.getItemKey();
        // 如果是弹出动态表单工具栏项被点击，则打开指定的动态表单
        if ("nckd_querymonthplan".equalsIgnoreCase(itemKey)) {
            FormShowParameter formShowParameter = new FormShowParameter();
            // 弹窗案例-动态表单 页面标识
            formShowParameter.setFormId("nckd_pm_monthplantable");

            //组织信息
            DynamicObject org = (DynamicObject) this.getModel().getValue("org");
            if(org==null){this.getView().showErrMessage("采购组织为空!","为空提示!");return;}
            Object orgId=org.getPkValue();
            //获取单据体当前选中行数据
            EntryGrid EntryEntity = this.getControl("billentry");
            int[] rows = EntryEntity.getSelectRows();
            if(rows.length<=0){
                this.getView().showMessage("请至少选中一行数据!");return;
            }
            HashSet<Object>matGroupIds=new HashSet<>();
            for(int i:rows){
                DynamicObject mat=(DynamicObject) this.getModel().getValue("material",i);
                if(mat!=null){
                    Object groupId=mat.getString("masterid.group.id");
                    Object groupName=mat.getString("masterid.group.name");
                    matGroupIds.add(groupId);
                }
            }
            //订单日期
            Date date= (Date) this.getModel().getValue("biztime");
            // 自定义传参，把当前单据的文本字段传过去
            formShowParameter.setCustomParam("orgId",orgId);
            formShowParameter.setCustomParam("groupId",matGroupIds);
            formShowParameter.setCustomParam("date",date);
            // 设置回调事件，回调插件为当前插件，标识为kdec_sfform
            //formShowParameter.setCloseCallBack(new CloseCallBack(this,"kdec_sfform"));
            // 设置打开类型为模态框（不设置的话指令参数缺失，没办法打开页面）
            formShowParameter.getOpenStyle().setShowType(ShowType.Modal);
            // 当前页面发送showform指令。注意也可以从其他页面发送指令，后续有文章介绍
            this.getView().showForm(formShowParameter);
        }
    }
}