package nckd.yanye.occ.plugin.report;

import java.util.ArrayList;
import java.util.List;
import kd.bos.algo.DataSet;
import kd.bos.algo.Row;

/**
 * author:zhangzhilong
 * date:2024/09/04
 */
public class DataSetToList {
    public static List<Long> getOneToList(DataSet ds , String key){
        DataSet copy = ds.copy();
        List<Long> arrayListlist = new ArrayList<>();
        while (copy.hasNext()) {
            Row next = copy.next();
            if (next.getLong(key) != null
                    && next.getLong(key)!= 0L) {
                arrayListlist.add(next.getLong(key));
            }
        }
        return arrayListlist;
    }

    public static List<Long> getMainbillentryidToList(DataSet ds){
        return getOneToList(ds,"mainbillentryid");
    }
}
