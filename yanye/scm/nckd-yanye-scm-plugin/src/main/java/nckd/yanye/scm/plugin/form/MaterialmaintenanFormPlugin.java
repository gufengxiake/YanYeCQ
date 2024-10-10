package nckd.yanye.scm.plugin.form;

import kd.bd.sbd.enums.EndDateCalTypeEnum;
import kd.bd.sbd.servicehelper.BDServiceHelper;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.bill.BillShowParameter;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.ComboProp;
import kd.bos.entity.property.OrgProp;
import kd.bos.exception.KDBizException;
import kd.bos.form.ShowType;
import kd.bos.form.control.Control;
import kd.bos.form.control.Label;
import kd.bos.form.field.*;
import kd.bos.form.field.events.AfterF7SelectEvent;
import kd.bos.form.field.events.AfterF7SelectListener;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import nckd.yanye.scm.common.utils.MaterialAttributeInformationUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author husheng
 * @date 2024-08-21 9:28
 * @description 物料维护单-弹框查询
 * nckd_materialmaintenan
 */
public class MaterialmaintenanFormPlugin extends AbstractBillPlugIn implements BeforeF7SelectListener, AfterF7SelectListener {
    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);

        BasedataEdit purchaseorgEdit = this.getControl("nckd_purchaseorg");
        purchaseorgEdit.addBeforeF7SelectListener(this);
        BasedataEdit buyerEdit = this.getControl("nckd_buyer");
        buyerEdit.addBeforeF7SelectListener(this);
        BasedataEdit materialnumberEdit = this.getControl("nckd_materialnumber");
        materialnumberEdit.addAfterF7SelectListener(this);
        BasedataEdit purchaseunit = this.getControl("nckd_purchaseunit");
        purchaseunit.addBeforeF7SelectListener(this);
        BasedataEdit inventoryunit = this.getControl("nckd_inventoryunit");
        inventoryunit.addBeforeF7SelectListener(this);
        BasedataEdit salesunit = this.getControl("nckd_salesunit");
        salesunit.addBeforeF7SelectListener(this);
        BasedataEdit mftunit = this.getControl("nckd_mftunit");
        mftunit.addBeforeF7SelectListener(this);

        this.addClickListeners("nckd_sourcenumberv");
    }

    @Override
    public void afterCreateNewData(EventObject e) {
        QFilter qFilter = new QFilter("number", QCP.equals, "1");
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bos_adminorg", new QFilter[]{qFilter});
        //创建组织默认江盐集团
        this.getModel().setValue("nckd_createorganiza", dynamicObject);
        this.getModel().setValue("nckd_initiatingdepart", RequestContext.get().getOrgId());
        super.afterCreateNewData(e);
    }

    @Override
    public void click(EventObject evt) {
        super.click(evt);

        Control source = (Control) evt.getSource();
        String key = source.getKey();
        if (key.equals("nckd_sourcenumberv")) {

            BillShowParameter billShowParameter = new BillShowParameter();
            billShowParameter.setFormId("nckd_materialrequest");
            billShowParameter.setPkId(this.getModel().getValue("nckd_sourceid"));
            // 打开方式
            billShowParameter.getOpenStyle().setShowType(ShowType.MainNewTabPage);
            this.getView().showForm(billShowParameter);
        }
    }

    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);

//        String materialmaintunit = (String) this.getModel().getValue("nckd_materialmaintunit");
//        if ("update".equals(materialmaintunit)) {
//            ComboEdit combo = this.getView().getControl("nckd_documenttype");
//            List<ComboItem> items = new ArrayList<>();
//            items.add(new ComboItem("", new LocaleString("生产类型"), "1", true));
//            items.add(new ComboItem("", new LocaleString("仓库类型"), "2", true));
//            items.add(new ComboItem("", new LocaleString("销售类型"), "4", true));
//            items.add(new ComboItem("", new LocaleString("采购类型"), "5", true));
//            combo.setComboItems(items);
//        } else if ("add".equals(materialmaintunit)) {
//            String nckdDocumenttype = (String) this.getModel().getValue("nckd_documenttype");
//            if ("1".equals(nckdDocumenttype)) {
//                // 生产信息创建组织
//                this.getModel().setValue("nckd_mftunit", this.getModel().getValue("nckd_baseunit"));
//                // 供货库存组织
//                this.getModel().setValue("nckd_supplyorgunitid", this.getModel().getValue("org"));
//                // 物料属性
//                String materialattribute = (String) this.getModel().getValue("nckd_materialattribute");
//                String materialattri = null;
//                if ("1".equals(materialattribute)) {
//                    materialattri = "10030";
//                } else if ("2".equals(materialattribute)) {
//                    materialattri = "10040";
//                }
//                this.getModel().setValue("nckd_materialattri", materialattri);
//                // BOM版本规则
//                this.getModel().setValue("nckd_bomversionrule", MaterialAttributeInformationUtils.getDefaultBOMRuleVer());
//            } else if ("2".equals(nckdDocumenttype)) {
//                this.getModel().setValue("nckd_inventoryunit", this.getModel().getValue("nckd_baseunit"));
//            } else if ("4".equals(nckdDocumenttype)) {
//                this.getModel().setValue("nckd_salesunit", this.getModel().getValue("nckd_baseunit"));
//            } else if ("5".equals(nckdDocumenttype)) {
//                this.getModel().setValue("nckd_purchaseunit", this.getModel().getValue("nckd_baseunit"));
//            }
//        }

        DynamicObject org = (DynamicObject) this.getModel().getValue("org");
        // 组织范围内属性页签
        DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle("nckd_orgpropertytab", new QFilter[]{new QFilter("nckd_entryentity.nckd_org", QCP.equals, org.getPkValue())});
        if (loadSingle != null) {
            List<DynamicObject> collect = loadSingle.getDynamicObjectCollection("nckd_entryentity").stream().filter(dynamicObject -> dynamicObject.getDynamicObject("nckd_org").getPkValue() == org.getPkValue()).collect(Collectors.toList());
            List<String> materialproperty = Arrays.stream(collect.get(0).getString("nckd_materialproperty").split(",")).filter(s -> StringUtils.isNotEmpty(s)).collect(Collectors.toList());
            if (materialproperty.contains("2")) {
                this.setEditShow(true);
            } else {
                this.setEditShow(false);
            }
        } else {
            this.setEditShow(false);
        }

        // 来源编码标签
        Label sourcenumberv = this.getControl("nckd_sourcenumberv");
        sourcenumberv.setText((String) this.getModel().getValue("nckd_sourcenumber"));
    }

    @Override
    public void beforeF7Select(BeforeF7SelectEvent beforeF7SelectEvent) {
        String name = beforeF7SelectEvent.getProperty().getName();
        List<QFilter> qFilters = new ArrayList<>();
        if ("nckd_buyer".equals(name)) {
            //  根据采购组获取采购员
            DynamicObject purchaseorg = (DynamicObject) this.getModel().getValue("nckd_purchaseorg");
            if (purchaseorg == null) {
                throw new KDBizException("请先选择采购组");
            }
            DynamicObject operatorgroup = BusinessDataServiceHelper.loadSingle(purchaseorg.getPkValue(), "bd_operatorgroup");
            List<Object> objects = operatorgroup.getDynamicObjectCollection("entryentity").stream().map(dynamicObject ->
                    dynamicObject.getDynamicObject("operator").getPkValue()
            ).collect(Collectors.toList());
            QFilter qFilter = new QFilter("id", QCP.in, objects);
            qFilters.add(qFilter);
        } else if ("nckd_purchaseorg".equals(name)) {
            // 业务组类型是采购组
            QFilter qFilter = new QFilter("operatorgrouptype", QCP.equals, "CGZ");
            qFilters.add(qFilter);
        } else if ("nckd_purchaseunit".equals(name) || "nckd_inventoryunit".equals(name) || "nckd_salesunit".equals(name) || "nckd_mftunit".equals(name)) {
            DynamicObject matDO = (DynamicObject) this.getModel().getValue("nckd_materialnumber");
            if (matDO != null) {
                DynamicObject bdMaterial = BusinessDataServiceHelper.loadSingle(matDO.getPkValue(), "bd_material");
                DynamicObject baseunitDO = bdMaterial.getDynamicObject("baseunit");
                List fixconunitlist = BDServiceHelper.getAssistMUListResult(matDO.getLong("id"), baseunitDO.getLong("id"), "1");
                QFilter fixconFilter = new QFilter("id", "in", fixconunitlist);
                qFilters.add(fixconFilter);
            }
        }
        beforeF7SelectEvent.setCustomQFilters(qFilters);
    }

    @Override
    public void afterF7Select(AfterF7SelectEvent afterF7SelectEvent) {
        //获取单据类型
        String nckddocumenttype = (String) this.getModel().getValue("nckd_documenttype");
        //获取单据维护类型
        String nckdmaterialmaintunit = (String) this.getModel().getValue("nckd_materialmaintunit");
        //获取选中的行
        ListSelectedRowCollection collection = afterF7SelectEvent.getListSelectedRowCollection();
        if (collection.size() > 0) {
            //查询选中行的物料
            DynamicObject material = BusinessDataServiceHelper.loadSingle(collection.get(0).getPrimaryKeyValue(), "bd_material");

            //update：修改物料属性   updateinfo：修改物料基本信息
            if ("update".equals(nckdmaterialmaintunit)) {
                attributeInfo(material, nckddocumenttype);
            } else if ("updateinfo".equals(nckdmaterialmaintunit)) {
                updateBaseInfo(material);
            }
        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);

        String name = e.getProperty().getName();
//        if ("nckd_materialmaintunit".equals(name)) {
//            ChangeData changeData = e.getChangeSet()[0];
//            String materialmaintunit = (String) changeData.getNewValue();
//            ComboEdit combo = this.getView().getControl("nckd_documenttype");
//            List<ComboItem> items = new ArrayList<>();
//            items.add(new ComboItem("", new LocaleString("生产类型"), "1", true));
//            items.add(new ComboItem("", new LocaleString("仓库类型"), "2", true));
//            items.add(new ComboItem("", new LocaleString("销售类型"), "4", true));
//            items.add(new ComboItem("", new LocaleString("采购类型"), "5", true));
//            if ("add".equals(materialmaintunit)) {
//                items.add(new ComboItem("", new LocaleString("财务类型"), "3", true));
//            }
//            combo.setComboItems(items);
//            this.getView().updateView();
//        } else
        if ("nckd_documenttype".equals(name)) {
            ChangeData changeData = e.getChangeSet()[0];
            String nckdDocumenttype = (String) changeData.getNewValue();
            String materialmaintunit = (String) this.getModel().getValue("nckd_materialmaintunit");
//            if ("add".equals(materialmaintunit)) {
//                if ("1".equals(nckdDocumenttype)) {
//                    this.getModel().setValue("nckd_mftunit", this.getModel().getValue("nckd_baseunit"));
//                    this.getModel().setValue("nckd_supplyorgunitid", this.getModel().getValue("org"));
//                } else if ("2".equals(nckdDocumenttype)) {
//                    this.getModel().setValue("nckd_inventoryunit", this.getModel().getValue("nckd_baseunit"));
//                } else if ("4".equals(nckdDocumenttype)) {
//                    this.getModel().setValue("nckd_salesunit", this.getModel().getValue("nckd_baseunit"));
//                } else if ("5".equals(nckdDocumenttype)) {
//                    this.getModel().setValue("nckd_purchaseunit", this.getModel().getValue("nckd_baseunit"));
//                }
//            } else
            if ("update".equals(materialmaintunit)) {
                DynamicObject materialnumber = (DynamicObject) this.getModel().getValue("nckd_materialnumber");
                if (materialnumber != null) {
                    DynamicObject bdMaterial = BusinessDataServiceHelper.loadSingle(materialnumber.getPkValue(), "bd_material");
                    attributeInfo(bdMaterial, nckdDocumenttype);
                }
            }
        } else if ("nckd_enableshelflifemgr".equals(name)) {
            ChangeData changeData = e.getChangeSet()[0];
            Boolean enableshelflifemgr = (Boolean) changeData.getNewValue();
            if (enableshelflifemgr) {
                this.getModel().setValue("nckd_calculationforenddat", EndDateCalTypeEnum.STARTDATEADDSHELFLIFE.getValue());
            }
        } else if ("nckd_materialattri".equals(name)) {
            ChangeData changeData = e.getChangeSet()[0];
            String materialattri = (String) changeData.getNewValue();
            this.getModel().setValue("nckd_materialattr", materialattri);
        }
    }

    /**
     * 计划基本信息是否展示
     *
     * @param flag
     */
    private void setEditShow(Boolean flag) {
        // 隐藏页签
        this.getView().setVisible(flag, "nckd_tabpageap6");

        // 设置字段非必填
        // 前端属性设置
        FieldEdit createorg = this.getControl("nckd_createorg");
        createorg.setMustInput(flag);
        // 后端属性设置
        OrgProp nckdCreateorg = (OrgProp) this.getModel().getDataEntityType().getProperty("nckd_createorg");
        nckdCreateorg.setMustInput(flag);

        // 前端属性设置
        FieldEdit materialattr = this.getControl("nckd_materialattr");
        materialattr.setMustInput(flag);
        // 后端属性设置
        ComboProp nckdMaterialattr = (ComboProp) this.getModel().getDataEntityType().getProperty("nckd_materialattr");
        nckdMaterialattr.setMustInput(flag);

        // 前端属性设置
        FieldEdit planmode = this.getControl("nckd_planmode");
        planmode.setMustInput(flag);
        // 后端属性设置
        ComboProp nckdPlanmode = (ComboProp) this.getModel().getDataEntityType().getProperty("nckd_planmode");
        nckdPlanmode.setMustInput(flag);
    }

    public void updateBaseInfo(DynamicObject material) {
        //选择物料后回填物料基本信息
        this.getModel().setValue("nckd_materialclassify", material.get("group"));//物料分类
        this.getModel().setValue("nckd_altermaterialclass", material.get("group"));//变更后物料分类
        this.getModel().setValue("nckd_materialnumber", material);//物料编码
        this.getModel().setValue("nckd_altermaterialnum", material.get("number"));//变更后物料编码
        this.getModel().setValue("nckd_materialname", material.get("name"));//物料名称
        this.getModel().setValue("nckd_altermaterialname", material.get("name"));//变更后物料名称
        this.getModel().setValue("nckd_specifications", material.get("modelnum"));//规格
        this.getModel().setValue("nckd_alterspecificat", material.get("modelnum"));//变更后规格
        this.getModel().setValue("nckd_model", material.get("nckd_model"));//型号
        this.getModel().setValue("nckd_altermodel", material.get("nckd_model"));//变更后型号
        this.getModel().setValue("nckd_baseunit", material.get("baseunit"));//基本单位
        this.getModel().setValue("nckd_materialtype", material.get("materialtype"));//物料类型
        this.getModel().setValue("nckd_oldmaterialnumber", material.get("oldnumber"));//旧物料编码
        this.getModel().setValue("nckd_alteroldnumber", material.get("oldnumber"));//变更后旧物料编码
        this.getModel().setValue("nckd_mnemoniccode", material.get("helpcode"));//助记码
        this.getModel().setValue("nckd_altermnemoniccode", material.get("helpcode"));//变更后助记码
        this.getModel().setValue("nckd_remark", material.get("description"));//描述
        this.getModel().setValue("nckd_alterremark", material.get("description"));//变更后描述
        this.getModel().setValue("nckd_materialrisk", material.get("hazardous"));//物料危险性
        this.getModel().setValue("nckd_altermaterialrisk", material.get("hazardous"));//变更后物料危险性
        this.getModel().setValue("nckd_outsourcing", material.get("enableoutsource"));//可委外
        this.getModel().setValue("nckd_alteroutsourcing", material.get("enableoutsource"));//变更后可委外
    }

    public void attributeInfo(DynamicObject material, String nckddocumenttype) {
        DynamicObject orgDynamicObject = (DynamicObject) this.getModel().getValue("org");
        QFilter qFilter = new QFilter("createorg", QCP.equals, orgDynamicObject.getPkValue())
                .and("masterid", QCP.equals, material.getPkValue());
        /**
         * 判断单据类型
         * 	1：生产类型
         * 	2：仓库类型
         * 	3：财务类型
         * 	4：销售类型
         * 	5：采购类型
         */
        switch (nckddocumenttype) {
            case "1":
                //设置生产基本信息
                setBasicProductionInformation(qFilter);

                // 组织范围内属性页签
                DynamicObject org = (DynamicObject) this.getModel().getValue("org");
                DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("nckd_orgpropertytab", new QFilter[]{new QFilter("nckd_entryentity.nckd_org", QCP.equals, org.getPkValue())});
                if (dynamicObject != null) {
                    List<DynamicObject> collect = dynamicObject.getDynamicObjectCollection("nckd_entryentity").stream().filter(dynamic -> dynamic.getDynamicObject("nckd_org").getPkValue() == org.getPkValue()).collect(Collectors.toList());
                    List<String> materialproperty = Arrays.stream(collect.get(0).getString("nckd_materialproperty").split(",")).filter(s -> StringUtils.isNotEmpty(s)).collect(Collectors.toList());
                    if (materialproperty.contains("2")) {
                        //计划基本信息
                        setBasicinformationplan(qFilter);
                    }
                }

                //质检基本信息
                setBasicQualityInspectionInformation(qFilter);
                break;
            case "2":
                //设置库存基本信息
                setBasicinventoryinformation(qFilter);
                break;
            case "3":
                //核算基本信息
                setBasicaccountinginformation(qFilter);
                break;
            case "4":
                //销售基本信息
                setBasicSalesInformation(qFilter);
                break;
            case "5":
                //采购基本信息
                setBasicProcurementInformation(qFilter);
                //物料采购员信息
                setMaterialpurchaserinformation(new QFilter("org", QCP.equals, orgDynamicObject.getPkValue())
                        .and("entryentity.materialmasterid", QCP.equals, material.getPkValue()));
                //质检基本信息
                setBasicQualityInspectionInformation(qFilter);
                break;
        }

    }

    //设置采购基本信息
    public void setBasicProcurementInformation(QFilter qFilter) {
        //物料采购基本信息
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bd_materialpurchaseinfo", new QFilter[]{qFilter});
        if (dynamicObject != null) {
            this.getModel().setValue("nckd_purchaseunit", dynamicObject.get("purchaseunit"));//采购单位
            this.getModel().setValue("nckd_iscontrolqty", dynamicObject.get("iscontrolqty"));//控制收货数量
            this.getModel().setValue("nckd_receiverateup", dynamicObject.get("receiverateup"));//收货超收比率(%)
            this.getModel().setValue("nckd_receiveratedown", dynamicObject.get("receiveratedown"));//收货欠收比率(%)
        }
    }

    //设置物料采购员信息
    public void setMaterialpurchaserinformation(QFilter qFilter) {
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("msbd_puropermaterctrl", new QFilter[]{qFilter});
        if (dynamicObject != null) {
            DynamicObject object = dynamicObject.getDynamicObjectCollection("entryentity").get(0);
            DynamicObject loadSingle = BusinessDataServiceHelper.loadSingle("bos_user", new QFilter[]{new QFilter("number", QCP.equals, object.getDynamicObject("operator").getString("operatornumber"))});
            //需要去查询采购组
            this.getModel().setValue("nckd_purchaseorg", object.get("operatorgroup"));//采购组
            this.getModel().setValue("nckd_buyer", loadSingle);//采购员
        }
    }

    //设置库存基本信息
    public void setBasicinventoryinformation(QFilter qFilter) {
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bd_materialinventoryinfo", new QFilter[]{qFilter});
        if (dynamicObject != null) {
            this.getModel().setValue("nckd_inventoryunit", dynamicObject.get("inventoryunit"));//库存单位
            this.getModel().setValue("nckd_minpackqty", dynamicObject.get("minpackqty"));//最小包装量
            this.getModel().setValue("nckd_ispurchaseinspect", dynamicObject.get("ispurchaseinspect"));//来料检验
            this.getModel().setValue("nckd_ismininvalert", dynamicObject.get("ismininvalert"));//启用最小库存预警
            this.getModel().setValue("nckd_mininvqty", dynamicObject.get("mininvqty"));//最小库存
            this.getModel().setValue("nckd_issaftyinvalert", dynamicObject.get("issaftyinvalert"));//启用安全库存预警
            this.getModel().setValue("nckd_saftyinvqty", dynamicObject.get("saftyinvqty"));//安全库存
            this.getModel().setValue("nckd_ismaxinvalert", dynamicObject.get("ismaxinvalert"));//启用最大库存预警
            this.getModel().setValue("nckd_maxinvqty", dynamicObject.get("maxinvqty"));//最大库存
            this.getModel().setValue("nckd_lotcoderule", dynamicObject.get("lotcoderule"));//批号规则
            this.getModel().setValue("nckd_enableshelflifemgr", dynamicObject.get("enableshelflifemgr"));//保质期管理
            this.getModel().setValue("nckd_shelflifeunit", dynamicObject.get("shelflifeunit"));//保质期单位
            this.getModel().setValue("nckd_shelflife", dynamicObject.get("shelflife"));//保质期
            this.getModel().setValue("nckd_calculationforenddat", dynamicObject.get("calculationforenddate"));//到期日计算方式
            this.getModel().setValue("nckd_leadtimeunit", dynamicObject.get("leadtimeunit"));//提前期单位
            this.getModel().setValue("nckd_dateofoverdueforin", dynamicObject.get("dateofoverdueforin"));//入库失效提前期
            this.getModel().setValue("nckd_dateofoverdueforout", dynamicObject.get("dateofoverdueforout"));//出库失效提前期
            this.getModel().setValue("nckd_enablewarnlead", dynamicObject.get("enablewarnlead"));//启用预警
            this.getModel().setValue("nckd_warnleadtime", dynamicObject.get("warnleadtime"));//预警提前期
            this.getModel().setValue("nckd_manustrategy", dynamicObject.get("manustrategy"));//制造策略
            this.getModel().setValue("nckd_outboundrule", dynamicObject.get("outboundrule"));//出库规则
        }
    }

    //设置销售基本信息
    public void setBasicSalesInformation(QFilter qFilter) {
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bd_materialsalinfo", new QFilter[]{qFilter});
        if (dynamicObject != null) {
            this.getModel().setValue("nckd_salesunit", dynamicObject.get("salesunit"));//销售单位
            this.getModel().setValue("nckd_iscontrolsendqty", dynamicObject.get("iscontrolqty"));//控制发货数量
            this.getModel().setValue("nckd_dlivrateceiling", dynamicObject.get("dlivrateceiling"));//发货超发比率(%)
            this.getModel().setValue("nckd_dlivratefloor", dynamicObject.get("dlivratefloor"));//发货欠发比率(%)
        }
    }

    //设置生产基本信息
    public void setBasicProductionInformation(QFilter qFilter) {
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bd_materialmftinfo", new QFilter[]{qFilter});
        if (dynamicObject != null) {
            this.getModel().setValue("nckd_mftunit", dynamicObject.get("mftunit"));//生产计量单位
            this.getModel().setValue("nckd_materialattri", dynamicObject.get("materialattr"));//物料属性
            this.getModel().setValue("nckd_departmentorgid", dynamicObject.get("departmentorgid"));//生产部门
            this.getModel().setValue("nckd_bomversionrule", dynamicObject.get("bomversionrule"));//BOM版本规则
            this.getModel().setValue("nckd_isjointproduct", dynamicObject.get("isjointproduct"));//可联副产品
            this.getModel().setValue("nckd_supplyorgunitid", dynamicObject.get("supplyorgunitid"));//供货库存组织
            this.getModel().setValue("nckd_issuemode", dynamicObject.get("issuemode"));//领送料方式
            this.getModel().setValue("nckd_isbackflush", dynamicObject.get("isbackflush"));//倒冲
        }
    }

    //设置核算基本信息
    public void setBasicaccountinginformation(QFilter qFilter) {
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bd_materialcalinfo", new QFilter[]{qFilter});
        if (dynamicObject != null) {
            this.getModel().setValue("nckd_group", dynamicObject.get("group"));//存货类别
        }
    }

    //设置计划基本信息
    public void setBasicinformationplan(QFilter qFilter) {
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("mpdm_materialplan", new QFilter[]{qFilter});
        if (dynamicObject != null) {
            this.getModel().setValue("nckd_createorg", dynamicObject.get("createorg"));//计划信息创建组织
            this.getModel().setValue("nckd_materialattr", dynamicObject.get("materialattr"));//物料属性
            this.getModel().setValue("nckd_planmode", dynamicObject.get("planmode"));//计划方式
            this.getModel().setValue("nckd_allowleadtime", dynamicObject.get("allowleadtime"));//允许提前期间（天）
            this.getModel().setValue("nckd_leadadvance", dynamicObject.get("leadadvance"));//提前容差（天）
            this.getModel().setValue("nckd_fallowdelayperiod", dynamicObject.get("allowdelayperiod"));//允许延后期间（天）
            this.getModel().setValue("nckd_delaytolerance", dynamicObject.get("delaytolerance"));//延后容差（天）
        }
    }

    //设置质检基本信息
    public void setBasicQualityInspectionInformation(QFilter qFilter) {
        DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("bd_inspect_cfg", new QFilter[]{qFilter});
        if (dynamicObject != null && dynamicObject.getDynamicObjectCollection("entryentity") != null) {
            this.getModel().deleteEntryData("nckd_entryentity");
            dynamicObject.getDynamicObjectCollection("entryentity").stream().forEach(t -> {
                int row = this.getModel().createNewEntryRow("nckd_entryentity");
                this.getModel().setValue("nckd_inspecttype", t.get("inspecttype"), row);
                this.getModel().setValue("nckd_nocheckflag", t.get("nocheckflag"), row);
            });
        }
    }
}
