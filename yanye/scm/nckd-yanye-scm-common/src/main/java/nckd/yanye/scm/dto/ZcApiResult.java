package nckd.yanye.scm.dto;


import kd.bos.openapi.common.custom.annotation.ApiModel;
import kd.bos.openapi.common.custom.annotation.ApiParam;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 响应参数
 *
 * @author liuxiao
 */

@ApiModel
public class ZcApiResult implements Serializable {

    @NotNull
    @ApiParam(value = "结果是否成功", required = true)
    private boolean success;

    @ApiParam("业务码")
    private String code;

    @ApiParam("信息")
    private String message;

    @ApiParam("业务信息")
    private String data;

    public ZcApiResult() {
    }

    public ZcApiResult(boolean success, String code, String message, String data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}










