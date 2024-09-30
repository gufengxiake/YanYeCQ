package nckd.yanye.hr.report.shebao;

import kd.bos.report.events.SortAndFilterEvent;
import kd.bos.report.plugin.AbstractReportFormPlugin;

import java.util.List;

/**
 * @author ：luxiao
 * @since ：Created in 17:39 2024/9/29
 */
public class DutyPersonnelReportFormPlugin extends AbstractReportFormPlugin {
    @Override
    public void setSortAndFilter(List<SortAndFilterEvent> allColumns) {
        super.setSortAndFilter(allColumns);
        for(SortAndFilterEvent event : allColumns){
            event.setSort(true);
            event.setFilter(true);
        }

    }

}
