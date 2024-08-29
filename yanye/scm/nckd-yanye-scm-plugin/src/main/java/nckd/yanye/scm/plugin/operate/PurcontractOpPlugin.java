package nckd.yanye.scm.plugin.operate;

import cn.hutool.core.util.ObjectUtil;
import dm.jdbc.util.StringUtil;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Module           :供应链云-合同管理-采购合同
 * Description      :采购合同保存操作插件
 *
 * @author : zhujintao
 * @date : 2024/7/11
 */
public class PurcontractOpPlugin extends AbstractOperationServicePlugIn {

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        //一般的操作插件校验表单的字段默认带出的有限，都是单据编码，名称等几个，要校验哪个需要自己加
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("biztimebegin");
        fieldKeys.add("biztimeend");
        fieldKeys.add("supplier");
        fieldKeys.add("material");
    }

    /**
     * 采购合同的“起始日期”与“截止日期”之内同一个订货供应商且同一个物料，有且只允许存在一个审核且生效状态且未关闭的采购合同，在采购合同进行保存时，
     * 进行判断，如不存在则允许提交，如存在重复物料，则进行提示：第X行的“物料编码”与采购合同“合同编号”的第X行存在重复物料；
     */
    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                // 保存时校验，只有一条数据
                for (ExtendedDataEntity rowDataEntity : this.getDataEntities()) {
                    //获得某一单据
                    DynamicObject bill = rowDataEntity.getDataEntity();
                    //获取条件
                    Date biztimebegin = bill.getDate("biztimebegin");
                    Date biztimeend = bill.getDate("biztimeend");
                    DynamicObject org = bill.getDynamicObject("org");
                    DynamicObject supplier = bill.getDynamicObject("supplier");
                    //获取分录行
                    DynamicObjectCollection entryColl = bill.getDynamicObjectCollection("billentry");
                    //进行提示：第X行的“物料编码”与采购合同“合同编号”的第X行存在重复物料
                    //构造成key = id ; value = 行号
                    Map<Object, String> mapIdAndSeq = entryColl.stream().filter(e -> !ObjectUtils.isEmpty(e.getDynamicObject("material")))
                            .collect(Collectors.toMap(k -> k.getDynamicObject("material").getPkValue(), v -> v.getString("seq")));
                    //构造成key = id ; value = 物料名称
                    Map<Object, String> mapIdAndName = entryColl.stream().filter(e -> !ObjectUtils.isEmpty(e.getDynamicObject("material")))
                            .collect(Collectors.toMap(k -> k.getDynamicObject("material").getPkValue(), v -> v.getDynamicObject("material").getDynamicObject("masterid").getString("name")));
                    //构造查询条件 ，这里没办法 每个合同的条件都不一样
                    QFilter qFilter = new QFilter("org", QCP.equals, org.getPkValue())
                            .and("billstatus", QCP.equals, "C")
                            .and("validstatus", QCP.equals, "B")
                            .and("closestatus", QCP.equals, "A")
                            .and("billentry.material", QCP.in, mapIdAndSeq.keySet())
                            .and(new QFilter("biztimebegin", QCP.less_equals, biztimebegin).and("biztimeend", QCP.large_equals, biztimebegin)
                                    .or(new QFilter("biztimebegin", QCP.less_equals, biztimeend).and("biztimeend", QCP.large_equals, biztimeend))
                                    .or(new QFilter("biztimebegin", QCP.large_equals, biztimebegin).and("biztimeend", QCP.less_equals, biztimeend)));
                    if (ObjectUtil.isNotEmpty(supplier)) {
                        qFilter.and("supplier", QCP.equals, supplier.getPkValue());
                    }
                    //String entityName, String selectProperties, QFilter[] filters
                    DynamicObject[] purcontractArr = BusinessDataServiceHelper.load("conm_purcontract", "id,billno,billentry.material,billentry.seq", qFilter.toArray());
                    if (purcontractArr.length > 0) {
                        for (DynamicObject purcontractBill : purcontractArr) {
                            //获取命中的采购合同，并将其分录转换为map
                            DynamicObjectCollection purcontractEntry = purcontractBill.getDynamicObjectCollection("billentry");
                            //构造成key = id ; value = 行号
                            Map<Object, String> mapIdAndSeq1 = purcontractEntry.stream().collect(Collectors.toMap(e -> e.getDynamicObject("material").getPkValue(), v -> v.getString("seq")));
                            for (DynamicObject entryDy : entryColl) {
                                Object pkValue = entryDy.getDynamicObject("material").getPkValue();
                                //查询出来的单据有多个分录，不见得每个分录的物料都要拦截
                                //如果mapIdAndLineno1.get()有值，说明有相同的
                                String billNo = purcontractBill.getString("billno");
                                String seq = mapIdAndSeq1.get(pkValue);
                                if (StringUtil.isNotEmpty(billNo) && StringUtil.isNotEmpty(seq)) {
                                    String materialSeq = mapIdAndSeq.get(pkValue);
                                    String materialName = mapIdAndName.get(pkValue);
                                    // 校验不通过，//进行提示：第X行的“物料名称”与采购合同“合同编号”的第X行存在重复物料
                                    this.addErrorMessage(rowDataEntity, String.format("第(%s)行的物料名称 (%s) 与采购合同(%s)的第(%s)行存在重复物料",
                                            materialSeq, materialName, billNo, seq));
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
