package nckd.yanye.hr.plugin.form.web.activity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.form.FormShowParameter;
import kd.hr.hom.common.entity.InfoGroupConfigEntity;
import kd.hr.hpfs.formplugin.privacy.AbstractDesensitizeFieldCommonPlugin;
import kd.sdk.hr.hom.common.InfoGroupEntity;

import java.util.EventObject;

/**
 * 单据标识：nckd_hom_multipleinfo_ext  名称：信息组页面
 * 菜单：PC端 玩美入职-》信息采集-》教育经历 列表查看页面
 * 隐藏标准版的 graduateschool 毕业院校(旧)控件  field1249297648691749888
 * author: chengchaohua
 * date: 2024-09-17
 */
public class AbstractDesensitizeFieldCollectMutiPluginEx extends AbstractDesensitizeFieldCommonPlugin {

    @Override
    public void afterBindData(EventObject e) {
//        super.afterBindData(e);

        FormShowParameter formShowParameter = this.getView().getFormShowParameter();
        String param = ((JSONObject)formShowParameter.getCustomParam("param")).toJSONString();
        InfoGroupEntity infoGroupEntity = (InfoGroupEntity)JSONObject.parseObject(param, InfoGroupEntity.class);
        InfoGroupConfigEntity infoGroupConfigEntity = (InfoGroupConfigEntity)JSONObject.parseObject(((JSONObject)formShowParameter.getCustomParam("config")).toJSONString(), InfoGroupConfigEntity.class);

        JSONArray jsonArray = (JSONArray)formShowParameter.getCustomParam("data");
        String infoGroupName = infoGroupEntity.getInfoGroupName(); // 分组名称，名称可能会人工修改
        Long infoGroupId = infoGroupEntity.getInfoGroupId(); // 分组id
        if ("教育经历".equals(infoGroupName) || 1247809451256209408L == infoGroupId) {
            if (param.contains("1249297648691749888")) {
                // 毕业院校(旧) 1249297648691749888,标准版自带的字段，某些方法体有用到，不能删除，就隐藏操作
                this.getView().setVisible(false , "fieldboard12492976486917498880");
            }
        }
    }
}
