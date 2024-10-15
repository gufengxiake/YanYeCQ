package nckd.yanye.hr.plugin.form.web.position;


import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.BeginOperationTransactionArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import kd.hr.hbp.common.util.HRStringUtils;
import kd.hr.hbp.opplugin.web.HRCoreBaseBillOp;
import org.apache.commons.lang3.ObjectUtils;
import java.util.*;

/**
 * Module           :组织发展云 -岗位信息维护
 *  @Description     :岗位维护确认变更，同步修改人员档案中的排序码，标识：hrpi_perregion
 *
 *
 * @author:guozhiwei
 * @date:2024-09-24
 */



public class PostitonBillChangPlugin extends HRCoreBaseBillOp {



    public void beginOperationTransaction(BeginOperationTransactionArgs args) {
        super.beginOperationTransaction(args);
    }

    public void afterExecuteOperationTransaction(AfterOperationArgs e) {

        String changeType = this.getOption().getVariableValue("changetype", "propertychange");
        if (HRStringUtils.equals(changeType, "propertychange")) {

            // 比较上次排序号和本次排序号，如果有变化，则同步修改人员档案中的排序码，标识
            DynamicObject[] positionBills = e.getDataEntities();
            List<String> numberList = new ArrayList<>();
            for (DynamicObject positionBill : positionBills) {
                numberList.add(positionBill.getString("number"));
            }
            QFilter qFilter = new QFilter("number", QCP.in, numberList);
            QFilter qFilter1 = new QFilter("status", QCP.equals, "C")
                    .and("enable", QCP.equals, "1");
            DynamicObject[] homsPositionbills = BusinessDataServiceHelper.load("homs_positionbill", "id,number,iscurrentversion,nckd_sortnum,nckd_lastsortnum,bsed", new QFilter[]{qFilter,qFilter1}, "number,hisnewversion desc");
            List<DynamicObject> homsPositionbillsList = new ArrayList<>();
            Set<String> seenNumbers = new HashSet<>(); // 用于追踪已添加的 number

            Arrays.stream(homsPositionbills)
                    .forEach(dynamicObject -> {
                        String number = dynamicObject.getString("number");
                        // 检查该 number 是否已经存在于 Set 中
                        if (!seenNumbers.contains(number)) {
                            seenNumbers.add(number); // 添加到 Set 以避免重复
                            homsPositionbillsList.add(dynamicObject); // 添加到 List
                        }
                    });

            // 比较上次排序号和本次排序号，如果有变化，则同步修改人员档案中的排序码，标识
            for (DynamicObject positionBill : homsPositionbillsList) {
                int nckdSortnum = positionBill.getInt("nckd_sortnum");

                int nckdLastsortnum = (Integer) ObjectUtils.defaultIfNull(positionBill.get("nckd_lastsortnum"), 0);

                if(nckdSortnum != nckdLastsortnum){

                    // // 生效日期在当前时间之前，则更新排序码
                    if(!new Date().before(positionBill.getDate("bsed"))){
                        // 获取人员 hrpi_empposorgrel 任职经历表
                        QFilter qFilter5 = new QFilter("position.number", QCP.equals, positionBill.getString("number"));
                        QFilter qFilter6 = new QFilter("datastatus", QCP.equals, "1")
                                .and("businessstatus", QCP.equals, "1");
                        DynamicObject[] hrpiEmpposorgrels = BusinessDataServiceHelper.load("hrpi_empposorgrel", "id,datastatus,person.id,position,position.number,nckd_personindex", new QFilter[]{qFilter5,qFilter6},"bred");
                        if(ObjectUtils.isNotEmpty(hrpiEmpposorgrels)){
                            Arrays.stream(hrpiEmpposorgrels)
                                    .forEach(dynamicObject -> {
                                        // 更新需要的字段
                                        dynamicObject.set("nckd_personindex", nckdSortnum);
                                    });
                            // 更新上次排序号
                            positionBill.set("nckd_lastsortnum", nckdSortnum);
                            SaveServiceHelper.update(hrpiEmpposorgrels);
                        }
                        SaveServiceHelper.update(positionBill);


                    }else{
                        break;
                    }
                }
            }
        }
    }



}
