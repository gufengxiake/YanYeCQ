package nckd.yanye.tmc.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.GetEntityTypeEventArgs;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.MulBasedataProp;
import kd.bos.form.field.BasedataEdit;
import nckd.yanye.tmc.common.AppflgConstant;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

/**
 * Module           :财务云-出纳-付款处理-付款金额(大写)字段回写
 * Description      :付款金额(大写)字段回写
 *
 * @author : guozhiwei
 * @date : 2024/8/28 11：03
 *
 */


public class CasPayBillEditPlugin extends AbstractBillPlugIn {


    private static List<String> SETTLETYPE = new ArrayList<String>();


    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
        if(StringUtils.isNotEmpty(AppflgConstant.TMC_SETTLETYPE)){
            String[] split = AppflgConstant.TMC_SETTLETYPE.split(",");
            SETTLETYPE = Arrays.asList(split);
        }
    }


    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        DynamicObject settletype = (DynamicObject) this.getModel().getValue("settletype");
        // 判断结算方式是否是银行承兑或商业承兑
        BasedataEdit draftbill = this.getControl("draftbill");
        //后端加校验
        MulBasedataProp  draftbill2 = (MulBasedataProp ) this.getModel().getDataEntityType().getProperty("draftbill");

        if(ObjectUtils.isNotEmpty(settletype) && SETTLETYPE.contains(settletype.getString("number"))){
            this.getView().setVisible(true,"draftbill");
            // 判断结算方式是否是银行承兑或商业承兑
            draftbill.setMustInput(true);
            //后端加校验
            draftbill2.setMustInput(true);
        }else {
            // 判断结算方式是否是银行承兑或商业承兑
            draftbill.setMustInput(false);
            //后端加校验
            draftbill2.setMustInput(false);
        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String key = e.getProperty().getName();
        ChangeData[] changeData = e.getChangeSet();
        Object newValue = changeData[0].getNewValue();

        if ("actpayamt".equals(key)) {
            if(newValue != null){
                this.payeeAmountChanged(newValue.toString());
            }
        }else if("settletype".equals(key)){

            // 判断结算方式是否是银行承兑或商业承兑
            BasedataEdit draftbill = this.getControl("draftbill");
            //后端加校验
            MulBasedataProp draftbill2 = (MulBasedataProp) this.getModel().getDataEntityType().getProperty("draftbill");
            if(isNotEmpty(newValue) && SETTLETYPE.contains(((DynamicObject) newValue).getString("number"))){
                // 判断结算方式是否是银行承兑或商业承兑
                draftbill.setMustInput(true);
                //后端加校验
                draftbill2.setMustInput(true);
            }else{
                // 判断结算方式是否是银行承兑或商业承兑
                draftbill.setMustInput(false);
                //后端加校验
                draftbill2.setMustInput(false);
            }
        }

    }

    @Override
    public void getEntityType(GetEntityTypeEventArgs e) {
        super.getEntityType(e);

        MainEntityType originalEntityType = e.getOriginalEntityType();
        try {
            e.setNewEntityType((MainEntityType)originalEntityType.clone());
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }





    public static boolean isNotEmpty(Object  key) {
        // 基础资料判空
        if(ObjectUtils.isEmpty(key) || ObjectUtils.isEmpty(((DynamicObject)key).getDataStorage())){
            return false;
        }
        return true;
    }

    /**
     * Description: 付款金额(大写)字段回写
     * @param amount
     */
    private void payeeAmountChanged(String amount) {
        String amountUpper = null;
        if (amount != null) {
            amountUpper = digitUppercase(Double.parseDouble(amount));
        }
        this.getModel().setValue("nckd_actpayamtupper",amountUpper);

    }

    public static String digitUppercase(double n) {
        String fraction[] = { "角", "分" };
        String digit[] = { "零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖" };
        String unit[][] = { { "元", "万", "亿" }, { "", "拾", "佰", "仟" } };

        String head = n < 0 ? "负" : "";
        n = Math.abs(n);

        String s = "";
        for (int i = 0; i < fraction.length; i++) {
            //优化double计算精度丢失问题
            BigDecimal nNum = new BigDecimal(n);
            BigDecimal decimal = new BigDecimal(10);
            BigDecimal scale = nNum.multiply(decimal).setScale(2, RoundingMode.HALF_EVEN);
            double d = scale.doubleValue();
            s += (digit[(int) (Math.floor(d * Math.pow(10, i)) % 10)] + fraction[i])
                    .replaceAll("(零.)+", "");
        }
        if (s.length() < 1) {
            s = "整";
        }
        int integerPart = (int) Math.floor(n);

        for (int i = 0; i < unit[0].length && integerPart > 0; i++) {
            String p = "";
            for (int j = 0; j < unit[1].length && n > 0; j++) {
                p = digit[integerPart % 10] + unit[1][j] + p;
                integerPart = integerPart / 10;
            }
            s = p.replaceAll("(零.)*零$", "").replaceAll("^$", "零") + unit[0][i]
                    + s;
        }
        return head
                + s.replaceAll("(零.)*零元", "元").replaceFirst("(零.)+", "")
                .replaceAll("(零.)+", "零").replaceAll("^整$", "零元整");
    }


}
