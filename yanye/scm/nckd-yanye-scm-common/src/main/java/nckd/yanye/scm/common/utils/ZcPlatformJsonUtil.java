package nckd.yanye.scm.common.utils;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.IDataModel;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import nckd.yanye.scm.common.PurapplybillConst;
import nckd.yanye.scm.common.SupplierConst;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 招采平台json组装工具类
 *
 * @author liuxiao
 */
public class ZcPlatformJsonUtil {

    /**
     * 公告发布-招标采购单
     *
     * @param model
     * @return
     */
    public static JSONObject getZbJson(IDataModel model) {
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
                //fixme 地址信息
                // 招标地址-国家
                put("country", "中国");
                // 招标地址-所属省
                put("province", "江西省");
                // 招标地址-所属市
                put("city", "南昌市");
                // 招标地址-所属区县
                put("area", "红谷滩区");
                // 招标地址-详细地址
                put("detailAddress", model.getValue(PurapplybillConst.NCKD_DETAILEDADDR3));
                // 招标方式
                put("biddingMethod", model.getValue(PurapplybillConst.NCKD_BIDDINGMETHOD));
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
                // todo-在线开评标。赋值biddingFileId。在线生成标书
                put("isKpbProject", model.getValue(PurapplybillConst.NCKD_BIDONLINE));
                // 标书费
                put("bookFee", model.getValue(PurapplybillConst.NCKD_TENDERFEE1));
            }
        };
        // 邀请供应商列表
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
        // 保证金
        zbJson.put("guarantee", new JSONObject() {
            {
                // 支持保证金缴纳形式-银行转账
                put("supportGf", 1);
                // 支持保证金缴纳形式-允许在线申请投标保函
//                put("supportOnlineGfLetter:", 1);
                // 保证金缴纳截止时间
                put("gfDeadline", model.getValue(PurapplybillConst.NCKD_DEPOSITENDTIME));
                // 保证金金额
                put("guaranteeFee", model.getValue(PurapplybillConst.NCKD_MARGINAMOUNT));
            }
        });
        // 内部文件
        zbJson.put("internalAttachments", new ArrayList<Integer>() {
            {
                DynamicObjectCollection xbAtts = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_INTERNALATTACHMENTS);
                Integer attGroupId = ZcPlatformApiUtil.addAttachmentGroup("ZB", "ZBWJ");
                for (DynamicObject obj : xbAtts) {
                    DynamicObject fbasedataId = obj.getDynamicObject("fbasedataId");
                    String name = fbasedataId.getString("name");
                    String url = (String) fbasedataId.get("url");
                    Integer internalAttachmentId = ZcPlatformApiUtil.uploadFile(name, url, attGroupId);
                    this.add(internalAttachmentId);
                }
            }
        });
        // 上传文件
        zbJson.put("biddingAttachmentIds", new ArrayList<Integer>() {
            {
                DynamicObjectCollection xbAtts = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_UPLOADFILE);
                Integer attGroupId = ZcPlatformApiUtil.addAttachmentGroup("ZB", "ZBWJ");
                for (DynamicObject obj : xbAtts) {
                    DynamicObject fbasedataId = obj.getDynamicObject("fbasedataId");
                    String name = fbasedataId.getString("name");
                    String url = (String) fbasedataId.get("url");
                    Integer biddingAttachmentId = ZcPlatformApiUtil.uploadFile(name, url, attGroupId);
                    this.add(biddingAttachmentId);
                }
            }
        });
        // 其他附件
        zbJson.put("otherAttachmentIds", new ArrayList<Integer>() {
            {
                DynamicObjectCollection xbAtts = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_OTHERANNEXES);
                Integer attGroupId = ZcPlatformApiUtil.addAttachmentGroup("ZB", "ZBWJ");
                for (DynamicObject obj : xbAtts) {
                    DynamicObject fbasedataId = obj.getDynamicObject("fbasedataId");
                    String name = fbasedataId.getString("name");
                    String url = (String) fbasedataId.get("url");
                    Integer otherAttachmentId = ZcPlatformApiUtil.uploadFile(name, url, attGroupId);
                    this.add(otherAttachmentId);
                }
            }
        });

        // 公告标题
        zbJson.put("title", model.getValue(PurapplybillConst.NCKD_ANNOUNCEMENTTITLE));
        // 公告内容
        zbJson.put("content", model.getValue(PurapplybillConst.NCKD_BIGNOTICECONTENT));
        return zbJson;
    }

    /**
     * 公告发布-询比采购单
     *
     * @param model
     * @return
     */
    public static JSONObject getXbJson(IDataModel model) {
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

        return xbJson;
    }

    /**
     * 公告发布-单一供应商
     *
     * @param model
     * @return
     */
    public static JSONObject getDyJson(IDataModel model) {
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
                put("priceType", model.getValue(PurapplybillConst.NCKD_QUOTATION2));
                // todo 项目地点
                // 招标地址-国家
                put("country", "中国");
                // 招标地址-所属省
                put("province", "江西省");
                // 招标地址-所属市
                put("city", "南昌市");
                // 招标地址-所属区县
                put("area", "红谷滩区");
                // 招标地址-详细地址
                put("detailAddress", model.getValue(PurapplybillConst.NCKD_DETAILEDADDR2));
            }
        };

        // 单一供应商
        dyJson.put("inviteSup", new JSONArray() {
            {
                add(new JSONObject() {
                    {
                        DynamicObject inviteObj = (DynamicObject) model.getValue(PurapplybillConst.NCKD_SUPPLIERS2);
                        // 供应商id
                        put("supplierId", inviteObj.getString(SupplierConst.NCKD_PLATFORMSUPID));
                        // 供应商名称
                        put("supplierName", inviteObj.getString(SupplierConst.NAME));
                        // 联合体公司

                    }
                });
            }
        });

        // 采购范围-附件

        // 公告标题
        dyJson.put("title", model.getValue(PurapplybillConst.NCKD_ANNOUNCEMENTTITLE));
        // 公告内容
        dyJson.put("content", model.getValue(PurapplybillConst.NCKD_BIGNOTICECONTENT));

        return dyJson;
    }

    /**
     * 公告发布-谈判采购单
     *
     * @param model
     * @return
     */
    public static JSONObject getTpJson(IDataModel model) {
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
                //fixme 地址信息
                // 谈判地址-国家
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
                String competeType = (String) model.getValue(PurapplybillConst.NCKD_COMPETITIONMODE);
                put("competeType", competeType);
                // 公开范围-“竞争方式”选择有限竞争，则不显示该字段
                if (!"2".equals(competeType)) {
                    put("openScope", model.getValue(PurapplybillConst.NCKD_PUBLICSCOPE1));
                }
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
                        DynamicObjectCollection tpAtts = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_NEGOTIATINGDOCUMENTS);
                        Integer attGroupId = ZcPlatformApiUtil.addAttachmentGroup("TP", "TPWJ");
                        for (DynamicObject obj : tpAtts) {
                            DynamicObject fbasedataId = obj.getDynamicObject("fbasedataId");
                            String name = fbasedataId.getString("name");
                            String url = (String) fbasedataId.get("url");
                            Integer negotiateFileId = ZcPlatformApiUtil.uploadFile(name, url, attGroupId);
                            this.add(negotiateFileId);
                        }
                    }
                });
                // todo 内部附件
                put("internalAttachments", new ArrayList<Integer>() {
                    {
                        DynamicObjectCollection tpAtts = (DynamicObjectCollection) model.getValue(PurapplybillConst.NCKD_INTERNALDOCUMENTS1);
                        Integer attGroupId = ZcPlatformApiUtil.addAttachmentGroup("TP", "NBFJ");
                        for (DynamicObject obj : tpAtts) {
                            DynamicObject fbasedataId = obj.getDynamicObject("fbasedataId");
                            String name = fbasedataId.getString("name");
                            String url = (String) fbasedataId.get("url");
                            Integer internalAttachmentId = ZcPlatformApiUtil.uploadFile(name, url, attGroupId);
                            this.add(internalAttachmentId);
                        }
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
                //公告标题-“竞争方式”为有限竞争就取 邀请函标题
                if ("2".equals(model.getValue(PurapplybillConst.NCKD_COMPETITIONMODE))) {
                    put("title", model.getValue(PurapplybillConst.NCKD_INVITATIONTITLE));
                } else {
                    put("noticeTitle", model.getValue(PurapplybillConst.NCKD_ANNOUNCEMENTTITLE));
                }
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
     * 公告作废-询比采购单
     */
    public static JSONObject getXbCancelJson() {
        JSONObject xbCancelJson = new JSONObject() {
            {
                // 是否对外网公示 1：是 0：否
                put("closePublicity", 0);
                //流标类型 1：终止 2：重新
                put("closeType", 1);
//                // 流标原因
//                put("closeReason", 5);
//                // 其他原因
//                put("otherReason", "其它原因");
//                //关闭公告
                put("notice", new JSONObject() {
                    {
                        put("noticeTitle", "流标公告");
//                        put("noticeContent", "测试流标公告内容");
                    }
                });
            }
        };
        return xbCancelJson;
    }

    /**
     * 公告作废-谈判采购单
     */
    public static JSONObject getTpCancelJson() {
        JSONObject tPCancelJson = new JSONObject() {
            {
                // 是否对外网公示 1：是 0：否
                put("closePublicity", 0);
                //流标类型 1：终止 2：重新
                put("closeType", 1);
//                // 流标原因
//                put("closeReason", 5);
//                // 其他原因
//                put("otherReason", "其它原因");
//                //关闭公告
//                put("notice", new JSONObject() {
//                    {
//                        put("title", "测试流标公告标题");
//                        put("content", "测试流标公告内容");
//                    }
//                });
            }
        };
        return tPCancelJson;
    }


    /**
     * 公告作废-招标采购单
     */
    public static JSONObject getZbCancelJson() {
        JSONObject zbCancelJson = new JSONObject() {
            {
                // 是否对外网发布 1：是 2：否
                put("isPublicity", 2);
                // 流标类型 1：终止招标 2：重新招标
                put("closeType", 1);
//                // 流标原因
//                put("closeReason", 5);
//                // 其他原因
//                put("otherReason", "其它原因");
//                // 公告标题
//                put("title", "测试流标公告标题");
//                // 公告内容
//                put("content", "测试流标公告内容");
            }
        };

        return zbCancelJson;
    }

}
