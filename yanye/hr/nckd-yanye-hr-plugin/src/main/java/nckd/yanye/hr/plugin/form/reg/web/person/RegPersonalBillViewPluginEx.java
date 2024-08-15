package nckd.yanye.hr.plugin.form.reg.web.person;

import com.alibaba.fastjson.JSONObject;
import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.EntityMetadataCache;
import kd.bos.entity.datamodel.events.BizDataEventArgs;
import kd.bos.orm.util.CollectionUtils;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.hr.hbp.business.servicehelper.HRMServiceHelper;
import kd.hr.hbp.formplugin.web.HRDynamicFormBasePlugin;
import kd.hr.hdm.business.reg.RegPeronalBillHelper;
import kd.hr.hdm.business.reg.RegProcessServiceHelper;
import kd.hr.hdm.business.reg.domain.service.bill.IPersonAboutService;
import kd.hr.hdm.common.reg.enums.RegDateUnitEnum;
import kd.hr.hdm.common.transfer.util.ObjectUtils;
import kd.hr.hdm.formplugin.reg.web.person.RegPersonalBillViewPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * author:tangyuxuan
 * date:2024-08-15
 */
public class RegPersonalBillViewPluginEx extends RegPersonalBillViewPlugin {

    @Override
    public void createNewData(BizDataEventArgs e) {
        super.createNewData(e);
        Object entity = e.getDataEntity();
        Long employeeId = this.getCustomId("employee");
        String regularStatus = RegProcessServiceHelper.getRegStatusByEmployee(employeeId);
        this.getView().getPageCache().put("regularStatus", regularStatus);
        this.getView().setStatus(OperationStatus.VIEW);
        Long erManFileId = this.getCustomId("ermanfile");
        List<Long> erManFileIdList = new ArrayList();
        erManFileIdList.add(erManFileId);
        List ermanfileList = (List) HRMServiceHelper.invokeHRService("hspm", "IHSPMService", "getCardFields", new Object[]{erManFileIdList});
        if (!CollectionUtils.isEmpty(ermanfileList)) {
            //Map<String, Object> ermanfile = (Map)ermanfileList.get(0);
            IPersonAboutService personAboutService = IPersonAboutService.getInstance();
            DynamicObject ermanfile = BusinessDataServiceHelper.loadSingle(erManFileId, "hspm_ermanfile");

            Map<String, Object> regReturnMap = personAboutService.buildRegInfo(ermanfile);

            if (!ObjectUtils.isEmpty(regReturnMap)) {

                /*  nckd_isshixiqi 是否有实习期
                    nckd_hetongshiyong 合同约定试用期时长
                    nckd_perprobationtime 单位
                    nckd_shixidikou 实习期时长（可抵扣试用期
                    nckd_perprobationtimedk 单位
                */
                ((DynamicObject)entity).set("nckd_isshixiqi", regReturnMap.get("nckd_isshixiqi"));
                ((DynamicObject)entity).set("nckd_shixidikou_a", buildProbationText(regReturnMap.get("nckd_hetongshiyong"),regReturnMap.get("nckd_perprobationtime")));
                ((DynamicObject)entity).set("nckd_probationtime_a", buildProbationText(regReturnMap.get("nckd_shixidikou"), regReturnMap.get("nckd_perprobationtimedk")));
            }

        }

    }

    private Long getCustomId(String paramKey) {
        Map<String, Object> customParams = this.getView().getFormShowParameter().getCustomParams();
        Object param = customParams.get(paramKey);
        return null != param ? Long.valueOf(param.toString()) : 0L;
    }

    /**
     * 合并试用期字段和单位字段
     * @param value 值
     * @param unit 单位
     */
    private String buildProbationText(Object value, Object unit) {
        String returnStr ;
        if (ObjectUtils.isEmpty(unit)) {
            returnStr = null;
        } else {
            String name = RegDateUnitEnum.getName((String)unit);
            returnStr = value.toString() + name;
        }
        return returnStr;

    }


}
