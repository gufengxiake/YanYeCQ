package nckd.yanye.scm.common.utils;


import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.exception.KDBizException;
import kd.bos.fileservice.extension.FileServiceExtFactory;
import kd.bos.logging.Log;
import kd.bos.logging.LogFactory;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QFilter;
import kd.bos.url.UrlService;
import nckd.yanye.scm.common.PurapplybillConst;
import nckd.yanye.scm.common.SupplierConst;
import nckd.yanye.scm.common.ZcPlatformConst;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * 招采平台json组装工具类
 *
 * @author liuxiao
 */
public class ZcPlatformJsonUtil {

    private static final Log log = LogFactory.getLog(ZcPlatformJsonUtil.class);


    /**
     * 公告发布-询比采购单
     *
     * @param model
     * @return
     */
    public static JSONObject getXbJson(ZcPlatformConst zcPlatformConst, IDataModel model) {
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
                String addressId = (String) model.getValue(PurapplybillConst.NCKD_ADDRESS);
                HashMap<String, String> map = formatAddress(addressId);
                // 收货地址
                put("address", new JSONObject() {
                    {
                        //国家
                        put("receivingCountry", map.get("国家"));
                        //省
                        put("receivingProvince", map.get("省"));
                        //市
                        put("receivingCity", map.get("市"));
                        //县
                        put("receivingArea", map.get("县区"));
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
                //邀请供应商。询比方式邀请询比必填
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
                //公开范围-公开询比，传
                if ("0".equals(inquiryMethod)) {
                    put("openScope", model.getValue(PurapplybillConst.NCKD_PUBLICSCOPE));
                }

                //报价时段查看供应商参与名单
                put("isSignupShowListing", model.getValue(PurapplybillConst.NCKD_VIEWLIST));

                //报名审核-邀请询比，不传
                if (!"0".equals(inquiryMethod)) {
                    put("checkType", model.getValue(PurapplybillConst.NCKD_REGISTERAUDIT));
                }

                //评审办法
                put("reviewModel", model.getValue(PurapplybillConst.NCKD_REVIEWMETHOD));

                // 是否需要线上评审。选是按钮制作电子标书
                String isReview = (String) model.getValue(PurapplybillConst.NCKD_WHETHERREVIEWOL);
                put("isReview", isReview);
                if ("1".equals(isReview)) {
                    Object value = model.getValue(PurapplybillConst.NCKD_REVIEWID);
                    if (Objects.isNull(value)) {
                        throw new KDBizException("请先制作标书/评审单!");
                    }
                    put("reviewId", value);
                }

                // 备注说明
                put("remarks", model.getValue(PurapplybillConst.NCKD_REMARKS));

                // 公告标题
                put("noticeTitle", model.getValue(PurapplybillConst.NCKD_ANNOUNCEMENTTITLE));

                // 定时发布-公告发布日期
                if ("timing".equals(model.getValue(PurapplybillConst.NCKD_PUBLISHSET))) {
                    put("noticePublishTime", model.getValue(PurapplybillConst.NCKD_TIMINGTIME));
                }
                // 公告内容
                put("noticeContent", model.getValue(PurapplybillConst.NCKD_BIGNOTICECONTENT));

                // 询比文件-GroupId
                DynamicObjectCollection xbAtts = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_INQUIRYDOCUMENT);
                Integer attGroupId = ZcPlatformApiUtil.addAttachmentGroup(zcPlatformConst, "XB", "XBWJ");
                for (DynamicObject obj : xbAtts) {
                    DynamicObject fbasedataId = obj.getDynamicObject("fbasedataId");
                    String name = fbasedataId.getString("name");
                    String url = fbasedataId.getString("url");
                    String realPath = FileServiceExtFactory.getAttachFileServiceExt().getRealPath(url);
                    realPath = UrlService.getAttachmentFullUrl(realPath);
                    realPath = convertToFullPath(realPath, name);

                    ZcPlatformApiUtil.uploadFile(zcPlatformConst, name, realPath, attGroupId);
                }
                put("inquiryFileGroupId", attGroupId);

                //内部文件
                DynamicObjectCollection attachments = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_INTERNALDOCUMENTS);
                put("internalAttachmentIds", getAttIdList(zcPlatformConst, attachments));

                //物料明细
                DynamicObjectCollection materielEntry = model.getEntryEntity(PurapplybillConst.ENTRYENTITYID_BILLENTRY);
                xbJson.put("materielList", new JSONArray() {
                    {
                        for (DynamicObject materiel : materielEntry) {
                            JSONObject materielObject = new JSONObject() {
                                {
                                    // spuCode
                                    put("spuCode", materiel.getString("seq"));
                                    // 品目编码
                                    put("materielCode", materiel.getDynamicObject("material").getString("masterid.number"));
                                    // 品目名称
                                    put("materielName", materiel.get(PurapplybillConst.BILLENTRY_MATERIALNAME));
                                    // 品目型号
                                    put("materielModel", materiel.getDynamicObject("material").getString("masterid.modelnum"));
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
        return addItemSchemaList(xbJson);
    }

    /**
     * 公告发布-谈判采购单
     *
     * @param model
     * @return
     */
    public static JSONObject getTpJson(ZcPlatformConst zcPlatformConst, IDataModel model) {
        // 组装json
        JSONObject tpJson = new JSONObject() {
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
                String projectType = (String) model.getValue(PurapplybillConst.NCKD_PROJECTTYPE1);
                put("projectType", projectType);
                // 处置方式-“项目类型”选择资产处置类显示该字段，同时必填
                if ("5".equals(projectType)) {
                    put("assetDisposalMethod", model.getValue(PurapplybillConst.NCKD_DISPOSALMETHOD1));
                }
                // 报价方式
                put("offerType", model.getValue(PurapplybillConst.NCKD_QUOTATION1));
                // 谈判方式
                put("negotiateMethod", model.getValue(PurapplybillConst.NCKD_NEGOTIATIONMODE));
                // 谈判地点
                String addressId = (String) model.getValue(PurapplybillConst.NCKD_NEGOTIATIONADDR);
                HashMap<String, String> map = formatAddress(addressId);
                // 谈判地址-国家
                put("country", map.get("国家"));
                // 谈判地址-所属省
                put("province", map.get("省"));
                // 谈判地址-所属市
                put("city", map.get("市"));
                // 谈判地址-所属区县
                put("area", map.get("县区"));
                // 谈判地址-详细地址
                put("address", model.getValue(PurapplybillConst.NCKD_DETAILEDADDR1));
                // 竞争方式
                String competeType = (String) model.getValue(PurapplybillConst.NCKD_COMPETITIONMODE);
                put("competeType", competeType);
                // 公开范围-“竞争方式”选择有限竞争，则不显示该字段
                if ("1".equals(competeType)) {
                    put("openScope", model.getValue(PurapplybillConst.NCKD_PUBLICSCOPE1));
                }
                // 报价时段查看供应商参与名单
                put("offerTimeScope", model.getValue(PurapplybillConst.NCKD_VIEWLIST1));
                // 报名审核
                put("bidApproval", model.getValue(PurapplybillConst.NCKD_REGISTERAUDIT1));
                // 评审办法
                put("reviewModel", model.getValue(PurapplybillConst.NCKD_REVIEWMETHOD1));
                // 是否需要线上评审
                String WHETHERREVIEWOL1 = (String) model.getValue(PurapplybillConst.NCKD_WHETHERREVIEWOL1);
                put("isReview", WHETHERREVIEWOL1);
                if ("1".equals(WHETHERREVIEWOL1)) {
                    Object value = model.getValue(PurapplybillConst.NCKD_REVIEWID);
                    if (Objects.isNull(value)) {
                        throw new KDBizException("请先制作标书/评审单!");
                    }
                    put("onlineReviewId", WHETHERREVIEWOL1);
                }

                // 谈判文件
                DynamicObjectCollection tpAttachments = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_NEGOTIATINGDOCUMENTS);
                put("negotiateFileIds", getAttIdList(zcPlatformConst, tpAttachments));

                // 内部附件
                DynamicObjectCollection nbAttachments = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_INTERNALDOCUMENTS1);
                put("internalAttachments", getAttIdList(zcPlatformConst, nbAttachments));

                // 标书费
                put("fileFee", model.getValue(PurapplybillConst.NCKD_TENDERFEE));
                // 平台服务费
                put("signUpFee", model.getValue(PurapplybillConst.NCKD_SERVICEFEE));
            }
        };

        //邀请供应商列表
        DynamicObjectCollection inviteCollection = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_SUPPLIERS1);
        tpJson.put("inviteList", new JSONArray() {
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
        tpJson.put("notice", new JSONObject() {
            {
                //公告标题
                put("title", model.getValue(PurapplybillConst.NCKD_ANNOUNCEMENTTITLE));
                //公告发布日期
                String publishSet = (String) model.getValue(PurapplybillConst.NCKD_PUBLISHSET);
                if ("timing".equals(publishSet)) {
                    put("noticeReleaseTime", model.getValue(PurapplybillConst.NCKD_TIMINGTIME));
                }
                //公告内容
                put("content", model.getValue(PurapplybillConst.NCKD_BIGNOTICECONTENT));
                // 发布媒体
                put("noticeMedium", model.getValue(PurapplybillConst.NCKD_PUBLISHMEDIA1));
            }
        });

        return tpJson;
    }

    /**
     * 公告发布-招标采购单
     *
     * @param model
     * @return
     */
    public static JSONObject getZbJson(ZcPlatformConst zcPlatformConst, IDataModel model) {
        String biddingMethod = (String) model.getValue(PurapplybillConst.NCKD_BIDDINGMETHOD);

        //组装json
        JSONObject zbJson = new JSONObject() {
            {
                // 招标名称
                put("orderName", model.getValue(PurapplybillConst.NCKD_TENDERNAME));
                // 招标估算（万元）
                put("budget", model.getValue(PurapplybillConst.NCKD_TENDERESTIMATE));
                // 中标方式
                put("winType", model.getValue(PurapplybillConst.NCKD_BIDMETHOD));
                // 报价方式
                put("offerType", model.getValue(PurapplybillConst.NCKD_QUOTATION3));
                // 报名开始时间
                put("signUpStartTime", model.getValue(PurapplybillConst.NCKD_REGSTARTTIME1));
                // 报名结束时间
                put("signUpEndTime", model.getValue(PurapplybillConst.NCKD_REGENDTIME1));
                // 开标时间
                put("openTime", model.getValue(PurapplybillConst.NCKD_BIDOPENTIME));
                // 项目类型
                put("biddingType", model.getValue(PurapplybillConst.NCKD_PROJECTTYPE3));
                //地址信息
                // 招标地址-国家
                String addressId = (String) model.getValue("nckd_tenderaddr");
                HashMap<String, String> map = formatAddress(addressId);
                put("country", map.get("国家"));
                // 招标地址-所属省
                put("province", map.get("省"));
                // 招标地址-所属市
                put("city", map.get("市"));
                // 招标地址-所属区县
                put("area", map.get("县区"));
                // 招标地址-详细地址
                put("detailAddress", model.getValue(PurapplybillConst.NCKD_DETAILEDADDR3));
                // 招标方式
                put("biddingMethod", biddingMethod);
                // 发布方式
                put("releaseMethod", model.getValue(PurapplybillConst.NCKD_PUBLISHINGMETHOD));
                // 公开范围
                put("openScope", model.getValue(PurapplybillConst.NCKD_PUBLICSCOPE2));
                // 报价时段查看供应商参与名单
                put("isSignupShowListing", model.getValue(PurapplybillConst.NCKD_VIEWLIST2));
                // 是否需要审核
                put("isApproval", model.getValue(PurapplybillConst.NCKD_REGISTERAUDIT2));
                //允许联合体报名
                put("isCombo", model.getValue(PurapplybillConst.NCKD_ALLOWJOINT));
                // 在线开评标。
                String isKpbProject = (String) model.getValue(PurapplybillConst.NCKD_BIDONLINE);
                put("isKpbProject", isKpbProject);
                if ("1".equals(isKpbProject)) {
                    Object value = model.getValue(PurapplybillConst.NCKD_REVIEWID);
                    if (Objects.isNull(value)) {
                        throw new KDBizException("请先制作标书/评审单!");
                    }
                    put("biddingFileId", model.getValue(PurapplybillConst.NCKD_REVIEWID));
                }
                // 标书费
                put("bookFee", model.getValue(PurapplybillConst.NCKD_TENDERFEE1));
            }
        };
        // 邀请供应商列表-邀请招标
        if ("2".equals(biddingMethod)) {
            DynamicObjectCollection inviteSupCollection = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_SUPPLIERS3);
            zbJson.put("inviteSup", new JSONArray() {
                {
                    for (DynamicObject inviteSup : inviteSupCollection) {
                        JSONObject inviteObj = new JSONObject() {
                            {
                                //供应商id
                                put("supplierId", inviteSup.getString("fbasedataid." + SupplierConst.NCKD_PLATFORMSUPID));
                                //供应商名称
                                put("supplierName", inviteSup.getString("fbasedataid." + SupplierConst.NAME));
                            }
                        };
                        this.add(inviteObj);
                    }
                }
            });
        }
        // 保证金
        zbJson.put("guarantee", new JSONObject() {
            {
                // 支持保证金缴纳形式-银行转账
                put("supportGf", 1);
                // 支持保证金缴纳形式-允许在线申请投标保函
//                put("supportOnlineGfLetter:", 1);
                // 保证金缴纳截止时间
                put("depositEndTime", model.getValue(PurapplybillConst.NCKD_DEPOSITENDTIME));
                // 保证金金额
                put("guaranteeFee", model.getValue(PurapplybillConst.NCKD_MARGINAMOUNT));
            }
        });
        // 内部文件
        DynamicObjectCollection nbAttachments = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_INTERNALATTACHMENTS);
        zbJson.put("internalAttachments", getAttIdList(zcPlatformConst, nbAttachments));
        // 上传文件
        DynamicObjectCollection scAttachments = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_UPLOADFILE);
        zbJson.put("biddingAttachmentIds", getAttIdList(zcPlatformConst, scAttachments));
        // 其他附件
        DynamicObjectCollection qtAttachments = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_OTHERANNEXES);
        zbJson.put("otherAttachmentIds", getAttIdList(zcPlatformConst, qtAttachments));

        // 公告标题
        zbJson.put("title", model.getValue(PurapplybillConst.NCKD_ANNOUNCEMENTTITLE));
        // 公告发布的时间
        String publishSet = (String) model.getValue(PurapplybillConst.NCKD_PUBLISHSET);
        if ("timing".equals(publishSet)) {
            zbJson.put("noticeReleaseTime", model.getValue(PurapplybillConst.NCKD_TIMINGTIME));
        }
        // 公告内容
        zbJson.put("content", model.getValue(PurapplybillConst.NCKD_BIGNOTICECONTENT));
        return zbJson;
    }

    /**
     * 公告发布-单一供应商
     *
     * @param model
     * @return
     */
    public static JSONObject getDyJson(ZcPlatformConst zcPlatformConst, IDataModel model) {
        JSONObject dyJson = new JSONObject() {
            {
                // 招标方式-邀请招标，2
                put("biddingMethod", 2);

                // 项目名称
                put("orderName", model.getValue(PurapplybillConst.NCKD_PROJECTNAME));
                // 项目金额（万元）
                put("budget", model.getValue(PurapplybillConst.NCKD_PROJECTAMOUNT));
                // 项目类型
                put("biddingType", model.getValue(PurapplybillConst.NCKD_PROJECTTYPE2));
                // 报价方式
                put("offerType", model.getValue(PurapplybillConst.NCKD_QUOTATION2));
                // 项目地点
                String addressId = (String) model.getValue(PurapplybillConst.NCKD_PROJECTADDR);
                HashMap<String, String> map = formatAddress(addressId);
                // 招标地址-国家
                put("country", map.get("国家"));
                // 招标地址-所属省
                put("province", map.get("省"));
                // 招标地址-所属市
                put("city", map.get("市"));
                // 招标地址-所属区县
                put("area", map.get("区县"));
                // 招标地址-详细地址
                put("detailAddress", model.getValue(PurapplybillConst.NCKD_DETAILEDADDR2));
                // 报名开始时间
                put("signUpStartTime", model.getValue(PurapplybillConst.NCKD_SIGNUPSTARTTIME));
                // 报名结束时间
                put("signUpEndTime", model.getValue(PurapplybillConst.NCKD_SIGNUPENDTIME));
                // 开标时间
                put("openTime", model.getValue(PurapplybillConst.NCKD_OPENTIME));
                // 在线开评标。
                put("isKpbProject", 0);
            }
        };
        // 邀请供应商列表-单一供应商
        dyJson.put("inviteSup", new JSONArray() {
            {
                add(new JSONObject() {
                    {
                        DynamicObject inviteObj = (DynamicObject) model.getValue(PurapplybillConst.NCKD_SUPPLIERS2);
                        // 供应商id
                        put("supplierId", inviteObj.getString(SupplierConst.NCKD_PLATFORMSUPID));
                        // 供应商名称
                        put("supplierName", inviteObj.getString(SupplierConst.NAME));
                    }
                });
            }
        });
        // 采购范围-附件
        DynamicObjectCollection cgAttachments = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_PROCUREMENTSCOPEATT);
        dyJson.put("biddingAttachmentIds", getAttIdList(zcPlatformConst, cgAttachments));
        // 内部文件-附件
        DynamicObjectCollection nbAttachments = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_INTERNALDOCUMENTS2);
        dyJson.put("internalAttachments", getAttIdList(zcPlatformConst, nbAttachments));
        // 项目文件-附件
        DynamicObjectCollection xmAttachments = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_PROJECTFILES);
        dyJson.put("otherAttachmentIds", getAttIdList(zcPlatformConst, xmAttachments));

        // 公告标题
        dyJson.put("title", model.getValue(PurapplybillConst.NCKD_ANNOUNCEMENTTITLE));
        // 公告发布的时间
        String publishSet = (String) model.getValue(PurapplybillConst.NCKD_PUBLISHSET);
        if ("timing".equals(publishSet)) {
            dyJson.put("noticeReleaseTime", model.getValue(PurapplybillConst.NCKD_TIMINGTIME));
        }
        // 公告内容
        dyJson.put("content", model.getValue(PurapplybillConst.NCKD_BIGNOTICECONTENT));

        return dyJson;
    }

    /**
     * 公告作废-询比采购单
     */
    public static JSONObject getXbCancelJson(HashMap<String, Object> cancelMap) {
        JSONObject xbCancelJson = new JSONObject() {
            {
                // 是否对外网公示 1：是 0：否
                put("closePublicity", 1);
                //流标类型 1：终止 2：重新
                put("closeType", 1);
                // 流标原因
                put("closeReason", cancelMap.get("closeReason"));
                // 其他原因
                put("otherReason", cancelMap.get("otherReason"));
                //关闭公告
                put("notice", new JSONObject() {
                    {
                        put("noticeTitle", cancelMap.get("title"));
                        put("noticeContent", cancelMap.get("content"));
                    }
                });
                // 附件说明
                put("closeAttachmentIds", cancelMap.get("closeAttachmentIds"));
            }
        };
        return xbCancelJson;
    }

    /**
     * 公告作废-谈判采购单
     */
    public static JSONObject getTpCancelJson(HashMap<String, Object> cancelMap) {
        JSONObject tPCancelJson = new JSONObject() {
            {
                // 是否对外网公示 1：是 0：否
                put("closePublicity", 1);
                //流标类型 1：终止 2：重新
                put("closeType", 1);
                // 流标原因
                put("closeReason", cancelMap.get("closeReason"));
                // 其他原因
                put("otherReason", cancelMap.get("otherReason"));
                //关闭公告
                put("notice", new JSONObject() {
                    {
                        put("title", cancelMap.get("title"));
                        put("content", cancelMap.get("content"));
                    }
                });
                // 附件说明
                put("closeAttachmentIds", cancelMap.get("closeAttachmentIds"));
            }
        };
        return tPCancelJson;
    }


    /**
     * 公告作废-招标采购单
     */
    public static JSONObject getZbCancelJson(HashMap<String, Object> cancelMap) {
        JSONObject zbCancelJson = new JSONObject() {
            {
                // 是否对外网发布 1：是 2：否
                put("isPublicity", 1);
                // 流标类型 1：终止招标 2：重新招标
                put("closeType", 1);
                // 流标原因
                put("closeReason", cancelMap.get("closeReason"));
                // 其他原因
                put("otherReason", cancelMap.get("otherReason"));
                // 公告标题
                put("title", cancelMap.get("title"));
                // 公告内容
                put("content", cancelMap.get("content"));
                // 附件说明
                put("closeAttachmentIds", cancelMap.get("closeAttachmentIds"));
            }
        };

        return zbCancelJson;
    }


    /**
     * 添加品目元数据
     *
     * @param jsonObject
     * @return
     */
    public static JSONObject addItemSchemaList(JSONObject jsonObject) {
        jsonObject.put("itemSchemaList", new JSONArray()
                .fluentAdd(new JSONObject()
                        .fluentPut("columnName", "品目编码")
                        .fluentPut("defineType", 1)
                        .fluentPut("defineCode", "materielCode")
                        .fluentPut("supplierVisible", 1)
                        .fluentPut("isEnable", 1)
                        .fluentPut("columnType", 1)
                )
                .fluentAdd(new JSONObject()
                        .fluentPut("columnName", "品目名称")
                        .fluentPut("defineType", 1)
                        .fluentPut("defineCode", "materielName")
                        .fluentPut("supplierVisible", 1)
                        .fluentPut("isEnable", 1)
                        .fluentPut("columnType", 2)
                )
                .fluentAdd(new JSONObject()
                        .fluentPut("columnName", "采购量")
                        .fluentPut("defineType", 1)
                        .fluentPut("defineCode", "materielNum")
                        .fluentPut("supplierVisible", 1)
                        .fluentPut("isEnable", 1)
                        .fluentPut("columnType", 3)
                )
                .fluentAdd(new JSONObject()
                        .fluentPut("columnName", "计量单位")
                        .fluentPut("defineType", 1)
                        .fluentPut("defineCode", "unitType")
                        .fluentPut("supplierVisible", 1)
                        .fluentPut("isEnable", 1)
                        .fluentPut("columnType", 4)
                )
                .fluentAdd(new JSONObject()
                        .fluentPut("columnName", "预算单价")
                        .fluentPut("defineType", 1)
                        .fluentPut("defineCode", "budgetUnitPrice")
                        .fluentPut("supplierVisible", 2)
                        .fluentPut("isEnable", 1)
                        .fluentPut("columnType", 5)
                )
                .fluentAdd(new JSONObject()
                        .fluentPut("columnName", "参数型号")
                        .fluentPut("defineType", 1)
                        .fluentPut("defineCode", "materielModel")
                        .fluentPut("supplierVisible", 1)
                        .fluentPut("isEnable", 1)
                        .fluentPut("columnType", 6)
                )
                .fluentAdd(new JSONObject()
                        .fluentPut("columnName", "品牌/制造商")
                        .fluentPut("defineType", 2)
                        .fluentPut("defineCode", "materielBrand")
                        .fluentPut("supplierVisible", 1)
                        .fluentPut("isEnable", 1)
                        .fluentPut("columnType", 0)
                )
                .fluentAdd(new JSONObject()
                        .fluentPut("columnName", "交货地点")
                        .fluentPut("defineType", 2)
                        .fluentPut("defineCode", "deliveryAddress")
                        .fluentPut("supplierVisible", 1)
                        .fluentPut("isEnable", 1)
                        .fluentPut("columnType", 0)
                )
                .fluentAdd(new JSONObject()
                        .fluentPut("columnName", "交货周期（天）")
                        .fluentPut("defineType", 2)
                        .fluentPut("defineCode", "deliveryDay")
                        .fluentPut("supplierVisible", 1)
                        .fluentPut("isEnable", 1)
                        .fluentPut("columnType", 0)
                )
                .fluentAdd(new JSONObject()
                        .fluentPut("columnName", "参数型号")
                        .fluentPut("defineType", 3)
                        .fluentPut("defineCode", "offerMaterielModel")
                        .fluentPut("supplierRequire", 2)
                        .fluentPut("isEnable", 1)
                        .fluentPut("columnType", 0)
                )
                .fluentAdd(new JSONObject()
                        .fluentPut("columnName", "品牌/制造商")
                        .fluentPut("defineType", 3)
                        .fluentPut("defineCode", "offerMaterielBrand")
                        .fluentPut("supplierRequire", 2)
                        .fluentPut("isEnable", 1)
                        .fluentPut("columnType", 0)
                )
                .fluentAdd(new JSONObject()
                        .fluentPut("columnName", "交货周期（天）")
                        .fluentPut("defineType", 3)
                        .fluentPut("defineCode", "offerDeliveryDay")
                        .fluentPut("supplierRequire", 2)
                        .fluentPut("isEnable", 1)
                        .fluentPut("columnType", 0)
                )
                .fluentAdd(new JSONObject()
                        .fluentPut("columnName", "控制价")
                        .fluentPut("defineType", 1)
                        .fluentPut("defineCode", "controlPrice")
                        .fluentPut("supplierVisible", 1)
                        .fluentPut("isEnable", 0)
                        .fluentPut("columnType", 7)
                )
        );
        return jsonObject;
    }


    /**
     * 获取附件id集合
     *
     * @param zcPlatformConst
     * @param attachments
     * @return
     */
    public static ArrayList<Integer> getAttIdList(ZcPlatformConst zcPlatformConst, DynamicObjectCollection attachments) {
        ArrayList<Integer> attList = new ArrayList<>();
        for (DynamicObject obj : attachments) {
            DynamicObject fbasedataId = obj.getDynamicObject("fbasedataId");
            String name = fbasedataId.getString("name");
            String url = (String) fbasedataId.get("url");
            String realPath = FileServiceExtFactory.getAttachFileServiceExt().getRealPath(url);
            realPath = UrlService.getAttachmentFullUrl(realPath);
            realPath = convertToFullPath(realPath, name);

            Integer attachmentId = ZcPlatformApiUtil.uploadFile(zcPlatformConst, name, realPath, null);

            attList.add(attachmentId);
        }
        return attList;
    }


    /**
     * 下载文件并返回文件绝对路径
     *
     * @param fileUrl
     * @return
     */
    public static String convertToFullPath(String fileUrl, String fileName) {
        String homeUrl = "/home";
        String saveDir = homeUrl + "/" + System.currentTimeMillis() + fileName;
        File file = new File(saveDir);

        String accessToken = RequestContext.get().getGlobalSessionId();
        fileUrl = fileUrl + "&access_token=" + accessToken;

        HttpUtil.downloadFile(fileUrl, file);
        return saveDir;
    }

    /**
     * 格式化地址信息至招采平台
     *
     * @param addressId
     * @return
     */
    public static HashMap<String, String> formatAddress(String addressId) {
        HashMap<String, String> map = new HashMap<>();
        DynamicObject addressObj = getAdminDivision(addressId);
        String fullName = addressObj.getString("fullname");

        String[] areas = fullName.split("_");

        if (areas.length == 3) {
            map.put("国家", "中国");
            map.put("省", areas[0]);
            map.put("市", areas[1]);
            map.put("县区", areas[2]);
        } else if (areas.length == 2) {
            if (isMunicipality(areas[0])) {
                String[] newAreas = {areas[0], "市辖区", areas[1]};
                map.put("国家", "中国");
                map.put("省", newAreas[0]);
                map.put("市", newAreas[1]);
                map.put("县区", newAreas[2]);
            }
        }

        if (map.size() != 4) {
            throw new KDBizException("请检查地址信息!");
        }

        return map;
    }

    /**
     * 获取行政区划id查询行政区划
     */
    private static DynamicObject getAdminDivision(String adminId) {
        DynamicObject dynamicObject = new DynamicObject();
        // 条件
        ORM orm = ORM.create();
        QFilter filter = new QFilter("id", "=", Long.parseLong(adminId));
        QFilter[] filters = new QFilter[]{filter};

        // 查询
        List<DynamicObject> dynamicObjects = orm.query("bd_admindivision", "id,name,fullname,country.id,country.name", filters, "id asc");

        if (!dynamicObjects.isEmpty()) {
            dynamicObject = dynamicObjects.get(0);
            return dynamicObject;
        }
        return dynamicObject;

    }

    /**
     * 行政区划过滤
     *
     * @param area
     * @return
     */
    private static boolean isMunicipality(String area) {
        return area.equals("北京市") || area.equals("天津市") || area.equals("上海市") || area.equals("重庆市");
    }


}

