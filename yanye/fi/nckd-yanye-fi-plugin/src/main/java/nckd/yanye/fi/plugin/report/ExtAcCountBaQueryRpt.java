package nckd.yanye.fi.plugin.report;


import com.bes.mq.util.CollectionUtils;
import kd.bos.algo.DataSet;
import kd.bos.algo.MapFunction;
import kd.bos.algo.Row;
import kd.bos.algo.RowMeta;
import kd.bos.algo.dataset.AbstractRow;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.entity.report.AbstractReportListDataPluginExt;
import kd.bos.entity.report.FilterInfo;
import kd.bos.event.AfterQueryEvent;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 财务-科目余额表
 * 表单标识：gl_rpt_accountba
 * author：xiaoxiaopeng
 * date：2024-09-18
 */
public class ExtAcCountBaQueryRpt extends AbstractReportListDataPluginExt {

    @Override
    public void afterQuery(AfterQueryEvent event) {
        super.afterQuery(event);
        // 获取标品报表数据结果集
        DataSet dataSet = event.getDataSet();
        // 获取表单过滤条件参数对象
        FilterInfo filter = event.getReportQueryParam().getFilter();
        // 获取是否显示数量&显示核算维度
        if (!filter.getBoolean("showqty")) {
            return;
        }
        if (filter.getBoolean("showassist")) {
            return;
        }
        if (!"basecurrency".equals(filter.getString("currency"))) {
            return;
        }

        DataSet copyDataSet = dataSet.copy();
        Map<Long, List<Row>> accountQtyRowsMap = new HashMap<>();
        copyDataSet.forEachRemaining(row -> {
            Long accountnumber = row.getLong("accountnumber");
            if (accountQtyRowsMap.containsKey(accountnumber)) {
                List<Row> rows = accountQtyRowsMap.get(accountnumber);
                rows.add(row);
            } else if (accountnumber != null) {
                List<Row> list = new ArrayList<>();
                list.add(row);
                accountQtyRowsMap.put(accountnumber, list);
            }
        });

        /**
         * yearbdebitlocal 年初余额-借方金额
         * yearbdebitqty 年初余额-借方数量
         * yearbcreditlocal 年初余额-贷方金额
         * yearbcreditqty 年初余额-贷方数量
         * begindebitlocal 期初余额-借方金额
         * begindebitqty 期初余额-借方数量
         * begincreditlocal 期初余额-贷方金额
         * begincreditqty 期初余额-贷方数量
         * debitlocal 本期发生额-借方金额
         * debitqty 本期发生额-借方数量
         * creditlocal 本期发生额-贷方金额
         * creditqty 本期发生额-贷方数量
         * yeardebitlocal 本年累计-借方金额
         * yeardebitqty 本年累计-借方数量
         * yearcreditlocal 本年累计-贷方金额
         * yearcreditqty 本年累计-贷方数量
         * enddebitlocal 期末余额-借方金额
         * enddebitqty 期末余额-借方金额
         * endcreditlocal 期末余额-贷方金额
         * endcreditqty 期末余额-贷方金额
         */
        RowMeta rowMeta = dataSet.getRowMeta();
        List<String> qty = Arrays.asList("yearbdebitqty", "yearbcreditqty", "begindebitqty", "begincreditqty", "debitqty", "creditqty", "yeardebitqty", "yearcreditqty", "enddebitqty", "endcreditqty");
        dataSet = dataSet.map(new MapFunction() {
            @Override
            public Object[] map(Row row) {
                Object[] data = ((AbstractRow) row).values();
                if (row.get("account") != null) {
                    List<Row> rows = accountQtyRowsMap.get(row.getLong("account"));
                    if (rows.size() <= 1) {
                        return data;
                    }
                    Map<String, BigDecimal> qtymap = initQtyMap(qty);
                    rows.stream().forEach(r -> {
                        qty.stream().forEach(field -> {
                            BigDecimal count =  r.getBigDecimal(field) == null ? BigDecimal.ZERO : r.getBigDecimal(field);
                            BigDecimal resultCount = (count.add(qtymap.get(field))).setScale(2, BigDecimal.ROUND_HALF_UP);
                            qtymap.put(field, resultCount);
                                }
                        );
                    });
                    qty.stream().forEach(field -> data[rowMeta.getFieldIndex(field)] = qtymap.get(field));
                }
                return data;
            }

            @Override
            public RowMeta getResultRowMeta() {
                return rowMeta;
            }
        });

        dataSet = dataSet.filter("account is not null or name = '合计'");
        event.setDataSet(dataSet);

    }

    private Map<String, BigDecimal> initQtyMap(List<String> qtyFields) {
        Map<String, BigDecimal> qtyMap = new HashMap<>();
        qtyFields.stream().forEach(x -> qtyMap.put(x, BigDecimal.ZERO));
        return qtyMap;
    }
}
