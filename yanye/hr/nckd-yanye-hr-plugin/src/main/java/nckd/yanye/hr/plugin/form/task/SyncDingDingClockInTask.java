package nckd.yanye.hr.plugin.form.task;

import com.alibaba.fastjson.JSONArray;
import kd.bos.context.RequestContext;
import kd.bos.exception.KDException;
import kd.bos.schedule.executor.AbstractTask;
import nckd.yanye.hr.common.utils.ClockInApiUtil;

import java.util.Map;

/**
 * 钉钉打卡同步任务
 *
 * @author liuxiao
 */
public class SyncDingDingClockInTask extends AbstractTask {

    @Override
    public void execute(RequestContext requestContext, Map<String, Object> map) throws KDException {

    }
}
