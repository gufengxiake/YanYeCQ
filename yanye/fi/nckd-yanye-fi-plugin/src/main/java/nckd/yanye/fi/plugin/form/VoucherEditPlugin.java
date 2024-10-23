package nckd.yanye.fi.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.bill.BillShowParameter;
import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.resource.ResManager;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.form.IPageCache;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.fi.gl.formplugin.voucher.VoucherEditValueGetter;
import kd.fi.gl.formplugin.voucher.VoucherEditView;
import kd.fi.gl.util.GLUtil;
import kd.hr.hbp.common.util.DatePattern;
import kd.hr.hbp.common.util.DateUtils;
import nckd.base.common.utils.capp.CacheBusinessData;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 *
 * Module           :财务云-租赁管理-退养人员工资
 * Description      :退休人员工资编辑插件
 *
 * @author guozhiwei
 * @date  2024/10/22 14:01
 *  标识:nckd_gl_voucher_ext
 *
 */

public class VoucherEditPlugin  extends AbstractBillPlugIn {

    public static final Map<String,String> KEY_MAP = new HashMap<>(6);

    private VoucherEditView voucherEditView;


    // "本期，今天，本周，本月"
    private static final List<String> DATE_KEYS = Arrays.asList(new String[]{"10", "865760366578131968", "13", "63"});
    // “上一期”
    private static final List<String> DATE_KEYS2 = Arrays.asList(new String[]{"865760791251411968"});
    // “下一期，下月”
    private static final List<String> DATE_KEYS3 = Arrays.asList(new String[]{"865760640340354048", "8"});

//                //  10 本周
//                // 865760366578131968 本期
//                // 865760791251411968 上一期
//                // 865760640340354048 下一期
//                // 13 今天
//                // 本月 63
//                // 24 过去3个月
//                // 8 下月

    @Override
    public void afterCreateNewData(EventObject e) {
        // 如果是新增页面。去掉 入职操作
        BillShowParameter bsp = (BillShowParameter) this.getView().getFormShowParameter();
        if (bsp.getStatus() == OperationStatus.ADDNEW) {
            String bookeddate = CacheBusinessData.get("bookeddate", this.getView().getFormShowParameter().getRootPageId());
            CacheBusinessData.remove("bookeddate", this.getView().getFormShowParameter().getRootPageId());

            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = sdf.format(date);
            Date date2 = DateUtils.stringToDate(dateStr, DatePattern.YYYY_MM_DD);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date2);

            // 获取上月最后一天
            calendar.set(Calendar.DAY_OF_MONTH, 1); // 设置为当前月的第一天
            calendar.add(Calendar.DAY_OF_MONTH, -1); // 上个月最后一天
            Date lastDayOfLastMonth = calendar.getTime();

            // 获取下月最后一天
            calendar.setTime(date2);
            calendar.add(Calendar.MONTH, 2); // 下个月
            calendar.set(Calendar.DAY_OF_MONTH, 1); // 设置为下个月的第一天
            calendar.add(Calendar.DAY_OF_MONTH, -1); // 最后一天
            Date lastDayOfNextMonth = calendar.getTime();


            if(DATE_KEYS.contains(bookeddate)){
                // 使用今天
                this.getModel().setValue("bizdate",date2);
                this.getModel().setValue("bookeddate",date2);
                dealPeriod(date2);
            }else if(DATE_KEYS2.contains(bookeddate)){
                // 上月月末最后一天
                this.getModel().setValue("bizdate",lastDayOfLastMonth);
                this.getModel().setValue("bookeddate",lastDayOfLastMonth);
                dealPeriod(lastDayOfLastMonth);
            }else if(DATE_KEYS3.contains(bookeddate)){
                // 下月月末最后一天
                this.getModel().setValue("bizdate",lastDayOfNextMonth);
                this.getModel().setValue("bookeddate",lastDayOfNextMonth);
                dealPeriod(lastDayOfNextMonth);
            }


        }

    }

    VoucherEditValueGetter getValueGetter() {
        return this.getVoucherEditView().getValueGetter();
    }

    private VoucherEditView getVoucherEditView() {
        if (null == this.voucherEditView) {
            this.voucherEditView = new VoucherEditView(this.getView(), this.getModel(), this.getPageCache());
        }

        return this.voucherEditView;
    }
    private void dealPeriod(Date newDate) {
        DynamicObject book = this.getValueGetter().getBook();
        if(book != null){
            IDataModel model = this.getModel();
            String sourceType = this.getValueGetter().getSourceType();
            String selectField = "id,periodtype.id,periodyear,periodnumber,begindate,enddate,isadjustperiod,number,name";
            DynamicObjectCollection periodsByBookedDate = GLUtil.getPeriodByDate(newDate, selectField, book.getLong("periodtype.id"));
            if (periodsByBookedDate == null || periodsByBookedDate.isEmpty()) {
                this.getView().showErrorNotification(String.format(ResManager.loadKDString("期间类型[%s] 不存在记账日期所对应的会计期间，请先维护期间基础资料", "VoucherEdit_44", "fi-gl-formplugin", new Object[0]), book.getDynamicObject("periodtype").getString("name")));
                return;
            }
            IPageCache pageCache = (IPageCache)this.getView().getService(IPageCache.class);
            if ("1".equals(sourceType) || "4".equals(sourceType)) {
                return;
            }

            DynamicObject curPeriod = BusinessDataServiceHelper.loadSingle(pageCache.get("book_curperiod"), "bd_period", "id,number,name,isadjustperiod");
            boolean isLegalPeriod = false;
            if (curPeriod != null) {
                List<Long> openedPeriodIdList = GLUtil.getOpenPeriod(this.getValueGetter().getOrgId(), this.getValueGetter().getBookTypeId());
                long curPeriodId = curPeriod.getLong("id");
                long periodId = ((DynamicObject)periodsByBookedDate.get(periodsByBookedDate.size() - 1)).getLong("id");

                for(int i = periodsByBookedDate.size() - 1; i >= 0; --i) {
                    DynamicObject periodDyn = (DynamicObject)periodsByBookedDate.get(i);
                    long periodIdByDate = periodDyn.getLong("id");
                    if (openedPeriodIdList.contains(periodIdByDate) || periodIdByDate >= curPeriodId) {
                        isLegalPeriod = true;
                        break;
                    }
                }

                if (!isLegalPeriod) {
                    this.getView().showErrorNotification(ResManager.loadKDString("不能新增已结账期间凭证。", "VoucherEdit_46", "fi-gl-formplugin", new Object[0]));
                }

                this.cachePeriodId(periodsByBookedDate, curPeriodId == periodId && curPeriod.getBoolean("isadjustperiod"), periodId);
                model.setValue("period", periodId);
            }

        } else {
            this.getView().showErrorNotification(ResManager.loadKDString("该核算主体默认主账簿为空", "VoucherEdit_47", "fi-gl-formplugin", new Object[0]));
        }



    }

    private void cachePeriodId(DynamicObjectCollection curPeriod, boolean curIsAdjust, long periodIdOnEdit) {
        String sourceType = this.getValueGetter().getSourceType();
        boolean enable = curPeriod.size() > 1 && !curIsAdjust && !"1".equals(sourceType) && !"4".equals(sourceType);
        this.getView().setEnable(enable, new String[]{"period"});
        StringBuilder sb = new StringBuilder();
        if (enable) {
            Iterator var8 = curPeriod.iterator();

            while(var8.hasNext()) {
                DynamicObject dyn = (DynamicObject)var8.next();
                sb.append(dyn.getString("id"));
                sb.append(",");
            }
        } else {
            sb.append(periodIdOnEdit);
        }

        this.getPageCache().put("ids", sb.toString());
    }

}
