package nckd.yanye.occ.plugin.report;

import kd.bos.algo.DataSet;
import kd.bos.algo.Row;
import kd.bos.algo.olap.util.ByteArrayInputStream;
import kd.bos.algo.olap.util.ByteArrayOutputStream;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.NumberFormatProvider;
import kd.bos.entity.report.AbstractReportColumn;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportColumn;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.fileservice.FileItem;
import kd.bos.fileservice.FileService;
import kd.bos.fileservice.FileServiceFactory;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.HyperLinkClickEvent;
import kd.bos.form.events.HyperLinkClickListener;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.report.ReportList;
import kd.bos.report.plugin.AbstractReportFormPlugin;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.hrcdsc.hrscas.formplugin.web.codescan.util.MyExportUtil;
import kd.sdk.plugin.Plugin;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * 行政区域销售情况-报表界面插件
 * 表单标识：nckd_salesinarea_rpt
 * author:zhangzhilong
 * date:2024/09/09
 */
public class SalesInAreaReportFormPlugin extends AbstractReportFormPlugin implements Plugin {
    @Override
    public void initDefaultQueryParam(ReportQueryParam queryParam) {
        super.initDefaultQueryParam(queryParam);
        FilterInfo filter = queryParam.getFilter();
        Long curLoginOrg = RequestContext.get().getOrgId();
        //给发货组织默认值
        filter.addFilterItem("nckd_org_q", curLoginOrg);
    }

    @Override
    public void processRowData(String gridPK, DynamicObjectCollection rowData, ReportQueryParam queryParam) {
        super.processRowData(gridPK, rowData, queryParam);
    }

    @Override
    public void resetColumns(List<AbstractReportColumn> columns) {
        super.resetColumns(columns);
    }

    @Override
    public void preProcessExportData(List<AbstractReportColumn> exportColumns, DynamicObjectCollection data, NumberFormatProvider numberFormatProvider) {
        super.preProcessExportData(exportColumns, data, numberFormatProvider);
//        ReportList reportList = this.getControl("reportlistap");
//        List<ReportColumn> reportColumnList = reportList.getReportColumnList(exportColumns);
//        for (ReportColumn reportColumn : reportColumnList) {
//            reportColumn.setScale(2);
//        }
    }
}