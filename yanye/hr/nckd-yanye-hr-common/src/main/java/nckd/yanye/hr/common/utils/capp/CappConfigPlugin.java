package nckd.yanye.hr.common.utils.capp;

import java.util.EventObject;
import kd.bos.dataentity.utils.StringUtils;
import kd.bos.encrypt.Encrypters;
import kd.bos.form.control.Control;
import kd.bos.form.control.events.BeforeClickEvent;
import kd.bos.form.control.events.ClickListener;
import kd.bos.form.events.AfterDoOperationEventArgs;
import kd.bos.form.plugin.AbstractFormPlugin;
import nckd.yanye.hr.common.utils.capp.CappConfig;

public class CappConfigPlugin
        extends AbstractFormPlugin
        implements ClickListener
{
    private static final String PICTURE_FIELD = "picturefield";
    private static final String ENCODE_VALUE = "encodevalue";
    private static final String TEXT_VALUE = "textvalue";
    private static final String USER_VALUE = "usevalue";

    public void registerListener(EventObject e) {
        super.registerListener(e);
        addClickListeners(new String[] { "save" });
    }


    public void beforeClick(BeforeClickEvent evt) {
        super.beforeClick(evt);
        if (StringUtils.equals("save", ((Control)evt.getSource()).getKey())) {

            getModel().setValue("code", ((String)getModel().getValue("code")).trim());
            getModel().setValue("desc", ((String)getModel().getValue("desc")).trim());
            String type = (String)getModel().getValue("type");

            if ("1".equals(type)) {

                getModel().setValue("picturefield", "");
                getModel().setValue("encodevalue", "");

                String textvalue = ((String)getModel().getValue("textvalue")).trim();
                getModel().setValue("usevalue", textvalue);
                getModel().setValue("textvalue", textvalue);
            } else if ("2".equals(type)) {

                getModel().setValue("textvalue", "");
                getModel().setValue("encodevalue", "");
                getModel().setValue("usevalue", getModel().getValue("picturefield"));
            } else if ("3".equals(type)) {

                getModel().setValue("textvalue", "");
                getModel().setValue("picturefield", "");
                String encodevalue = ((String)getModel().getValue("encodevalue")).trim();
                String uservalue = (String)getModel().getValue("usevalue");
                String temp = encodevalue;
                if (!StringUtils.equals(uservalue, encodevalue) && !StringUtils.isBlank(encodevalue)) {
                    temp = Encrypters.encode(encodevalue);
                }
                getModel().setValue("usevalue", temp);
                getModel().setValue("encodevalue", temp);
            }
        }
    }





    public void afterDoOperation(AfterDoOperationEventArgs afterDoOperationEventArgs) {
        super.afterDoOperation(afterDoOperationEventArgs);
        if ("save".equals(afterDoOperationEventArgs.getOperateKey()))
            CappConfig.refreshConfigValueCache((String)getModel().getValue("code"));
    }
}