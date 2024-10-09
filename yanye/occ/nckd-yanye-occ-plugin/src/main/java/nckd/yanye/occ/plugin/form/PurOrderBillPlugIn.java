package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.EventObject;
import java.util.HashSet;

/**
 * 采购订单表单插件
 * 表单标识：nckd_pm_purorderbill_ext
 * author:吴国强 2024-07-12
 */
public class PurOrderBillPlugIn extends AbstractBillPlugIn {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 工具栏注册监听（注意这里是把整个工具栏注册监听，工具栏项是没有运行时控件模型的）
        Toolbar toolbar = this.getControl("tbmainentry");
        if (toolbar != null) {
            // 注意itemClick和click的区别
            toolbar.addItemClickListener(this);
        }

    }

    @Override
    public void afterCreateNewData(EventObject e) {
        DynamicObject billtype= (DynamicObject) this.getModel().getValue("billtype");
        String number=billtype.getString("number");
        //小包装盐
        if("pm_purorderbill_BT3".equals(number)){
            //承运组织 默认华康本部
            this.getModel().setItemValueByNumber("nckd_orgyf", "11401", 0);

        }else if("pm_purorderbill_BT111".equals(number)){//大包装盐
            DynamicObject purOrg= (DynamicObject) this.getModel().getValue("org");
            //承运组织默认采购组织
            this.getModel().setValue("nckd_orgyf",purOrg);
        }

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
            if (org == null) {
                this.getView().showErrorNotification("采购组织为空!");
                return;
            }
            Object orgId = org.getPkValue();
            //获取单据体当前选中行数据
            EntryGrid EntryEntity = this.getControl("billentry");
            int[] rows = EntryEntity.getSelectRows();
            if (rows.length <= 0) {
                this.getView().showErrorNotification("请至少选中一行数据!");
                return;
            }
            HashSet<Object> matGroupIds = new HashSet<>();
            HashSet<Object> matGroupNums = new HashSet<>();
            for (int i : rows) {
                DynamicObject mat = (DynamicObject) this.getModel().getValue("material", i);
                if (mat != null) {
                    //物料分组Id
                    Object groupId = mat.getString("masterid.group.id");
                    matGroupIds.add(groupId);
                    //物料分组编码
                    Object groupNum = mat.getString("masterid.group.number");
                    matGroupNums.add(groupNum);
                }
            }
            //订单日期
            Date date = (Date) this.getModel().getValue("biztime");
            // 自定义传参，把当前单据的文本字段传过去
            formShowParameter.setCustomParam("orgId", orgId);//组织Id
            formShowParameter.setCustomParam("groupIds", matGroupIds);
            formShowParameter.setCustomParam("groupNum", matGroupNums);
            formShowParameter.setCustomParam("date", date);
            // 设置回调事件，回调插件为当前插件，标识为kdec_sfform
            //formShowParameter.setCloseCallBack(new CloseCallBack(this,"kdec_sfform"));
            // 设置打开类型为模态框（不设置的话指令参数缺失，没办法打开页面）
            formShowParameter.getOpenStyle().setShowType(ShowType.Modal);
            // 当前页面发送showform指令。注意也可以从其他页面发送指令，后续有文章介绍
            this.getView().showForm(formShowParameter);
        }
        //获取运费单价
        else if ("nckd_getfreightprice".equalsIgnoreCase(itemKey)) {
            //运费承担方
            Object yunfei = this.getModel().getValue("nckd_yunfei", 0);
            //当承运方为企业
            if (yunfei != null && yunfei.toString().equalsIgnoreCase("A")) {
                //物流路线
                DynamicObject address = (DynamicObject) this.getModel().getValue("nckd_address", 0);
                if (address == null) {
                    this.getView().showErrorNotification("物流路线为空!");
                    return;
                }
                Object addressId = address.getPkValue();

                //承运方类型
                Object yfTepy = this.getModel().getValue("nckd_yunfeity", 0);
                if (yfTepy == null || yfTepy.equals("")) {
                    this.getView().showErrorNotification("承运方类型不允许为空");
                    return;
                } else if (yfTepy.equals("A")) {
                    //承运方类型为组织
                    DynamicObject cyOrg = (DynamicObject) this.getModel().getValue("nckd_orgyf", 0);
                    if (cyOrg == null) {
                        this.getView().showErrorNotification("承运组织为空!");
                        return;
                    }
                    Object cyOrgId = cyOrg.getPkValue();
                    //装卸商
                    DynamicObject zxssupplier = (DynamicObject) this.getModel().getValue("nckd_zxs", 0);
                    if (zxssupplier == null) {
                        this.getView().showErrorNotification("装卸商为空");
                        return;
                    }
                    Object zxssupplierId = zxssupplier.getPkValue();
                    //承运商
                    DynamicObject supplier = (DynamicObject) this.getModel().getValue("nckd_cys", 0);
                    if (supplier == null) {
                        this.getView().showErrorNotification("承运商为空");
                        return;
                    }
                    Object supplierId = supplier.getPkValue();
                    String message= this.setPrices(cyOrgId, supplierId, addressId);
                    message+= this.zxfsetPrices(cyOrgId,zxssupplierId);
                    if(message.length()>0){
                        this.getView().showErrorNotification(message);
                        return;
                    }
                } else if (yfTepy.equals("B")) {
                    //承运方类型为供应商
                    DynamicObject cyOrg = (DynamicObject) this.getModel().getValue("org", 0);
                    if (cyOrg == null) {
                        this.getView().showErrorNotification("采购组织为空!");
                        return;
                    }
                    Object cyOrgId = cyOrg.getPkValue();
                    //承运方类型为供应商
                    //装卸商
                    DynamicObject zxssupplier = (DynamicObject) this.getModel().getValue("nckd_zxs", 0);
                    if (zxssupplier == null) {
                        this.getView().showErrorNotification("装卸商为空");
                        return;
                    }
                    Object zxssupplierId = zxssupplier.getPkValue();
                    DynamicObject supplier = (DynamicObject) this.getModel().getValue("nckd_cys", 0);
                    if (supplier == null) {
                        this.getView().showErrorNotification("承运商为空");
                        return;
                    }
                    Object supplierId = supplier.getPkValue();
                    String message= this.setPrices(cyOrgId, supplierId, addressId);
                    message+= this.zxfsetPrices(cyOrgId,zxssupplierId);
                    if(message.length()>0){
                        this.getView().showErrorNotification(message);
                    }

                }
                this.getView().showSuccessNotification("获取完成！");

            } else {
                this.getView().showSuccessNotification("承运方未选择企业,无需获取运费/装卸费单价!");
            }


        }
    }

    //获取运费单价
    private String setPrices(Object cyOrgId, Object supplierId, Object addressId) {
        //表单标识(采购合同)
        String number = "conm_purcontract";
        //查询字段
        String fieldkey = "id,billno,biztimebegin,billentry.id as entryId,billentry.priceandtax price";

        QFilter qFilter = new QFilter("org", QCP.equals, cyOrgId)
                .and("supplier", QCP.equals, supplierId)//供应商
                .and("billstatus", QCP.equals, "C")
                .and("closestatus", QCP.equals, "A")
                .and("billentry.nckd_route", QCP.equals, addressId)
                .and("type.number",QCP.equals,"CGYFHT")//合同类型为运费合同
                .and("billentry.nckd_basedatafieldfyxm.number",QCP.equals,"FYXM0025");//费用项目为运输费
        QFilter[] filters = new QFilter[]{qFilter};
        DynamicObjectCollection yfPricesDs = QueryServiceHelper.query(number, fieldkey, filters, "biztimebegin desc");
        if (yfPricesDs.size() > 0) {
            BigDecimal prices = yfPricesDs.get(0).getBigDecimal("price");
            if (prices.compareTo(BigDecimal.ZERO) > 0) {
                int row = this.getModel().getEntryRowCount("billentry");
                for (int i = 0; i < row; i++) {
                    this.getModel().setValue("nckd_pricefieldyf", prices, i);//运费单价
                }

            }
        } else {
            return "未查询到对应单价,请检查采购合同是否维护运费单价!";
        }
        return "";
    }

    //获取装卸费单价
    private String zxfsetPrices(Object cyOrgId, Object supplierId) {
        //表单标识(采购合同)
        String number = "conm_purcontract";
        //查询字段
        String fieldkey = "id,billno,biztimebegin,billentry.id as entryId,billentry.priceandtax price";

        QFilter qFilter = new QFilter("org", QCP.equals, cyOrgId)
                .and("supplier", QCP.equals, supplierId)//装卸商
                .and("billstatus", QCP.equals, "C")
                .and("closestatus", QCP.equals, "A")
                .and("type.number",QCP.equals,"CGYFHT")//合同类型为运费合同
                .and("billentry.nckd_basedatafieldfyxm.number",QCP.equals,"FYXM0026");//费用项目为装卸费
        QFilter[] filters = new QFilter[]{qFilter};
        DynamicObjectCollection yfPricesDs = QueryServiceHelper.query(number, fieldkey, filters, "biztimebegin desc");
        if (yfPricesDs.size() > 0) {
            BigDecimal prices = yfPricesDs.get(0).getBigDecimal("price");
            if (prices.compareTo(BigDecimal.ZERO) > 0) {
                int row = this.getModel().getEntryRowCount("billentry");
                for (int i = 0; i < row; i++) {
                    this.getModel().setValue("nckd_zxfprice", prices, i);//装卸费单价
                }

            }
        } else {
            return "未查询到对应单价,请检查采购合同是否维护装卸单价!";
        }
        return "";
    }
}