package nckd.yanye.scm.plugin.form;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.exception.KDBizException;
import kd.bos.form.IFormView;
import kd.bos.form.control.RichTextEditor;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.form.events.BeforeDoOperationEventArgs;
import kd.bos.form.operate.FormOperate;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.operation.SaveServiceHelper;
import nckd.yanye.scm.common.PurapplybillConst;
import nckd.yanye.scm.common.SupplierConst;
import nckd.yanye.scm.plugin.utils.ZcPlatformApiUtil;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;

/**
 * 采购申请单-表单插件
 *
 * @author liuxiao
 */
public class PurapplyBillFormPlugin extends AbstractFormPlugin {
    /**
     * 按钮标识-推送招采平台
     */
    final static String PUSHTOZC = "bar_pushtozc";

    /**
     * 按钮标识-公告查看
     */
    final static String ANNOUNCEMENT = "bar_announcement";

    /**
     * 按钮标识-作废
     */
    final static String CANCELORDER = "bar_cancelorder";


    @Override
    public void beforeDoOperation(BeforeDoOperationEventArgs args) {
        FormOperate formOperate = (FormOperate) args.getSource();
        String operateKey = formOperate.getOperateKey();
        //保存之前将富文本的数据保存在大文本中
        if ("save".equals(operateKey) || "submit".equals(operateKey)) {
            RichTextEditor richTextEditor = this.getControl(PurapplybillConst.NCKD_NOTICECONTENT);
            String text = richTextEditor.getText();
            this.getModel().setValue(PurapplybillConst.NCKD_BIGNOTICECONTENT, text);
        }
    }

    @Override
    public void afterBindData(EventObject e) {
        //富文本数据回显
        String largeText = (String) this.getModel().getValue(PurapplybillConst.NCKD_BIGNOTICECONTENT);
        if (largeText == null || largeText.trim().isEmpty()) {
            return;
        }
        RichTextEditor richTextEditor = this.getControl(PurapplybillConst.NCKD_NOTICECONTENT);
        richTextEditor.setText(largeText);
    }


    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        // 侦听主菜单按钮点击事件
        this.addItemClickListeners("tbmain");
    }

    /**
     * 按钮点击事件
     *
     * @param evt
     */
    @Override
    public void itemClick(ItemClickEvent evt) {
        IDataModel model = this.getModel();
        IFormView view = this.getView();
        super.itemClick(evt);
        switch (evt.getItemKey()) {
            // 推送招采平台
            case PUSHTOZC:
                if (!Objects.equals(model.getValue(PurapplybillConst.BILLSTATUS), "C")) {
                    throw new KDBizException("采购申请单未审核!");
                }
                if (((boolean) model.getValue(PurapplybillConst.NCKD_PUSHED) == true)) {
                    throw new KDBizException("该采购申请单已经推送至招采平台!");
                }
                if (((boolean) model.getValue(PurapplybillConst.NCKD_WHETHERPUSH) == false) ||
                        model.getValue(PurapplybillConst.NCKD_PROCUREMENTS) == "annualcontract") {
                    throw new KDBizException("该采购申请单未勾选“是否推送招采平台”!");
                }

                // 初始化结果json
                JSONObject resultJson = null;

                String procurements = (String) model.getValue(PurapplybillConst.NCKD_PROCUREMENTS);
                if ("pricecomparison".equals(procurements) || "singlebrand".equals(procurements)) {
                    resultJson = xb(model);
                } else if ("competitive".equals(procurements)) {
                    // 竞争性谈判的相关代码
                    resultJson = tp(model);

                } else if ("singlesupplier".equals(procurements)) {
                    // 单一供应商的相关代码
                } else if ("bidprocurement".equals(procurements)) {
                    // 招投采购的相关代码
                } else {
                    throw new KDBizException("该单据不可推送!");
                }

                //fixme
                if (resultJson.getBooleanValue("success")) {
                    this.getModel().setValue(PurapplybillConst.NCKD_PURCHASEID, resultJson.getJSONObject("data").getString("orderId"));
                    this.getModel().setValue(PurapplybillConst.NCKD_NOTICEID, resultJson.getJSONObject("data").getString("noticeId"));
                    this.getModel().setValue(PurapplybillConst.NCKD_PUSHED, true);
                    SaveServiceHelper.saveOperate(this.getView().getEntityId(), new DynamicObject[]{this.getModel().getDataEntity(true)});
                    this.getView().showSuccessNotification("公告发布成功!");
                } else {
                    this.getView().showErrorNotification("发布失败!" + resultJson.getString("message"));
                }


                break;
            // 公告查看
            case ANNOUNCEMENT:
                announcement();
                break;
            // 询比采购线上评审文件制作
            case "制作标书":

                break;
            // 作废
            case CANCELORDER:
                if (((boolean) model.getValue(PurapplybillConst.NCKD_PUSHED) == false)) {
                    throw new KDBizException("该采购申请单未推送至招采平台!");
                }
                // 招采平台id
                String orderId = (String) model.getValue(PurapplybillConst.NCKD_PURCHASEID);
                // 对应公告id
                String noticeId = (String) model.getValue(PurapplybillConst.NCKD_NOTICEID);

                JSONObject jsonObject = ZcPlatformApiUtil.cancelXBD(orderId, noticeId);
                if (jsonObject.getBooleanValue("success")) {
                    this.getView().showSuccessNotification("作废成功!");
                } else {
                    this.getView().showErrorNotification("作废失败!" + jsonObject.getString("message"));
                }

                break;
            default:
                break;
        }
    }


    /**
     * 询比价单-公告发布
     *
     * @param model
     * @return
     */
    private JSONObject xb(IDataModel model) {
        //组装json
        JSONObject xbJson = new JSONObject();
        //询比价单信息-order
        xbJson.put("order", new JSONObject() {
            {
                //询比价单名称
                put("orderName", model.getValue(PurapplybillConst.NCKD_INQUIRYLISTNAME));
                //项目类型
                String projectType = (String) model.getValue(PurapplybillConst.NCKD_PROJECTTYPE);
                put("projectType", projectType);
                //处置方式:资产处置类必填
                if ("5".equals(projectType)) {
                    put("assetDisposalMethod", model.getValue(PurapplybillConst.NCKD_DISPOSALMETHOD));
                }
                //报价方式
                put("offerType", model.getValue(PurapplybillConst.NCKD_QUOTATION));
                //fixme 收货地址
                String adminId = (String) model.getValue(PurapplybillConst.NCKD_ADDRESS);
                String fullname = "";
                ORM orm = ORM.create();
                QFilter filter = new QFilter("id", QCP.equals, Long.parseLong(adminId));
                QFilter[] filters = new QFilter[]{filter};
                List<DynamicObject> dynamicObjects = orm.query("bd_admindivision", "id,name,fullname,country.id,country.name", filters, "id asc");

                if (!dynamicObjects.isEmpty()) {
                    DynamicObject dynamicObject = dynamicObjects.get(0);
                    fullname = dynamicObject.getString("fullname");
                }
                String[] parts = fullname.split("_");
                String province = parts[0];
                String city = parts[1];
                String county = parts[2];

                put("address", new JSONObject() {
                    {
                        //国家
                        put("receivingCountry", "中国");
                        //省
                        put("receivingProvince", province);
                        //市
                        put("receivingCity", city);
                        //县
                        put("receivingArea", county);
                        //详细地址
                        put("address", model.getValue(PurapplybillConst.NCKD_DETAILEDADDR));
                    }
                });
                //发布媒体
                put("noticeMedium", model.getValue(PurapplybillConst.NCKD_PUBLISHMEDIA));
                //报价单是否在线签章
                put("quotationElectronicSignature", model.getValue(PurapplybillConst.NCKD_QUOTATIONSIGN));
                //报价附件是否在线签章
                put("quotationAccessoryElectronicSignature", model.getValue(PurapplybillConst.NCKD_QUOTATIONATTSIGN));
                //报名截止时间
                put("offerEndTime", model.getValue(PurapplybillConst.NCKD_DEADLINE));
                //采购类型
                put("purchaseType", model.getValue(PurapplybillConst.NCKD_PURCHASETYPE));
                //报价含税
                put("offerRequireTax", model.getValue(PurapplybillConst.NCKD_INCLUDESTAX));
                //允许对部分品目报价
                put("allowPartialOffer", model.getValue(PurapplybillConst.NCKD_ALLOWFORPARTIAL));
                //报价币种
                DynamicObject currency = (DynamicObject) model.getValue(PurapplybillConst.NCKD_QUOTATIONCURRENCY);
                String currencyNumber = currency.getString("number");
                put("currencyType", Objects.equals(currencyNumber, "CNY") ? "1" : "2");
                //是否设置控制总价
                String controlPrice = (String) model.getValue(PurapplybillConst.NCKD_CONTROLPRICE);
                put("isControlPrice", controlPrice);
                //控制总价
                if ("1".equals(controlPrice)) {
                    put("controlPrice", model.getValue(PurapplybillConst.NCKD_TOTALPRICE));
                }
                //是否必须上传报价附件
                put("isNeedOfferFile", model.getValue(PurapplybillConst.NCKD_MUSTUPATT));
                //询比方式
                String inquiryMethod = (String) model.getValue(PurapplybillConst.NCKD_INQUIRYMETHOD);
                put("inquiryType", inquiryMethod);
                //fixme 供应商。询比方式邀请询比必填
                if ("1".equals(inquiryMethod)) {
                    DynamicObjectCollection inviteCollection = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_SUPPLIERS);
                    xbJson.put("inviteList", new JSONArray() {
                        {
                            for (DynamicObject dynamicObject : inviteCollection) {
                                JSONObject inviteObj = new JSONObject() {
                                    {
                                        //供应商id
                                        put("supplierId", dynamicObject.getString("fbasedataid." + SupplierConst.NCKD_PLATFORMSUPID));
                                        //供应商名称
                                        put("supplierName", dynamicObject.getString("fbasedataid." + SupplierConst.NAME));
                                    }
                                };
                                add(inviteObj);
                            }
                        }
                    });
                }

                //比价方式
                String compareMethod = (String) model.getValue(PurapplybillConst.NCKD_COMPARMETHOD);

                put("compareType", compareMethod);
                //公开范围-邀请询比，不传
                if (!"1".equals(inquiryMethod)) {
                    put("openScope", model.getValue(PurapplybillConst.NCKD_PUBLICSCOPE));
                }

                //报价时段查看供应商参与名单
                put("isSignupShowListing", model.getValue(PurapplybillConst.NCKD_VIEWLIST));
                //报名审核-邀请询比，不传
                if (!"1".equals(inquiryMethod)) {
                    put("checkType", model.getValue(PurapplybillConst.NCKD_REGISTERAUDIT));
                }

                //评审办法
                put("reviewModel", model.getValue(PurapplybillConst.NCKD_REVIEWMETHOD));

                //是否需要线上评审
                put("isReview", model.getValue(PurapplybillConst.NCKD_WHETHERREVIEWOL));

                //备注说明
                put("remarks", model.getValue(PurapplybillConst.NCKD_REMARKS));
                //公告标题-如果是邀请询比就取 邀请函标题
                if ("1".equals(inquiryMethod)) {
                    put("noticeTitle", model.getValue(PurapplybillConst.NCKD_INVITATIONTITLE));
                } else {
                    put("noticeTitle", model.getValue(PurapplybillConst.NCKD_ANNOUNCEMENTTITLE));
                }
                //公告发布日期
                put("noticePublishTime", model.getValue(PurapplybillConst.NCKD_TIMINGTIME));
                //公告内容
                put("noticeContent", model.getValue(PurapplybillConst.NCKD_BIGNOTICECONTENT));

                //询比文件
                DynamicObjectCollection xbAtts = (DynamicObjectCollection) model.getValue("nckd_inquirydocument");
                Integer attGroupId = ZcPlatformApiUtil.addAttachmentGroup("XB", "XBWJ");
                for (DynamicObject obj : xbAtts) {
                    DynamicObject fbasedataId = obj.getDynamicObject("fbasedataId");
                    String name = fbasedataId.getString("name");
                    String url = (String) fbasedataId.get("url");
                    ZcPlatformApiUtil.uploadFile(name, url, attGroupId);
                }
                put("inquiryFileGroupId", attGroupId);
                //内部文件
                ArrayList<Integer> attachmentIds = new ArrayList<>();
                DynamicObjectCollection interAtts = (DynamicObjectCollection) model.getValue("nckd_internaldocuments");
                for (DynamicObject obj : interAtts) {
                    DynamicObject fbasedataId = obj.getDynamicObject("fbasedataId");
                    String name = fbasedataId.getString("name");
                    String url = (String) fbasedataId.get("url");
                    Integer attachmentId = ZcPlatformApiUtil.uploadFile(name, url, attGroupId);
                    attachmentIds.add(attachmentId);
                }
                put("internalAttachmentIds", attachmentIds);

                //物料明细
                DynamicObjectCollection materielEntry = model.getEntryEntity(PurapplybillConst.ENTRYENTITYID_BILLENTRY);
                xbJson.put("materielList", new JSONArray() {
                    {
                        for (DynamicObject materiel : materielEntry) {
                            JSONObject materielObject = new JSONObject() {
                                {
                                    // 品目名称
                                    put("materielName", materiel.get(PurapplybillConst.BILLENTRY_MATERIALNAME));
                                    // 计量单位
                                    put("unitType", ((DynamicObject) ((DynamicObject) materiel.get(PurapplybillConst.BILLENTRY_MATERIAL)).get("purchaseunit")).getString("name"));
                                    // 采购量
                                    put("materielNum", materiel.get(PurapplybillConst.BILLENTRY_APPLYQTY));
                                    // 预算单价
                                    put("budgetUnitPrice", materiel.get(PurapplybillConst.BILLENTRY_PRICEANDTAX));
                                }
                            };
                            add(materielObject);
                        }
                    }
                });
            }
        });


        //调用公告发布接口
        JSONObject xbjd = ZcPlatformApiUtil.addXBD(xbJson);

        return xbjd;
    }

    /**
     * 谈判采购-公告发布
     *
     * @param model
     * @return
     */
    private JSONObject tp(IDataModel model) {
        // 组装json
        JSONObject xbJson = new JSONObject() {
            {
                // 采购单名称
                put("orderName", model.getValue(PurapplybillConst.NCKD_NEGOTIATEDNAME));
                // 谈判采购编号
                put("customOrderCode", model.getValue(PurapplybillConst.NCKD_NEGOTIATEDNUM));
                // 谈判采购预算（万元）
                put("budgetAmount", model.getValue(PurapplybillConst.NCKD_NEGOTIATEDBUDGET));
                // 报名开始时间
                put("offerStartTime", model.getValue(PurapplybillConst.NCKD_REGSTARTTIME));
                // 报名结束时间
                put("offerEndTime", model.getValue(PurapplybillConst.NCKD_REGENDTIME));
                // 提交响应文件截止时间
                put("negotiateTime", model.getValue(PurapplybillConst.NCKD_SUBDEADTIME));
                // 项目类型
                put("projectType", model.getValue(PurapplybillConst.NCKD_PROJECTTYPE1));
                // 处置方式
                put("assetDisposalMethod", model.getValue(PurapplybillConst.NCKD_DISPOSALMETHOD1));
                // 报价方式
                put("offerType", model.getValue(PurapplybillConst.NCKD_QUOTATION1));
                // 谈判方式
                put("negotiateMethod", model.getValue(PurapplybillConst.NCKD_NEGOTIATIONMODE));
                // 谈判地址-国家
                //fixme 地址信息
                put("country", "中国");
                // 谈判地址-所属省
                put("province", "江西省");
                // 谈判地址-所属市
                put("city", "南昌市");
                // 谈判地址-所属区县
                put("area", "红谷滩区");
                // 谈判地址-详细地址
                put("address", model.getValue(PurapplybillConst.NCKD_DETAILEDADDR1));
                // 竞争方式
                put("competeType", model.getValue(PurapplybillConst.NCKD_COMPETITIONMODE));
                // 公开范围
                put("openScope", model.getValue(PurapplybillConst.NCKD_PUBLICSCOPE1));
                // 报价时段查看供应商参与名单
                put("offerTimeScope", model.getValue(PurapplybillConst.NCKD_VIEWLIST1));
                // 报名审核
                put("bidApproval", model.getValue(PurapplybillConst.NCKD_REGISTERAUDIT1));
                // 评审办法
                put("reviewModel", model.getValue(PurapplybillConst.NCKD_REVIEWMETHOD1));
                // 是否需要线上评审
                put("isReview", model.getValue(PurapplybillConst.NCKD_WHETHERREVIEWOL1));
                // todo 谈判文件
                put("negotiateFileIds", new ArrayList<Integer>() {
                    {

                    }
                });
                // todo 内部附件
                put("internalAttachments", new ArrayList<Integer>() {
                    {

                    }
                });
                // 标书费
                put("fileFee", model.getValue(PurapplybillConst.NCKD_TENDERFEE));
                // 平台服务费
                put("signUpFee", model.getValue(PurapplybillConst.NCKD_SERVICEFEE));
            }
        };

        //邀请供应商列表
        DynamicObjectCollection inviteCollection = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_SUPPLIERS1);
        xbJson.put("inviteList", new JSONArray() {
            {
                for (DynamicObject dynamicObject : inviteCollection) {
                    JSONObject inviteObj = new JSONObject() {
                        {
                            //供应商id
                            put("supplierId", dynamicObject.getString("fbasedataid." + SupplierConst.NCKD_PLATFORMSUPID));
                            //供应商名称
                            put("supplierName", dynamicObject.getString("fbasedataid." + SupplierConst.NAME));
                        }
                    };
                    add(inviteObj);
                }
            }
        });


        // 公告
        xbJson.put("notice", new JSONObject() {
            {
                //公告标题-“竞争方式”为有限竞争就取 邀请函标题
                if ("2".equals(model.getValue(PurapplybillConst.NCKD_COMPETITIONMODE))) {
                    put("noticeTitle", model.getValue(PurapplybillConst.NCKD_INVITATIONTITLE));
                } else {
                    put("noticeTitle", model.getValue(PurapplybillConst.NCKD_ANNOUNCEMENTTITLE));
                }
                //公告发布日期
                put("noticePublishTime", model.getValue(PurapplybillConst.NCKD_TIMINGTIME));
                //公告内容
                put("noticeContent", model.getValue(PurapplybillConst.NCKD_BIGNOTICECONTENT));
                // 发布媒体
                put("noticeMedium", model.getValue(PurapplybillConst.NCKD_PUBLISHMEDIA1));
            }
        });

        return null;
    }

    /**
     * TODO
     * 5、点击“公告查看”按钮，自动进行单点登陆，且跳转至相应的公告查看界面，
     * 如未发布公告，则进行提醒“该采购申请单未推送至招采平台”；
     */
    private void announcement() {
        IFormView view = this.getView();
        IDataModel model = this.getModel();

        if (((boolean) model.getValue(PurapplybillConst.NCKD_PUSHED) == false)) {
            throw new KDBizException("该采购申请单未推送至招采平台!");
        }

        String procurements = (String) model.getValue(PurapplybillConst.NCKD_PROCUREMENTS);
        String orderId = (String) model.getValue(PurapplybillConst.NCKD_PURCHASEID);
        String url = ZcPlatformApiUtil.viewNotice(procurements, orderId);
        // 跳转页面
        getView().openUrl(url);
    }

}


















