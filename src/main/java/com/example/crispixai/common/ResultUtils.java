package com.example.crispixai.common;

/**
 * 缁撴灉绫? * @author cjs
 */



/**
 * 杩斿洖宸ュ叿绫?(杩欐牱鎴戜滑澶栭潰濡傛灉瑕佹垚鍔熺殑鎺ュ彛灏辩洿鎺ヨ皟杩欎釜灏辫浜?
 * @author axuan
 */
public class ResultUtils {
    /**
     * 鎴愬姛鐨勮繑鍥?     * @param data
     * @return
     * @param <T>
     */
    public static <T>  BaseResponse<T> success(T data) {
        return new BaseResponse<>(0,data,"success");
    }

    /**
     * 澶辫触鐨勮繑鍥?     * @param errorCode
     * @return
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode){
        return new BaseResponse<>(errorCode);
    }
    /**
     * 澶辫触鐨勮繑鍥?鑷畾涔夋弿杩板拰娑堟伅)
     * @param errorCode
     * @return
     */

    public static <T> BaseResponse<T> error(ErrorCode errorCode,String description,String message){
        return new BaseResponse<>(errorCode.getCode(),message,null,description);

    }
    /**
     * 澶辫触鐨勮繑鍥?鑷畾涔夋弿杩?
     * @param errorCode
     * @return
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode,String description){
        return new BaseResponse<>(errorCode.getCode(),errorCode.getMessage(),null,description);
    }
    /**
     * 澶辫触鐨勮繑鍥?鑷畾涔塩ode)
     * @param errorCode
     * @return
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode,int code){
        return new BaseResponse<>(code, errorCode.getMessage(), null, errorCode.getDescription());
    }
    /**
     * 澶辫触鐨勮繑鍥?鑷畾涔夊叏閮?
     * @param code
     * @param description
     * @param message
     * @return
     */
    public static <T> BaseResponse<T> error(int code,String description,String message){
        return new BaseResponse<>(code, message,null, description);
    }

}
