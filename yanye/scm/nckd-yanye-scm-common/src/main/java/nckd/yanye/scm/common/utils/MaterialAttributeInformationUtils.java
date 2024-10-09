package nckd.yanye.scm.common.utils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.druid.support.logging.Log;
import com.alibaba.druid.support.logging.LogFactory;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.exception.KDBizException;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.basedata.BaseDataServiceHelper;
import kd.bos.servicehelper.operation.OperationServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

/**
 * @author husheng
 * @date 2024-08-22 14:00
 * @description 物料属性信息
 */
public class MaterialAttributeInformationUtils {
    private static final Log logger = LogFactory.getLog(MaterialAttributeInformationUtils.class);

    /**
     * 【物料类型】为‘物资’+【物料属性】为‘自制’+【自制物料类型】“产成品”时
     * 集合中的参数代表单据类型
     */
    public static List<String> finishedGoodsList = Arrays.asList("1", "2", "3", "4");
    /**
     * 【物料类型】为‘物资’+【物料属性】为‘自制’+【自制物料类型】“半成品”时
     */
    public static List<String> semiFinishedList = Arrays.asList("1", "2", "3");
    /**
     * 【物料类型】为‘物资’或‘资产’+【物料属性】为‘外购’时
     */
    public static List<String> outsourcingList = Arrays.asList("1", "2", "3", "5");
    /**
     * 【物料类型】为‘费用’+【物料属性】为‘外购’时
     */
    public static List<String> feeOutsourcingList = Arrays.asList("3", "5");

    public static List<String> list = Arrays.asList("1", "2", "3", "4", "5");

    /**
     * 生成单位信息
     * @param material
     * @param dynamicObject
     */
    public static void saveBdMultimeasureunit(DynamicObject material, DynamicObject dynamicObject) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_multimeasureunit");
        // 物料
        newDynamicObject.set("materialid", material);
        // 计量单位
        newDynamicObject.set("measureunitid", dynamicObject.get("nckd_measureunitid"));
        // 换算分母
        newDynamicObject.set("denominator", dynamicObject.get("nckd_denominator"));
        // 换算分子
        newDynamicObject.set("numerator", dynamicObject.get("nckd_numerator"));
        // 换算类型
        newDynamicObject.set("converttype", dynamicObject.get("nckd_converttype"));
        // 单位精度
        newDynamicObject.set("precision", dynamicObject.get("nckd_precision"));

        SaveServiceHelper.save(new DynamicObject[]{newDynamicObject});
    }

    /**
     * 核算信息设置存货类别并提交审核
     *
     * @param material
     * @param org
     */
    public static void setCheckInfoMaterialcategory(DynamicObject material, DynamicObject org) {
        Long materialcategory = null;

        // 根据物料分类与存货类别关系配置获取存货类别
        Long groupId = material.getDynamicObject("group").getLong("id");
        QFilter qFilter = new QFilter("nckd_entryentity.nckd_materialclassify", QCP.equals, groupId)
                .and("nckd_entryentity.nckd_org", QCP.equals, org.getLong("id"));
        DynamicObject[] objects = BusinessDataServiceHelper.load("nckd_materialcategorymap", "nckd_entryentity.nckd_materialclassify,nckd_entryentity.nckd_materialcategory,nckd_entryentity.nckd_org", qFilter.toArray());
        if (objects.length > 0) {
            List<DynamicObject> collect = objects[0].getDynamicObjectCollection("nckd_entryentity").stream()
                    .filter(t -> t.getDynamicObject("nckd_materialclassify").getLong("id") == groupId && t.getDynamicObject("nckd_org") != null && t.getDynamicObject("nckd_org").getLong("id") == org.getLong("id"))
                    .collect(Collectors.toList());
            materialcategory = collect.get(0).getDynamicObject("nckd_materialcategory").getLong("id");
        } else {
            QFilter filter = new QFilter("nckd_entryentity.nckd_materialclassify", QCP.equals, groupId)
                    .and("nckd_entryentity.nckd_org", QCP.equals, 0);
            DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("nckd_materialcategorymap", "nckd_entryentity.nckd_materialclassify,nckd_entryentity.nckd_materialcategory", filter.toArray());
            if (dynamicObjects.length > 0) {
                List<DynamicObject> collect = dynamicObjects[0].getDynamicObjectCollection("nckd_entryentity").stream()
                        .filter(t -> t.getDynamicObject("nckd_materialclassify").getLong("id") == groupId)
                        .collect(Collectors.toList());
                materialcategory = collect.get(0).getDynamicObject("nckd_materialcategory").getLong("id");
            }
        }

        // 设置存货类别并提交审核
        if (materialcategory != null) {
            QFilter qFilter1 = new QFilter("nckd_materialnumber", QCP.equals, material.getLong("id"))
                    .and("org", QCP.equals, org.getLong("id"))
                    .and("nckd_materialmaintunit", QCP.equals, "add")
                    .and("nckd_documenttype", QCP.equals, "3");
            DynamicObject dynamicObject = BusinessDataServiceHelper.loadSingle("nckd_materialmaintenan", qFilter1.toArray());
            if (dynamicObject != null) {
                DynamicObject object = BusinessDataServiceHelper.loadSingle(materialcategory, "bd_materialcategory");
                dynamicObject.set("nckd_group", object);
                MaterialAttributeInformationUtils.processData(dynamicObject);
            }
        }
    }

    /**
     * 生产基本信息
     *
     * @param org      申请组织
     * @param material 物料
     */
    public static void defaultProductionInfo(DynamicObject org, DynamicObject material) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_materialmftinfo");

        // 物料
        newDynamicObject.set("masterid", material);
        // 编码
        newDynamicObject.set("number", material.getString("number"));
        // 生产信息创建组织
        newDynamicObject.set("createorg", org);
        // 生产计量单位
        newDynamicObject.set("mftunit", material.getDynamicObject("baseunit"));
        // 数据状态
        newDynamicObject.set("status", "A");
        // 生产信息控制策略
        newDynamicObject.set("ctrlstrategy", getCtrlStrgy(org));
        // 使用状态
        newDynamicObject.set("enable", "1");
        // 物料属性
        newDynamicObject.set("materialattr", "10030");
        // BOM版本规则
        newDynamicObject.set("bomversionrule", getDefaultBOMRuleVer());
        // 可主产品
        newDynamicObject.set("ismainproduct", 1);
        // 供货库存组织
        newDynamicObject.set("supplyorgunitid", org);
        // 入库上限允差（%）
        newDynamicObject.set("rcvinhighlimit", new BigDecimal(0));
        // 入库下限允差（%）
        newDynamicObject.set("rcvinlowlimit", new BigDecimal(0));
        // 汇报上限允差（%）
        newDynamicObject.set("rpthighlimit", new BigDecimal(0));
        // 汇报下限允差（%）
        newDynamicObject.set("rptlowlimit", new BigDecimal(0));
        // 领送料方式
        newDynamicObject.set("issuemode", "11010");
        // 倒冲
        newDynamicObject.set("isbackflush", "A");
        // 领料上限允差（%）
        newDynamicObject.set("issinhighlimit", new BigDecimal(0));
        // 领料下限允差（%）
        newDynamicObject.set("issinlowlimit", new BigDecimal(0));
        // 组件发料信息来源
        newDynamicObject.set("invinfosrc", "D");
        // 最小发料批量
        newDynamicObject.set("minbatchnum", new BigDecimal(1));
        // 最小发料批量单位
        newDynamicObject.set("minbatchunit", material.getDynamicObject("baseunit"));
        // 创建人
        newDynamicObject.set("creator", RequestContext.get().getCurrUserId());

        // 数据处理
        processData(newDynamicObject);
    }

    /**
     * 计划基本信息
     *
     * @param org      申请组织
     * @param material 物料
     */
    public static void defaultPlanInfo(DynamicObject org, DynamicObject material) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("mpdm_materialplan");

        // 物料
        newDynamicObject.set("masterid", material);
        // 物料
        newDynamicObject.set("material", material);
        // 编码
        newDynamicObject.set("number", material.getString("number"));
        // 计划信息创建组织
        newDynamicObject.set("createorg", org);
        // 物料属性
        newDynamicObject.set("materialattr", "10030");
        // 数据状态
        newDynamicObject.set("status", "A");
        // 控制策略
        newDynamicObject.set("ctrlstrategy", getCtrlStrgy(org));
        // 使用状态
        newDynamicObject.set("enable", "1");
        // 计划方式
        newDynamicObject.set("planmode", "D");
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

        // 数据处理
        processData(newDynamicObject);
    }

    /**
     * 库存基本信息
     *
     * @param org      申请组织
     * @param material 物料
     */
    public static void defaultStockInfo(DynamicObject org, DynamicObject material) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_materialinventoryinfo");

        // 物料
        newDynamicObject.set("masterid", material);
        // 物料
        newDynamicObject.set("material", material);
        // 库存信息创建组织
        newDynamicObject.set("createorg", org);
        // 库存单位
        newDynamicObject.set("inventoryunit", material.getDynamicObject("baseunit"));
        // 库存信息数据状态
        newDynamicObject.set("status", "A");
        // 库存信息控制策略
        newDynamicObject.set("ctrlstrategy", getCtrlStrgy(org));
        // 库存信息使用状态
        newDynamicObject.set("enable", "1");
        // 基本单位
        newDynamicObject.set("baseunit", material.getDynamicObject("baseunit"));
        // 允许预留
        newDynamicObject.set("isreserve", 1);
        // 预留期限
        newDynamicObject.set("reservationperiod", 0);
        // 序列号生成时点
        newDynamicObject.set("sngentimepoint", "4");
        // 保质期
        newDynamicObject.set("shelflife", 0);
        // 入库失效提前期
        newDynamicObject.set("dateofoverdueforin", 0);
        // 出库失效提前期
        newDynamicObject.set("dateofoverdueforout", 0);
        // 预警提前期
        newDynamicObject.set("warnleadtime", 0);
        // 创建人
        newDynamicObject.set("creator", RequestContext.get().getCurrUserId());

        // 数据处理
        processData(newDynamicObject);
    }

    /**
     * 销售基本信息
     *
     * @param org      申请组织
     * @param material 物料
     */
    public static void defaultMarketInfo(DynamicObject org, DynamicObject material) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_materialsalinfo");

        // 物料
        newDynamicObject.set("masterid", material);
        // 物料
        newDynamicObject.set("material", material);
        // 销售信息创建组织
        newDynamicObject.set("createorg", org);
        // 销售单位
        newDynamicObject.set("salesunit", material.getDynamicObject("baseunit"));
        // 销售信息数据状态
        newDynamicObject.set("status", "A");
        // 销售信息控制策略
        newDynamicObject.set("ctrlstrategy", getCtrlStrgy(org));
        // 销售信息使用状态
        newDynamicObject.set("enable", "1");
        // 创建人
        newDynamicObject.set("creator", RequestContext.get().getCurrUserId());

        // 数据处理
        processData(newDynamicObject);
    }

    /**
     * 采购基本信息
     *
     * @param org      申请组织
     * @param material 物料
     */
    public static void defaultPurchaseInfo(DynamicObject org, DynamicObject material) {
        DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("bd_materialpurchaseinfo");

        // 物料
        newDynamicObject.set("masterid", material);
        // 物料
        newDynamicObject.set("material", material);
        // 采购信息创建组织
        newDynamicObject.set("createorg", org);
        // 采购单位
        newDynamicObject.set("purchaseunit", material.getDynamicObject("baseunit"));
        // 采购信息数据状态
        newDynamicObject.set("status", "A");
        // 采购信息控制策略
        newDynamicObject.set("ctrlstrategy", getCtrlStrgy(org));
        // 采购信息使用状态
        newDynamicObject.set("enable", "1");
        // 创建人
        newDynamicObject.set("creator", RequestContext.get().getCurrUserId());

        // 数据处理
        processData(newDynamicObject);
    }

    // 获取控制策略
    public static String getCtrlStrgy(DynamicObject org) {
        String ctrlStrgy = BaseDataServiceHelper.getBdCtrlStrgy("bd_materialmftinfo", String.valueOf(org.getPkValue()));
        if (ctrlStrgy != null && ctrlStrgy.length() > 0) {
            String[] ctrlStrgys = ctrlStrgy.split(",");
            if (ctrlStrgys.length > 1) {
                String[] var3 = ctrlStrgys;
                int var4 = ctrlStrgys.length;

                for (int var5 = 0; var5 < var4; ++var5) {
                    String ctr = var3[var5];
                    if (StringUtils.isNotEmpty(ctr)) {
                        return ctr;
                    }
                }
            }
        }

        return ctrlStrgy;
    }

    /**
     * 数据处理
     *
     * @param dynamicObject 单据数据
     */
    public static void processData(DynamicObject dynamicObject) {
        // 单据标识
        String entityNumber = dynamicObject.getDataEntityType().getName();

        // 保存
        OperationResult saveOperate = SaveServiceHelper.saveOperate(entityNumber, new DynamicObject[]{dynamicObject}, OperateOption.create());
        if (saveOperate.isSuccess()) {
            // 提交
            OperationResult submitOperate = OperationServiceHelper.executeOperate("submit", entityNumber, new DynamicObject[]{dynamicObject}, OperateOption.create());
            if (submitOperate.isSuccess()) {
                // 审核
                OperationResult auditOperate = OperationServiceHelper.executeOperate("audit", entityNumber, new DynamicObject[]{dynamicObject}, OperateOption.create());
                if (!auditOperate.isSuccess()) {
                    logger.error(auditOperate.getMessage() + auditOperate.getAllErrorOrValidateInfo());
                    throw new KDBizException(auditOperate.getMessage() + auditOperate.getAllErrorOrValidateInfo());
                }
            } else {
                logger.error(submitOperate.getMessage() + submitOperate.getAllErrorOrValidateInfo());
                throw new KDBizException(submitOperate.getMessage() + submitOperate.getAllErrorOrValidateInfo());
            }
        } else {
            logger.error(saveOperate.getMessage() + saveOperate.getAllErrorOrValidateInfo());
            throw new KDBizException(saveOperate.getMessage() + saveOperate.getAllErrorOrValidateInfo());
        }
    }

    /**
     * 获取BOM版本规则
     */
    public static DynamicObject getDefaultBOMRuleVer() {
        QFilter qFilter = new QFilter("isdefault", "=", Boolean.TRUE);
        qFilter.and(new QFilter("enable", "=", Boolean.TRUE));
        DynamicObject rule = BusinessDataServiceHelper.loadSingleFromCache("bd_bomversionrule_new", "id", qFilter.toArray());
        return rule;
    }

    /**
     * 数据处理(反审核)
     *
     * @param dynamicObject 单据数据
     */
    public static void reverseprocessData(DynamicObject dynamicObject) {
        // 单据标识
        String entityNumber = dynamicObject.getDataEntityType().getName();
        //反审核
        OperationServiceHelper.executeOperate("unaudit", entityNumber, new DynamicObject[]{dynamicObject}, OperateOption.create());
    }
}
