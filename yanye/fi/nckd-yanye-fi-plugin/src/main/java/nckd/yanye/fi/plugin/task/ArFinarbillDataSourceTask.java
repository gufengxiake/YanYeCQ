package nckd.yanye.fi.plugin.task;

import com.kingdee.bos.util.backport.Arrays;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.earlywarn.EarlyWarnContext;
import kd.bos.entity.earlywarn.warn.plugin.IEarlyWarnDataSource;
import kd.bos.entity.filter.FilterCondition;
import kd.bos.entity.tree.TreeNode;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import java.util.List;
import java.util.Map;


public class ArFinarbillDataSourceTask implements IEarlyWarnDataSource {

    private static final String KEY_BILLTYPE =	"ar_finarbill_BT";

    @Override
    public List<QFilter> buildFilter(String s, FilterCondition filterCondition, EarlyWarnContext earlyWarnContext) {
        QFilter qFilter = new QFilter("billtype.number", QCP.equals,KEY_BILLTYPE)
                .and("billstatus",QCP.equals,"C");
        return Arrays.asList(qFilter.toArray());
    }

    @Override
    public DynamicObjectCollection getData(String s, List<QFilter> list, EarlyWarnContext earlyWarnContext) {
        DynamicObject[] dynamicObjects = BusinessDataServiceHelper.load("ar_finarbill", "id,billno,planentity.planmaterial,planentity.unplansettleamt", new QFilter[]{list.get(0)});
        DynamicObjectCollection dynamicObjectCollection = new DynamicObjectCollection();
        if (dynamicObjects.length > 0){
            dynamicObjectCollection.addAll(Arrays.asList(dynamicObjects));
        }
        return dynamicObjectCollection;
    }

    @Override
    public List<Map<String, Object>> getCommonFilterColumns(String s) {
        return null;
    }

    @Override
    public TreeNode getSingleMessageFieldTree(String s) {
        return new TreeNode();
    }

    @Override
    public TreeNode getMergeMessageFieldTree(String s) {
        return new TreeNode();
    }
}
