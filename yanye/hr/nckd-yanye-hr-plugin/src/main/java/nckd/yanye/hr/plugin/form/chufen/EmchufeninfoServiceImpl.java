package nckd.yanye.hr.plugin.form.chufen;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.sdk.hr.hspm.business.helper.HpfsChgexternalrecordQueueHelper;
import kd.sdk.hr.hspm.business.service.AttacheHandlerService;
import kd.sdk.hr.hspm.common.dto.InfoClassifyEntityKeyDTO;
import kd.sdk.hr.hspm.common.result.HrpiServiceOperateResult;

import java.util.*;
import java.util.stream.Collectors;

public class EmchufeninfoServiceImpl  implements  IEmchufeninfoService{

    private static final Log LOGGER = LogFactory.getLog(EmchufeninfoServiceImpl.class);
    protected final AttacheHandlerService attacheHandlerService = AttacheHandlerService.getInstance();
    private final EmchufeninfoRepository emchufeninfoRepository = EmchufeninfoRepository.getInstance();

    @Override
    public DynamicObject getEmchufeninfoByPkId(Long var1) {
        return this.emchufeninfoRepository.getEmchufeninfo(var1, "");
    }

    @Override
    public HrpiServiceOperateResult insertEmchufeninfo(DynamicObject var1) {
        // 核心人力云->人员信息->分类维护表单 ,处分信息表单，nckd_hspm_chufeninfo
        InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO = InfoClassifyEntityKeyDTO.getEntityKeyEnumByFormKey("nckd_hspm_chufeninfo");
        Map<String, DynamicObjectCollection> paramMap = new HashMap(16);
        paramMap.put(infoClassifyEntityKeyDTO.getSourceKey(), this.emchufeninfoRepository.getInvokeSaveByEmchufeninfo(var1));
        Map<String, Object> resultMap = this.attacheHandlerService.invokeSaveOrUpdate(paramMap);
        HrpiServiceOperateResult operateResult = HrpiServiceOperateResult.build(resultMap);
        if (operateResult.isSuccess()) {
            Long pkId = (Long)operateResult.getDataMapForIds().get(0);
            var1.set("id", pkId);
            LOGGER.info(String.format(Locale.ROOT, "insertEmchufeninfo the id is %s.", pkId));
            HpfsChgexternalrecordQueueHelper.sendHisNonLineInsertMsg(var1, infoClassifyEntityKeyDTO.getSourceKey(), infoClassifyEntityKeyDTO.getFormKey());
        }

        return operateResult;
    }

    @Override
    public HrpiServiceOperateResult updateEmchufeninfo(Long var1, DynamicObject var2) {
        // 核心人力云->人员信息->分类维护表单  nckd_hspm_chufeninfo  处分信息表单
        InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO = InfoClassifyEntityKeyDTO.getEntityKeyEnumByFormKey("nckd_hspm_chufeninfo");
        Map<String, DynamicObjectCollection> paramMap = new HashMap(16);
        paramMap.put(infoClassifyEntityKeyDTO.getSourceKey(), this.emchufeninfoRepository.getInvokeUpdateByEmchufeninfo(var1, var2));
        Map<String, Object> resultMap = this.attacheHandlerService.invokeSaveOrUpdate(paramMap);
        HrpiServiceOperateResult operateResult = HrpiServiceOperateResult.build(resultMap);
        if (operateResult.isSuccess()) {
            LOGGER.info(String.format(Locale.ROOT, "updateEmchufeninfo the id is %s.", var1));
            HpfsChgexternalrecordQueueHelper.sendHisNonLineUpdateMsg(var2, infoClassifyEntityKeyDTO.getSourceKey(), infoClassifyEntityKeyDTO.getFormKey());
        }

        return operateResult;
    }

    @Override
    public HrpiServiceOperateResult deleteEmchufeninfo(List<Long> var1) {
        // 核心人力云->人员信息->分类维护表单  nckd_hspm_chufeninfo  处分信息表单
        InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO = InfoClassifyEntityKeyDTO.getEntityKeyEnumByFormKey("nckd_hspm_chufeninfo");
        DynamicObject[] dbDyArr = this.emchufeninfoRepository.queryHrpiEmchufeninfoForPerChg(var1);
        Map<String, Object> resultMap = this.attacheHandlerService.invokeDel(var1, infoClassifyEntityKeyDTO.getSourceKey(), Boolean.TRUE);
        HrpiServiceOperateResult.validate(resultMap);
        HrpiServiceOperateResult operateResult = HrpiServiceOperateResult.build(resultMap);
        if (operateResult.isSuccess()) {
            LOGGER.info(String.format(Locale.ROOT, "deleteEmchufeninfo the id is %s.", var1));
            HpfsChgexternalrecordQueueHelper.sendBatchHisNonLineDeleteMsg(dbDyArr, infoClassifyEntityKeyDTO.getSourceKey(), infoClassifyEntityKeyDTO.getFormKey());
        }

        return operateResult;
    }

    @Override
    public HrpiServiceOperateResult saveImportEmchufeninfo(String importtype, DynamicObject[] dataEntities) {
        // 核心人力云->人员信息->分类维护表单  nckd_hspm_chufeninfo  处分信息表单
        InfoClassifyEntityKeyDTO infoClassifyEntityKeyDTO = InfoClassifyEntityKeyDTO.getEntityKeyEnumByFormKey("nckd_hspm_chufeninfo");
        DynamicObject[] importSaveDys = this.emchufeninfoRepository.getImportSaveByEmchufeninfo(dataEntities);
        Map<String, Object> resultMap = this.attacheHandlerService.invokeHisNonLineImportData(importtype, importSaveDys);
        HrpiServiceOperateResult operateResult = HrpiServiceOperateResult.build(resultMap);
        if (operateResult.isSuccess()) {
            LOGGER.info(String.format(Locale.ROOT, "saveImportEmchufeninfo the id is %s.", operateResult.getDataMapForIds()));
            HpfsChgexternalrecordQueueHelper.sendBatchHisNonLineInsertMsg(dataEntities, infoClassifyEntityKeyDTO.getSourceKey(), infoClassifyEntityKeyDTO.getFormKey());
        }

        return operateResult;
    }

    @Override
    public List<Long> queryExistsIdByPkIdList(List<Long> pkIdList) {
        DynamicObject[] existsPkIdArr = this.emchufeninfoRepository.queryByPkIdList(pkIdList);
        return (List)(existsPkIdArr.length > 0 ? (List) Arrays.stream(existsPkIdArr).map((dy) -> {
            return dy.getLong("id");
        }).collect(Collectors.toList()) : new ArrayList());
    }
}
