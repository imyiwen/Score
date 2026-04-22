package com.score.common;

import lombok.Data;

/**
 * @author imyiwen
 * @data 2026/4/21 17:22
 */

@Data
public class ResultVo<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> ResultVo<T> success(T data){
        ResultVo<T> resultVo = new ResultVo<T>();
        resultVo.setCode(200);
        resultVo.setMsg("Success!");
        resultVo.setData(data);
        return resultVo;
    }

    public static <T> ResultVo<T> success(){
        return success(null);
    }

    public static <T> ResultVo<T> error(String msg) {
        ResultVo<T> resultVo = new ResultVo<T>();
        resultVo.setCode(500);
        resultVo.setMsg(msg);
        resultVo.setData(null);
        return resultVo;
    }
}
