package nckd.yanye.hr.plugin.form.web.activity;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.control.events.UploadListener;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.hr.hom.formplugin.web.activity.AbstractCollectDynViewPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * 单据标识：hom_infogroupdataadd  名称：新增多行信息组数据页面
 * 菜单：玩美入职-》信息采集
 * 需求：根据学历字段的值来控制其它字段的默认值
 * author: chengchaohua
 * date: 2024-09-03
 */
public class CollectActivityAddPluginEx  extends AbstractCollectDynViewPlugin implements UploadListener {
    private static final Log logger = LogFactory.getLog(CollectActivityAddPluginEx.class);


    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String key = e.getProperty().getName();
        if(StringUtils.equals("field1249297648691749891", key)) {
            // 学历：field1249297648691749891
            // 学位：field1249297648691749892
            // 毕业院系名称:field2004400643116121089
            // 第一专业：field1249297648691749893
            IDataModel model = this.getModel();
            DynamicObject  xueli = (DynamicObject )model.getValue("field1249297648691749891");
            Map mapxueli = new HashMap<String,String>();
            mapxueli.put("1010_S","博士研究生");
            mapxueli.put("1020_S","硕士研究生");
            mapxueli.put("1030_S","本科");
            mapxueli.put("1040_S","大专");
            String xuelinumber = xueli.getString("number");
            if (mapxueli.get(xuelinumber) == null) {
                // 1050_S:高中,1060_S:中专，1061_S:技校，1070_S:初中，1080_S:小学，1090_S:其它
                // 当【学历】不等于“1010_S 博士研究生、1020_S 硕士研究生、1030_S 本科、1040_S 大专”时，
                // 【学位】默认赋值基础资料码值为“1000_S 无”
                model.setItemValueByNumber("field1249297648691749892","1000_S"); // 1000_S 无
                // 【毕业院系名称】默认赋值为文本”无“
                model.setValue("field2004400643116121089","无");
                // 【第一专业】默认赋值为文本”无"
                model.setValue("field1249297648691749893","无");
            }
        }

    }
}
