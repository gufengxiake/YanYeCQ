package nckd.yanye.scm.common;

import com.alibaba.fastjson.JSONObject;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.ILocaleString;
import kd.bos.dataentity.metadata.clr.DataEntityPropertyCollection;
import kd.bos.dataentity.metadata.dynamicobject.DynamicObjectType;
import kd.bos.dataentity.metadata.dynamicobject.DynamicProperty;
import kd.bos.entity.*;
import kd.bos.entity.property.BasedataProp;
import kd.bos.entity.property.EntryProp;
import kd.bos.servicehelper.devportal.BizCloudServiceHelp;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * ConstClassAutoGenerator util = new ConstClassAutoGenerator("fileSrc", "classSrc", "className","bamp", "bd_measureunits");
 * util.InitEntityConstantJavaFile();
 * util.getOutputEntityConstantJavaFile();
 */
public class ConstClassAutoGenerator {

    private String src;
    private String classSrc;
    private String infoname;
    private String appId;
    private String entityId;

    StringBuilder outputEntityInfoJavaFile;
    StringBuilder outputEntityConstantJavaFile;

    public ConstClassAutoGenerator() {
    }

    public ConstClassAutoGenerator(String src, String classSrc, String infoname, String appId, String entityId) {
        this.src = src;
        this.classSrc = classSrc;
        this.infoname = infoname;
        this.appId = appId;
        this.entityId = entityId;
        outputEntityInfoJavaFile = new StringBuilder();
        outputEntityConstantJavaFile = new StringBuilder();
    }

    /**********************输出Constant类部分开始**********************/
    public void InitEntityConstantJavaFile() throws Exception {
        outPutEntityConstantJavaFileHead();
        outPutEntityConstantJavaFilefield();
        outPutEntityConstantJAVAFile();
    }

    public String outPutEntityConstantJavaFileHead() throws BadHanyuPinyinOutputFormatCombination {

        DynamicObjectType dt = EntityMetadataCache.getDataEntityType(this.getEntityid());
        AppInfo appInfo = AppMetadataCache.getAppInfo(((MainEntityType) dt).getAppId());
        DynamicObject bizCloudInfo = BizCloudServiceHelp.getBizCloudByID(appInfo.getCloudId());

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(this.classSrc).append(";\n\n");


        sb.append("/**\n");
        sb.append(" * Module           : ").append(bizCloudInfo.getString("name")).append("--").append(appInfo.getName()).append("模块--").append(dt.getDisplayName()).append("单据\n");
        sb.append(" * Description      : 单据常量类\n");
        sb.append(" * @date            : ").append(format.format(new Date())).append("\n");
        sb.append(" * @author          : Generator\n");
        sb.append(" * @version         : 1.0\n");
        sb.append(" */\n");
        sb.append("public class ").append(getOneUpperCase(this.infoname)).append("Const {\n\n");
        outputEntityConstantJavaFile.append(sb);


        return sb.toString();
    }

    public String outPutEntityConstantJavaFilefield() {
        StringBuilder sb = new StringBuilder();
        StringBuilder allPorper = new StringBuilder();
        sb.append("\tpublic static final String FORMBILLID = \"").append(this.getEntityid()).append("\";\n");
        sb.append("\tpublic static final String ID = \"id\";\n\n");

        DynamicObjectType dt = EntityMetadataCache.getDataEntityType(this.getEntityid());
        DataEntityPropertyCollection propertys = dt.getProperties();

        for (kd.bos.dataentity.metadata.IDataEntityProperty iDataEntityProperty : propertys) {
            DynamicProperty property = (DynamicProperty) iDataEntityProperty;
            try {
                if (property.getPropertyType() == null) {
                    if (property instanceof EntryProp) {
                        //首先加上分录实体标识
                        sb.append("\t/**\n");
                        sb.append("\t * 分录").append(property.getName()).append("实体标识\n");
                        sb.append("\t */\n");
                        sb.append("\tpublic static final String ENTRYENTITYID_").append(property.getName().toUpperCase()).append(" = \"").append(property.getName()).append("\";\n\n");
                        String entryname = getOneUpperCaseAndDeleteGysbs(property.getName()) + "_";
                        String entryname_index = property.getName() + ".";
                        EntryType entryDT = (EntryType) ((EntryProp) property).getDynamicCollectionItemPropertyType();
                        DataEntityPropertyCollection entryPropertys = entryDT.getProperties();
                        for (kd.bos.dataentity.metadata.IDataEntityProperty dataEntityProperty : entryPropertys) {
                            DynamicProperty entryProperty = (DynamicProperty) dataEntityProperty;
                            if (entryProperty.getPropertyType() == null) {
                                //子分录**************************************Start*******//
                                if (entryProperty instanceof EntryProp) {
                                    //首先加上子分录实体标识
                                    sb.append("\t/**\n");
                                    sb.append("\t * 子分录" + entryProperty.getName() + "实体标识\n");
                                    sb.append("\t */\n");
                                    sb.append("\tpublic static final String SUBENTRYENTITYID_" + entryProperty.getName().toUpperCase() + " = \"" + entryProperty.getName() + "\";\n\n");
                                    String subEntryname = getOneUpperCaseAndDeleteGysbs(entryProperty.getName()) + "_";
                                    EntryType subEntryDT = (EntryType) ((EntryProp) entryProperty).getDynamicCollectionItemPropertyType();
                                    DataEntityPropertyCollection subEntryPropertys = subEntryDT.getProperties();
                                    for (int k = 0; k < subEntryPropertys.size(); k++) {
                                        DynamicProperty subEntryProperty = (DynamicProperty) subEntryPropertys.get(k);
                                        if (subEntryProperty.getPropertyType() == null) {
                                            //孙分录**************************************Start*******//
                                            if (subEntryProperty instanceof EntryProp) {
                                                //首先加上子分录实体标识
                                                sb.append("\t/**\n");
                                                sb.append("\t * 孙分录" + subEntryProperty.getName() + "实体标识\n");
                                                sb.append("\t */\n");
                                                sb.append("\tpublic static final String GSENTRYENTITYID_" + subEntryProperty.getName().toUpperCase() + " = \"" + subEntryProperty.getName() + "\";\n\n");
                                                String gsEntryname = getOneUpperCaseAndDeleteGysbs(subEntryProperty.getName()) + "_";
                                                EntryType gsEntryDT = (EntryType) ((EntryProp) subEntryProperty).getDynamicCollectionItemPropertyType();
                                                DataEntityPropertyCollection gsEntryPropertys = gsEntryDT.getProperties();
                                                for (int l = 0; l < gsEntryPropertys.size(); l++) {
                                                    DynamicProperty gsEntryProperty = (DynamicProperty) gsEntryPropertys.get(l);
                                                    if (gsEntryProperty.getPropertyType() == null) {
                                                        continue;
                                                    }

                                                    if ("id".equals(gsEntryProperty.getName()) || "entityId".equals(gsEntryProperty.getName()) || "seq".equals(gsEntryProperty.getName()) || gsEntryProperty.getName().indexOf("_tag") > 5
                                                            || (gsEntryProperty.getDisplayName() == null && gsEntryProperty.getName().indexOf("_id") > 0)) {//不处理的字段
                                                        continue;
                                                    } else if (gsEntryProperty.getPropertyType().equals(ILocaleString.class) || gsEntryProperty.getPropertyType().equals(String.class)) {
                                                        sb.append("\t/**\n");
                                                        sb.append("\t * Type:String,Name:" + gsEntryProperty.getDisplayName() + "\n");
                                                        sb.append("\t */\n");
                                                        sb.append("\tpublic static final String " + subEntryname + getOneUpperCaseAndDeleteGysbs(gsEntryProperty.getName()) + " = \"" + gsEntryProperty.getName() + "\";\n\n");
                                                    } else if (gsEntryProperty.getPropertyType().equals(DynamicObject.class)) {
                                                        String baseType = "";
                                                        if (gsEntryProperty instanceof BasedataProp) {
                                                            if (((BasedataProp) gsEntryProperty).getBaseEntityId() != null) {
                                                                baseType = "sourceEntityId:" + ((BasedataProp) gsEntryProperty).getBaseEntityId() + ",";
                                                            }
                                                        }
                                                        sb.append("\t/**\n");
                                                        sb.append("\t * Type:DynamicObject," + baseType + "Name:" + gsEntryProperty.getDisplayName() + "\n");
                                                        sb.append("\t */\n");
                                                        sb.append("\tpublic static final String " + subEntryname + getOneUpperCaseAndDeleteGysbs(gsEntryProperty.getName()) + " = \"" + gsEntryProperty.getName() + "\";\n\n");
                                                    } else {
                                                        sb.append("\t/**\n");
                                                        sb.append("\t * Type:" + gsEntryProperty.getPropertyType().getName() + ",Name:" + gsEntryProperty.getDisplayName() + "\n");
                                                        sb.append("\t */\n");
                                                        sb.append("\tpublic static final String " + subEntryname + getOneUpperCaseAndDeleteGysbs(gsEntryProperty.getName()) + " = \"" + gsEntryProperty.getName() + "\";\n\n");
                                                    }

                                                }

                                            } else {
                                                System.out.println(property.getName());
                                            }
                                            //孙分录****************************************End*****//
                                            continue;
                                        }
                                        if ("id".equals(subEntryProperty.getName()) || "entityId".equals(subEntryProperty.getName()) || "seq".equals(subEntryProperty.getName()) || subEntryProperty.getName().indexOf("_tag") > 5
                                                || (subEntryProperty.getDisplayName() == null && subEntryProperty.getName().indexOf("_id") > 0)) {//不处理的字段
                                            continue;
                                        } else if (subEntryProperty.getPropertyType().equals(ILocaleString.class) || subEntryProperty.getPropertyType().equals(String.class)) {
                                            sb.append("\t/**\n");
                                            sb.append("\t * Type:String,Name:" + subEntryProperty.getDisplayName() + "\n");
                                            sb.append("\t */\n");
                                            sb.append("\tpublic static final String " + subEntryname + getOneUpperCaseAndDeleteGysbs(subEntryProperty.getName()) + " = \"" + subEntryProperty.getName() + "\";\n\n");
                                        } else if (subEntryProperty.getPropertyType().equals(DynamicObject.class)) {
                                            String baseType = "";
                                            if (subEntryProperty instanceof BasedataProp) {
                                                if (((BasedataProp) subEntryProperty).getBaseEntityId() != null) {
                                                    baseType = "sourceEntityId:" + ((BasedataProp) subEntryProperty).getBaseEntityId() + ",";
                                                }
                                            }
                                            sb.append("\t/**\n");
                                            sb.append("\t * Type:DynamicObject," + baseType + "Name:" + subEntryProperty.getDisplayName() + "\n");
                                            sb.append("\t */\n");
                                            sb.append("\tpublic static final String " + subEntryname + getOneUpperCaseAndDeleteGysbs(subEntryProperty.getName()) + " = \"" + subEntryProperty.getName() + "\";\n\n");
                                        } else {
                                            sb.append("\t/**\n");
                                            sb.append("\t * Type:" + subEntryProperty.getPropertyType().getName() + ",Name:" + subEntryProperty.getDisplayName() + "\n");
                                            sb.append("\t */\n");
                                            sb.append("\tpublic static final String " + subEntryname + getOneUpperCaseAndDeleteGysbs(subEntryProperty.getName()) + " = \"" + subEntryProperty.getName() + "\";\n\n");
                                        }
                                    }
                                } else {
                                    System.out.println(property.getName());
                                }
                                continue;
                                //子分录****************************************End*****//
                            }
                            if ("id".equals(entryProperty.getName()) || "entityId".equals(entryProperty.getName()) || "seq".equals(entryProperty.getName()) || entryProperty.getName().indexOf("_tag") > 5
                                    || (entryProperty.getDisplayName() == null && entryProperty.getName().indexOf("_id") > 0)) {
                                //不处理的字段
                                if ("id".equals(entryProperty.getName()) || "entityId".equals(entryProperty.getName())) {
                                    allPorper.append(entryname_index + entryProperty.getName() + ",");
                                } else if (property.getName().indexOf("_id") > 0) {

                                } else if (property.getName().indexOf("_tag") > 5) {

                                }
                                continue;
                            } else if (entryProperty.getPropertyType().equals(ILocaleString.class) || entryProperty.getPropertyType().equals(String.class)) {
                                sb.append("\t/**\n");
                                sb.append("\t * Type:String,Name:" + entryProperty.getDisplayName() + "\n");
                                sb.append("\t */\n");
                                sb.append("\tpublic static final String " + entryname + getOneUpperCaseAndDeleteGysbs(entryProperty.getName()) + " = \"" + entryProperty.getName() + "\";\n\n");
                                allPorper.append(entryname_index + entryProperty.getName() + ",");
                            } else if (entryProperty.getPropertyType().equals(DynamicObject.class)) {
                                String baseType = "";
                                if (entryProperty instanceof BasedataProp) {
                                    if (((BasedataProp) entryProperty).getBaseEntityId() != null) {
                                        baseType = "sourceEntityId:" + ((BasedataProp) entryProperty).getBaseEntityId() + ",";
                                    }
                                }
                                sb.append("\t/**\n");
                                sb.append("\t * Type:DynamicObject," + baseType + "Name:" + entryProperty.getDisplayName() + "\n");
                                sb.append("\t */\n");
                                sb.append("\tpublic static final String " + entryname + getOneUpperCaseAndDeleteGysbs(entryProperty.getName()) + " = \"" + entryProperty.getName() + "\";\n\n");
                                allPorper.append(entryname_index + entryProperty.getName() + ",");
                            } else {
                                sb.append("\t/**\n");
                                sb.append("\t * Type:" + entryProperty.getPropertyType().getName() + ",Name:" + entryProperty.getDisplayName() + "\n");
                                sb.append("\t */\n");
                                sb.append("\tpublic static final String " + entryname + getOneUpperCaseAndDeleteGysbs(entryProperty.getName()) + " = \"" + entryProperty.getName() + "\";\n\n");
                                allPorper.append(entryname_index + entryProperty.getName() + ",");
                            }
                        }
                    } else {
                        System.out.println(property.getName());
                    }
                    continue;
                }
                if ("id".equals(property.getName()) || "entityId".equals(property.getName()) || "multilanguagetext".equals(property.getName()) || property.getName().indexOf("_tag") > 5
                        || (property.getDisplayName() == null && property.getName().indexOf("_id") > 0)) {
                    //不处理的字段
                    if ("id".equals(property.getName()) || "entityId".equals(property.getName())) {
                        allPorper.append(property.getName()).append(",");
                    } else if (property.getName().indexOf("_id") > 0) {
                        //待处理

                    }

                    continue;
                } else if (property.getPropertyType().equals(ILocaleString.class) || property.getPropertyType().equals(String.class)) {
                    sb.append("\t/**\n");
                    sb.append("\t * Type:String,Name:" + property.getDisplayName() + "\n");
                    sb.append("\t */\n");
                    sb.append("\tpublic static final String " + getOneUpperCaseAndDeleteGysbs(property.getName()) + " = \"" + property.getName() + "\";\n\n");
                    allPorper.append(property.getName()).append(",");
                    continue;
                } else if (property.getPropertyType().equals(DynamicObject.class)) {
                    String baseType = "";
                    if (property instanceof BasedataProp) {
                        baseType = "sourceEntityId:" + ((BasedataProp) property).getBaseEntityId() + ",";
                    }
                    sb.append("\t/**\n");
                    sb.append("\t * Type:DynamicObject," + baseType + "Name:" + property.getDisplayName() + "\n");
                    sb.append("\t */\n");
                    sb.append("\tpublic static final String " + getOneUpperCaseAndDeleteGysbs(property.getName()) + " = \"" + property.getName() + "\";\n\n");
                    allPorper.append(property.getName() + ",");
                    continue;
                } else {
                    sb.append("\t/**\n");
                    sb.append("\t * Type:" + property.getPropertyType().getName() + ",Name:" + property.getDisplayName() + "\n");
                    sb.append("\t */\n");
                    sb.append("\tpublic static final String " + getOneUpperCaseAndDeleteGysbs(property.getName()) + " = \"" + property.getName() + "\";\n\n");
                    allPorper.append(property.getName()).append(",");
                    continue;
                }
            } catch (Exception e) {
                e.toString();
            }
        }
        if (allPorper.length() > 0) {//s
            String properStr = allPorper.substring(0, allPorper.length() - 1);
            sb.append("\tpublic static final String ALLPROPERTY = \"" + properStr + "\";\n\n");
        }
        outputEntityConstantJavaFile.append(sb);
        return sb.toString();
    }

    public String outPutEntityConstantJAVAFile() throws Exception {
        outputEntityConstantJavaFile.append("}");
        return outputEntityConstantJavaFile.toString();
    }

    /**********************输出Constant类部分结束**********************/
    public StringBuilder getOutputEntityInfoJavaFile() {
        return outputEntityInfoJavaFile;
    }

    public void setOutputEntityInfoJavaFile(StringBuilder outputEntityInfoJavaFile) {
        this.outputEntityInfoJavaFile = outputEntityInfoJavaFile;
    }

    public StringBuilder getOutputEntityConstantJavaFile() {
        return outputEntityConstantJavaFile;
    }


    public void setOutputEntityConstantJavaFile(StringBuilder outputEntityConstantJavaFile) {
        this.outputEntityConstantJavaFile = outputEntityConstantJavaFile;
    }


    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getClasssrc() {
        return classSrc;
    }

    public void setClasssrc(String classSrc) {
        this.classSrc = classSrc;
    }

    public String getInfoname() {
        return infoname;
    }

    public void setInfoname(String infoname) {
        this.infoname = infoname;
    }

    public String getEntityid() {
        return entityId;
    }

    public void setEntityid(String entityId) {
        this.entityId = entityId;
    }


    public String getOneUpperCase(String a) {
        if (a == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String case1 = a.substring(0, 1).toUpperCase();
        builder.append(case1).append(a.substring(1));
        return builder.toString();
    }

    public String getOneUpperCaseAndDeleteGysbs(String a) {
        if (a == null) {
            return "";
        }
        if (a.indexOf(appId) == 0) {
            //找到了,则截取
            a = a.substring(appId.length());
        }
        return a.toUpperCase();
    }


    public void generate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"get".equalsIgnoreCase(request.getMethod())) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("msg", "请求方式必须为GET。");
            this.response(request, response, result);
            return;
        }
        RequestContext ctx = RequestContext.create();
        String datacenterId = request.getParameter("datacenterId");
        if (!StringUtils.isEmpty(datacenterId)) {
            ctx.setAccountId(datacenterId);
        }
        String tenantCode = request.getParameter("tenantCode");
        if (!StringUtils.isEmpty(tenantCode)) {
            ctx.setTenantId(tenantCode);
            ctx.setTenantCode(tenantCode);
        }

        String fileSrc = request.getParameter("fileSrc");
        String appId = request.getParameter("appId");
        String entityId = request.getParameter("entityId");
        String className = request.getParameter("className");

        if (fileSrc.endsWith("/")) {
            fileSrc = fileSrc.substring(0, fileSrc.length() - 1);
        }
        String classSrc = fileSrc.substring(fileSrc.indexOf("src/main/java/"))
                .replace("src/main/java/", "").replace("/", ".");
        File file = new File(fileSrc);
        if (!file.exists()) {
            file.mkdirs();
        }
        ConstClassAutoGenerator util = new ConstClassAutoGenerator(fileSrc, classSrc, className, appId, entityId);
        try {
            util.InitEntityConstantJavaFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String path = fileSrc + "/" + util.getOneUpperCase(util.getInfoname()) + "Const.java";
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert fileOutputStream != null;
        OutputStreamWriter out = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
        BufferedWriter bw = new BufferedWriter(out);
        try {
            bw.flush();
            bw.write(util.getOutputEntityConstantJavaFile().toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bw.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("msg", "OK");
        this.response(request, response, result);
    }

    private void response(HttpServletRequest request, HttpServletResponse response, Map<String, Object> result) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        writer.write(JSONObject.toJSONString(result));
        writer.flush();
        writer.close();
    }
}
