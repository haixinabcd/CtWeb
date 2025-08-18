package com.hxx.data;

import com.alibaba.fastjson2.annotation.JSONField;

import java.io.ByteArrayOutputStream;

public class TaskEnd {
    public String taskid;
    public int processed;
    public String taskendhead;
    public int  taskendstatus;
    @JSONField(serialize = false)
    public  ByteArrayOutputStream taskendBS;
}
