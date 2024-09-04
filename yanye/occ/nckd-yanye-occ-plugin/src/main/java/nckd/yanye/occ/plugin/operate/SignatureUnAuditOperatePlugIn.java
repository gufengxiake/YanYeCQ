package nckd.yanye.occ.plugin.operate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.formula.RowDataModel;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

public class SignatureUnAuditOperatePlugIn extends AbstractOperationServicePlugIn {

    /**
     * 操作执行前，准备加载单据数据之前，触发此事件
     * @remark
     * 插件可以在此事件中，指定需要加载的字段
     */
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        // 要求加载预计送货日期、最迟送货日期字段
        e.getFieldKeys().add(DelivaryDateValidator.KEY_DELIVERYDATE);
        e.getFieldKeys().add(DelivaryDateValidator.KEY_SRCBILLENTITY);
    }

    /**
     * 执行操作校验前，触发此事件
     * @remark
     * 插件可以在此事件，调整预置的操作校验器；或者增加自定义操作校验器
     */
    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        // 添加自定义的校验器
        e.addValidator(new DelivaryDateValidator());
    }
}

/**
 * 自定义操作校验器
 *
 * @author rd_JohnnyDing
 */
class DelivaryDateValidator extends AbstractValidator {

    /** 来源分类Id */
    public final static String KEY_DELIVERYDATE = "nckd_sourceentryid";
    public final static String KEY_SRCBILLENTITY = "nckd_srcbillentity";

    /**
     * 返回校验器的主实体：系统将自动对此实体数据，逐行进行校验
     */
    @Override
    public String getEntityKey() {
        return this.entityKey;
    }

    /**
     * 给校验器传入上下文环境及单据数据包之后，调用此方法；
     * @remark
     * 自定义校验器，可以在此事件进行本地变量初始化：如确认需要校验的主实体
     */
    @Override
    public void initializeConfiguration() {
        super.initializeConfiguration();
        // 在此方法中，确认校验器检验的主实体：送货子单据体
        // 需要对送货子单据体行，逐行判断预计送货日期
        this.entityKey = "entryentity";
    }

    /**
     * 校验器初始化完毕，从单据数据包中，提取出了主实体数据行，开始校验前，调用此方法；
     * @remark
     * 此方法，比initializeConfiguration更晚调用；
     * 在此方法调用this.getDataEntities()，可以获取到需校验的主实体数据行
     * 不能在本方法中，确认需要校验的主实体
     */
    @Override
    public void initialize() {
        super.initialize();
    }


    /**
     * 执行自定义校验
     */
    @Override
    public void validate() {

        // 定义一个行数据存取模型：用于方便的读取本实体、及父实体、单据头上的字段
        RowDataModel rowDataModel = new RowDataModel(this.entityKey, this.getValidateContext().getSubEntityType());
        //SimpleDateFormat timesdf = new SimpleDateFormat("yyyy-MM-dd");
        HashSet<Object>sourentryIdList=new HashSet<>();
        // 逐行校验预计送货
        for(ExtendedDataEntity rowDataEntity : this.getDataEntities()){
            rowDataModel.setRowContext(rowDataEntity.getDataEntity());

            String srcbillentity=rowDataModel.getValue(KEY_SRCBILLENTITY).toString();
            //判断源单分类Id是否已下推销售出库单
            if("im_saloutbill".equalsIgnoreCase(srcbillentity)){
                Object sourceentryid = rowDataModel.getValue(KEY_DELIVERYDATE);
                sourentryIdList.add(sourceentryid);
            }
        }
        if(!sourentryIdList.isEmpty()){
            // 构造QFilter
            QFilter depqFilter = new QFilter("billentry.srcbillentryid", QCP.in, sourentryIdList)
                    .and("billentry.srcbillentity",QCP.equals,"im_saloutbill");

            //销售出库单是否存在
            DynamicObjectCollection depcollections = QueryServiceHelper.query("im_saloutbill",
                    "id", depqFilter.toArray(), "");
            if(!depcollections.isEmpty()){
                // 校验不通过，输出一条错误提示
                this.addErrorMessage(this.getDataEntities()[0], "上游销售出库单存在下游销售出库单据，不允许反审核。");
            }
        }

    }
}
