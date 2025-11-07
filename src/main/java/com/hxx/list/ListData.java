package com.hxx.list;

import com.alibaba.fastjson2.JSON;
import com.hxx.data.TaskEnd;
import com.hxx.data.Task;
import com.hxx.websocket.AiSocketServer;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.Session;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArraySet;

public class ListData {

    static {
        //TaskThread taskThread= new TaskThread();
        //System.out.println("TaskThread init~~~");
        //new Thread(taskThread).start();
    }

    public static TaskEnd addTaskEnd(Task task) {
        task.taskid = task.uid + "_" + System.currentTimeMillis() + "_" + nIndex;

        TaskEnd taskEnd = new TaskEnd();
        taskEnd.taskid = task.taskid;
        taskEnd.processed = 0;
        ListData.taskEndMap.put(taskEnd.taskid, taskEnd);
        nIndex++;
        return taskEnd;
    }

    public static int sendTask(Task task, HttpServletRequest request) {
        int ok=AiSocketServer.send_task(task,request);
        return ok;
    }
    /*
        public static TaskEnd GetTaskEndOk(Task task){
            TaskEnd taskEnd=ListData.endMap.get(task.taskid);
            if (taskEnd!=null) {
                while (true) {
                    try {
                        Thread.sleep(1 * 1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (taskEnd.processed == -1) {
                        break;
                    }
                    if (taskEnd.processed == 1000) {
                        break;
                    }
                }
            }
            return taskEnd;
        }

        public static String getTaskString(String taskid){
            String taskjson=null;
            int nSize= ListData.taskList.size();
            for(int i=0;i<nSize;i++){
                Task task=ListData.taskList.get(i);
                if (task.taskid.equals(taskid)){
                    taskjson= JSON.toJSONString(task);
                    break;
                }
            }
            return taskjson;
        }


        public static Task getTask(String taskid){
            Task retTask=null;
            int nSize= ListData.taskList.size();
            for(int i=0;i<nSize;i++){
                Task task=ListData.taskList.get(i);
                if (task.taskid.equals(taskid)){
                    retTask= task;
                    break;
                }
            }
            return retTask;
        }
     */

    public static TaskEnd getTaskEnd(String taskid){
        TaskEnd taskEnd= ListData.taskEndMap.get(taskid);
        return taskEnd;
    }

    public static void  removeTaskEnd(String taskid){
        ListData.taskEndMap.remove(taskid);
    }
    private static Map<String, TaskEnd> taskEndMap=new HashMap<String, TaskEnd>();

    public static CopyOnWriteArraySet<AiSocketServer> wsList= new CopyOnWriteArraySet<AiSocketServer>();

    public static Map<String,CopyOnWriteArraySet<AiSocketServer>> wsMap= new HashMap<>();

    private static int nIndex=0;
}
