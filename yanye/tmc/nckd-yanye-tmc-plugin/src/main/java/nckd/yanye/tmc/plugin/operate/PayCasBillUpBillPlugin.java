package nckd.yanye.tmc.plugin.operate;

import kd.bos.bec.api.IEventServicePlugin;
import kd.bos.bec.model.EntityEvent;
import kd.bos.bec.model.KDBizEvent;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Module           :财务云-出纳-付款处理-付款金额(大写)字段回写
 * Description      :付款金额(大写)字段回写
 *
 * @author : guozhiwei
 * @date : 2024/8/28 17：29
 *
 */

public class PayCasBillUpBillPlugin implements IEventServicePlugin {

    private static Log logger = LogFactory.getLog(WaitSignedAccptPlugin.class);

    @Override
    public Object handleEvent(KDBizEvent evt) {
        logger.info("付款处理保存处理执行插件:-------------------");
        logger.info("插件参数businesskeys：{}", ((EntityEvent) evt).getBusinesskeys());
        List<String> businesskeys = ((EntityEvent) evt).getBusinesskeys();
        List<Long> businessKeyLongs = businesskeys.stream()
                .map(Long::valueOf) // 将每个 String 转换为 Long
                .collect(Collectors.toList());
        DynamicObject[] load = BusinessDataServiceHelper.load("cas_paybill", "id,nckd_actpayamtupper,actpayamt", new QFilter[]{new QFilter("id", QCP.in, businessKeyLongs)});
        for (DynamicObject casPaybill : load) {
            String nckdActpayamtupper = casPaybill.getString("nckd_actpayamtupper");
            if(StringUtils.isNotEmpty(nckdActpayamtupper)){
                return null;
            }
            // 付款金额(大写)字段回写
            String amount = casPaybill.getBigDecimal("actpayamt").toString();
            String amountUpper = digitUppercase(Double.parseDouble(amount));
            casPaybill.set("nckd_actpayamtupper",amountUpper);
            SaveServiceHelper.save(new DynamicObject[]{casPaybill});
        }

        return null;
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
