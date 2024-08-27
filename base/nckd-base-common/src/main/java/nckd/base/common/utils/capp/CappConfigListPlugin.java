package nckd.base.common.utils.capp;

import kd.bos.context.RequestContext;
import kd.bos.data.BusinessDataReader;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.dynamicobject.DynamicObjectType;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.list.IListDataProvider;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.BeforeCreateListDataProviderArgs;
import kd.bos.list.BillList;
import kd.bos.list.IListView;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.mvc.list.ListDataProvider;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * 单据名称：capp配置表  标识：capp_config 列表插件
 * 用于维护系统配置或接口参数
 * author: chengchaohua
 * date: 2024-08-27
 */
public class CappConfigListPlugin extends AbstractListPlugin
{
  private static final String DELETE = "delete";

  public void beforeItemClick(BeforeItemClickEvent evt) {
    super.beforeItemClick(evt);
    String itemKey = evt.getItemKey();

    BillList billList = (BillList)getControl("billlistap");
    ListSelectedRowCollection listSelectedRowCol = billList.getSelectedRows();

    if (StringUtils.equals("delete", itemKey)) {
      if (listSelectedRowCol.size() == 0) {
        getView().showTipNotification("请选择要执行的数据。");
        evt.setCancel(true);
      } else {
        String formId = ((IListView)getView()).getBillFormId();
        QFilter qFilter = (new QFilter("id", "in", listSelectedRowCol.getPrimaryKeyValues())).and(new QFilter("enable", "=", "1"));
        Map<Object, DynamicObject> map = BusinessDataServiceHelper.loadFromCache(formId, new QFilter[] { qFilter });
        if (map.size() > 0) {
          getView().showTipNotification("请先禁用数据。");
          evt.setCancel(true);
          return;
        }
      }
    }
  }


  public void itemClick(ItemClickEvent evt) {
    super.itemClick(evt);

    String formId = ((IListView)getView()).getBillFormId();

    BillList billList = (BillList)getControl("billlistap");

    ListSelectedRowCollection listSelectedRowCol = billList.getSelectedRows();


    Date currentDate = new Date();
    DynamicObject modifier = null;
    if (StringUtils.equals("enablebutton", evt.getItemKey()) || StringUtils.equals("disablebutton", evt.getItemKey())) {
      long userId = RequestContext.get().getCurrUserId();
      MainEntityType userDT = EntityMetadataCache.getDataEntityType("bos_user");
      modifier = BusinessDataReader.loadSingle(Long.valueOf(userId), (DynamicObjectType)userDT);
    }

    if (StringUtils.equals("enablebutton", evt.getItemKey())) {

      if (listSelectedRowCol != null && listSelectedRowCol.size() > 0) {
        List<DynamicObject> listSelectedRowCol2 = new ArrayList<>();

        for (ListSelectedRow tempRowDataId : listSelectedRowCol) {

          DynamicObject tempRowData = BusinessDataServiceHelper.loadSingle(tempRowDataId.getPrimaryKeyValue(), formId);
          if (!StringUtils.equals(tempRowData.getString("enable"), "1")) {

            tempRowData.set("enable", "1");

            tempRowData.set("modifydatefield", currentDate);
            tempRowData.set("modifierfield", modifier);
            listSelectedRowCol2.add(tempRowData);
          }
        }
        if (listSelectedRowCol2.size() > 0) {

          Object[] result = SaveServiceHelper.save(listSelectedRowCol2.<DynamicObject>toArray(new DynamicObject[0]));

          ((IListView)getView()).refresh();
          getView().showSuccessNotification("启用更新成功" + result.length + "条。");

          for (DynamicObject obj : listSelectedRowCol2) {
            CappConfig.refreshConfigValueCache(obj.getString("code"));
          }

          StringBuilder opDescription = new StringBuilder();
          for (DynamicObject dynamicObject : listSelectedRowCol2) {
            opDescription.append(dynamicObject.getString("code")).append("，");
          }
          CappLogUtil.cappOperationLog("启用", "编码：" + opDescription + "操作成功。", "capp_config");
        } else {
          getView().showTipNotification("数据已为可用状态。");
        }
      }
    } else if (StringUtils.equals("disablebutton", evt.getItemKey())) {

      if (listSelectedRowCol != null && listSelectedRowCol.size() > 0) {
        List<DynamicObject> listSelectedRowCol2 = new ArrayList<>();

        for (ListSelectedRow tempRowDataId : listSelectedRowCol) {

          DynamicObject tempRowData = BusinessDataServiceHelper.loadSingle(tempRowDataId.getPrimaryKeyValue(), formId);
          if (!StringUtils.equals(tempRowData.getString("enable"), "0")) {
            tempRowData.set("enable", "0");
            tempRowData.set("modifydatefield", currentDate);
            tempRowData.set("modifierfield", modifier);
            listSelectedRowCol2.add(tempRowData);
          }
        }
        if (listSelectedRowCol2.size() > 0) {

          Object[] result = SaveServiceHelper.save(listSelectedRowCol2.<DynamicObject>toArray(new DynamicObject[0]));

          ((IListView)getView()).refresh();
          getView().showSuccessNotification("禁用更新成功" + result.length + "条。");

          for (DynamicObject obj : listSelectedRowCol2) {
            CappConfig.removeConfigValueCache(obj.getString("code"));
          }

          StringBuilder opDescription = new StringBuilder();
          for (DynamicObject dynamicObject : listSelectedRowCol2) {
            opDescription.append(dynamicObject.getString("code")).append("，");
          }
          CappLogUtil.cappOperationLog("禁用", "编码：" + opDescription + "操作成功。", "capp_config");
        } else {
          getView().showTipNotification("数据已为禁用状态。");
        }
      }
    } else if (StringUtils.equals("refreshcache", evt.getItemKey())) {

      if (listSelectedRowCol != null && listSelectedRowCol.size() > 0) {
        int executnum = 0;

        for (ListSelectedRow tempRowDataId : listSelectedRowCol) {

          DynamicObject tempRowData = BusinessDataServiceHelper.loadSingle(tempRowDataId.getPrimaryKeyValue(), formId);
          if (StringUtils.equals(tempRowData.getString("enable"), "1")) {

            CappConfig.refreshConfigValueCache(tempRowData.getString("code"));
            executnum++;
          }
        }
        getView().showSuccessNotification("更新缓存成功" + executnum + "条。");
      }
    } else if (StringUtils.equals("delete", evt.getItemKey())) {





      for (ListSelectedRow tempRowDataId : listSelectedRowCol) {

        DynamicObject tempRowData = BusinessDataServiceHelper.loadSingle(tempRowDataId.getPrimaryKeyValue(), formId);
        CappConfig.removeConfigValueCache(tempRowData.getString("code"));
      }
    }
  }



  public void beforeCreateListDataProvider(BeforeCreateListDataProviderArgs args) {
    super.beforeCreateListDataProvider(args);
    args.setListDataProvider((IListDataProvider)new CappConfigListDataProvider());
  }

  static class CappConfigListDataProvider
    extends ListDataProvider {
    public DynamicObjectCollection getData(int arg0, int arg1) {
      DynamicObjectCollection rows = super.getData(arg0, arg1);
      if (rows.isEmpty()) {
        return rows;
      }

      for (DynamicObject row : rows) {
        if (StringUtils.equals("3", row.getString("type")) && !StringUtils.isBlank(row.getString("usevalue"))) {
          row.set("usevalue", "••••••");
        }
      }
      return rows;
    }
  }
}
