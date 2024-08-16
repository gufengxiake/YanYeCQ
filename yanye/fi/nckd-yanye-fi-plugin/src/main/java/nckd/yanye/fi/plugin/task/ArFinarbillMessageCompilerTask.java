package nckd.yanye.fi.plugin.task;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.earlywarn.EarlyWarnContext;
import kd.bos.entity.earlywarn.kit.StringTemplateParser;
import kd.bos.entity.earlywarn.kit.StringUtil;
import kd.bos.entity.earlywarn.warn.plugin.IEarlyWarnMessageCompiler;
import kd.bos.servicehelper.BusinessDataServiceHelper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

public class ArFinarbillMessageCompilerTask implements IEarlyWarnMessageCompiler {
    @Override
    public String getSingleMessage(String s, List<String> list, DynamicObject dynamicObject, EarlyWarnContext earlyWarnContext) {
        dynamicObject = BusinessDataServiceHelper.loadSingle(dynamicObject.getPkValue(),"ar_finarbill");
        DynamicObjectCollection dynamicObjectCollection = dynamicObject.getDynamicObjectCollection("planentity");
        String detailStr = "";
        BigDecimal total = BigDecimal.ZERO;
        for (DynamicObject dynamic : dynamicObjectCollection){
            if (dateFormat(dynamic.getDate("planduedate")).equals(dateFormat(new Date()))){
                total = total.add(dynamic.getBigDecimal("unplansettleamt"));
                detailStr += dateFormatByDate(dynamic.getDate("planduedate"))+"未结算金额："+dynamic.getBigDecimal("unplansettleamt").setScale(2, RoundingMode.HALF_UP)+"。";
            }
        }
        Map<String, String> map = new HashMap<>();
        for (String field : list) {
            String value = "";
            if ("totalunsettleamount".equals(field)){
                value = total.setScale(2, RoundingMode.HALF_UP).toString();
            }else if ("detail".equals(field)){
                value = detailStr;
            }else {
                String[] arr = StringUtil.split(field, ".");
                Object objValue = getValue(dynamicObject, arr);
                value = objValue == null?"" : objValue.toString();
            }
            map.put(field,value);
        }
        StringTemplateParser parser = new StringTemplateParser();
        return parser.parse(s,name->map.get(name));
    }
    private String getValue(DynamicObject data, String[] arr) {
        if(null == arr || arr.length == 0){
            return "";
        }
        Object obj = data.get(arr[0]);
        if(obj instanceof DynamicObject){
            return getValue((DynamicObject)obj, Arrays.copyOfRange(arr, 1, arr.length));
        }
        return StringUtil.toSafeString(obj);
    }

    @Override
    public String getMergeMessage(String s, List<String> list, EarlyWarnContext earlyWarnContext) {
        return null;
    }

    private String dateFormat(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        return sdf.format(date);
    }
    private String dateFormatByDate(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        return sdf.format(date);
    }
}
