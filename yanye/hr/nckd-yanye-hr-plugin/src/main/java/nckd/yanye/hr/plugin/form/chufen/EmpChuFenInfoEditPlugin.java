package nckd.yanye.hr.plugin.form.chufen;

import kd.bos.dataentity.entity.DynamicObject;
import kd.sdk.hr.hspm.common.dto.InfoClassifyEntityKeyDTO;
import kd.sdk.hr.hspm.formplugin.infoclassify.InfoClassifyAttachmentEditPlugin;
import kd.sdk.hr.hspm.formplugin.infoclassify.InfoClassifyEditPlugin;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 信息批量维护：处分信息-分类维护表单插件，标识：nckd_hspm_chufeninfo
 */
public class EmpChuFenInfoEditPlugin extends InfoClassifyEditPlugin {

    private final IEmchufeninfoService empproexpService = IEmchufeninfoService.getInstance();

    public EmpChuFenInfoEditPlugin() {

    }


}
