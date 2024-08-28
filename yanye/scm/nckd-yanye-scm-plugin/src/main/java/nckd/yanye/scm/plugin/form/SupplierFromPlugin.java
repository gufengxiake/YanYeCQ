package nckd.yanye.scm.plugin.form;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.operate.FormOperate;
import kd.bos.form.plugin.AbstractFormPlugin;
import nckd.yanye.scm.common.SupplierConst;
import nckd.yanye.scm.common.utils.ZcPlatformApiUtil;

import java.util.HashMap;

/**
 * 供应商新增添加招采id
 * 单据编码：nckd_bd_supplier_ext
 *
 * @author ：luxiao
 * @since ：Created in 11:46 2024/8/26
 */
public class SupplierFromPlugin extends AbstractFormPlugin {

    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        FormOperate formOperate = (FormOperate) args.getSource();
        String operateKey = formOperate.getOperateKey();

        // 保存或提交时根据社会统一代码获取招采平台供应商id
        if ("save".equals(operateKey) || "submit".equals(operateKey)) {
            String societycreditcode = (String) this.getModel().getValue(SupplierConst.SOCIETYCREDITCODE);
            //获取招采平台供应商列表
            JSONArray allSuppliers = ZcPlatformApiUtil.getAllZcSupplier();
            //遍历招采平台供应商列表，对应社会统一代码与id
            HashMap<String, String> supplierMap = new HashMap<>();
            for (int i = 0; i < allSuppliers.size(); i++) {
                JSONObject zcSupplier = allSuppliers.getJSONObject(i);
                String socialCreditCode = zcSupplier.getString("socialCreditCode");
                String companyId = zcSupplier.getString("companyId");
                supplierMap.put(socialCreditCode, companyId);
            }
            supplierMap.put(null, null);
            this.getModel().setValue(SupplierConst.NCKD_PLATFORMSUPID, supplierMap.get(societycreditcode));
            args.setCancel(false);

        }
    }


}
