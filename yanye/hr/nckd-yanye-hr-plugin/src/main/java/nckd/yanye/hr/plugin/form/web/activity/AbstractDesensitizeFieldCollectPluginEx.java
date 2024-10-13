package nckd.yanye.hr.plugin.form.web.activity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.DateProp;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Button;
import kd.bos.form.control.Control;
import kd.bos.form.control.events.BeforeClickEvent;
import kd.bos.form.field.DateEdit;
import kd.hr.hpfs.formplugin.privacy.AbstractDesensitizeFieldCommonPlugin;

import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;


/**
 * PC端-》信息采集协作-》基本信息 ,当选退伍军人为是，入伍和退伍时间设置必填处理
 * 信息组页面 ：nckd_hom_infogroupdyn_ext
 * 2024-09-12
 * chengchaohua
 */
public class AbstractDesensitizeFieldCollectPluginEx extends AbstractDesensitizeFieldCommonPlugin {
    @Override
    public void registerListener(EventObject e) {
        Button button = this.getView().getControl("savesingles001"); // 基本信息的保存按钮
        button.addClickListener(this);
        Button button2 = this.getView().getControl("editsingles002"); // 联系方式的修改按钮
        button2.addClickListener(this);
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        // 把单据每个字段的标识和页面显示id值列举出来; infoGroupEntityList数据结构： [{"imgKey":"","infoGroupFieldList":[{"fieldName":"是否退伍军人","fieldMustInput":true,"fieldKey":"nckd_veteran","fieldId":2004353791935130624}]}]
        Map<String, Long> fieldForIdMap = fieldForIdMap(); // key: nckd_veteran ,value: 2004353791935130624
        Long nckdVeteranId = fieldForIdMap.get("nckd_veteran"); // nckd_veteran:是否退伍军人字段   nckdVeteranId:2004353791935130624
        String tuiwuid = "field" + nckdVeteranId; // 是否退伍军人fieldid
        String key = e.getProperty().getName();
        // 信息采集协作，基本信息：是否退伍军人：field2004353791935130624，入伍时间：field2004391669838916608，退伍时间：field2004391669838916609
        if(StringUtils.equals(tuiwuid, key)) {
            String istuiwu = (String)this.getModel().getValue(tuiwuid); // "field2004353791935130624"
            String ruwudateid = "field" + fieldForIdMap.get("nckd_ruwudate"); // 入伍时间 field2004391669838916608
            String tuiwudateid = "field" + fieldForIdMap.get("nckd_tuiwudate"); // 退伍时间 field2004391669838916609
            if ("YES".equals(istuiwu)) {
                // 选了是退伍军人
                // 前端属性设置（前端判断必填校验）
                DateEdit property1 = (DateEdit) this.getControl(ruwudateid); // 入伍时间
                property1.setMustInput(true);
                DateEdit property2 = (DateEdit) this.getControl(tuiwudateid);
                property2.setMustInput(true);
                // 后端属性设置（后端判断必填校验）
                DateProp Prop1 = (DateProp)this.getModel().getDataEntityType().getProperty(ruwudateid); // 入伍时间
                Prop1.setMustInput(true);
                DateProp Prop2 = (DateProp)this.getModel().getDataEntityType().getProperty(tuiwudateid);
                Prop2.setMustInput(true);
                // 可编辑
                this.getView().setEnable(true, ruwudateid);
                this.getView().setEnable(true, tuiwudateid);
            } else {
                // 必填设置
                DateEdit property1 = (DateEdit) this.getControl(ruwudateid);
                property1.setMustInput(false);
                DateEdit property2 = (DateEdit) this.getControl(tuiwudateid);
                property2.setMustInput(false);

                DateProp Prop1 = (DateProp)this.getModel().getDataEntityType().getProperty(ruwudateid);
                Prop1.setMustInput(false);
                DateProp Prop2 = (DateProp)this.getModel().getDataEntityType().getProperty(tuiwudateid);
                Prop2.setMustInput(false);
                // 值置空，不可编辑
                this.getModel().setValue(ruwudateid,null);
                this.getView().setEnable(false, ruwudateid);
                this.getModel().setValue(tuiwudateid,null);
                this.getView().setEnable(false, tuiwudateid);
            }
        }
    }

    @Override
    public void beforeClick(BeforeClickEvent evt) {
        super.beforeClick(evt);
        String key = ((Control)evt.getSource()).getKey();
        // 获取每个页面字段的显示的id值
        Map<String, Long> fieldForIdMap = fieldForIdMap(); // key: nckd_veteran ,value: 2004353791935130624
        // 基本信息 保存标识：savesingles001
        if ("savesingles001".equals(key)) {
            // 基本信息的保存按钮 savesingles001
            Long nckdVeteranId = fieldForIdMap.get("nckd_veteran"); // nckd_veteran:是否退伍军人字段   nckdVeteranId:2004353791935130624

            String istuiwu = (String)this.getModel().getValue("field" + nckdVeteranId);
            FormShowParameter showParameter = this.getView().getFormShowParameter();
            if ("YES".equals(istuiwu)) {
                // 入伍时间 field2004391669838916608
                Date ruwudate = (Date)this.getModel().getValue("field" + fieldForIdMap.get("nckd_ruwudate")); // 入伍时间 2004391669838916608
                String warnstr = "";
                if (ruwudate == null) {
                    warnstr = warnstr + "入伍时间、";
                }
                // 退伍时间 field2004391669838916609
                Date tuiwudate = (Date)this.getModel().getValue("field" + fieldForIdMap.get("nckd_tuiwudate")); // 退伍时间 2004391669838916609
                if (tuiwudate == null) {
                    warnstr = warnstr + "退伍时间、";
                }
                if (warnstr.length() > 0) {
                    this.getView().showTipNotification("以下信息为必填，请填写：" + warnstr.substring(0,warnstr.length() -1));
                    evt.setCancel(true);
                } else {
                    // 日期比较
                    if (!ruwudate.before(tuiwudate)) {
                        this.getView().showTipNotification("退伍时间要大于入伍时间" );
                        evt.setCancel(true);
                    }
                }
            }
        } else if ("editsingles002".equals(key)) {
            // 联系方式的修改按钮 editsingles002
            Object address = this.getModel().getValue("field1249295461932639234"); // 通讯地址国家/地区 "fieldKey":"countrycode"
            if(address == null) {
                this.getModel().setItemValueByNumber("field1249295461932639234","001"); // 通讯地址国家/地区：001 中国
            }
        }
    }
    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        // 把单据每个字段的标识和页面显示id值列举出来; infoGroupEntityList数据结构： [{"imgKey":"","infoGroupFieldList":[{"fieldName":"是否退伍军人","fieldMustInput":true,"fieldKey":"nckd_veteran","fieldId":2004353791935130624}]}]
        Map<String, Long> fieldForIdMap = fieldForIdMap(); // key: nckd_veteran ,value: 2004353791935130624
        Long nckdVeteranId = fieldForIdMap.get("nckd_veteran"); // nckd_veteran:是否退伍军人字段   nckdVeteranId:2004353791935130624
        if (nckdVeteranId != null) {
            // 是否退伍军人字段：field2004353791935130624
            String istuiwu = (String)this.getModel().getValue("field" + nckdVeteranId);
            if ("YES".equals(istuiwu)) {
                // 是退伍军人，设置必填
                // 前端属性设置（前端判断必填校验）
                DateEdit property1 = (DateEdit) this.getControl("field" + fieldForIdMap.get("nckd_ruwudate")); // 入伍时间 2004391669838916608
                property1.setMustInput(true);
                DateEdit property2 = (DateEdit) this.getControl("field" + fieldForIdMap.get("nckd_tuiwudate")); // 退伍时间 2004391669838916609
                property2.setMustInput(true);
            }
        }
    }

    // 把单据每个字段的标识和页面显示id值列举出来;
    private Map<String, Long> fieldForIdMap() {
        FormShowParameter showParameter = this.getView().getFormShowParameter();
        JSONObject jsonObject = (JSONObject)showParameter.getCustomParam("config");
        JSONArray infoGroupEntityList = jsonObject.getJSONArray("infoGroupEntityList");
        int k = infoGroupEntityList.size();
        // 把单据每个字段的标识和页面显示id值列举出来; infoGroupEntityList数据结构： [{"imgKey":"","infoGroupFieldList":[{"fieldName":"是否退伍军人","fieldMustInput":true,"fieldKey":"nckd_veteran","fieldId":2004353791935130624}]}]
        Map<String, Long> fieldForIdMap = new HashMap<String, Long>(); // key: nckd_veteran ,value: 2004353791935130624
        for (int i=0; i < k ;i++) {
            JSONObject info = infoGroupEntityList.getJSONObject(i);
            JSONArray infoGroupFieldList = info.getJSONArray("infoGroupFieldList");
            int size = infoGroupFieldList.size();
            for (int j=0; j < size; j++) {
                JSONObject jsonObject1 = infoGroupFieldList.getJSONObject(j);
                fieldForIdMap.put(jsonObject1.getString("fieldKey"),jsonObject1.getLongValue("fieldId"));
            }
        }
        return fieldForIdMap;
    }

}
