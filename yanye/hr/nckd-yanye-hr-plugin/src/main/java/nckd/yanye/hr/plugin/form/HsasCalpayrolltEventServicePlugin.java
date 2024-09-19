package nckd.yanye.hr.plugin.form;

import com.alibaba.fastjson.JSONObject;
import kd.bos.bec.api.IEventServicePlugin;
import kd.bos.bec.model.EntityEvent;
import kd.bos.bec.model.KDBizEvent;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
/**
 * Module           :薪酬福利云-薪资核算-薪酬发放
 * Description      :薪资审批单生成后概览添加到备注
 * nckd_hsas_approvebill_ext
 * @author : yaosijie
 * @date : 2024/9/19
 */
public class HsasCalpayrolltEventServicePlugin implements IEventServicePlugin {

    private static Log logger = LogFactory.getLog(HsasCalpayrolltEventServicePlugin.class);

    @Override
    public Object handleEvent(KDBizEvent evt) {
        if (evt instanceof EntityEvent) {
            //薪资审批单id
            List<String> businesskeys = ((EntityEvent) evt).getBusinesskeys();
            logger.info("薪资审批单id：{}",JSONObject.toJSONString(businesskeys));
            //查询薪资审批单
            DynamicObject hsasApprovebillObject = BusinessDataServiceHelper.loadSingle(businesskeys.get(0), "hsas_approvebill");
            if (Objects.nonNull(hsasApprovebillObject)){
                //审批单(hsas_approvebill)分录calentryentity
                DynamicObjectCollection hsasApprovebill = hsasApprovebillObject.getDynamicObjectCollection("calentryentity");
                //总人数
                int num = hsasApprovebill.size();

                //核算名单id 集合 calpersonid
                List<Long> calpersonid = hsasApprovebill.stream().map(t->t.getLong("calpersonid")).collect(Collectors.toList());

                //核算名单 hsas_calperson
                QFilter hsasCalpersonqFilter = new QFilter("id", QCP.in,calpersonid);
                DynamicObject[] objects = BusinessDataServiceHelper.load("hsas_calperson", "id,caltask,calresultid", new QFilter[]{hsasCalpersonqFilter});

                //薪资核算任务hsas_calpayrolltask
                QFilter hsasCalpayrolltaskqFilter = new QFilter("id",QCP.in, Arrays.stream(objects).map(t->t.getLong("caltask.id")).collect(Collectors.toList()));
                DynamicObject[] objects2 = BusinessDataServiceHelper.load("hsas_calpayrolltask", "id,calrulev", new QFilter[]{hsasCalpayrolltaskqFilter});

                //计算规则hsas_calrule
                QFilter hsasCalruleqFilter = new QFilter("id",QCP.in, Arrays.stream(objects2).map(t->t.getLong("calrulev.id")).collect(Collectors.toList()));
                DynamicObject[] objects3 = BusinessDataServiceHelper.load("hsas_calrule", "id,totalsalary,netsalary", new QFilter[]{hsasCalruleqFilter});


                //薪酬项目id（总薪资）
                List<Long> caltaskcalrulevtotalsalaryid = Arrays.stream(objects3).map(t->t.getLong("totalsalary.id")).collect(Collectors.toList());

                //薪酬项目id（净薪资）
                List<Long> caltaskcalrulevnetsalaryid = Arrays.stream(objects3).map(t->t.getLong("netsalary.id")).collect(Collectors.toList());


                //核算结果id
                List<Long> calresultid = Arrays.stream(objects).map(t->t.getLong("calresultid")).collect(Collectors.toList());

                QFilter calresultQfilter = new QFilter("id",QCP.in,calresultid);
                //核算列表
                DynamicObject[] hsasCaltableObjects = BusinessDataServiceHelper.load("hsas_caltable", "id,hsas_caltableentry.salaryitem,hsas_caltableentry.calamountvalue", new QFilter[]{calresultQfilter});

                //核算列表分录
                DynamicObjectCollection dynamicObjectCollection = new DynamicObjectCollection();
                DynamicObjectCollection dynamicObjectCollection1 = new DynamicObjectCollection();
                for (DynamicObject dynamicObject : Arrays.asList(hsasCaltableObjects)){
                    dynamicObjectCollection.addAll(dynamicObject.getDynamicObjectCollection("hsas_caltableentry").stream().filter(t -> caltaskcalrulevtotalsalaryid.contains(t.getLong("salaryitem.id")))
                            .collect(Collectors.toList()));
                    dynamicObjectCollection1.addAll(dynamicObject.getDynamicObjectCollection("hsas_caltableentry").stream().filter(t -> caltaskcalrulevnetsalaryid.contains(t.getLong("salaryitem.id")))
                            .collect(Collectors.toList()));
                }
                //总薪资合计
                BigDecimal salaryTotal = dynamicObjectCollection.stream().map(t->t.getBigDecimal("calamountvalue")).reduce(BigDecimal.ZERO,BigDecimal::add);

                //净薪资合计
                BigDecimal netWorthTotal = dynamicObjectCollection1.stream().map(t->t.getBigDecimal("calamountvalue")).reduce(BigDecimal.ZERO,BigDecimal::add);
                System.out.println();
                //备注赋值
                hsasApprovebillObject.set("description","概览：总人数："+num+"，总薪资合计："+salaryTotal.setScale(2)+"，净薪资合计："+netWorthTotal.setScale(2));
                SaveServiceHelper.update(hsasApprovebillObject);

            }
        }
        return null;
    }

}
