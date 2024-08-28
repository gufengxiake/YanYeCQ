package nckd.base.common.utils.capp;

import kd.bos.context.RequestContext;
import kd.bos.log.api.AppLogInfo;
import kd.bos.servicehelper.log.LogServiceHelper;

import java.util.Date;


/**
 * 系统日志--自定义开发
 * author: chengchaohua
 * date: 2024-08-27
 */
public class CappLogUtil
{
  public static void cappOperationLog(String operationName, String opDescription, String bizObjID) {
    Long userId = Long.valueOf(RequestContext.get().getUserId());
    AppLogInfo appLogInfo = new AppLogInfo();

    appLogInfo.setUserID(userId);

    appLogInfo.setOrgID(Long.valueOf(RequestContext.get().getOrgId()));

    appLogInfo.setOpTime(new Date());

    appLogInfo.setOpName(operationName);

    appLogInfo.setOpDescription(opDescription);

    appLogInfo.setBizObjID(bizObjID);

    appLogInfo.setBizAppID("1J9X9OLBSOQK");
    appLogInfo.setClientType(RequestContext.get().getClient());
    appLogInfo.setClientIP(RequestContext.get().getLoginIP());
    LogServiceHelper.addLog(appLogInfo);
  }
}
