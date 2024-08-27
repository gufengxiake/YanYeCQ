package nckd.yanye.hr.common.utils.capp;

import java.util.Date;
import kd.bos.context.RequestContext;
import kd.bos.log.api.AppLogInfo;
import kd.bos.servicehelper.log.LogServiceHelper;

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
