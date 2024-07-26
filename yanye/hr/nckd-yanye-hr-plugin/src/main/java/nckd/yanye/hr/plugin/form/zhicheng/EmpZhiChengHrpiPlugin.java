package nckd.yanye.hr.plugin.form.zhicheng;

import kd.bos.bill.OperationStatus;
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
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.ComboEdit;
import kd.bos.form.field.DateEdit;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.sdk.plugin.Plugin;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

/**
 *核心人力云->人员信息-》附表弹框
 * 人员档案，职称信息，页面编码: nckd_hspm_perprotitl_ext3
 * 2024-07-26
 * chengchaohua
 */
public class EmpZhiChengHrpiPlugin extends AbstractFormPlugin implements Plugin {

    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
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
            DateEdit apiaddressProperty2 = (DateEdit)this.getControl("nckd_pinrenenddaten");
            apiaddressProperty2.setMustInput(false);
            DateProp prop2 = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenenddaten");
            prop2.setMustInput(false);

            // 隐藏：聘任单位
            this.getView().setVisible(false , "nckd_pinrenorg");
            BasedataEdit apiaddressProperty3 = (BasedataEdit)this.getControl("nckd_pinrenorg");
            apiaddressProperty3.setMustInput(false);
            BasedataProp prop3 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
            prop2.setMustInput(false);
        }

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
            DateEdit apiaddressProperty2 = (DateEdit)this.getControl("nckd_pinrenenddaten");
            apiaddressProperty2.setMustInput(true);
            DateProp prop2 = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenenddaten");
            prop2.setMustInput(true);
            // 显示：聘任单位
            this.getView().setVisible(true , "nckd_pinrenorg");
            BasedataEdit apiaddressProperty3 = (BasedataEdit)this.getControl("nckd_pinrenorg");
            apiaddressProperty3.setMustInput(true);
            BasedataProp prop3 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
            prop2.setMustInput(true);
        }else {
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
            DateEdit apiaddressProperty2 = (DateEdit)this.getControl("nckd_pinrenenddaten");
            apiaddressProperty2.setMustInput(false);
            DateProp prop2 = (DateProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenenddaten");
            prop2.setMustInput(false);

            // 隐藏：聘任单位
            this.getView().setVisible(false , "nckd_pinrenorg");
            BasedataEdit apiaddressProperty3 = (BasedataEdit)this.getControl("nckd_pinrenorg");
            apiaddressProperty3.setMustInput(false);
            BasedataProp prop3 = (BasedataProp)this.getModel().getDataEntityType().getProperty("nckd_pinrenorg");
            prop2.setMustInput(false);
        }
    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String fieldKey = e.getProperty().getName();
        IDataModel model = this.getModel();
        // 1)
        if(StringUtils.equals("nckd_type", fieldKey)) {
            // 类型 值切换
            String nckd_type = (String)model.getValue("nckd_type");
            // 当“类型”字段选择码值为“职称”(zhicheng)后，展示“是否公司聘任”该字段，
            if("zhicheng".equals(nckd_type)) {
                // 显示“是否公司聘任”该字段
                this.getView().setVisible(true , "nckd_ispinren");
            } else {
                // 隐藏,“是否公司聘任”该字段
                this.getView().setVisible(false , "nckd_ispinren");
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
        // 3)
        if(StringUtils.equals("nckd_ispinren", fieldKey)) {
            // 是否公司聘任 值切换
            Boolean nckd_iszuigao = (Boolean)model.getValue("nckd_ispinren");
            if(nckd_iszuigao) {
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
                // 隐藏：聘任日期
//                this.getView().setVisible(false , "nckd_pinrendate");
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
    public void beforeItemClick(BeforeItemClickEvent evt) {
        super.beforeItemClick(evt);
    }

    @Override
    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        super.afterDoOperation(afterDoOperationEventArgs);
        if("do_save".equals(afterDoOperationEventArgs.getOperateKey())) {
            // 个人仅允许保存一条最高职称和一条最高职业技能，当开关开启后，原来最高的那条职称/职业技能的“是否最高”开关自动关闭
            IDataModel model = this.getModel();
            Boolean nckd_iszuigao = (Boolean)model.getValue("nckd_iszuigao");
            if(nckd_iszuigao) {
                DBRoute hr = new DBRoute("hr");
                OperationStatus status = this.getView().getFormShowParameter().getStatus();

                /*Long id = 0L;
                String idstr = (String)this.getView().getFormShowParameter().getCustomParam("pkid");
                if (idstr == null) {
                    // 新增时，查最新的一笔记录
                    String query = "select fid from t_hrpi_perprotitle where fdatastatus='1' and fiscurrentversion='1' and fpersonid=1966668644033235968 order by fcreatetime desc";
                    List<Long> cardIds = DB.query(hr, query,
                            resultSet -> {
                                List<Long> valuetemp = new ArrayList<Long>();
                                while (resultSet.next()) {
                                    valuetemp.add(resultSet.getLong(1));
                                }
                                return valuetemp;
                            });

                    if (cardIds.size() > 0) {
                        id = cardIds.get(0); // 当前记录fid值
                    }

                } else {
                    // 当前记录fid值
                    id = Long.parseLong(idstr);
                }*/
                Long personid =(Long) this.getView().getFormShowParameter().getCustomParam("person");
                // 类型
                String nckd_type = (String)model.getValue("nckd_type");

                // 该人全部记录更新为非最高
                SqlBuilder builder = new SqlBuilder();
                builder.append("UPDATE t_hrpi_perprotitle SET fk_nckd_iszuigao='0' WHERE fiscurrentversion='1' and fpersonid = ? and fk_nckd_iszuigao='1' and fk_nckd_type = ?", personid, nckd_type);

                boolean execute = DB.execute(hr, builder);
//                // 将本次记录更新为最高
//                SqlBuilder builder2 = new SqlBuilder();
//                builder2.append("UPDATE t_hrpi_perprotitle SET fk_nckd_iszuigao='1' WHERE fid = ? ", id);
//                int j = DB.update(hr, builder2);
//                model.getValue("nckd_type");
            }
        }
    }
}
