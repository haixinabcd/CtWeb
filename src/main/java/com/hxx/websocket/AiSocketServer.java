package com.hxx.websocket;

import com.alibaba.fastjson2.JSON;
import com.hxx.data.Task;
import com.hxx.data.TaskEnd;
import com.hxx.list.ListData;
import com.hxx.aidata.TaskProgress;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

@ServerEndpoint(value = "/ws")
public class AiSocketServer{
    private Session session;
    private String  taskid;
    private int     aid;
    public String node;
    private AtomicBoolean isjod=new AtomicBoolean();//是否工作
    public AiSocketServer(){
        aid=0;
        isjod.set(false);
        System.out.println(" WebSocket init~~~");
    }

    @OnOpen
    public void open(Session session){
        System.out.println("new Session");
    }
    @OnClose
    public void close(Session session){
        ListData.wsList.remove(this);
        Iterator<Map.Entry<String, CopyOnWriteArraySet<AiSocketServer>>> iterator = ListData.wsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CopyOnWriteArraySet<AiSocketServer>> entry = iterator.next();
            entry.getValue().remove(this);
            if (entry.getValue().isEmpty()){
                iterator.remove();
            }
        }
    }
    @OnError
    public void onError(Session session, Throwable error){
        System.out.println("发生错误");
        error.printStackTrace();
    }
    public void onPong(Session session){
        System.out.println("ping");
    }
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println(message);
        TaskProgress  taskProgress=JSON.parseObject(message, TaskProgress.class);
        if (taskProgress==null){
            aid=0;
            return;
        }
        //注册
        if (taskProgress.taskid==null || taskProgress.taskid.isEmpty() ){
            isjod.set(false);
            node=taskProgress.node;
            aid=taskProgress.aid;
            this.session=session;
            if (node==null || node.isEmpty()){
                ListData.wsList.add(this); //ai客户端
            }else{
                if (ListData.wsMap.containsKey(node)){
                    ListData.wsMap.get(node).add(this);
                }else{
                    CopyOnWriteArraySet<AiSocketServer> tmp=new CopyOnWriteArraySet<>();
                    tmp.add(this);
                    ListData.wsMap.put(node,tmp);
                }
            }
            return;
        }
        TaskEnd taskEnd=ListData.getTaskEnd(taskProgress.taskid);
        if (taskEnd!=null){
            taskEnd.processed=taskProgress.processed;
            if (taskEnd.processed==-1){
                Integer result= 1;
                taskEnd.taskEndQ.add(result);
                isjod.set(false);
            }
            if (taskEnd.processed==999) {
                taskid=taskEnd.taskid;
            }
            if (taskEnd.processed==1000) {
                taskEnd.taskendstatus=taskProgress.taskendstatus;
                taskEnd.taskendhead=taskProgress.taskendhead;
                Integer result= 1;
                taskEnd.taskEndQ.add(result);
                isjod.set(false);
                taskid=null;
            }
        }
    }

    @OnMessage
    public void onMessage(ByteBuffer bytes, Session session) {
        int len=bytes.remaining();
        //System.out.println("Received binary data: " + len + " bytes");
        if (taskid!=null) {
            TaskEnd taskEnd = ListData.getTaskEnd(taskid);
            if (taskEnd != null) {
                if (taskEnd.taskendBS==null){
                    taskEnd.taskendBS=new ByteArrayOutputStream();
                }
                byte[] bt=new byte[len];
                bytes.get(bt, 0, len);
                try {
                    taskEnd.taskendBS.write(bt);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int send(String message){
        int ok=0;
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            ok=-1;
            throw new RuntimeException(e);
        }
        return  ok;
    }

    public int send(byte[] data){
        int ok=0;
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            session.getBasicRemote().sendBinary(buffer);
        } catch (IOException e) {
            ok=-1;
            throw new RuntimeException(e);
        }
        return  ok;
    }
    public int send(byte[] data,int leng){
        int ok=0;
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data,0,leng);
            session.getBasicRemote().sendBinary(buffer);
        } catch (IOException e) {
            ok=-1;
            throw new RuntimeException(e);
        }
        return  ok;
    }

    public static int  send_task(Task task, HttpServletRequest request){
        int ok=-1;
        if (task.node==null || task.node.isEmpty()){
            if (ListData.wsList.size()==0){
                return ok;
            }
        }else{
            if (ListData.wsMap.isEmpty()){
                return ok;
            }
            if (ListData.wsMap.get(task.node)==null || ListData.wsMap.get(task.node).size()==0){
                return ok;
            }
        }

        AiSocketServer  aiSocketServerJob=null;
        System.out.println(task.taskid);
        for(;;){
            aiSocketServerJob=null;

            if (task.node==null || task.node.isEmpty()) {
                for (AiSocketServer aiSocketServer : ListData.wsList) {
                    if (aiSocketServer.isjod.get() == false) {
                        aiSocketServer.isjod.set(true);
                        aiSocketServerJob = aiSocketServer;
                        break;
                    }
                }
            }else{
                if (ListData.wsMap.containsKey(task.node)){
                    for (AiSocketServer aiSocketServer : ListData.wsMap.get(task.node)) {
                        if (aiSocketServer.isjod.get() == false) {
                            aiSocketServer.isjod.set(true);
                            aiSocketServerJob = aiSocketServer;
                            break;
                        }
                    }
                }else{
                    for (AiSocketServer aiSocketServer : ListData.wsList) {
                        if (aiSocketServer.isjod.get() == false) {
                            aiSocketServer.isjod.set(true);
                            aiSocketServerJob = aiSocketServer;
                            break;
                        }
                    }
                }
            }

            if (aiSocketServerJob!=null) {
                System.out.println(task.aid);
                task.aid=aiSocketServerJob.aid;
                if (aiSocketServerJob.node==null ||  aiSocketServerJob.node.isEmpty()){
                }else{
                    String node="/"+aiSocketServerJob.node;
                    if (task.taskuri.startsWith(node)){
                        task.taskuri=task.taskuri.substring(node.length());
                    }
                }

                if (task.taskmethod.equals("GET")){
                    task.processed=1000;
                    String taskjson= JSON.toJSONString(task);
                    System.out.println(taskjson);
                    ok=aiSocketServerJob.send(taskjson);
                    aiSocketServerJob.isjod.set(false);
                    break;
                }
                //post
                task.processed=999;
                String taskhead=task.taskhead;
                task.taskhead="";
                String taskjson= JSON.toJSONString(task);
                System.out.println(taskjson);
                ok=aiSocketServerJob.send(taskjson);
                if (ok==-1){
                    aiSocketServerJob.isjod.set(false);
                    break;
                }
                if (task.taskmethod.equals("POST")){
                    InputStream reader = null;
                    try {
                        reader = request.getInputStream();
                        int contentLength=1024*4;
                        byte[] body = new byte[contentLength];
                        int all=0;
                        for(;;){
                            int rd= reader.read(body);
                            if (rd==-1){
                                break;
                            }
                            all+=rd;
                            aiSocketServerJob.send(body,rd);
                        }
                        System.out.println("reader: " +all);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    task.processed=1000;
                    task.taskhead=taskhead;
                    String taskokjson= JSON.toJSONString(task);
                    System.out.println(taskokjson);
                    ok=aiSocketServerJob.send(taskokjson);
                    aiSocketServerJob.isjod.set(false);
                }
                break;
            }
            return -1;
        }
        return ok;
    }

    public  static int  resetjob(int aid){
        int ok=-1;
        for( AiSocketServer aiSocketServer: ListData.wsList){
            if (aiSocketServer.aid==aid){
                aiSocketServer.isjod.set(false);
                ok=0;
            }
        }
        return ok;
    }
}