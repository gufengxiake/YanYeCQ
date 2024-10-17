package nckd.yanye.hr.plugin.form.hesuanmingdan;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.hr.hbp.common.constants.HRBaseConstants;
import kd.sdk.swc.hsas.business.extpoint.calperson.IAddCalPersonExtService;
import kd.sdk.swc.hsas.common.events.calperson.AfterAddCalpersonEvent;

/**
 * @author husheng
 * @date 2024-10-16 13:44
 * @description 业务扩展插件-对核算名单（nckd_hsas_calperson_ext）二开新增字段自动赋值
 */
public class CalPersonEmpposorgrelExtService implements IAddCalPersonExtService {

    @Override
    public void afterPackagePerson(AfterAddCalpersonEvent event) {
        //从动态对象中取出自然人id person.id
        DynamicObjectCollection calPersons = event.getCalPersons();

        //往动态对象里面塞值
        calPersons.stream().forEach(calPerson -> {
            QFilter qFilter = new QFilter(HRBaseConstants.ID, QFilter.equals, calPerson.getLong("empposorgrelhrv"));
//                    .and("iscurrentversion", QCP.equals, "1")
//                    .and("datastatus", QCP.equals, "1");
            // 计薪人员任职经历
            DynamicObject empposorgrelhr = BusinessDataServiceHelper.loadSingle("hsas_empposorgrelhr", qFilter.toArray());

            if(empposorgrelhr != null){
                QFilter filter = new QFilter("position", QFilter.equals, empposorgrelhr.getDynamicObject("position").getLong("id"))
                        .and("person.number",QCP.equals,empposorgrelhr.getDynamicObject("person").getString("number"))
                        .and("iscurrentversion", QCP.equals, "1")
                        .and("datastatus", QCP.equals, "1");
                // 任职经历基础页面
                DynamicObject empposorgrel = BusinessDataServiceHelper.loadSingle("hrpi_empposorgrel", filter.toArray());

                if(empposorgrel != null){
                    // 干部类型
                    calPerson.set("nckd_ganbutype", empposorgrel.getDynamicObject("nckd_ganbutype"));
                    // 职级
                    calPerson.set("nckd_zhiji", empposorgrel.getDynamicObject("nckd_zhiji"));
                }
            }
        });
    }
}
