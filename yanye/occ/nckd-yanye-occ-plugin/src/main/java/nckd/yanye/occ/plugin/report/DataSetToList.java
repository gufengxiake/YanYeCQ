package nckd.yanye.occ.plugin.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.report.ReportColumn;

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
        copy.close();
        return arrayListlist;
    }

    public static List<Long> getMainbillentryidToList(DataSet ds){
        return getOneToList(ds,"mainbillentryid");
    }

    public static ReportColumn createReportColumn(String fileKey, String fileType, String name) {
        ReportColumn column = new ReportColumn();
        column.setFieldKey(fileKey);
        column.setFieldType(fileType);
        column.setCaption(new LocaleString(name));
        if (Objects.equals(fileType, ReportColumn.TYPE_DECIMAL)) {
            column.setScale(2);
        }
        return column;
    }
}
