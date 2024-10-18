package nckd.yanye.hr.plugin.form.task;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.exception.KDException;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.schedule.executor.AbstractTask;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.hr.hbp.common.util.DatePattern;
import kd.hr.hbp.common.util.DateUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Module           :组织发展云 -岗位信息维护
 *  @Description     :岗位申请单 定时任务-更新任职经历基础页面 nckd_hrpi_empposorgre_ext的 排序号
 *
 *
 * @author:guozhiwei
 * @date:2024-10-8 10：30
 */


public class SyncPostTionBillTask extends AbstractTask {

    private static Log logger = LogFactory.getLog(SyncPostTionBillTask.class);

    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {
        this.syncPosition();
    }


    private void syncPosition() {

        //  获取需要更新的岗位申请单，
        // 生效日期大于昨天最大的生效日期，并且状态为生效

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(date);
        Date date2 = DateUtils.stringToDate(dateStr, DatePattern.YYYY_MM_DD);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date2);

        // 计算前一天
        calendar.add(Calendar.DATE, -1); // 减去一天
        Date previousDate = calendar.getTime();

        // 计算后一天
        calendar.setTime(date2); // 重置为 date2
        calendar.add(Calendar.DATE, 1); // 加一天
        Date nextDate = calendar.getTime();


        QFilter qFilter = new QFilter("datastatus", QCP.equals, "1")
                .and("iscurrentversion", QCP.equals, "1")
                .and("modifytime", QCP.large_than, previousDate)
                .and("bsed", QCP.less_than, nextDate);

        DynamicObject[] homsPositionbills = BusinessDataServiceHelper.load("homs_positionbill", "id,number,bsed,nckd_sortnum,nckd_lastsortnum,modifytime", new QFilter[]{qFilter},"modifytime asc");


        // 使用流过滤出 nckd_sortnum 和 nckd_lastsortnum 不相等的数据
        List<DynamicObject> filteredList = Arrays.stream(homsPositionbills)
                .filter(dynamicObject -> {
                    Integer nckdSortNum = dynamicObject.getInt("nckd_sortnum");
                    Integer nckdLastSortNum = dynamicObject.getInt("nckd_lastsortnum");

                    // 如果两个都是 null，返回 false
                    if (nckdSortNum == null && nckdLastSortNum == null) {
                        return false;
                    }
                    // 如果一个为 null，直接返回另外一个的比较结果
                    if (nckdSortNum == null || nckdLastSortNum == null) {
                        return true; // 如果其中一个为 null，视为不相等
                    }
                    // 两者都不为 null，进行正常的比较
                    return !nckdSortNum.equals(nckdLastSortNum);
                })
                .collect(Collectors.toList());
        if(ObjectUtils.isNotEmpty(filteredList)){
            logger.info("共找到 " + filteredList.size() + " 个岗位申请单需要更新排序号");
            logger.info("开始更新排序号,filteredList:{}", filteredList);

            filteredList.forEach(positionBill -> {

                Object nckdSortnum = positionBill.get("nckd_sortnum");
                // 获取人员 hrpi_empposorgrel 任职经历表
                QFilter qFilter5 = new QFilter("position.number", QCP.equals, positionBill.getString("number"));
                QFilter qFilter6 = new QFilter("datastatus", QCP.equals, "1")
//                        .and("datastatus", QCP.equals, "1")
                        .and("businessstatus", QCP.equals, "1");
                DynamicObject[] hrpiEmpposorgrels = BusinessDataServiceHelper.load("hrpi_empposorgrel", "id,datastatus,person.id,position,position.number,nckd_personindex", new QFilter[]{qFilter5,qFilter6});
                if(ObjectUtils.isNotEmpty(hrpiEmpposorgrels)){
                    Arrays.stream(hrpiEmpposorgrels)
                            .forEach(dynamicObject -> {
                                // 更新需要的字段
                                dynamicObject.set("nckd_personindex", nckdSortnum);
                            });
                    // 更新上次排序号
                    SaveServiceHelper.update(hrpiEmpposorgrels);
                }
                positionBill.set("nckd_lastsortnum", nckdSortnum);
                SaveServiceHelper.update(positionBill);
            });

        }

    }

}
