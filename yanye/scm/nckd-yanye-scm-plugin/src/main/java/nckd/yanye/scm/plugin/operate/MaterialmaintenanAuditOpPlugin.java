package nckd.yanye.scm.plugin.operate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import kd.bos.coderule.api.CodeRuleInfo;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.coderule.CodeRuleServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.scm.common.utils.MaterialAttributeInformationUtils;

/**
 * @author husheng
 * @date 2024-08-19 15:13
 * @description 物料维护单-审核
 * nckd_materialmaintenan
 */
public class MaterialmaintenanAuditOpPlugin extends AbstractOperationServicePlugIn {
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);

        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.addAll(this.billEntityType.getAllFields().keySet());
    }

    @Override
    public void afterExecuteOperationTransaction(AfterOperationArgs e) {
        super.afterExecuteOperationTransaction(e);

        Arrays.stream(e.getDataEntities()).forEach(data -> {
            /**
             * 物料属性
             * 自制	1
             * 外购	2
             */
            String materialattribute = data.getString("nckd_materialattribute");
            /**
             * 自制物料类型
             * 产成品	1
             * 半成品	2
             */
            String selfmaterialtype = data.getString("nckd_selfmaterialtype");
            // 申请组织
            DynamicObject org = data.getDynamicObject("org");
            /**
             * 单据类型：
             * 生产类型 1
             * 仓库类型 2
             * 财务类型 3
             * 销售类型 4
             * 采购类型 5
             */
            String documenttype = data.getString("nckd_documenttype");
            //单据维护类型 add：修改物料基本信息
            if ("updateinfo".equals(data.getString("nckd_materialmaintunit"))) {
                baseInfo(data);
            }
            switch (documenttype) {
                case "1":
                    // 生产基本信息
                    this.productionInfo(data);
                    // 计划基本信息
                    this.planInfo(data);

                    // 【物料属性】为‘自制’+【自制物料类型】‘产成品’+【申请组织】‘江西盐业包装有限公司’
                    if ("1".equals(materialattribute) && "1".equals(selfmaterialtype) && "113".equals(org.getString("number"))) {
                        // 质检基本信息
                        this.inspectInfo(data);
                    }

                    break;
                case "2":
                    // 库存基本信息
                    this.stockInfo(data);
                    break;
                case "3":
                    // 核算基本信息
                    this.checkInfo(data);
                    break;
                case "4":
                    // 销售基本信息
                    this.marketInfo(data);
                    break;
                case "5":
                    // 采购基本信息
                    this.purchaseInfo(data);
                    // 物料采购员信息
                    this.buyerInfo(data);
                    // 质检基本信息
                    this.inspectInfo(data);
                    break;
            }
        });
    }

    /**
     * 生产基本信息
     */
    private void productionInfo(DynamicObject dynamicObject) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_materialmftinfo");
        //单据维护类型：update:修改物料属性
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            QFilter qFilter = new QFilter("createorg", QCP.equals, dynamicObject.getDynamicObject("org").getPkValue())
                    .and("masterid", QCP.equals, dynamicObject.getDynamicObject("nckd_materialnumber").getPkValue());
            newDynamicObject = BusinessDataServiceHelper.loadSingle("bd_materialmftinfo", new QFilter[]{qFilter});
            if (ObjectUtil.isNull(newDynamicObject)) {
                return;
            }
        } else if ("add".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            DynamicObject material = this.getMaterial(dynamicObject);

            // 物料
            newDynamicObject.set("masterid", material);
            // 编码
            newDynamicObject.set("number", dynamicObject.getDynamicObject("nckd_materialnumber").getString("number"));
            // 生产信息创建组织
            newDynamicObject.set("createorg", dynamicObject.get("org"));
            // 数据状态
            newDynamicObject.set("status", "A");
            // 生产信息控制策略
            newDynamicObject.set("ctrlstrategy", MaterialAttributeInformationUtils.getCtrlStrgy(dynamicObject.getDynamicObject("org")));
            // 使用状态
            newDynamicObject.set("enable", "1");
            // 可主产品
            newDynamicObject.set("ismainproduct", 1);
            // 入库上限允差（%）
            newDynamicObject.set("rcvinhighlimit", new BigDecimal(0));
            // 入库下限允差（%）
            newDynamicObject.set("rcvinlowlimit", new BigDecimal(0));
            // 汇报上限允差（%）
            newDynamicObject.set("rpthighlimit", new BigDecimal(0));
            // 汇报下限允差（%）
            newDynamicObject.set("rptlowlimit", new BigDecimal(0));

            // 领料上限允差（%）
            newDynamicObject.set("issinhighlimit", new BigDecimal(0));
            // 领料下限允差（%）
            newDynamicObject.set("issinlowlimit", new BigDecimal(0));
            // 组件发料信息来源
            newDynamicObject.set("invinfosrc", "D");
            // 最小发料批量
            newDynamicObject.set("minbatchnum", new BigDecimal(1));
            // 最小发料批量单位
            newDynamicObject.set("minbatchunit", dynamicObject.get("nckd_mftunit"));
            // 创建人
            newDynamicObject.set("creator", RequestContext.get().getCurrUserId());
        }
        commonnproductionInfo(newDynamicObject, dynamicObject);
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            // 数据处理
            MaterialAttributeInformationUtils.reverseprocessData(newDynamicObject);
        }
        // 数据处理
        MaterialAttributeInformationUtils.processData(newDynamicObject);
    }

    private void commonnproductionInfo(DynamicObject newDynamicObject, DynamicObject dynamicObject) {
        // 生产计量单位
        newDynamicObject.set("mftunit", dynamicObject.get("nckd_mftunit"));
        // 物料属性
        newDynamicObject.set("materialattr", dynamicObject.get("nckd_materialattri"));
        // 生产部门
        newDynamicObject.set("departmentorgid", dynamicObject.get("nckd_departmentorgid"));
        // BOM版本规则
        newDynamicObject.set("bomversionrule", dynamicObject.get("nckd_bomversionrule"));
        // 可联副产品
        newDynamicObject.set("isjointproduct", dynamicObject.get("nckd_isjointproduct"));
        // 供货库存组织
        newDynamicObject.set("supplyorgunitid", dynamicObject.get("nckd_supplyorgunitid"));
        // 领送料方式
        newDynamicObject.set("issuemode", dynamicObject.get("nckd_issuemode"));
        // 倒冲
        newDynamicObject.set("isbackflush", dynamicObject.get("nckd_isbackflush"));
    }

    /**
     * 计划基本信息
     */
    private void planInfo(DynamicObject dynamicObject) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("mpdm_materialplan");
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            QFilter qFilter = new QFilter("createorg", QCP.equals, dynamicObject.getDynamicObject("org").getPkValue())
                    .and("masterid", QCP.equals, dynamicObject.getDynamicObject("nckd_materialnumber").getPkValue());
            newDynamicObject = BusinessDataServiceHelper.loadSingle("mpdm_materialplan", new QFilter[]{qFilter});
            if (ObjectUtil.isNull(newDynamicObject)) {
                return;
            }
        } else if ("add".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            DynamicObject material = this.getMaterial(dynamicObject);

            // 物料
            newDynamicObject.set("masterid", material);
            // 物料
            newDynamicObject.set("material", material);
            // 编码
            newDynamicObject.set("number", dynamicObject.getDynamicObject("nckd_materialnumber").getString("number"));
            // 数据状态
            newDynamicObject.set("status", "A");
            // 控制策略
            newDynamicObject.set("ctrlstrategy", MaterialAttributeInformationUtils.getCtrlStrgy(dynamicObject.getDynamicObject("nckd_createorg")));
            // 使用状态
            newDynamicObject.set("enable", "1");
            // 预留类型
            newDynamicObject.set("reservedtype", "C");
            // 成品率
            newDynamicObject.set("yield", new BigDecimal(1));
            // 损耗率
            newDynamicObject.set("wastagerate", new BigDecimal(0));
            // 损耗计算公式
            newDynamicObject.set("wastagerateformula", "B");
            // 提前期类型
            newDynamicObject.set("leadtimetype", "A");
            // 固定提前期（天）
            newDynamicObject.set("fixedleadtime", 0);
            // 变动提前期（天）
            newDynamicObject.set("changeleadtime", 0);
            // 检验提前期（天）
            newDynamicObject.set("inspectionleadtime", 0);
            // 前处理时间（天）
            newDynamicObject.set("preprocessingtime", 0);
            // 后处理时间（天）
            newDynamicObject.set("postprocessingtime", 0);
            // 变动批量
            newDynamicObject.set("changebatch", 0);
            // 批量政策
            newDynamicObject.set("lotpolicy", "A");
            // 批量数量/取整倍数
            newDynamicObject.set("batchqty", new BigDecimal(0));
            // 批量增量
            newDynamicObject.set("batchincrement", new BigDecimal(0));
            // 最小批量
            newDynamicObject.set("minlotsize", new BigDecimal(0));
            // 最大批量
            newDynamicObject.set("maxlotsize", new BigDecimal(0));
            // 分割符号
            newDynamicObject.set("separatorsymbol", "A");
            // 分割间隔周期（天）
            newDynamicObject.set("intervalperiod", new BigDecimal(0));
            // 分割基数
            newDynamicObject.set("partitionbase", new BigDecimal(0));
            // 动态周期（天）
            newDynamicObject.set("dynamiccycle", new BigDecimal(0));
            // 固定周期（天）
            newDynamicObject.set("fixedperiod", new BigDecimal(0));
            // 创建人
            newDynamicObject.set("creator", RequestContext.get().getCurrUserId());
        }
        commonplanInfo(newDynamicObject, dynamicObject);
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            // 数据处理
            MaterialAttributeInformationUtils.reverseprocessData(newDynamicObject);
        }
        // 数据处理
        MaterialAttributeInformationUtils.processData(newDynamicObject);
    }

    private void commonplanInfo(DynamicObject newDynamicObject, DynamicObject dynamicObject) {
        // 计划信息创建组织
        newDynamicObject.set("createorg", dynamicObject.get("nckd_createorg"));
        // 物料属性
        newDynamicObject.set("materialattr", dynamicObject.get("nckd_materialattr"));
        // 计划方式
        newDynamicObject.set("planmode", dynamicObject.get("nckd_planmode"));
        // 允许提前期间（天）
        newDynamicObject.set("allowleadtime", dynamicObject.get("nckd_allowleadtime"));
        // 提前容差（天）
        newDynamicObject.set("leadadvance", dynamicObject.get("nckd_leadadvance"));
        // 允许延后期间（天）
        newDynamicObject.set("allowdelayperiod", dynamicObject.get("nckd_fallowdelayperiod"));
        // 延后容差（天）
        newDynamicObject.set("delaytolerance", dynamicObject.get("nckd_delaytolerance"));
    }

    /**
     * 库存基本信息
     */
    private void stockInfo(DynamicObject dynamicObject) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_materialinventoryinfo");
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            QFilter qFilter = new QFilter("createorg", QCP.equals, dynamicObject.getDynamicObject("org").getPkValue())
                    .and("masterid", QCP.equals, dynamicObject.getDynamicObject("nckd_materialnumber").getPkValue());
            newDynamicObject = BusinessDataServiceHelper.loadSingle("bd_materialinventoryinfo", new QFilter[]{qFilter});
            if (ObjectUtil.isNull(newDynamicObject)) {
                return;
            }
        } else if ("add".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            DynamicObject material = this.getMaterial(dynamicObject);

            // 物料
            newDynamicObject.set("masterid", material);
            // 物料
            newDynamicObject.set("material", material);
            // 库存信息创建组织
            newDynamicObject.set("createorg", dynamicObject.get("org"));
            // 库存单位
            newDynamicObject.set("inventoryunit", dynamicObject.get("nckd_inventoryunit"));
            // 库存信息数据状态
            newDynamicObject.set("status", "A");
            // 库存信息控制策略
            newDynamicObject.set("ctrlstrategy", MaterialAttributeInformationUtils.getCtrlStrgy(dynamicObject.getDynamicObject("org")));
            // 库存信息使用状态
            newDynamicObject.set("enable", "1");
            // 基本单位
            newDynamicObject.set("baseunit", dynamicObject.get("nckd_inventoryunit"));
            // 允许预留
            newDynamicObject.set("isreserve", 1);
            // 预留期限
            newDynamicObject.set("reservationperiod", 0);
            // 序列号生成时点
            newDynamicObject.set("sngentimepoint", "4");
            // 创建人
            newDynamicObject.set("creator", RequestContext.get().getCurrUserId());
        }
        commonstockInfo(newDynamicObject, dynamicObject);
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            // 数据处理
            MaterialAttributeInformationUtils.reverseprocessData(newDynamicObject);
        }
        // 数据处理
        MaterialAttributeInformationUtils.processData(newDynamicObject);
    }

    private void commonstockInfo(DynamicObject newDynamicObject,DynamicObject dynamicObject){
        // 库存单位
        newDynamicObject.set("inventoryunit", dynamicObject.get("nckd_inventoryunit"));
        // 最小包装量
        newDynamicObject.set("minpackqty", dynamicObject.get("nckd_minpackqty"));
        // 来料检验
        newDynamicObject.set("ispurchaseinspect", dynamicObject.get("nckd_ispurchaseinspect"));
        // 启用最小库存预警
        newDynamicObject.set("ismininvalert", dynamicObject.get("nckd_ismininvalert"));
        // 最小库存
        newDynamicObject.set("mininvqty", dynamicObject.get("nckd_mininvqty"));
        // 启用安全库存预警
        newDynamicObject.set("issaftyinvalert", dynamicObject.get("nckd_issaftyinvalert"));
        // 安全库存
        newDynamicObject.set("saftyinvqty", dynamicObject.get("nckd_saftyinvqty"));
        // 启用最大库存预警
        newDynamicObject.set("ismaxinvalert", dynamicObject.get("nckd_ismaxinvalert"));
        // 最大库存
        newDynamicObject.set("maxinvqty", dynamicObject.get("nckd_maxinvqty"));
        if (dynamicObject.get("nckd_lotcoderule") != null) {
            // 启用批号管理
            newDynamicObject.set("enablelot", 1);
            // 批号规则
            newDynamicObject.set("lotcoderule", dynamicObject.get("nckd_lotcoderule"));
        }
        if (dynamicObject.getBoolean("nckd_enableshelflifemgr")) {
            // 保质期管理
            newDynamicObject.set("enableshelflifemgr", dynamicObject.get("nckd_enableshelflifemgr"));
            // 保质期单位
            newDynamicObject.set("shelflifeunit", dynamicObject.get("nckd_shelflifeunit"));
            // 保质期
            newDynamicObject.set("shelflife", dynamicObject.get("nckd_shelflife"));
            // 计算方向
            newDynamicObject.set("caldirection", "3");
            // 到期日计算方式
            newDynamicObject.set("calculationforenddate", dynamicObject.get("nckd_calculationforenddat"));
            // 提前期单位
            newDynamicObject.set("leadtimeunit", dynamicObject.get("nckd_leadtimeunit"));
            // 入库失效提前期
            newDynamicObject.set("dateofoverdueforin", dynamicObject.get("nckd_dateofoverdueforin"));
            // 出库失效提前期
            newDynamicObject.set("dateofoverdueforout", dynamicObject.get("nckd_dateofoverdueforout"));
        }
        if (dynamicObject.getBoolean("nckd_enablewarnlead")) {
            // 启用预警
            newDynamicObject.set("enablewarnlead", dynamicObject.get("nckd_enablewarnlead"));
            // 预警提前期
            newDynamicObject.set("warnleadtime", dynamicObject.get("nckd_warnleadtime"));
        }
        // 制造策略
        newDynamicObject.set("manustrategy", dynamicObject.get("nckd_manustrategy"));
        // 出库规则
        newDynamicObject.set("outboundrule", dynamicObject.get("nckd_outboundrule"));
    }

    /**
     * 核算基本信息
     */
    private void checkInfo(DynamicObject dynamicObject) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_materialcalinfo");
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            QFilter qFilter = new QFilter("createorg", QCP.equals, dynamicObject.getDynamicObject("org").getPkValue())
                    .and("masterid", QCP.equals, dynamicObject.getDynamicObject("nckd_materialnumber").getPkValue());
            newDynamicObject = BusinessDataServiceHelper.loadSingle("bd_materialcalinfo", new QFilter[]{qFilter});
            if (ObjectUtil.isNull(newDynamicObject)) {
                return;
            }
        } else if ("add".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            DynamicObject material = this.getMaterial(dynamicObject);

            // 物料
            newDynamicObject.set("masterid", material);
            // 物料
            newDynamicObject.set("material", material);
            // 核算信息创建组织
            newDynamicObject.set("createorg", dynamicObject.get("org"));

            // 核算信息数据状态
            newDynamicObject.set("status", "A");
            // 核算信息控制策略
            newDynamicObject.set("ctrlstrategy", MaterialAttributeInformationUtils.getCtrlStrgy(dynamicObject.getDynamicObject("org")));
            // 核算信息使用状态
            newDynamicObject.set("enable", "1");
            // 创建人
            newDynamicObject.set("creator", RequestContext.get().getCurrUserId());
        }
        commoncheckInfo(newDynamicObject, dynamicObject);
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            // 数据处理
            MaterialAttributeInformationUtils.reverseprocessData(newDynamicObject);
        }
        // 数据处理
        MaterialAttributeInformationUtils.processData(newDynamicObject);
    }

    private void commoncheckInfo(DynamicObject newDynamicObject, DynamicObject dynamicObject) {
        // 存货类别
        newDynamicObject.set("group", dynamicObject.get("nckd_group"));
    }

    /**
     * 销售基本信息
     */
    private void marketInfo(DynamicObject dynamicObject) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_materialsalinfo");
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            QFilter qFilter = new QFilter("createorg", QCP.equals, dynamicObject.getDynamicObject("org").getPkValue())
                    .and("masterid", QCP.equals, dynamicObject.getDynamicObject("nckd_materialnumber").getPkValue());
            newDynamicObject = BusinessDataServiceHelper.loadSingle("bd_materialsalinfo", new QFilter[]{qFilter});
            if (ObjectUtil.isNull(newDynamicObject)) {
                return;
            }
        } else if ("add".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            DynamicObject material = this.getMaterial(dynamicObject);

            // 物料
            newDynamicObject.set("masterid", material);
            // 物料
            newDynamicObject.set("material", material);
            // 销售信息创建组织
            newDynamicObject.set("createorg", dynamicObject.get("org"));
            // 销售信息数据状态
            newDynamicObject.set("status", "A");
            // 销售信息控制策略
            newDynamicObject.set("ctrlstrategy", MaterialAttributeInformationUtils.getCtrlStrgy(dynamicObject.getDynamicObject("org")));
            // 销售信息使用状态
            newDynamicObject.set("enable", "1");
            // 创建人
            newDynamicObject.set("creator", RequestContext.get().getCurrUserId());
        }
        commonmarketInfo(newDynamicObject, dynamicObject);
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            // 数据处理
            MaterialAttributeInformationUtils.reverseprocessData(newDynamicObject);
        }
        // 数据处理
        MaterialAttributeInformationUtils.processData(newDynamicObject);
    }

    private void commonmarketInfo(DynamicObject newDynamicObject, DynamicObject dynamicObject) {
        // 销售单位
        newDynamicObject.set("salesunit", dynamicObject.get("nckd_salesunit"));
        // 控制发货数量
        newDynamicObject.set("iscontrolqty", dynamicObject.get("nckd_iscontrolsendqty"));
        // 发货超发比率(%)
        newDynamicObject.set("dlivrateceiling", dynamicObject.get("nckd_dlivrateceiling"));
        // 发货欠发比率(%)
        newDynamicObject.set("dlivratefloor", dynamicObject.get("nckd_dlivratefloor"));
    }

    /**
     * 采购基本信息
     */
    private void purchaseInfo(DynamicObject dynamicObject) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_materialpurchaseinfo");
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            QFilter qFilter = new QFilter("createorg", QCP.equals, dynamicObject.getDynamicObject("org").getPkValue())
                    .and("masterid", QCP.equals, dynamicObject.getDynamicObject("nckd_materialnumber").getPkValue());
            newDynamicObject = BusinessDataServiceHelper.loadSingle("bd_materialpurchaseinfo", new QFilter[]{qFilter});
            if (ObjectUtil.isNull(newDynamicObject)) {
                return;
            }
        } else if ("add".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            DynamicObject material = this.getMaterial(dynamicObject);

            // 物料
            newDynamicObject.set("masterid", material);
            // 物料
            newDynamicObject.set("material", material);
            // 采购信息创建组织
            newDynamicObject.set("createorg", dynamicObject.get("org"));
            // 采购信息数据状态
            newDynamicObject.set("status", "A");
            // 采购信息控制策略
            newDynamicObject.set("ctrlstrategy", MaterialAttributeInformationUtils.getCtrlStrgy(dynamicObject.getDynamicObject("org")));
            // 采购信息使用状态
            newDynamicObject.set("enable", "1");
            // 创建人
            newDynamicObject.set("creator", RequestContext.get().getCurrUserId());
        }
        commonpurchaseInfo(newDynamicObject, dynamicObject);
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            // 数据处理
            MaterialAttributeInformationUtils.reverseprocessData(newDynamicObject);
        }
        // 数据处理
        MaterialAttributeInformationUtils.processData(newDynamicObject);
    }

    private void commonpurchaseInfo(DynamicObject newDynamicObject, DynamicObject dynamicObject) {
        // 采购单位
        newDynamicObject.set("purchaseunit", dynamicObject.get("nckd_purchaseunit"));
        // 控制收货数量
        newDynamicObject.set("iscontrolqty", dynamicObject.get("nckd_iscontrolqty"));
        // 收货超收比率(%)
        newDynamicObject.set("receiverateup", dynamicObject.get("nckd_receiverateup"));
        // 收货欠收比率(%)
        newDynamicObject.set("receiveratedown", dynamicObject.get("nckd_receiveratedown"));
    }

    /**
     * 物料采购员信息
     */
    private void buyerInfo(DynamicObject dynamicObject) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("msbd_puropermaterctrl");
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            QFilter qFilter = new QFilter("createorg", QCP.equals, dynamicObject.getDynamicObject("org").getPkValue())
                    .and("masterid", QCP.equals, dynamicObject.getDynamicObject("nckd_materialnumber").getPkValue());
            newDynamicObject = BusinessDataServiceHelper.loadSingle("msbd_puropermaterctrl", new QFilter[]{qFilter});
            if (ObjectUtil.isNull(newDynamicObject)) {
                return;
            }
        } else if ("add".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            CodeRuleInfo codeRule = CodeRuleServiceHelper.getCodeRule("msbd_puropermaterctrl", newDynamicObject, null);
            String number = CodeRuleServiceHelper.readNumber(codeRule, newDynamicObject);
            // 编码
            newDynamicObject.set("number", number);
            // 名称
            newDynamicObject.set("name", number);
            // 控制维度
            newDynamicObject.set("controldimension", "OPER_MATER");
            // 控制类型
            newDynamicObject.set("controltype", "ALLOW");
            // 数据状态
            newDynamicObject.set("status", "A");
            // 使用状态
            newDynamicObject.set("enable", "1");
            // 数据类型
            newDynamicObject.set("datatype", "msbd_puropermaterctrl");
            // 创建人
            newDynamicObject.set("creator", RequestContext.get().getCurrUserId());
        }
        commonbuyerInfo(newDynamicObject, dynamicObject);
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            // 数据处理
            MaterialAttributeInformationUtils.reverseprocessData(newDynamicObject);
        }
        // 数据处理
        MaterialAttributeInformationUtils.processData(newDynamicObject);
    }

    private void commonbuyerInfo(DynamicObject newDynamicObject, DynamicObject dynamicObject) {
        // 采购组织
        newDynamicObject.set("org", dynamicObject.get("org"));
        // 采购员
        DynamicObject operator = BusinessDataServiceHelper.loadSingle("bd_operator", new QFilter[]{new QFilter("operatornumber", QCP.equals, dynamicObject.getDynamicObject("nckd_buyer").getString("number"))});
        // 可采明细
        DynamicObjectCollection entryentity = newDynamicObject.getDynamicObjectCollection("entryentity");
        DynamicObject addNew = entryentity.addNew();
        // 采购员编码
        addNew.set("operator", operator);
        // 采购组编码
        addNew.set("operatorgroup", dynamicObject.get("nckd_purchaseorg"));
        // 获取物料
        DynamicObject material = dynamicObject.getDynamicObject("nckd_materialnumber");
        // 物料采购信息
        DynamicObject single = BusinessDataServiceHelper.loadSingle("bd_materialpurchaseinfo", new QFilter[]{new QFilter("masterid", QCP.equals, material.getPkValue())});
        // 主物料
        addNew.set("materialmasterid", material);
        // 物料编码
        addNew.set("material", single);
    }


    /**
     * 质检基本信息
     */
    private void inspectInfo(DynamicObject dynamicObject) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_inspect_cfg");
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            QFilter qFilter = new QFilter("createorg", QCP.equals, dynamicObject.getDynamicObject("org").getPkValue())
                    .and("masterid", QCP.equals, dynamicObject.getDynamicObject("nckd_materialnumber").getPkValue());
            newDynamicObject = BusinessDataServiceHelper.loadSingle("bd_inspect_cfg", new QFilter[]{qFilter});
            if (ObjectUtil.isNull(newDynamicObject)) {
                return;
            }
        } else if ("add".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            DynamicObject material = this.getMaterial(dynamicObject);

            // 物料
            newDynamicObject.set("masterid", material);
            // 物料
            newDynamicObject.set("material", material);
            // 质检信息创建组织
            newDynamicObject.set("createorg", dynamicObject.get("org"));
            // 质检信息数据状态
            newDynamicObject.set("status", "A");
            // 质检信息控制策略
            newDynamicObject.set("ctrlstrategy", MaterialAttributeInformationUtils.getCtrlStrgy(dynamicObject.getDynamicObject("org")));
            // 质检信息使用状态
            newDynamicObject.set("enable", "1");
            // 创建人
            newDynamicObject.set("creator", RequestContext.get().getCurrUserId());
        }
        commoninspectInfo(newDynamicObject, dynamicObject);
        if ("update".equals(dynamicObject.getString("nckd_materialmaintunit"))) {
            // 数据处理
            MaterialAttributeInformationUtils.reverseprocessData(newDynamicObject);
        }
        // 数据处理
        MaterialAttributeInformationUtils.processData(newDynamicObject);
    }

    private void commoninspectInfo(DynamicObject newDynamicObject, DynamicObject dynamicObject) {
        // 检验控制
        DynamicObjectCollection entryentity = newDynamicObject.getDynamicObjectCollection("entryentity");

        dynamicObject.getDynamicObjectCollection("nckd_entryentity").stream().forEach(dynamicObject1 -> {
            DynamicObject addNew = entryentity.addNew();
            // 业务类型
            addNew.set("inspecttype", dynamicObject1.get("nckd_inspecttype"));
            // 免检设置
            addNew.set("nocheckflag", dynamicObject1.get("nckd_nocheckflag"));
        });
    }

    private void baseInfo(DynamicObject dynamicObject) {
        //查询物料
        DynamicObject dynamic = BusinessDataServiceHelper.loadSingle(dynamicObject.getDynamicObject("nckd_materialnumber").getPkValue(), "bd_material");
        dynamic.set("name", dynamicObject.get("nckd_altermaterialname"));//物料名称
        dynamic.set("modelnum", dynamicObject.get("nckd_alterspecificat"));//规格
        dynamic.set("modelnum", dynamicObject.get("nckd_altermodel"));//型号
        dynamic.set("modifier", RequestContext.get().getCurrUserId());//修改人
        dynamic.set("modifytime", new Date());//修改时间
        dynamic.set("group", dynamicObject.get("nckd_altermaterialclass"));//物料分组
        dynamic.set("helpcode", dynamicObject.get("nckd_altermnemoniccode"));//助记码
        dynamic.set("oldnumber", dynamicObject.get("nckd_alteroldnumber"));//旧物料编码
        dynamic.set("description", dynamicObject.get("nckd_alterremark"));//描述
        dynamic.set("hazardous", dynamicObject.get("nckd_altermaterialrisk"));//物料危险性
        dynamic.set("enableoutsource", dynamicObject.get("nckd_alteroutsourcing"));//可委外
        SaveServiceHelper.update(dynamic);
    }

    // 获取物料
    private DynamicObject getMaterial(DynamicObject dynamicObject) {
        String nckdMaterialnumber = dynamicObject.getDynamicObject("nckd_materialnumber").getString("number");
        DynamicObject bd_material = BusinessDataServiceHelper.loadSingle("bd_material", new QFilter[]{new QFilter("number", QCP.equals, nckdMaterialnumber)});
        return bd_material;
    }

}
