package nckd.yanye.occ.plugin.Convert;

import kd.mpscmm.msbd.botp.BillUnitConvertPlugin;
import kd.mpscmm.msbd.common.context.CommonContext;
/**
 *
 * 场景：针对渠道的单位转换场景
 * 1 目前标品主要考虑的是以采销起单作为核心单据，如果下推的是渠道单据，则配置标品插件可以直接支持到渠道的相关单位及数量的转换
 * 2 如果核心单据为渠道侧的单据，则不支持。
 * 主要针对场景2进行客开说明：
 *
 * 说明：
 * 1 本类是一个demo，继承BillUnitConvertPlugin类来实现对核心单据信息上下文的重写
 *
 * 2 kd.mpscmm.msbd.botp.BillUnitConvertPlugin 是供应链中台插件
 * 如果是渠道在上游要调用，必须追加对mpscmm-msbd.zip相关jar包的依赖
 *
 * 3 重写getMainBillContext方法，在上下文中重写核心单据对应的字段标识
 *
 * 4 UnitConvertPluginMainBillConstant类为上下文的常量类，直接使用对应的常量即可
 *
 */
public class BillUnitConvertExtPlugin extends BillUnitConvertPlugin {
    @Override
    protected CommonContext getMainBillContext(String mainBillEntity) {
        CommonContext context = super.getMainBillContext(mainBillEntity);
        context.getProperytMapping().put("MAINBILL_UNITKEY", "unit");
        //context.getProperytMapping().put("MAINBILL_UNITKEY", "subentryentity.sub_unitid");
        return context;
    }
}