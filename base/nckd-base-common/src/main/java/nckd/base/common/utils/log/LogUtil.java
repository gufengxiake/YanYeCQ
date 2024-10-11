package nckd.base.common.utils.log;

import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

import java.util.Date;

/**
 * 调用三方接口日志查询表-新增调用日志
 * 单据标识：nckd_general_log
 *
 * @author ：luxiao
 * @since ：Created in 14:54 2024/10/10
 */
public class LogUtil {
    /**
     * 新增调用日志
     *
     * @param docNumber 调用单据标识
     * @param docName   调用单据名称
     * @param system    调用系统
     * @param url       调用接口url
     * @param request   请求报文
     * @param response  返回报文
     */
    public static void newApiLog(String docNumber, String docName, String system, String url, String request, String response) {
        DynamicObject apiLog = BusinessDataServiceHelper.newDynamicObject("nckd_general_log");
        // 调用单据标识
        apiLog.set("number", docNumber);
        // 调用单据名称
        apiLog.set("name", docName);
        // 数据状态：已审核
        apiLog.set("status", "C");
        // 调用人
        apiLog.set("creator", RequestContext.get().getCurrUserId());
        // 使用状态：可用
        apiLog.set("enable", "1");
        // 调用系统
        apiLog.set("nckd_system", system);
        // 调用接口url
        apiLog.set("nckd_interfaceurl", url);
        // 调用接口时间
        apiLog.set("createtime", new Date());
        // 请求报文
        apiLog.set("nckd_parameter", request);
        // 返回报文
        apiLog.set("nckd_returnparameter", response);

        SaveServiceHelper.save(new DynamicObject[]{apiLog});
    }
}
