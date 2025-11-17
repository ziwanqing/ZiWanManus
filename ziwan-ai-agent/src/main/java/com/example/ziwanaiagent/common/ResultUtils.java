package com.example.ziwanaiagent.common;


import com.example.ziwanaiagent.exception.ErrorCode;

/**
 * 封装返回结果
 */
public class ResultUtils {
    /**
     * 成功
     */
    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(0,data,"成功");
    }
    /**
     * 失败
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message){
        return new BaseResponse<>(errorCode.getCode(),null,message);
    }

    public static <T> BaseResponse<T> error(int code, String message){
        return new BaseResponse<>(code,null,message);
    }
}
