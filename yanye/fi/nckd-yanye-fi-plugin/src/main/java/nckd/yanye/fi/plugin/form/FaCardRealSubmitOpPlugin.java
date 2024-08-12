package nckd.yanye.fi.plugin.form;

import cn.hutool.core.util.ObjectUtil;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Module           :财务云-固定资产-资产卡片
 * Description      :实物卡片提交操作校验插件
 *
 * @author : yaosijie
 * @date : 2024/78/12
 */
public class FaCardRealSubmitOpPlugin extends AbstractOperationServicePlugIn {


    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        //一般的操作插件校验表单的字段默认带出的有限，都是单据编码，名称等几个，要校验哪个需要自己加
        List<String> fieldKeys = e.getFieldKeys();
        fieldKeys.add("nckd_checkboxfield");
        fieldKeys.add("nckd_decimalfield");
        fieldKeys.add("nckd_basedatafield");
        fieldKeys.add("billno");
        fieldKeys.add("usestatus");
        fieldKeys.add("nckd_decimalfield1");
        fieldKeys.add("nckd_decimalfield2");
    }
    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                ExtendedDataEntity[] entities = this.getDataEntities();
                Arrays.asList(entities).forEach(k -> {
                    DynamicObject dynamic = k.getDataEntity();
                    String billno = dynamic.getString("billno");
                    //研发设备
                    boolean checkboxfield = dynamic.getBoolean("nckd_checkboxfield");
                    //研发时长
                    BigDecimal decimalfield = dynamic.getBigDecimal("nckd_decimalfield");
                    //研发项目
                    DynamicObject project = dynamic.getDynamicObject("nckd_basedatafield");
                    DynamicObject usestatus = dynamic.getDynamicObject("usestatus");//使用状态
                    BigDecimal depreciationProvision = dynamic.getBigDecimal("nckd_decimalfield1");//计提折旧比例
                    BigDecimal rent = dynamic.getBigDecimal("nckd_decimalfield2");//租金
                    if (checkboxfield){
                        if (ObjectUtil.isNull(decimalfield) || ObjectUtil.isNull(project))
                        this.addErrorMessage(k, String.format("卡片编号：(%s),【研发设备】选中，研发时长和研发项目必录",
                                billno));
                    }
                    if ("00103".equals(usestatus.getString("number")) && rent.compareTo(BigDecimal.ZERO) <= 0){
                        this.addErrorMessage(k, String.format("卡片编号：(%s),【使用状态】为全部租出，租金必录",
                                billno));
                    }
                    if ("00111".equals(usestatus.getString("number")) && (depreciationProvision.compareTo(BigDecimal.ZERO) <= 0 || rent.compareTo(BigDecimal.ZERO) <= 0)){
                        this.addErrorMessage(k, String.format("卡片编号：(%s),【使用状态】为部分租出，计提折旧比率和租金必录",
                                billno));
                    }
                });
            }
        });
    }
}
