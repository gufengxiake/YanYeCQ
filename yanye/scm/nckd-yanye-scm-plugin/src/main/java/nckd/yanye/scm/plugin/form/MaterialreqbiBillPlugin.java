package nckd.yanye.scm.plugin.form;

import java.text.SimpleDateFormat;
import java.util.Date;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.formula.RowDataModel;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.occ.ocbase.common.util.DynamicObjectUtils;

public class MaterialreqbiBillPlugin  extends AbstractOperationServicePlugIn {

    /**
     * 领料申请单 领料申请单，如物料启用辅助属性，则辅助属性必填 ”
     * 表单插件  nckd_im_materialreqbi_ext
     * author:黄文波 2024-10-10
     */

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        // 要求加载预计送货日期、最迟送货日期字段
//        e.getFieldKeys().add(DelivaryDateValidator.KEY_DELIVERYDATE);
//        e.getFieldKeys().add(DelivaryDateValidator.KEY_LASTDATE);

        //加载物料、辅助属性字段
        e.getFieldKeys().add(DelivaryDateValidator.KEY_material);
        e.getFieldKeys().add(DelivaryDateValidator.KEY_auxpty);

    }

    /**
     * 执行操作校验前，触发此事件
     *
     * @remark 插件可以在此事件，调整预置的操作校验器；或者增加自定义操作校验器
     */
    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        // 添加自定义的校验器：送货日期校验器
        e.addValidator(new DelivaryDateValidator());
    }


    /**
     * 自定义操作校验器：校验送货日期
     *
     * @author rd_JohnnyDing
     */
    class DelivaryDateValidator extends AbstractValidator {

//    /** 预计送货日期字段标识 */
//    public final static String KEY_DELIVERYDATE = "deliverydate";
//    /** 最迟送货日期字段标识 */
//    public final static String KEY_LASTDATE = "lastdate";

        /**
         * 物料字段标识
         */
        public final static String KEY_material = "material";
        /**
         * 辅助属性字段标识
         */
        public final static String KEY_auxpty = "auxpty";


        /**
         * 返回校验器的主实体：系统将自动对此实体数据，逐行进行校验
         */
        @Override
        public String getEntityKey() {
            return this.entityKey;
        }

        /**
         * 给校验器传入上下文环境及单据数据包之后，调用此方法；
         *
         * @remark 自定义校验器，可以在此事件进行本地变量初始化：如确认需要校验的主实体
         */
        @Override
        public void initializeConfiguration() {
            super.initializeConfiguration();
            // 在此方法中，确认校验器检验的主实体：物料信息单据体
            // 需要对送货子单据体行，逐行判断辅助单位
            this.entityKey = "billentry";
        }

        /**
         * 校验器初始化完毕，从单据数据包中，提取出了主实体数据行，开始校验前，调用此方法；
         *
         * @remark 此方法，比initializeConfiguration更晚调用；
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
            SimpleDateFormat timesdf = new SimpleDateFormat("yyyy-MM-dd");

            // 逐行校验预计送货
            for (ExtendedDataEntity rowDataEntity : this.getDataEntities()) {
                rowDataModel.setRowContext(rowDataEntity.getDataEntity());
//            Date deliveryDate = (Date)rowDataModel.getValue(KEY_DELIVERYDATE);
//            Date lastDate = (Date)rowDataModel.getValue(KEY_LASTDATE);
                String resut = "";
                DynamicObject material = (DynamicObject) rowDataModel.getValue(KEY_material);
                DynamicObject auxpty = (DynamicObject) rowDataModel.getValue(KEY_auxpty);

                //获取物料信息，根据物料信息查询是否启用的辅助属性
                Object materialId = material.getPkValue();


                if (material == null) return;
                //商品Id
                Object itemId = material.getPkValue();
                material = BusinessDataServiceHelper.loadSingle(itemId, material.getDynamicObjectType().getName());
                if (material.getBoolean("isuseauxpty")) {
                    DynamicObjectCollection auxptyentry = material.getDynamicObjectCollection("auxptyentry");
                    for (DynamicObject auxptyent : auxptyentry) {
                        String num = DynamicObjectUtils.getDynamicObject(auxptyent, "auxpty").getString("number");
//                     auxptyent.getDynamicObject("");
                        if ("001".equals(num)) {
                            resut = "A";
                            continue;
                        }
//                    System.out.println(num);
                    }
                }

                if ("A".equals(resut)) {

                    if (auxpty == null) {
                        this.addErrorMessage(rowDataEntity,
                                String.format("预计送货日期(%s)，不能晚于最迟送货日期(%s)！"
                                ));

                    }
                }

//            if (deliveryDate.compareTo(lastDate) > 0 ){
//                // 校验不通过，输出一条错误提示
//                this.addErrorMessage(rowDataEntity,
//                        String.format("预计送货日期(%s)，不能晚于最迟送货日期(%s)！",
//                                timesdf.format(deliveryDate), timesdf.format(lastDate)));
//            }
            }
        }
    }
}