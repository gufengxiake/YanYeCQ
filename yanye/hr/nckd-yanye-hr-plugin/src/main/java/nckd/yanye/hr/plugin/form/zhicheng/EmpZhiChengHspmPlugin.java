package nckd.yanye.hr.plugin.form.zhicheng;

import kd.bos.base.AbstractBasePlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.db.DB;
import kd.bos.db.DBRoute;
import kd.bos.db.SqlBuilder;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.BasedataProp;
import kd.bos.entity.property.DateProp;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.field.*;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.sdk.plugin.Plugin;

import java.util.Date;
import java.util.EventObject;

/**
 * 基础资料插件
 * 核心人力云->人员信息->分类维护表单 信息批量处理
 * 职称信息表单 nckd_hspm_perprotitle_ext
 * 2024-07-26
 * chengchaohua
 */
public class EmpZhiChengHspmPlugin extends AbstractBasePlugIn implements Plugin {

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
    }

    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
        // 是否最高设置必填
        FieldEdit nckd_iszuigao = (FieldEdit) this.getControl("nckd_iszuigao");
        nckd_iszuigao.setMustInput(true);
        // 是否公司聘任设置必填
        FieldEdit nckd_ispinreny = (FieldEdit) this.getControl("nckd_ispinren");
        nckd_ispinreny.setMustInput(true);
        IDataModel model = this.getModel();
        String nckd_type = (String)model.getValue("nckd_type");
        // 当“类型”字段选择码值为“职称”(zhicheng)后，展示“是否公司聘任”该字段，
        if("zhicheng".equals(nckd_type)) {
            // 显示“是否公司聘任”该字段
            this.getView().setVisible(true , "nckd_ispinren");
        } else {
            // 隐藏,“是否公司聘任”该字段
            this.getView().setVisible(false , "nckd_ispinren");
            // 隐藏：聘任单位
            this.getView().setVisible(false , "nckd_pinrenorg");
            // 隐藏：聘任日期
            this.getView().setVisible(false , "nckd_pinrendate");
            // API地址设置为非必填，页面上的必填和数据校验的必填中去掉
            DateEdit apiaddressProperty = (DateEdit)this.getControl("nckd_pinrendate");
            apiaddressProperty.setMustInput(false);
            DateProp prop = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrendate");
            prop.setMustInput(false);

            // 隐藏：聘任终止日期
            this.getView().setVisible(false , "nckd_pinrenenddaten");

            // 隐藏：聘任单位
            this.getView().setVisible(false , "nckd_pinrenorg");
            BasedataEdit apiaddressProperty3 = (BasedataEdit)this.getControl("nckd_pinrenorg");
            apiaddressProperty3.setMustInput(false);
            BasedataProp prop3 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
            prop3.setMustInput(false);
        }

        // “是否公司聘任”字段
        Boolean nckd_ispinren = (Boolean)model.getValue("nckd_ispinren");
        if(nckd_ispinren) {
            // 显示：聘任日期
            this.getView().setVisible(true , "nckd_pinrendate");
            DateEdit apiaddressProperty = (DateEdit)this.getControl("nckd_pinrendate");
            apiaddressProperty.setMustInput(true);
            DateProp prop = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrendate");
            prop.setMustInput(true);
            // 显示：聘任终止日期
            this.getView().setVisible(true , "nckd_pinrenenddaten");
            // 显示：聘任单位
            this.getView().setVisible(true , "nckd_pinrenorg");
            BasedataEdit apiaddressProperty3 = (BasedataEdit)this.getControl("nckd_pinrenorg");
            apiaddressProperty3.setMustInput(true);
            BasedataProp prop3 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
            prop3.setMustInput(true);
        }else {
            // 隐藏：聘任单位
            this.getView().setVisible(false , "nckd_pinrenorg");
            BasedataEdit apiaddressProperty3 = (BasedataEdit)this.getControl("nckd_pinrenorg");
            apiaddressProperty3.setMustInput(false);
            BasedataProp prop3 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
            prop3.setMustInput(false);
            // 隐藏：聘任日期
            this.getView().setVisible(false , "nckd_pinrendate");
            // 页面上的必填和数据校验的必填中去掉
            DateEdit apiaddressProperty = (DateEdit)this.getControl("nckd_pinrendate");
            apiaddressProperty.setMustInput(false);
            DateProp prop = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrendate");
            prop.setMustInput(false);

            // 隐藏：聘任终止日期
            this.getView().setVisible(false , "nckd_pinrenenddaten");

        }

    }

    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        super.beforeItemClick(evt);
        if (StringUtils.equals("saveandrelease", evt.getItemKey())) {

        }

    }

    @Override
        public void propertyChanged(PropertyChangedArgs e) {
        String fieldKey = e.getProperty().getName();
        IDataModel model = this.getModel();
        // 1) 类型：nckd_type 职称，职业技能
        if(StringUtils.equals("nckd_type", fieldKey)) {
            // 类型 值切换
            String nckd_type = (String)model.getValue("nckd_type");
            // 当“类型”字段选择码值为“职称”(zhicheng)后，展示“是否公司聘任”该字段，
            if("zhicheng".equals(nckd_type)) {
                // 1.1)职称,显示“是否公司聘任”该字段
                this.getView().setVisible(true , "nckd_ispinren");
            } else {
                // 1.2)职业技能
                // 置空4个值
                this.getModel().setValue("nckd_ispinren",false); // 是否公司聘任,不选
                this.getModel().setValue("nckd_pinrendate",null); // 聘任日期
                this.getModel().setValue("nckd_pinrenenddaten",null); // 聘任终止日期
                this.getModel().setValue("nckd_pinrenorg",null); // 聘任单位
                // 隐藏,“是否公司聘任”该字段
                this.getView().setVisible(false , "nckd_ispinren");
                // 隐藏：聘任日期
                this.getView().setVisible(false , "nckd_pinrendate");
                DateEdit apiaddressProperty = (DateEdit)this.getControl("nckd_pinrendate");
                apiaddressProperty.setMustInput(false);
                DateProp prop = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrendate");
                prop.setMustInput(false);

                // 隐藏：聘任终止日期
                this.getView().setVisible(false , "nckd_pinrenenddaten");

                // 隐藏：聘任单位
                this.getView().setVisible(false , "nckd_pinrenorg");
                BasedataEdit apiaddressProperty3 = (BasedataEdit)this.getControl("nckd_pinrenorg");
                apiaddressProperty3.setMustInput(false);
                BasedataProp prop3 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
                prop3.setMustInput(false);
            }
        }
       // 2)
        if(StringUtils.equals("nckd_iszuigao", fieldKey)) {
            // 是否最高 值切换
            Boolean nckd_iszuigao = (Boolean)model.getValue("nckd_iszuigao");
            if(nckd_iszuigao) {
                this.getView().showMessage("该职称/职业技能登记将保存为最高职称/职业技能");
            }
        }
        // 3)是否公司聘任
        if(StringUtils.equals("nckd_ispinren", fieldKey)) {
            // 是否公司聘任 值切换
            Boolean nckd_ispinren = (Boolean)model.getValue("nckd_ispinren");
            if(nckd_ispinren) {
                // 显示：聘任单位
                this.getView().setVisible(true , "nckd_pinrenorg");
                BasedataEdit apiaddressPropertyp1 = (BasedataEdit)this.getControl("nckd_pinrenorg");
                apiaddressPropertyp1.setMustInput(true);
                BasedataProp propp1 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
                propp1.setMustInput(true);
                // 显示：聘任日期
                this.getView().setVisible(true , "nckd_pinrendate");
                DateEdit apiaddressProperty = (DateEdit)this.getControl("nckd_pinrendate");
                apiaddressProperty.setMustInput(true);
                DateProp prop = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrendate");
                prop.setMustInput(true);
                // 显示：聘任终止日期
                this.getView().setVisible(true , "nckd_pinrenenddaten");
                DateEdit apiaddressProperty2 = (DateEdit)this.getControl("nckd_pinrenenddaten");
                apiaddressProperty2.setMustInput(true);
                DateProp prop2 = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenenddaten");
                prop2.setMustInput(true);
            }else {
                // 置空3个值
                this.getModel().setValue("nckd_pinrendate",null); // 聘任日期
                this.getModel().setValue("nckd_pinrenenddaten",null); // 聘任终止日期
                this.getModel().setValue("nckd_pinrenorg",null); // 聘任单位
                // 隐藏：聘任日期
                this.getView().setVisible(false , "nckd_pinrendate");
                // API地址设置为非必填，页面上的必填和数据校验的必填中去掉
                DateEdit apiaddressProperty = (DateEdit)this.getControl("nckd_pinrendate");
                apiaddressProperty.setMustInput(false);
                DateProp prop = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrendate");
                prop.setMustInput(false);

                // 隐藏：聘任终止日期
                this.getView().setVisible(false , "nckd_pinrenenddaten");
                DateEdit apiaddressProperty2 = (DateEdit)this.getControl("nckd_pinrenenddaten");
                apiaddressProperty2.setMustInput(false);
                DateProp prop2 = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenenddaten");
                prop2.setMustInput(false);

                // 隐藏：聘任单位
                this.getView().setVisible(false , "nckd_pinrenorg");
                BasedataEdit apiaddressProperty3 = (BasedataEdit)this.getControl("nckd_pinrenorg");
                apiaddressProperty3.setMustInput(false);
                BasedataProp prop3 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
                prop3.setMustInput(false);
            }
        }
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        super.afterDoOperation(afterDoOperationEventArgs);
        if("save".equals(afterDoOperationEventArgs.getOperateKey()) || "update".equals(afterDoOperationEventArgs.getOperateKey())) {
            // 个人仅允许保存一条最高职称和一条最高职业技能，当开关开启后，原来最高的那条职称/职业技能的“是否最高”开关自动关闭
            IDataModel model = this.getModel();
            Boolean nckd_iszuigao = (Boolean)model.getValue("nckd_iszuigao");
            if(nckd_iszuigao) {
                Long id  = (Long)model.getValue("id");
                DynamicObject person = (DynamicObject)model.getValue("person");
                Long personid =(Long) person.getPkValue();
                // 类型
                String nckd_type = (String)model.getValue("nckd_type");

                // 该人全部记录更新为非最高
                SqlBuilder builder = new SqlBuilder();
                builder.append("UPDATE t_hrpi_perprotitle SET fk_nckd_iszuigao='0' WHERE fiscurrentversion='1' and fpersonid = ? and fk_nckd_iszuigao='1' and fk_nckd_type = ?", personid, nckd_type);
                DBRoute hr = new DBRoute("hr");
                boolean execute = DB.execute(hr, builder);
                // 将本次记录更新为最高
                SqlBuilder builder2 = new SqlBuilder();
                builder2.append("UPDATE t_hrpi_perprotitle SET fk_nckd_iszuigao='1' WHERE fid = ? ", id);
                int j = DB.update(hr, builder2);
                model.getValue("nckd_type");
            }
        }

    }
}