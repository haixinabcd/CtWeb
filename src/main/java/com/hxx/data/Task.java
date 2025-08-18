package com.hxx.data;

import com.alibaba.fastjson2.annotation.JSONField;

import java.io.ByteArrayOutputStream;

public class Task {
    public int uid;
    public int aid;
    public String node;
    public String taskid;
    public String taskmethod;
    public String taskuri;
    public String taskhead;
    public int processed;
    public int    state;// 初始 0，任务 1，结束 2
    @JSONField(serialize = false)
    public ByteArrayOutputStream taskBS;
}
