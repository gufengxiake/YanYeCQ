package nckd.yanye.hr.plugin.form.yearkaohe;

import kd.bos.dataentity.entity.LocaleString;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.form.field.ComboItem;
import kd.sdk.hr.hspm.common.dto.InfoClassifyEntityKeyDTO;
import kd.sdk.hr.hspm.opplugin.InfoclassifyImportPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmyearkaoheImportPlugin extends InfoclassifyImportPlugin {

    public EmyearkaoheImportPlugin() {

    }

    public String getDefaultImportType() {
        // 核心人力云->人员信息->分类维护表单 ,年度考核信息，nckd_hspm_yearkaohe
        return InfoClassifyEntityKeyDTO.getEntityKeyEnumByFormKey("nckd_hspm_yearkaohe").getDefaultImportType();
    }

    public List<String> getDefaultLockUIs() {
        // 核心人力云->人员信息->分类维护表单 ,年度考核信息，nckd_hspm_yearkaohe
        return InfoClassifyEntityKeyDTO.getEntityKeyEnumByFormKey("nckd_hspm_yearkaohe").getDefaultLockUIs();
    }

    public List<ComboItem> getOverrideFieldsConfig() {
        Map<String, LocaleString> itemMap = new HashMap(16);
        itemMap.put("boid", null);
        itemMap.put("person", new LocaleString(ResManager.loadKDString("工号", "PercontactImportPlugin_0", "hr-hspm-opplugin", new Object[0])));
        return this.getOverrideFieldsConfig(itemMap);
    }

    public String getDefaultKeyFields() {
        return "boid,person";
    }

    public String getBillFormId() {
        // 核心人力云->人员信息->分类维护表单 ,年度考核信息，nckd_hspm_yearkaohe
        return InfoClassifyEntityKeyDTO.getEntityKeyEnumByFormKey("nckd_hspm_yearkaohe").getFormKey();
    }

}