package nckd.yanye.hr.plugin.form.empout;

import kd.bos.dataentity.entity.DynamicObject;
import kd.hr.hspm.business.domian.service.infoclassify.IAttachmentService;
import kd.sdk.hr.hspm.common.result.HrpiServiceOperateResult;
import nckd.yanye.hr.plugin.form.yearkaohe.EmyearkaoheServiceImpl;

import java.util.List;

public interface IEmporgreloutService extends IAttachmentService {

    public static final IEmporgreloutService empproexpService = new EmporgreloutServiceImpl();
    static IEmporgreloutService getInstance() {
        return empproexpService;
    }

    DynamicObject getEmchufeninfoByPkId(Long var1);

    HrpiServiceOperateResult insertEmchufeninfo(DynamicObject var1);

    HrpiServiceOperateResult updateEmchufeninfo(Long var1, DynamicObject var2);

    HrpiServiceOperateResult deleteEmchufeninfo(List<Long> var1);

    HrpiServiceOperateResult saveImportEmchufeninfo(String var1, DynamicObject[] var2);

    List<Long> queryExistsIdByPkIdList(List<Long> var1);
}
