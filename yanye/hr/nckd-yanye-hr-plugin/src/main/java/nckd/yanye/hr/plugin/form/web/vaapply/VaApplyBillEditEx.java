package nckd.yanye.hr.plugin.form.web.vaapply;

import kd.bos.context.RequestContext;
import kd.bos.data.BusinessDataReader;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.db.DB;
import kd.bos.db.DBRoute;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.form.ConfirmCallBackListener;
import kd.bos.form.ConfirmTypes;
import kd.bos.form.MessageBoxOptions;
import kd.bos.form.MessageBoxResult;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.events.MessageBoxClosedEvent;
import kd.hr.hbp.common.util.HRStringUtils;
import kd.hr.hbp.formplugin.web.HRCoreBaseBillEdit;
import nckd.base.common.utils.capp.CappConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 休假申请，标识：nckd_wtabm_vaapplysel_ext
 * 需求：休假申请中对于年假申请，每年申请的次数有限制，组织也有限制
 * author:chengchaohua
 * date:2024-08-30
 */
public class VaApplyBillEditEx  extends HRCoreBaseBillEdit {


    @Override
    public void beforeItemClick(BeforeItemClickEvent evt) {
        super.beforeItemClick(evt);
        String itemKey = evt.getItemKey();
        if (HRStringUtils.equals("bar_submit", itemKey)) {
            // 1.取当前组织
            DynamicObject org =  (DynamicObject)this.getModel().getValue("org");
            String orgnumber = org.getString("number");
            // 2.目前只有 119.01 晶昊本部，121 江西富达盐化有限公司 需要判断请年假的次数
            String orgstrs =  CappConfig.getConfigValue("nianjia_org","119.01,121");
            String[] orgList = orgstrs.split(",");
            for (String orgtemp : orgList) {
                if (orgnumber.equals(orgtemp)) {
                    // start--取“休假信息”分录下，休假类型为‘年假’，开始日期取最小的日期
                    List<String> startList = new ArrayList<>(); // 暂存年假各分录的开始日期
                    DynamicObjectCollection entryRows = this.getModel().getEntryEntity("entryentity"); // “休假信息”分录
                    for (DynamicObject entryObj : entryRows) {
                        DynamicObject typeobj =  (DynamicObject)entryObj.get("entryvacationtype"); // 休假类型
                        // 1030_S : 年假
                        if ("1030_S".equals(typeobj.getString("number"))) {
                            startList.add(entryObj.getString("entrystarttimetext")); // 开始日期
                            if (startList.size() > 0) {
                                // 1)开始日期取最小的日期
                                String mindate = Collections.min(startList);
                                // 取年份
                                int year = Integer.parseInt(mindate.substring(0,4));

                                // 2)获取当前操作人员信息
                                long userId = Long.valueOf(RequestContext.get().getUserId());
                                MainEntityType userDT = EntityMetadataCache.getDataEntityType("bos_user");
                                DynamicObject currUser = BusinessDataReader.loadSingle(userId, userDT);
                                String currgonghao = currUser.getString("number");

                                // 3)设置请假限制次数,每年最多可请年假的请假单单据个数
                                String nianjia_num =  CappConfig.getConfigValue("nianjia_num","4");
                                int xianzhinum = Integer.parseInt(nianjia_num);

                                // 4)查询：某年份下已请了年假的请假单个数
                                String sql2 = " select fstartdate,count(1) num" +
                                        " from (" +
                                        " select a.fbillno,YEAR(MIN(c.fstartdate)) fstartdate" +
                                        " from t_wtabm_vaapply a" +
                                        " inner join t_hrpi_person b on b.fid = a.fpersonid" +
                                        " inner join t_wtabm_vaapplyentry c on c.fid = a.fid" +
                                        " inner join t_wtbd_vacationtype d on d.fid = c.fvacationtypeid" +
                                        " where a.fbillstatus in ('B','D','C') and b.fnumber = '" + currgonghao + "'" +
                                        " and d.fnumber = '1030_S'" +
                                        " group by a.fbillno" +
                                        " ) a" +
                                        " where fstartdate=" + year  +
                                        " group by fstartdate" ;

                                List<Integer> numberlist2 = DB.query(DBRoute.of("hr"), sql2,
                                        resultSet -> {
                                            List<Integer> valuetemp = new ArrayList<Integer>();
                                            while (resultSet.next()) {
                                                valuetemp.add(resultSet.getInt("num"));
                                            }
                                            return valuetemp;
                                        });

                                int num2 = numberlist2.size();
                                if (num2 > 0) {
                                    // 5）当年有请年假的单据个数：numberlist2.get(0)
                                    if ( (xianzhinum - 1) == numberlist2.get(0)) {
                                        // 6）本年最后一次可提交年假的请假单，显示确认消息
                                        ConfirmCallBackListener confirmCallBacks = new ConfirmCallBackListener("yearCount", this);
                                        String confirmTip = "您" + year + "年申请(年假)次数剩本次最后一次，请确认是否继续提交？";
                                        this.getView().showConfirm(confirmTip, MessageBoxOptions.YesNo, ConfirmTypes.Default, confirmCallBacks);
                                        evt.setCancel(true);
                                    } else if (xianzhinum <= numberlist2.get(0)) {
                                        // 7）该年份请年假已经请完了
                                        this.getView().showTipNotification("您" +year + "年申请(年假)次数已满，不能再次申请");
                                        evt.setCancel(true);
                                    }
                                }
                            }
                        }
                    }
                    // end--
                }
            }
        }
    }

    @Override
    public void confirmCallBack(MessageBoxClosedEvent messageBoxClosedEvent) {
        super.confirmCallBack(messageBoxClosedEvent);
        IDataModel model = this.getModel();
        if (StringUtils.equals("yearCount", messageBoxClosedEvent.getCallBackId())) {
            if (messageBoxClosedEvent.getResult() == MessageBoxResult.Yes) {
                // 点击‘是’继续后续操作
                getView().invokeOperation("submit");
            }
        }
    }
}
