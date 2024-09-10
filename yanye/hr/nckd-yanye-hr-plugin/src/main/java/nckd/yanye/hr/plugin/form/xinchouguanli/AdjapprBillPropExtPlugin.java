package nckd.yanye.hr.plugin.form.xinchouguanli;

import kd.sdk.swc.hcdm.business.extpoint.salarystd.IHcdmContrastPropExtPlugin;
import kd.sdk.swc.hcdm.business.extpoint.salarystd.event.ContrastPropLoadEvent;
import kd.sdk.swc.hcdm.common.stdtab.ContrastPropConfigEntity;

import java.util.List;
import java.util.Map;

/**
 * 员工定调薪申请单-插件
 * 单据标识：nckd_hcdm_adjapprbill_ext
 *
 * @author liuxiao
 * @since 2024/08/19
 */
public class AdjapprBillPropExtPlugin implements IHcdmContrastPropExtPlugin {

    /**
     * 当进行标准表匹配（调用匹配接口）、对照属性取数（自主查询）时，
     * 会进入该方法。可以根据候选人id来加载人员身上的对照属性的值
     *
     * @param e
     */
    @Override
    public void loadContrastPropValue(ContrastPropLoadEvent e) {
        IHcdmContrastPropExtPlugin.super.loadContrastPropValue(e);
        List<ContrastPropConfigEntity> propCfg = e.getPropCfgList();
        List<Long> fileIds = e.getAdjFileIdList();
        boolean isVersion = e.isVersion();
        Map<Long, Map<Long, Object>> propValues = e.getPropValues();
        for (ContrastPropConfigEntity cfg : propCfg) {
            String name = cfg.getName();
            String number = cfg.getNumber();
        }


    }
}
