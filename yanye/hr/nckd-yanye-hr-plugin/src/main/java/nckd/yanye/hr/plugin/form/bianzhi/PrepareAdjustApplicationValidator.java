package nckd.yanye.hr.plugin.form.bianzhi;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.IDataEntityProperty;
import kd.bos.entity.ExtendedDataEntity;
import kd.bos.entity.MainEntityType;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.validate.AbstractValidator;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.servicehelper.DispatchServiceHelper;
import kd.bos.servicehelper.MetadataServiceHelper;
import kd.hr.haos.business.service.staff.externalInterface.bean.StaffBo;
import kd.hr.haos.business.service.staff.externalInterface.bean.StaffUseOrgBo;
import kd.hr.hbp.common.mservice.HRMServiceResult;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Module           :HR中控服务云-HR基础组织-人力编制-编制调整申请审核校验
 * Description      :编制调整申请操作校验
 *
 * @author guozhiwei
 * @date  2024/9/13 10：40
 * 标识 nckd_preadjustapplic
 */

public class PrepareAdjustApplicationValidator extends AbstractOperationServicePlugIn {


    private static Log logger = LogFactory.getLog(PrepareAdjustApplicationValidator.class);

    @Override
    public void onPreparePropertys(PreparePropertysEventArgs e) {
        super.onPreparePropertys(e);
        // 提前加载表单里的字段
        List<String> fieldKeys = e.getFieldKeys();
        MainEntityType dt = MetadataServiceHelper.getDataEntityType("nckd_preadjustapplic");
        Map<String, IDataEntityProperty> fields = dt.getAllFields();
        fields.forEach((Key, value) -> {
            fieldKeys.add(Key);
        });
    }



    @Override
    public void onAddValidators(AddValidatorsEventArgs e) {
        super.onAddValidators(e);
        e.addValidator(new AbstractValidator() {
            @Override
            public void validate() {
                ExtendedDataEntity[] dataEntities = this.getDataEntities();
                for (ExtendedDataEntity dataEntity : dataEntities) {
                    DynamicObject dataEntityObj = dataEntity.getDataEntity();
                    boolean flag = false;
                    // 判断是否为上周单据
                    Long staffid = (Long) dataEntityObj.getDynamicObject("nckd_haos_staff").getPkValue();
                    Object[] objects3 = new Object[1];
                    objects3[0] = staffid;
                    String errorMsg = null;
                    // 获取编制信息
                    HRMServiceResult haosStaffResponse =  DispatchServiceHelper.invokeService("kd.hrmp.haos.servicehelper","haos","IStaffExternalService","queryStaffById",objects3);
                    logger.info("调用查询服务获取编制信息返回结果："+haosStaffResponse);
                    if("success".equals(haosStaffResponse.getReturnCode())){
                        // 调用成功
                        StaffBo returnData = (StaffBo) haosStaffResponse.getReturnData();
                        List<StaffUseOrgBo> useOrgEntryBoList = returnData.getUseOrgEntryBoList();
                        // 获取循环单据体数据，找到编制中匹配的数据，然后进行
                        DynamicObjectCollection entryentityCols = dataEntityObj.getDynamicObjectCollection("nckd_bentryentity");
                        // 使用流构建一个Map<Long,DynamicObject>
                        Map<Long, DynamicObject> resultMap = Arrays.stream(entryentityCols.toArray(new DynamicObject[0]))
                                .collect(Collectors.toMap(
                                        obj -> (Long) obj.get("nckd_adminorg.id"),
                                        obj -> {
                                            return obj;
                                        }
                                ));
                        useOrgEntryBoList.stream().forEach(useOrgEntryBo -> {
                            DynamicObject dynamicObject = resultMap.get(useOrgEntryBo.getAdminOrgBoId());
                            if(ObjectUtils.isNotEmpty(dynamicObject)){
                                // 含上级的编制人数
                                Object nckdAdjustlatenum = dynamicObject.get("nckd_adjustlatenum");
                                // 直属人数
                                Object nckdRelbdirectnum = dynamicObject.get("nckd_relbdirectnum");
                                if(!StringUtils.equals("A", (String) dynamicObject.get("nckd_lowermost"))){
                                    // 更新组织编制人数
                                    useOrgEntryBo.setYearStaffNumWithSub(ObjectUtils.isNotEmpty(nckdAdjustlatenum) ? (Integer) nckdAdjustlatenum : null);
                                    useOrgEntryBo.setYearStaff(ObjectUtils.isNotEmpty(nckdRelbdirectnum) ? (Integer) nckdRelbdirectnum : null);
                                }else{
                                    useOrgEntryBo.setYearStaff(ObjectUtils.isNotEmpty(nckdRelbdirectnum) ? (Integer) nckdRelbdirectnum : null);
                                }
                                // 更新岗位编制人数,如果不存在岗位则跳过
                                DynamicObjectCollection nckdCentryentity = dynamicObject.getDynamicObjectCollection("nckd_centryentity");
                                if(ObjectUtils.isNotEmpty(nckdCentryentity)){
                                    Map<Long, DynamicObject> centrMap = Arrays.stream(nckdCentryentity.toArray(new DynamicObject[0]))
                                            .collect(Collectors.toMap(
                                                    obj -> (Long) obj.get("nckd_cdutyworkrole.id"),
                                                    obj -> {
                                                        return obj;
                                                    }
                                            ));
                                    useOrgEntryBo.getPositionDimensionBoList().stream().forEach(positionDimensionBo -> {
                                        DynamicObject centerDynamicObject = centrMap.get(positionDimensionBo.getKeyFieldId());
                                        if(ObjectUtils.isNotEmpty(centerDynamicObject)){
                                            Object nckdPostadjustlatenum = centerDynamicObject.get("nckd_postadjustlatenum");
                                            positionDimensionBo.setYearStaff(ObjectUtils.isNotEmpty(nckdPostadjustlatenum) ? (int) nckdPostadjustlatenum : null);
                                        }
                                    });
                                }
                            }
                        });
                        returnData.setUseOrgEntryBoList(useOrgEntryBoList);
                        Object[] objects = new Object[1];
                        objects[0] = returnData;
                        // 调用更新服务

                        // 调用校验服务
                        HRMServiceResult haosValidateResult = DispatchServiceHelper.invokeService("kd.hrmp.haos.servicehelper","haos","IStaffExternalService","validateStaff",objects);
                        logger.info("调用校验服务返回结果："+haosValidateResult);
                        if(!"success".equals(haosValidateResult.getReturnCode())){
                            errorMsg = haosValidateResult.getMessage();
                        }
                    }else{
                        errorMsg = haosStaffResponse.getMessage();
                    }
                    // 判断是否在本周内
                    if (StringUtils.isNotEmpty(errorMsg)) {
                        this.addErrorMessage(dataEntity, "单据" + dataEntityObj.getString("billno") +","+ errorMsg);
                    }
                }
            }
        });
    }

}
