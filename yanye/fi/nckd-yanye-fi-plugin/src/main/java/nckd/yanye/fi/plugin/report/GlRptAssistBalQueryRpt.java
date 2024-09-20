package nckd.yanye.fi.plugin.report;


import com.icbc.api.internal.apache.http.impl.cookie.S;
import kd.bos.algo.DataSet;
import kd.bos.algo.MapFunction;
import kd.bos.algo.Row;
import kd.bos.algo.RowMeta;
import kd.bos.algo.dataset.AbstractRow;
import kd.bos.entity.report.AbstractReportListDataPluginExt;
import kd.bos.entity.report.FilterInfo;
import kd.bos.event.AfterQueryEvent;

import java.math.BigDecimal;
import java.util.*;

/**
 * 财务-核算维度余额表
 * 表单标识：nckd_gl_rpt_assistbal_ext
 * author：xiaoxiaopeng
 * date：2024-09-19
 */
public class GlRptAssistBalQueryRpt extends AbstractReportListDataPluginExt {

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

        RowMeta rowMeta1 = dataSet.getRowMeta();
        if (rowMeta1.getDataTypeOrdinals().length == 0){
            return;
        }
        DataSet copyDataSet = dataSet.copy();
        Map<String, List<Row>> accountQtyRowsMap = new HashMap<>();
        copyDataSet.forEachRemaining(row -> {
            String accountnumber = row.getString("assval");
            String account = row.getString("account");
            String resultAccount = accountnumber+account;
            if (accountQtyRowsMap.containsKey(resultAccount)) {
                List<Row> rows = accountQtyRowsMap.get(resultAccount);
                rows.add(row);
            } else if (accountnumber != null) {
                List<Row> list = new ArrayList<>();
                list.add(row);
                accountQtyRowsMap.put(resultAccount, list);
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

        //_rowtype, assval, number, account, currencyid, orgid, measureunit, measureunitname, currencylocalid, name, treeid, treeparent, treename,
        // treenumber, longnumber, treelevel, treeleaf, yearbegindebitfor, yearbegincreditfor, yearbegindebitlocal, yearbegincreditlocal,
        // yearbegindebitqty, yearbegincreditqty, yearbegindebitrpt, yearbegincreditrpt, begindebitfor, begincreditfor, begindebitlocal,
        // begincreditlocal, begindebitqty, begincreditqty, begindebitrpt, begincreditrpt, debitfor, creditfor, debitlocal, creditlocal, debitqty,
        // creditqty, debitrpt, creditrpt, yeardebitfor, yearcreditfor, yeardebitlocal, yearcreditlocal, yeardebitqty, yearcreditqty, yeardebitrpt,
        // yearcreditrpt, enddebitfor, endcreditfor, enddebitlocal, endcreditlocal, enddebitqty, endcreditqty, enddebitrpt, endcreditrpt, namectrldate
        RowMeta rowMeta = dataSet.getRowMeta();
        List<String> qty = Arrays.asList("yearbegindebitqty", "yearbegincreditqty", "begindebitqty", "begincreditqty", "debitqty", "creditqty", "yeardebitqty", "yearcreditqty", "enddebitqty", "endcreditqty");
        dataSet = dataSet.map(new MapFunction() {
            @Override
            public Object[] map(Row row) {
                Object[] data = ((AbstractRow) row).values();
                if (row.get("assval") != null) {
                    String account = row.getString("account");
                    List<Row> rows = accountQtyRowsMap.get(row.getString("assval")+account);
                    if (rows.size() <= 1) {
                        return data;
                    }
                    Map<String, BigDecimal> qtymap = initQtyMap(qty);
                    rows.stream().forEach(r -> {
                            qty.stream().forEach(field -> {
                                        BigDecimal count =  r.getBigDecimal(field) == null ? BigDecimal.ZERO : r.getBigDecimal(field);
                                        BigDecimal resultCount = count.add(qtymap.get(field));
                                        qtymap.put(field, resultCount);
                                    }
                            );
                    });
                    if ("Account".equals(data[0])){
                        qty.stream().forEach(field -> data[rowMeta.getFieldIndex(field)] = qtymap.get(field));
                    }
                }
                return data;
            }

            @Override
            public RowMeta getResultRowMeta() {
                return rowMeta;
            }
        });

        dataSet = dataSet.filter("_rowtype = 'Account' or name = '合计'");//assval is not null or
        event.setDataSet(dataSet);

    }

    private Map<String, BigDecimal> initQtyMap(List<String> qtyFields) {
        Map<String, BigDecimal> qtyMap = new HashMap<>();
        qtyFields.stream().forEach(x -> qtyMap.put(x, BigDecimal.ZERO));
        return qtyMap;
    }
}
