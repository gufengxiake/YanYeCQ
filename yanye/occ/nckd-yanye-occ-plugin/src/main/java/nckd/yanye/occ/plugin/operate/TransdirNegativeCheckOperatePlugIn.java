package nckd.yanye.occ.plugin.operate;

import kd.bos.algo.DataSet;
import kd.bos.algo.GroupbyDataSet;
import kd.bos.algo.Row;
import kd.bos.dataentity.entity.DynamicObject;
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

import java.math.BigDecimal;

public class TransdirNegativeCheckOperatePlugIn extends AbstractOperationServicePlugIn {
    /**
     * 操作执行前，准备加载单据数据之前，触发此事件
     *
     * @remark 插件可以在此事件中，指定需要加载的字段
     */
    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        e.getFieldKeys().add(TransDirValidator.KEY_CONFIGUREDCODE);
        e.getFieldKeys().add(TransDirValidator.KEY_OUTINVTYPE);
        e.getFieldKeys().add(TransDirValidator.KEY_AUXPTY);
        e.getFieldKeys().add(TransDirValidator.KEY_PRODUCEDATE);
        e.getFieldKeys().add(TransDirValidator.KEY_BIZOPERATOR);
        e.getFieldKeys().add(TransDirValidator.KEY_WAREHOUSE);
        e.getFieldKeys().add(TransDirValidator.KEY_MATERIAL);
        e.getFieldKeys().add(TransDirValidator.KEY_PROJECT);
        e.getFieldKeys().add(TransDirValidator.KEY_OUTOWNER);
        e.getFieldKeys().add(TransDirValidator.KEY_UNIT2ND);
        e.getFieldKeys().add(TransDirValidator.KEY_UNIT);
        e.getFieldKeys().add(TransDirValidator.KEY_WAREHOUSE);
        e.getFieldKeys().add(TransDirValidator.KEY_LOCATION);
        e.getFieldKeys().add(TransDirValidator.KEY_ORG);
        e.getFieldKeys().add(TransDirValidator.KEY_EXPIRYDATE);
        e.getFieldKeys().add(TransDirValidator.KEY_TRACKNUMBER);
        e.getFieldKeys().add(TransDirValidator.KEY_OUTKEEPER);
        e.getFieldKeys().add(TransDirValidator.KEY_BASEUNIT);
        e.getFieldKeys().add(TransDirValidator.KEY_OUTOWNERTYPE);
        e.getFieldKeys().add(TransDirValidator.KEY_OUTINVSTATUS);
        e.getFieldKeys().add(TransDirValidator.KEY_LOTNUMBER);
        e.getFieldKeys().add(TransDirValidator.KEY_OUTKEEPERTYPE);
        e.getFieldKeys().add("isvirtualbill");//是否虚单
    }

    /**
     * 执行操作校验前，触发此事件
     *
     * @remark 插件可以在此事件，调整预置的操作校验器；或者增加自定义操作校验器
     */
    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {

        // 添加自定义的校验器
        e.addValidator(new TransDirValidator());
    }

    /**
     * 自定义操作校验器
     *
     * @author
     */
    class TransDirValidator extends AbstractValidator {

        //余额表维度映射字段
        public final static String KEY_ORG = "outorg";//调出组织
        public final static String KEY_EXPIRYDATE = "expirydate";//到期日
        public final static String KEY_TRACKNUMBER = "tracknumber";//跟踪号
        public final static String KEY_OUTKEEPER = "outkeeper";//出库保管者
        public final static String KEY_BIZOPERATOR = "nckd_ywy";//业务员
        public final static String KEY_BASEUNIT = "baseunit";//基本单位
        public final static String KEY_OUTINVTYPE = "outinvtype";//出库库存类型
        public final static String KEY_OUTOWNERTYPE = "outownertype";//出库货主类型
        public final static String KEY_CONFIGUREDCODE = "configuredcode";//配置号
        public final static String KEY_OUTINVSTATUS = "outinvstatus";//出库库存状态
        public final static String KEY_LOTNUMBER = "lotnumber";//批号
        public final static String KEY_OUTKEEPERTYPE = "outkeepertype";//出库保管者类型
        public final static String KEY_UNIT = "unit";//计量单位
        public final static String KEY_MATERIAL = "material";//物料
        public final static String KEY_WAREHOUSE = "outwarehouse";//仓库
        public final static String KEY_UNIT2ND = "unit2nd";//辅助单位
        public final static String KEY_AUXPTY = "auxpty";//辅助属性
        public final static String KEY_PROJECT = "project";//项目号
        public final static String KEY_OUTOWNER = "outowner";//出库货主
        public final static String KEY_PRODUCEDATE = "producedate";//生产日期
        public final static String KEY_LOCATION = "outlocation";//出库仓位

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
            // 在此方法中，确认校验器检验的主实体：送货子单据体
            //this.entityKey = "billentry";
            this.entityKey = "im_transdirbill";
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
            // 逐行校验预计送货
            for (ExtendedDataEntity rowDataEntity : this.getDataEntities()) {
                rowDataModel.setRowContext(rowDataEntity.getDataEntity());
                DynamicObject billtype = (DynamicObject) rowDataModel.getValue("billtype");
                String name = billtype.getString("name");
                boolean isvirtualbill = (boolean) rowDataModel.getValue("isvirtualbill");
                if ("借货归还单".equals(name) && !isvirtualbill) {
                    //单据编号
                    String billno = rowDataModel.getValue("billno").toString();

                    //表单标识
                    String number = "im_transdirbill";//直接调拨单
                    //查询字段
                    String fieldkey = "outorg.id orgid,nckd_ywy.id operatorid,billentry.expirydate expirydate,billentry.tracknumber tracknumber,billentry.outkeeper.id outkeeperid,billentry.baseunit.id baseunitid,billentry.outinvtype.id outinvtypeid," +
                            "billentry.outownertype outownertype,billentry.configuredcode.id configuredcodeid,billentry.outinvstatus.id outinvstatusid,billentry.lotnumber lotnumber,billentry.outkeepertype outkeepertype,billentry.unit.id unitid,billentry.material.masterid materialid,billentry.materialname matname," +
                            "billentry.outwarehouse.id warehouseid,billentry.unit2nd.id unit2ndid,billentry.auxpty.id auxptyid,billentry.project.id projectid,billentry.outowner.id outownerid,billentry.producedate producedate,billentry.outlocation.id locationid,billentry.baseqty baseqty";
                    //过滤条件
                    QFilter qFilter = new QFilter("billno", QCP.equals, billno);
                    //查询统计数据
                    DataSet DBSet = QueryServiceHelper.queryDataSet("getSalOut", number, fieldkey, new QFilter[]{qFilter}, "");
                    //设置group by
                    GroupbyDataSet groupby = DBSet.groupBy(new String[]{"orgid", "operatorid", "expirydate", "tracknumber", "outkeeperid", "baseunitid", "outinvtypeid", "outownertype", "configuredcodeid", "outinvstatusid", "lotnumber", "outkeepertype", "unitid", "materialid", "matname", "warehouseid", "unit2ndid", "auxptyid", "projectid", "outownerid", "producedate", "locationid"});
                    groupby = groupby.sum("baseqty");
                    DataSet groupDb = groupby.finish();
                    if (groupDb.hasNext()) {
                        Row monItem = groupDb.next();
                        BigDecimal qty = monItem.getBigDecimal("baseqty");
                        Object orgid = monItem.get("orgid");
                        Object operatorid = monItem.get("operatorid");
                        Object expirydate = monItem.get("expirydate");
                        Object tracknumber = monItem.get("tracknumber");
                        Object outkeeperid = monItem.get("outkeeperid");
                        Object baseunitid = monItem.get("baseunitid");
                        Object outinvtypeid = monItem.get("outinvtypeid");
                        Object outownertype = monItem.get("outownertype");
                        Object configuredcodeid = monItem.get("configuredcodeid");
                        Object outinvstatusid = monItem.get("outinvstatusid");
                        Object lotnumber = monItem.get("lotnumber");
                        Object outkeepertype = monItem.get("outkeepertype");
                        Object unitid = monItem.get("unitid");
                        Object materialid = monItem.get("materialid");
                        Object warehouseid = monItem.get("warehouseid");
                        Object unit2ndid = monItem.get("unit2ndid");
                        Object auxptyid = monItem.get("auxptyid");
                        Object projectid = monItem.get("projectid");
                        Object outownerid = monItem.get("outownerid");
                        Object producedate = monItem.get("producedate");
                        Object locationid = monItem.get("locationid");
                        Object matName=monItem.get("matname");

                        QFilter Filter = new QFilter("nckd_orgfield.id", QCP.equals, orgid)
                                .and("nckd_expirydate", QCP.equals, expirydate)
                                .and("nckd_tracknumber", QCP.equals, tracknumber)
                                .and("nckd_keeper.id", QCP.equals, outkeeperid)
                                .and("nckd_fapplyuserid.id", QCP.equals, operatorid)
                                .and("nckd_fbaseunitid.id", QCP.equals, baseunitid)
                                .and("nckd_invtype.id", QCP.equals, outinvtypeid)
                                .and("nckd_fownertype", QCP.equals, outownertype)
                                .and("nckd_configuredcode.id", QCP.equals, configuredcodeid)
                                .and("nckd_finvstatusid.id", QCP.equals, outinvstatusid)
                                .and("nckd_lotnum", QCP.equals, lotnumber)
                                .and("nckd_keepertype", QCP.equals, outkeepertype)
                                .and("nckd_funitid.id", QCP.equals, unitid)
                                .and("nckd_fmaterialid.id", QCP.equals, materialid)
                                .and("warehouseid.id", QCP.equals, warehouseid)
                                .and("nckd_funit2ndid.id", QCP.equals, unit2ndid)
                                .and("nckd_auxpty.id", QCP.equals, auxptyid)
                                .and("nckd_project.id", QCP.equals, projectid)
                                .and("nckd_fownerid.id", QCP.equals, outownerid)
                                .and("nckd_producedate", QCP.equals, producedate)
                                .and("nckd_flocationid.id", QCP.equals, locationid);
                        DynamicObjectCollection jhDynaObj = QueryServiceHelper.query("nckd_jxyy_xsjhyeb", "nckd_fmaterialid.id matid,nckd_baseqty qty", new QFilter[]{Filter});
                        if (jhDynaObj != null && !jhDynaObj.isEmpty()) {
                            BigDecimal jhQty = BigDecimal.ZERO;
                            for (DynamicObject dataObject : jhDynaObj) {
                                jhQty = jhQty.add(dataObject.getBigDecimal("qty"));
                            }
                            if (qty.compareTo(jhQty) > 0) {
                                this.addErrorMessage(this.getDataEntities()[0], "负库存校验失败,物料："+matName.toString()+" 的调出数量["+qty.stripTrailingZeros().toPlainString()+"]大于借货余额库存数量["+jhQty.stripTrailingZeros().toPlainString()+"]");
                            }
                        }


                    }

                }

            }
        }
    }
}
