package nckd.yanye.occ.plugin.form;

import kd.bos.bill.AbstractBillPlugIn;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;

/*
要货订单表单插件
 */
public class OcbSaleOrderBillPlugIn extends AbstractBillPlugIn {
    @Override
    public void propertyChanged(PropertyChangedArgs e) {

        String propName = e.getProperty().getName();
        //批准数量
        if (propName.equals("approveqty")) {
            DynamicObject billType= (DynamicObject) this.getModel().getValue("billtypedata",0);
            String name=billType.getString("name");
            Object id=billType.getPkValue();
            this.setStock(name,id);

        }//单据类型
        else if(propName.equalsIgnoreCase("billtypedata")){
            DynamicObject billType= (DynamicObject) e.getChangeSet()[0].getNewValue();
            if(billType!=null){
                String name=billType.getString("name");
                Object id=billType.getPkValue();
                this.setStock(name,id);
            }

        }
    }

    /*
    设置仓库
     */
    private void setStock(String name,Object id ){
        if(name.equalsIgnoreCase("车销订单")||id.toString().equalsIgnoreCase("100000")){
            //车销订单默认仓库，为当前组织指定的默认借货仓
            DynamicObject org= (DynamicObject) this.getModel().getValue("saleorgid",0);
            Object orgId=org.getPkValue();
            // 构造QFilter
            QFilter qFilter = new QFilter("createorg", QCP.equals,orgId)
                    .and("status",QCP.equals,"C")
                    .and("nckd_isjh",QCP.equals,"1");

            //查找借货仓
            DynamicObjectCollection collections = QueryServiceHelper.query("bd_warehouse",
                    "id,number", qFilter.toArray(), "audittime");
            if(!collections.isEmpty()){
                DynamicObject stockItem=collections.get(0);
                String number=stockItem.getString("number");
                int row=this.getModel().getEntryRowCount("plane_entry");
                for (int i=0;i<row;i++){
                    this.getModel().setItemValueByNumber("sub_warehouseid1",number,i);
                }
            }

        }else if(name.equalsIgnoreCase("赊销订单")||name.equalsIgnoreCase("访销订单")||name.equalsIgnoreCase("自提订单")){
            // 赊销订单、访销订单、自提订单 按部门设置对应仓库
            DynamicObject org= (DynamicObject) this.getModel().getValue("saleorgid",0);
            Object orgId=org.getPkValue();
            //销售部门
            DynamicObject dept= (DynamicObject) this.getModel().getValue("departmentid",0);
            if(dept!=null){
                Object deptId=dept.getPkValue();

                //从部门 仓库设置基础资料中获取对应仓库
                // 构造QFilter
                QFilter qFilter = new QFilter("createorg", QCP.equals,orgId)
                        .and("status",QCP.equals,"C")
                        .and("nckd_bm",QCP.equals,deptId);

                //查找部门对应仓库
                DynamicObjectCollection collections = QueryServiceHelper.query("nckd_bmcksz",
                        "id,nckd_ck.number number", qFilter.toArray(), "modifytime");
                if(!collections.isEmpty()){
                    DynamicObject stockItem=collections.get(0);
                    String number=stockItem.getString("number");
                    int row=this.getModel().getEntryRowCount("plane_entry");
                    for (int i=0;i<row;i++){
                        this.getModel().setItemValueByNumber("sub_warehouseid1",number,i);
                    }
                }
            }
        }
    }


}
