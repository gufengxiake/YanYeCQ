package nckd.yanye.hr.plugin.form.wanmei;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.entity.datamodel.events.ChangeData;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.entity.property.BasedataProp;
import kd.bos.entity.property.ComboProp;
import kd.bos.entity.property.DecimalProp;
import kd.bos.form.field.*;
import kd.bos.servicehelper.user.UserServiceHelper;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.EventObject;

/**
 * 核心人力云-》玩美入职-》入职信息
 * 入职办理单 nckd_hom_onbrdinfo_ext，页面类型: 单据
 * date:2024-07-24
 * author:chengchaohua
 */
public class OnbrdinfoErKaiPlugin  extends AbstractBillPlugIn {

    @Override
    public void beforeBindData(EventObject e) {
        super.beforeBindData(e);
        // 1)设置必填标识
        // 是否有实习期
        FieldEdit isshixiqi = (FieldEdit) this.getControl("nckd_isshixiqi");
        isshixiqi.setMustInput(true);
        // 2)根据开关，对字段进行显隐和必入标识
        IDataModel model = this.getModel();
        // 是否有实习期 nckd_isshixiqi
        Boolean nckd_isshixiqi = (Boolean)model.getValue("nckd_isshixiqi");
        if(nckd_isshixiqi){
            // 2.1）如果有实习期，实习期时长（可抵扣试用期）nckd_shixidikou 和单位 nckd_perprobationtimedk 设置为必录和可编辑
            // 前端属性设置
            DecimalEdit nckd_shixidikouProperty = (DecimalEdit) this.getControl("nckd_shixidikou");
            nckd_shixidikouProperty.setMustInput(true);
            // 后端属性设置
            DecimalProp prop = (DecimalProp)this.getModel().getDataEntityType().getProperty("nckd_shixidikou");
            prop.setMustInput(true);
            // 解锁
            this.getView().setEnable(true, "nckd_shixidikou");

            // 前端属性设置
            ComboEdit nckd_perprobationtimedkProperty = (ComboEdit) this.getControl("nckd_perprobationtimedk");
            nckd_perprobationtimedkProperty.setMustInput(true);
            // 后端属性设置
            ComboProp prop2 = (ComboProp)this.getModel().getDataEntityType().getProperty("nckd_perprobationtimedk");
            prop2.setMustInput(true);
            // 解锁
            this.getView().setEnable(true, "nckd_perprobationtimedk");

        } else {
            // 2.2）如果无实习期，实习期时长（可抵扣试用期）nckd_shixidikou 和单位 nckd_perprobationtimedk 设置为必录和可编辑
            // 前端属性设置
            DecimalEdit nckd_shixidikouProperty = (DecimalEdit) this.getControl("nckd_shixidikou");
            nckd_shixidikouProperty.setMustInput(false);
            // 后端属性设置
            DecimalProp prop = (DecimalProp)this.getModel().getDataEntityType().getProperty("nckd_shixidikou");
            prop.setMustInput(false);
            // 锁定
            this.getView().setEnable(false, "nckd_shixidikou");

            // 前端属性设置
            ComboEdit nckd_perprobationtimedkProperty = (ComboEdit) this.getControl("nckd_perprobationtimedk");
            nckd_perprobationtimedkProperty.setMustInput(false);
            // 后端属性设置
            ComboProp prop2 = (ComboProp)this.getModel().getDataEntityType().getProperty("nckd_perprobationtimedk");
            prop2.setMustInput(false);
            // 锁定
            this.getView().setEnable(false, "nckd_perprobationtimedk");
        }
    }

    // 新增时
    @Override
    public void afterCreateNewData(EventObject e) {
        super.afterCreateNewData(e);
        DynamicObject workcalendar = (DynamicObject)this.getModel().getValue("workcalendar");
        if (workcalendar == null) {
            // 工作日历
            IDataModel model = this.getModel();
            model.setItemValueByNumber("workcalendar","001");
        }

        // 获取当前登录人id
        long currentUserId = UserServiceHelper.getCurrentUserId();
        this.getModel().setValue("handler", currentUserId);

        // 入职日期 effectdatebak,入职地点 onbrdtcitybak,增加必录标识。备份的字段 没有带表字段
        DateEdit apiaddressProperty1 = (DateEdit)this.getControl("effectdatebak");
        apiaddressProperty1.setMustInput(true);
        BasedataEdit apiaddressProperty2 = (BasedataEdit)this.getControl("onbrdtcitybak");
        apiaddressProperty2.setMustInput(true);

    }

    @Override
    public void propertyChanged(PropertyChangedArgs e) {
        super.propertyChanged(e);
        String fieldKey = e.getProperty().getName();
        IDataModel model = this.getModel();
        // 入职日期的值
        Date effectdate = (Date)model.getValue("effectdate");
        // 是否有实习期的值
        Boolean nckd_isshixiqi = (Boolean)model.getValue("nckd_isshixiqi");
        // 1）开始-是否有实习期 nckd_isshixiqi 值变更
        if(StringUtils.equals("nckd_isshixiqi", fieldKey)) {
            if(nckd_isshixiqi) {
                // 1.1）如果有实习期，实习期时长（可抵扣试用期）nckd_shixidikou 和单位 nckd_perprobationtimedk 设置为必录和可编辑
                // 需要‘入职日期’不为空，有值
                if (effectdate == null) {
                    ChangeData changeSet = e.getChangeSet()[0];
                    // ‘入职日期’为空,不允许选择‘有实习期’
                    model.setValue("nckd_isshixiqi",changeSet.getOldValue());
                    this.getView().showMessage("开启‘实习期’，请先填入‘入职日期’，用于‘实习期抵扣后试用期时长’计算使用。");
                   return;
                }
                // 前端属性设置
                DecimalEdit nckd_shixidikouProperty = (DecimalEdit) this.getControl("nckd_shixidikou");
                nckd_shixidikouProperty.setMustInput(true);
                // 后端属性设置
                DecimalProp prop = (DecimalProp)this.getModel().getDataEntityType().getProperty("nckd_shixidikou");
                prop.setMustInput(true);
                // 解锁
                this.getView().setEnable(true, "nckd_shixidikou");

                // 前端属性设置
                ComboEdit nckd_perprobationtimedkProperty = (ComboEdit) this.getControl("nckd_perprobationtimedk");
                nckd_perprobationtimedkProperty.setMustInput(true);
                // 后端属性设置
                ComboProp prop2 = (ComboProp)this.getModel().getDataEntityType().getProperty("nckd_perprobationtimedk");
                prop2.setMustInput(true);
                // 解锁
                this.getView().setEnable(true, "nckd_perprobationtimedk");

            } else {
                // 1.2）如果无实习期，实习期时长（可抵扣试用期）nckd_shixidikou 和单位 nckd_perprobationtimedk 设置为必录和可编辑
                // 前端属性设置
                DecimalEdit nckd_shixidikouProperty = (DecimalEdit) this.getControl("nckd_shixidikou");
                nckd_shixidikouProperty.setMustInput(false);
                // 后端属性设置
                DecimalProp prop = (DecimalProp)this.getModel().getDataEntityType().getProperty("nckd_shixidikou");
                prop.setMustInput(false);
                // 锁定
                this.getView().setEnable(false, "nckd_shixidikou");

                // 前端属性设置
                ComboEdit nckd_perprobationtimedkProperty = (ComboEdit) this.getControl("nckd_perprobationtimedk");
                nckd_perprobationtimedkProperty.setMustInput(false);
                // 后端属性设置
                ComboProp prop2 = (ComboProp)this.getModel().getDataEntityType().getProperty("nckd_perprobationtimedk");
                prop2.setMustInput(false);
                // 锁定
                this.getView().setEnable(false, "nckd_perprobationtimedk");

                // 1.3)关闭了‘实习期’，对“实习期时长（可抵扣试用期）”nckd_shixidikou值设置为值为0
                model.setValue("nckd_shixidikou",0);
                // 重新计算“实习期抵扣后试用期时长"
                if (effectdate == null) {
                    // 如果‘入职日期’为空，‘预转正日期’也为空，走标准版逻辑，不手工处理‘预转正日期’
                } else {
                    // ‘入职日期’不为空，更新“实习期抵扣后试用期时长"，把‘合同约定试用期时长’值和单位都 赋值给 ‘实习期抵扣后试用期时长’
                    model.setValue("probationtime",model.getValue("nckd_hetongshiyong"));
                    model.setValue("perprobationtime",model.getValue("nckd_perprobationtime")); // 单位
                }
            }

        }
        // 结束：1）是否有实习期 nckd_isshixiqi 值变更

        // 2) 入职日期 effectdate 值有变化时
        if(StringUtils.equals("effectdate", fieldKey)) {
            if (effectdate == null) {
                if (nckd_isshixiqi) {
                    ChangeData changeSet = e.getChangeSet()[0];
                    // 已开启‘有实习期’，‘入职日期’不能为空
                    model.setValue("effectdate",changeSet.getOldValue()); // 页面未显示日期
                    this.getView().showMessage("已开启‘实习期’，‘入职日期’不能为空。");
                    return;
                }
            } else {
                // 重新计算‘实习期抵扣后试用期时长’值和单位
                if (nckd_isshixiqi) {
                    // 已开启‘是否有实习期’ 的计算
                    chongxinjisuandk ();
                } else {
                    // 未开启‘是否有实习期’ 的计算
                    chongxinjisuandk2 ();
                }
            }
        }

        // 3) 合同约定试用期时长 nckd_hetongshiyong 值有变化时
        if(StringUtils.equals("nckd_hetongshiyong", fieldKey)) {
            // 重新计算‘实习期抵扣后试用期时长’值和单位
            if (nckd_isshixiqi) {
                // 已开启‘是否有实习期’ 的计算
                chongxinjisuandk ();
            } else {
                // 未开启‘是否有实习期’ 的计算
                chongxinjisuandk2 ();
            }
        }
        // 3.2) 合同约定试用期时长 的单位 nckd_perprobationtime 值有变化时
        if(StringUtils.equals("nckd_perprobationtime", fieldKey)) {
            if (nckd_isshixiqi) {
                // 已开启‘是否有实习期’ 的计算
                chongxinjisuandk ();
            } else {
                // 未开启‘是否有实习期’ 的计算
                chongxinjisuandk2 ();
            }
        }

        // 4) 实习期时长（可抵扣试用期） nckd_shixidikou 值有变化时
        if(StringUtils.equals("nckd_shixidikou", fieldKey)) {
            if (effectdate == null) {
                ChangeData changeSet = e.getChangeSet()[0];
                // ‘入职日期’为空,不允许选择‘有实习期’
                model.setValue("nckd_shixidikou",changeSet.getOldValue()); // 值回到更新前
                this.getView().showMessage("修改‘实习期时长（可抵扣试用期）’，请先填入‘入职日期’，用于‘实习期抵扣后试用期时长’计算使用。");
                return;
            } else {
                // 已开启‘是否有实习期’ 的计算
                chongxinjisuandk ();
            }
        }

        // 4.2) 实习期时长（可抵扣试用期）的单位 nckd_perprobationtimedk 值有变化时
        if(StringUtils.equals("nckd_perprobationtimedk", fieldKey)) {
            if (effectdate == null) {
                ChangeData changeSet = e.getChangeSet()[0];
                // ‘入职日期’为空,不允许选择‘有实习期’
                model.setValue("nckd_shixidikou",changeSet.getOldValue()); // 值回到更新前
                this.getView().showMessage("修改‘实习期时长（可抵扣试用期）的单位’，请先填入‘入职日期’，用于‘实习期抵扣后试用期时长’计算使用。");
                return;
            } else {
                // 已开启‘是否有实习期’ 的计算
                chongxinjisuandk ();
            }
        }

    }

    // 重新计算‘实习期抵扣后试用期时长’值和单位（选择：没有实习期）
    void chongxinjisuandk2 () {
        IDataModel model = this.getModel();
        // '合同约定试用期时长' 和 ‘实习期抵扣后试用期时长’ 的值和单位一致
        model.setValue("probationtime",model.getValue("nckd_hetongshiyong"));  // 实习期抵扣后试用期时长
        model.setValue("perprobationtime",model.getValue("nckd_perprobationtime")); // 实习期抵扣后试用期时长，单位
    }

    // 重新计算‘实习期抵扣后试用期时长’值和单位（选择：有实习期）
    void chongxinjisuandk () {
        IDataModel model = this.getModel();
        // 入职日期
        Date effectdate = (Date)model.getValue("effectdate");
        // 合同约定试用期时长 和 实习期时长（可抵扣试用期） 单位是否一致
        if (model.getValue("nckd_perprobationtime").equals(model.getValue("nckd_perprobationtimedk"))) {
            // 单位一致，时长直相减
            // 实习期抵扣后试用期时长 = 合同约定试用期时长 - 实习期时长（可抵扣试用期）
            int temp1 = 0;
            if (model.getValue("nckd_hetongshiyong") != null) {
                temp1 = (int)model.getValue("nckd_hetongshiyong");
            }

            int temp2 = 0;
            if (model.getValue("nckd_shixidikou") != null) {
                temp2 =  (int)model.getValue("nckd_shixidikou");
            }
            int jisuan = temp1 - temp2;
            model.setValue("probationtime",jisuan);
            model.setValue("perprobationtime",model.getValue("nckd_perprobationtime")); // 实习期抵扣后试用期时长，单位
        } else {
            // 合同约定试用期时长 和 实习期时长（可抵扣试用期） 单位不一致，统一按天来计算
            // 1）入职日期 + 合同约定试用期时长 得到一个新日期A
            int temp1 = 0;
            if (model.getValue("nckd_hetongshiyong") != null) {
                temp1 = (int)model.getValue("nckd_hetongshiyong");
            }
            LocalDate localDateA = newDateAdd(effectdate, (String) model.getValue("nckd_perprobationtime"), temp1);
            // 2)新日期A - 实习期时长（可抵扣试用期）,得到一个新日期B
            int temp2 = 0;
            if (model.getValue("nckd_shixidikou") != null) {
                temp2 = (int)model.getValue("nckd_shixidikou");
            }
            LocalDate localDateB = newDateJian(localDateA, (String) model.getValue("nckd_perprobationtimedk"), temp2);
            // 3) 新日期B - 入职日期 = 相差天数
            long daysBetween = ChronoUnit.DAYS.between(dateToLocalDate(effectdate), localDateB);
            // 4) 给‘实习期抵扣后试用期时长’赋值
            model.setValue("probationtime",(int)daysBetween); // 实习期抵扣后试用期时长
            model.setValue("perprobationtime","3"); // 实习期抵扣后试用期时长，单位设置为：天
        }
    }


    /**
     * 原始日期增加一段时间，计算出新日期
     * @param startDate 开始日期
     * @param danwei 单位（1：月，2：周，3：天）
     * @param num 数量
     * @return
     */
    LocalDate newDateAdd(Date startDate, String danwei, int num) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = sdf.format(startDate);
        String[] parts = formattedDate.split("-");
        int year = Integer.parseInt(parts[0]); // 年
        int month = Integer.parseInt(parts[1]); // 月
        int day = Integer.parseInt(parts[2]); // 日
        LocalDate originalDate = LocalDate.of(year, month, day);
        LocalDate newDate = null;
        if ("1".equals(danwei)) {
            // 增加的单位为月
            newDate = originalDate.plusMonths(num);

        } else if ("2".equals(danwei)) {
            // 增加的单位为周
            newDate = originalDate.plusWeeks(num);
        } else if ("3".equals(danwei)) {
            // 增加的单位为天
            newDate = originalDate.plusDays(num);
        }

        return newDate;
    }

    /**
     * 原始日期减少一段时间，计算出新日期
     * @param startDate 开始日期
     * @param danwei 单位（1：月，2：周，3：天）
     * @param num 数量
     * @return
     */
    LocalDate newDateJian(LocalDate startDate, String danwei, int num) {
        LocalDate newDate = null;
        if ("1".equals(danwei)) {
            // 增加的单位为月
            newDate = startDate.minusMonths(num);
        } else if ("2".equals(danwei)) {
            // 增加的单位为周
            newDate = startDate.minusWeeks(num);
        } else if ("3".equals(danwei)) {
            // 增加的单位为天
            newDate = startDate.minusDays(num);
        }
        return newDate;
    }

    // Date日期类型转LocalDate日期类型
    LocalDate dateToLocalDate(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = sdf.format(date);
        String[] parts = formattedDate.split("-");
        int year = Integer.parseInt(parts[0]); // 年
        int month = Integer.parseInt(parts[1]); // 月
        int day = Integer.parseInt(parts[2]); // 日
        return LocalDate.of(year, month, day);
    }

    // LocalDate日期类型转Date日期类型
    Date localDateToDate(LocalDate date) {
        // 使用系统默认时区，并且时间是当天的午夜0点
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zonedDateTime = date.atStartOfDay(zoneId);
        // 将 ZonedDateTime 转换为 Date
        return Date.from(zonedDateTime.toInstant());
    }

}
