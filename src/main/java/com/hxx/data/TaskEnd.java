package com.hxx.data;

import com.alibaba.fastjson2.annotation.JSONField;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TaskEnd {
    public String taskid;
    public int processed;
    public String taskendhead;
    public int  taskendstatus;
    @JSONField(serialize = false)
    public  ByteArrayOutputStream taskendBS;
    @JSONField(serialize = false)
    public BlockingQueue<Integer> taskEndQ = new ArrayBlockingQueue<Integer>(1);
}
