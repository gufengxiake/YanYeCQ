package nckd.yanye.hr.plugin.form.mobile.collect;

import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.DateProp;
import kd.bos.form.FormShowParameter;
import kd.bos.form.control.Button;
import kd.bos.form.control.events.BeforeClickEvent;
import kd.bos.form.field.DateEdit;
import kd.hr.hom.formplugin.mobile.collect.InfoGroupDynViewMobilePlugin;
import kd.sdk.hr.hom.common.InfoGroupEntity;

import java.util.*;

/**
 * 入职邀约，用户移动端填 信息采集 教育经历
 * 采集详情 ：nckd_hom_collectinfod_ext
 * 隐藏标准版的graduateschool 毕业院校控件
 * 2024-09-10
 * chengchaohua
 */
public class InfoGroupDynViewMobilePluginEx extends InfoGroupDynViewMobilePlugin {
    @Override
    public void registerListener(EventObject e) {
        Button button = this.getView().getControl("barsave"); // 基本信息的保存按钮
        button.addClickListener(this);
    }

    @Override
    public void afterBindData(EventObject e) {
        super.afterBindData(e);
        // 隐藏控件，graduateschool 毕业院校 （教育经历 标准版字段），因为已经使用存文本字段代替了. field1249297648691749888
        FormShowParameter showParameter = this.getView().getFormShowParameter();
        String jsonObject = (String)showParameter.getCustomParam("infoGroupEntity");
        InfoGroupEntity infoGroupEntity = (InfoGroupEntity) JSONObject.parseObject(jsonObject, InfoGroupEntity.class);
        String infoGroupName = infoGroupEntity.getInfoGroupName(); // 分组名称，名称可能会人工修改
        Long infoGroupId = infoGroupEntity.getInfoGroupId(); // 分组id
        if ("基本信息".equals(infoGroupName) || 1247661585950874624L == infoGroupId) {
            // 由于是二开字段，切换环境可能导致field的值不一致，使用字段标识
            // nckd_veteran	是否退伍军人,nckd_ruwudate 入伍时间, nckd_tuiwudate	退伍时间
            Map<String, String> fieldkeytofieldidmap = fieldkeytofieldid();
            // 基本信息字段：是否退伍军人 2004353791935130624
            if (StringUtils.isNotBlank(fieldkeytofieldidmap.get("nckd_veteran"))) {
                String istuiwu = (String)this.getModel().getValue(fieldkeytofieldidmap.get("nckd_veteran")); // field2004353791935130624
                String ruwudatefield = fieldkeytofieldidmap.get("nckd_ruwudate"); // 入伍时间field
                String tuiwudatefield = fieldkeytofieldidmap.get("nckd_tuiwudate"); // 退伍时间field
                if ("YES".equals(istuiwu)) {
                    // 是退伍军人，设置必填
                    // 前端属性设置（前端判断必填校验）
                    DateEdit property1 = (DateEdit) this.getControl(ruwudatefield); // 入伍时间 field2004391669838916608
                    property1.setMustInput(true);
                    DateEdit property2 = (DateEdit) this.getControl(tuiwudatefield); // 退伍时间 "field2004391669838916609"
                    property2.setMustInput(true);
                    // 锁定日期，可编辑
                    this.getView().setEnable(true , ruwudatefield); // 入伍时间
                    this.getView().setEnable(true , tuiwudatefield); // 退伍时间
                } else {
                    // 锁定日期，不可编辑
                    this.getView().setEnable(false , ruwudatefield); // 入伍时间
                    this.getView().setEnable(false , tuiwudatefield); // 退伍时间
                }
            }
        }

        if ("教育经历".equals(infoGroupName) || 1247809451256209408L == infoGroupId) {
            if (jsonObject.contains("1249297648691749888")) {
                // 毕业院校(旧) 1249297648691749888,标准版自带的字段，某些方法体有用到，不能删除，就隐藏操作
                this.getView().setVisible(false , "field1249297648691749888");
            }
        }

    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String key = e.getProperty().getName();
        // 信息采集协作，基本信息：是否退伍军人：field2004353791935130624，入伍时间：field2004391669838916608，退伍时间：field2004391669838916609
        // nckd_veteran	是否退伍军人,nckd_ruwudate 入伍时间, nckd_tuiwudate	退伍时间
        Map<String, String> fieldkeytofieldidmap = fieldkeytofieldid();
        if(StringUtils.equals(fieldkeytofieldidmap.get("nckd_veteran"), key)) {
            String istuiwu = (String)this.getModel().getValue(fieldkeytofieldidmap.get("nckd_veteran")); // 是否退伍军人:"field2004353791935130624"
            String ruwudatefield = fieldkeytofieldidmap.get("nckd_ruwudate"); // 入伍时间field
            String tuiwudatefield = fieldkeytofieldidmap.get("nckd_tuiwudate"); // 退伍时间field
            if ("YES".equals(istuiwu)) {
                // 选了是退伍军人
                // 前端属性设置（前端判断必填校验）
                DateEdit property1 = (DateEdit) this.getControl(ruwudatefield); // 入伍时间
                property1.setMustInput(true);
                DateEdit property2 = (DateEdit) this.getControl(tuiwudatefield); // 退伍时间
                property2.setMustInput(true);
                // 后端属性设置（后端判断必填校验）
                DateProp Prop1 = (DateProp)this.getModel().getDataEntityType().getProperty(ruwudatefield); // 入伍时间
                Prop1.setMustInput(true);
                DateProp Prop2 = (DateProp)this.getModel().getDataEntityType().getProperty(tuiwudatefield); // 退伍时间
                Prop2.setMustInput(true);
                // 可编辑
                this.getView().setEnable(true, ruwudatefield); // 入伍时间
                this.getView().setEnable(true, tuiwudatefield); // 退伍时间
            } else {
                DateEdit property1 = (DateEdit) this.getControl(ruwudatefield); // 入伍时间
                property1.setMustInput(false);
                DateEdit property2 = (DateEdit) this.getControl(tuiwudatefield); // 退伍时间
                property2.setMustInput(false);

                DateProp Prop1 = (DateProp)this.getModel().getDataEntityType().getProperty(ruwudatefield); // 入伍时间
                Prop1.setMustInput(false);
                DateProp Prop2 = (DateProp)this.getModel().getDataEntityType().getProperty(tuiwudatefield); // 退伍时间
                Prop2.setMustInput(false);
                // 值置空，不可编辑
                this.getModel().setValue(ruwudatefield,null); // 入伍时间
                this.getView().setEnable(false, ruwudatefield); // 入伍时间
                this.getModel().setValue(tuiwudatefield,null); // 退伍时间
                this.getView().setEnable(false, tuiwudatefield); // 退伍时间
            }
        }
        // 学历：field1249297648691749891
        if (StringUtils.equals("field1249297648691749891", key)) {
            FormShowParameter showParameter = this.getView().getFormShowParameter();
            String jsonObject = (String)showParameter.getCustomParam("infoGroupEntity");
            InfoGroupEntity infoGroupEntity = (InfoGroupEntity) JSONObject.parseObject(jsonObject, InfoGroupEntity.class);
            String infoGroupName = infoGroupEntity.getInfoGroupName();
            Long infoGroupId = infoGroupEntity.getInfoGroupId(); // 分组id
            if ("教育经历".equals(infoGroupName) || 1247809451256209408L == infoGroupId) {
                if (jsonObject.contains("1249297648691749888")) {
                    // 毕业院校(旧) 1249297648691749888,标准版自带的字段，某些方法体有用到，不能删除，就隐藏操作
                    this.getView().setVisible(false , "field1249297648691749888");
                }
            }
        }
    }

    @Override
    public void beforeClick(BeforeClickEvent evt) {
        super.beforeClick(evt);
        Object source = evt.getSource();
        if (source instanceof Button) {
            Button button = (Button) source;
            if ("barsave".equals(button.getKey())) {
                FormShowParameter showParameter = this.getView().getFormShowParameter();
                String jsonObject = (String)showParameter.getCustomParam("infoGroupEntity");
                InfoGroupEntity infoGroupEntity = (InfoGroupEntity) JSONObject.parseObject(jsonObject, InfoGroupEntity.class);
                String infoGroupName = infoGroupEntity.getInfoGroupName();
                Long infoGroupId = infoGroupEntity.getInfoGroupId(); // 分组id
                // 当前分组每个字段
                List<InfoGroupEntity.InfoGroupField> infoGroupFieldList = infoGroupEntity.getInfoGroupFieldList();
                int size = infoGroupFieldList.size();
                if ("基本信息".equals(infoGroupName) || 1247661585950874624L == infoGroupId) {
                    // 由于是二开字段，切换环境可能导致field的值不一致，使用字段标识
                    // nckd_veteran	是否退伍军人,nckd_ruwudate 入伍时间, nckd_tuiwudate	退伍时间
                    Map<String, String> fieldkeytofieldid = fieldkeytofieldid();
                    String nckdVeteran = fieldkeytofieldid.get("nckd_veteran");
                    if (StringUtils.isNotBlank(nckdVeteran)) {
                       String istuiwu = (String)this.getModel().getValue(nckdVeteran); // field2004353791935130624
                       if ("YES".equals(istuiwu)) {
                           // 入伍时间 field2004391669838916608
                           Date ruwudate = (Date)this.getModel().getValue(fieldkeytofieldid.get("nckd_ruwudate"));
                           String warnstr = "";
                           if (ruwudate == null) {
                               warnstr = warnstr + "入伍时间、";
                           }
                           // 退伍时间 field2004391669838916609
                           Date tuiwudate = (Date)this.getModel().getValue(fieldkeytofieldid.get("nckd_tuiwudate")); // "field2004391669838916609"
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
                   }
                } else if("教育经历".equals(infoGroupName) || 1247809451256209408L == infoGroupId) {
                    for (int i=0; i < size; i++) {
                        // 循环该页签下的每个字段
                        InfoGroupEntity.InfoGroupField infoGroupField = infoGroupFieldList.get(i);
                        Long fieldKey = infoGroupField.getFieldId();
                        if (1255753217539722240L == fieldKey) {
                            // 毕业证书编号
                            String biye = (String)this.getModel().getValue("field1255753217539722240");
                            if (StringUtils.isEmpty(biye)) {
                                this.getModel().setValue("field1255753217539722240","无");
                            }
                        }
                        if (1249297648691749905L == fieldKey) {
                            // 学位证证书编号
                            String xuewei = (String)this.getModel().getValue("field1249297648691749905");
                            if (StringUtils.isEmpty(xuewei)) {
                                this.getModel().setValue("field1249297648691749905","无");
                            }
                        }
                        if (1408187915879632897L == fieldKey) {
                            // 国外学历学位认证书编号
                            String wai = (String)this.getModel().getValue("field1408187915879632897");
                            if (StringUtils.isEmpty(wai)) {
                                this.getModel().setValue("field1408187915879632897","无");
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 取当前页签页面的字段标识与field的映射
     * @return
     */
    Map<String,String> fieldkeytofieldid() {
        Map map = new HashMap<String,String>();
        FormShowParameter showParameter = this.getView().getFormShowParameter();
        String jsonObject = (String)showParameter.getCustomParam("infoGroupEntity");
        InfoGroupEntity infoGroupEntity = (InfoGroupEntity) JSONObject.parseObject(jsonObject, InfoGroupEntity.class);
        String infoGroupName = infoGroupEntity.getInfoGroupName();
        Long infoGroupId = infoGroupEntity.getInfoGroupId(); // 分组id
        // 当前分组每个字段
        List<InfoGroupEntity.InfoGroupField> infoGroupFieldList = infoGroupEntity.getInfoGroupFieldList();
        int size = infoGroupFieldList.size();
        for (int i=0; i < size; i++) {
            InfoGroupEntity.InfoGroupField infoGroupField = infoGroupFieldList.get(i);
            Long fieldId = infoGroupField.getFieldId(); // 页面实际处理字段id
            String fieldName = infoGroupField.getFieldName(); // 字段名称
            String fieldKey = infoGroupField.getFieldKey();// 字段标识
            map.put(fieldKey,"field" + fieldId);
        }
        return map;
    }

}
