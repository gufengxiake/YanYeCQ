package nckd.yanye.scm.plugin.form;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.*;
/**
 * Module           :制造云-生产任务管理-生产工单（新）
 * Description      :物料-业务处理对应单单据插件
 *
 * @author : yaosijie
 * @date : 2024/7/23
 */
public class BusinessProcessOrderFormPlugin extends AbstractFormPlugin implements Plugin, BeforeF7SelectListener {

    @Override
    public void registerListener(EventObject e) {
        //注册基础资料点击前的监听
        BasedataEdit materielfieldEdit = this.getView().getControl("nckd_materielfield");
        materielfieldEdit.addBeforeF7SelectListener(this);
        BasedataEdit warehouseEdit = this.getView().getControl("nckd_warehouse");
        warehouseEdit.addBeforeF7SelectListener(this);
        BasedataEdit mainproduceEdit = this.getView().getControl("nckd_mainproduce");
        mainproduceEdit.addBeforeF7SelectListener(this);

        super.registerListener(e);
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
        String name = beforeF7SelectEvent.getProperty().getName();
        List<QFilter> qFilters = new ArrayList<>();
        if (name.equals("nckd_materielfield")){
            //构造物料库存信息查询条件（"1" 表示物料允许负库存）
            QFilter qFilter = new QFilter("isallowneginv", QCP.equals, "1");
            DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("bd_materialinventoryinfo", "id,masterid", new QFilter[]{qFilter});
            Set<Long> mates = new HashSet<>();
            Arrays.stream(dynamicObjects).forEach(t->{
                DynamicObject dynamicObject = t.getDynamicObject("masterid");
                mates.add(dynamicObject.getLong("masterid"));
            });
            //构造物料信息查询条件
            QFilter MetesqFilter = new QFilter("id", QCP.in, mates);
            qFilters.add(MetesqFilter);
        }else if (name.equals("nckd_warehouse")){
            DynamicObject dynamicObject = (DynamicObject)this.getModel().getValue("org");
            Long orgId = dynamicObject.getLong("id");
            //构造仓库查询条件
            QFilter qFilter = new QFilter("org", QCP.equals, orgId);
            qFilters.add(qFilter);
        }else if (name.equals("nckd_mainproduce")){
            //构造物料生产信息查询条件("10030" 标识自制件)
            QFilter qFilter = new QFilter("materialattr", QCP.equals, "10030");
            qFilters.add(qFilter);
        }
        beforeF7SelectEvent.setCustomQFilters(qFilters);
    }
}
