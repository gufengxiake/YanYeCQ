package nckd.yanye.scm.plugin.form;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.EntityType;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.list.BillList;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :制造云-生产任务管理-物料-业务处理对应单
 * Description      :物料-业务处理对应单列表插件
 *
 * @author : zhujintao
 * @date : 2024/7/23
 */
public class BussProcessOrderGenerateListPlugin extends AbstractListPlugin {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners("nckd_generate");
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        String itemKey = evt.getItemKey();
        if ("nckd_generate".equals(itemKey)) {
            //巴拉巴拉
            String operationKey = evt.getOperationKey();
        }
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        super.afterDoOperation(afterDoOperationEventArgs);
        String operateKey = afterDoOperationEventArgs.getOperateKey();
        if (StringUtils.equals("generate", operateKey)) {
            createInvcountScheme();
        }
    }

    private void createInvcountScheme() {
        BillList billlistap = this.getView().getControl("billlistap");
        ListSelectedRowCollection selectedRows = billlistap.getSelectedRows();
        EntityType entityType = billlistap.getEntityType();
        //获取选中行pkid
        Object[] primaryKeyValues = selectedRows.getPrimaryKeyValues();
        //获取完整数据
        DynamicObject[] bussProcessOrderArr = BusinessDataServiceHelper.load(primaryKeyValues, entityType);
        //判断所选数据是不是处于对应的业务期间
        Set<Object> orgSet = Arrays.stream(bussProcessOrderArr).map(e -> e.getDynamicObject("org").getPkValue()).collect(Collectors.toSet());
        //拼装业务期间查询QFilter
        QFilter qFilter = new QFilter("org", QCP.in, orgSet).and("entry.isenabled", QCP.equals, true);
        DynamicObject[] sysctrlentityArr = BusinessDataServiceHelper.load("cal_sysctrlentity", "id,org,entry.periodtype,entry.currentperiod", qFilter.toArray());
        //转化为Map结构
        Map<Object, DynamicObject> sysctrlentityMap = Arrays.stream(sysctrlentityArr).collect(Collectors.toMap(k -> k.getDynamicObject("org").getPkValue(), v -> v));
        for (DynamicObject bussProcessOrder : bussProcessOrderArr) {
            String billno = bussProcessOrder.getString("billno");
            Object pkValue = bussProcessOrder.getDynamicObject("org").getPkValue();
            DynamicObject sysctrlentity = sysctrlentityMap.get(pkValue);
            if (ObjectUtil.isEmpty(sysctrlentity)) {
                this.getView().showErrorNotification(billno + "对应的核算期间设置未找到");
                break;
            }
        }
        //遍历设置盘点方案
        for (DynamicObject bussProcessOrder : bussProcessOrderArr) {
            String billno = bussProcessOrder.getString("billno");
            //组织
            Object org = bussProcessOrder.getDynamicObject("org").getPkValue();
            DynamicObjectCollection bussProcessOrderEntry = bussProcessOrder.getDynamicObjectCollection("nckd_bussinessentries");
            DynamicObject sysctrlentity = sysctrlentityMap.get(org);
            //截至日期
            Date enddate = sysctrlentity.getDate("enddate");
            //新增盘点方案
            DynamicObject invcountscheme = BusinessDataServiceHelper.newDynamicObject("im_invcountscheme");
            DynamicObjectCollection dynamicObjectCollection = invcountscheme.getDynamicObjectCollection("");
            //DynamicObject invcountschemeEntry = dynamicObjectCollection.addNew();
            invcountscheme.set("org", org);
            Date date = new Date();
            String yyyyMMdd = DateUtil.format(date, "yyyyMMdd");
            invcountscheme.set("billno", "PDFA-" + yyyyMMdd + "-000001");
            invcountscheme.set("schemename", "PDFA-" + yyyyMMdd + "-000001");
            invcountscheme.set("counttype", "A");
            invcountscheme.set("org", org);
            invcountscheme.set("backupcondition", "enddateinvacc");
            invcountscheme.set("enddate", enddate);
            invcountscheme.set("accessnode", "end");
            invcountscheme.set("excludeenddate", false);
            invcountscheme.set("completestatus", "A");
            invcountscheme.set("gencountstatus", "A");
            invcountscheme.set("billstatus", "A");
            invcountscheme.set("comment", "");
            //countzeroinv  freezeoutin  enablecheck   nogenotherinout
            invcountscheme.set("defaultvalue", "B");
            invcountscheme.set("mulorg", org);
            invcountscheme.set("warehouse", null);
            invcountscheme.set("location", null);
            List<DynamicObject> materialList = bussProcessOrderEntry.stream().map(e -> e.getDynamicObject("nckd_materielfield")).collect(Collectors.toList());
            invcountscheme.set("material", materialList);
            long currUserId = RequestContext.get().getCurrUserId();
            invcountscheme.set("creator", currUserId);
            invcountscheme.set("createtime", date);
            invcountscheme.set("modifier", currUserId);
            invcountscheme.set("lastupdateuser", currUserId);
            invcountscheme.set("modifytime", date);
            invcountscheme.set("lastupdatetime", date);
            OperationResult operationResult = OperationServiceHelper.executeOperate("save", "im_invcountscheme", new DynamicObject[]{invcountscheme}, OperateOption.create());
            if (!operationResult.isSuccess()) {
                this.getView().showErrorNotification(billno + "对应的盘点方案新增失败");
                break;
            }
        }
    }
}
