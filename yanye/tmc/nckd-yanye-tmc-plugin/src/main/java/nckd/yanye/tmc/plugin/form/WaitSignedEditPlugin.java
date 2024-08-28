package nckd.yanye.tmc.plugin.form;

import java.util.Date;
import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.fi.fa.common.util.DateUtil;
import java.util.Arrays;
import java.util.List;

/**
 * Module           :资金云-电子票据-票据管理-待签收票据处理
 * Description      :待签收票据插件
 *
 *
 * @author guozhiwei
 * @date  2024/8/14 17:16
 * 标识 nckd_cdm_electronic_s_ext
 *
 *
 */


public class WaitSignedEditPlugin extends AbstractBillPlugIn {

    private final List<String> NAME_LIST = Arrays.asList(new String[]{"issueticketdate", "exchangebillexpiredate"});


    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);

        // 监听出票日期和汇票到期日， 汇票到期日-出票日期，计算承兑期限
        String name = e.getProperty().getName();
        if(NAME_LIST.contains(name)){
            Date issueticketdate = "issueticketdate".equals(name)?(Date) e.getChangeSet()[0].getNewValue(): (Date) this.getModel().getValue("issueticketdate");
            Date exchangebillexpiredate = "exchangebillexpiredate".equals(name)?(Date) e.getChangeSet()[0].getNewValue(): (Date) this.getModel().getValue("exchangebillexpiredate");


            if(issueticketdate != null && exchangebillexpiredate != null){
                int diffMonths = DateUtil.getDiffMonthsByLocalDate(issueticketdate,exchangebillexpiredate,false,true);
                if(diffMonths>6){
                    this.getModel().setValue("nckd_acceptance_period","B");
                }else{
                    this.getModel().setValue("nckd_acceptance_period","A");
                }
            }
        }
    }

}
