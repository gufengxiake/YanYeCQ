package nckd.yanye.hr.plugin.form.empout;

import kd.sdk.hr.hspm.formplugin.infoclassify.InfoClassifyEditPlugin;
import nckd.yanye.hr.plugin.form.yearkaohe.IEmyearkaoheService;

/**
 * 信息批量维护：处分信息-分类维护表单插件，标识：nckd_hspm_yearkaohe
 */
public class EmpPorgrelOutEditPlugin extends InfoClassifyEditPlugin {

    private final IEmporgreloutService empproexpService = IEmporgreloutService.getInstance();

    public EmpPorgrelOutEditPlugin() {

    }


}
