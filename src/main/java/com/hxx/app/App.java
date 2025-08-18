package com.hxx.app;

import com.alibaba.fastjson2.JSON;
import com.hxx.aidata.TaskProgress;
import com.hxx.data.Task;
import javax.websocket.*;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@ClientEndpoint
public class App {
    private Task task;
    private Session session;
    private int aid;
    private int isjod;//是否工作
    public static String aiurl;
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connected to server");
        this.session = session;
        //注册
        TaskProgress taskProgress = new TaskProgress();
        taskProgress.aid=60;
        taskProgress.node="ai";
        taskProgress.taskid = "";
        String messLast = JSON.toJSONString(taskProgress);
        int ok=send(messLast);
        System.out.println(messLast);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("message=" + message);
        Task tmptask = JSON.parseObject(message, Task.class);
        if (tmptask==null){
            senderr(task);
            return;
        }
        if (tmptask.taskmethod.equals("GET")) {
            sendUriThread(tmptask);
            return;
        }
        if (tmptask.processed == 999) {
            task=tmptask;
        }
        if (tmptask.processed == 1000) {
            task.processed=tmptask.processed;
            task.taskhead=tmptask.taskhead;
            sendUriThread(task);
        }
    }

    @OnMessage
    public void onMessage(ByteBuffer bytes, Session session) {
        int len = bytes.remaining();
        if (task==null){
            return;
        }
        if(task.taskBS == null){
            task.taskBS = new ByteArrayOutputStream();
        }
        byte[] bt = new byte[len];
        bytes.get(bt, 0, len);
        try {
            task.taskBS.write(bt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Connection closed: ");
    }


    public void onPong(Session session) {
        System.out.println("Connection closed: ");
    }
    public int senderr(Task task)
    {
        TaskProgress taskProgress = new TaskProgress();
        taskProgress.taskid = task.taskid;
        taskProgress.processed = -1;
        String messLast = JSON.toJSONString(taskProgress);
        int ok=send(messLast);
        System.out.println(messLast);
        return ok;
    }

    public int sendUriThread(Task task){
        TaskThread taskThread= new TaskThread();
        taskThread.task=task;
        taskThread.App=this;
        new Thread(taskThread).start();
        //sendUri(task);
        return 0;
    }
    public int sendUri(Task task) {
        int ok = 0;
        CloseableHttpClient client = HttpClients.createDefault();
        String http = aiurl +  task.taskuri;
        System.out.println(http);
        HttpPost httpPost = null;
        HttpGet httpGet = null;
        Map<String, String> headersMap = JSON.parseObject(task.taskhead, Map.class);

        if (task.taskmethod.equals("POST")) {
            httpPost = new HttpPost(http);
            for (Map.Entry<String, String> entry : headersMap.entrySet()) {
                if (entry.getKey().toLowerCase().equals("content-length")) {
                } else {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
            }
            ByteArrayEntity entity = new ByteArrayEntity(task.taskBS.toByteArray());
            httpPost.setEntity(entity);
        } else {
            httpGet = new HttpGet(http);
            for (Map.Entry<String, String> entry : headersMap.entrySet()) {
                if (entry.getKey().toLowerCase().equals("content-length")) {
                } else {
                    httpGet.setHeader(entry.getKey(), entry.getValue());
                }
            }
        }
        CloseableHttpResponse response = null;
        try {
            if (task.taskmethod.equals("POST")) {
                response = client.execute(httpPost);
            } else {
                response = client.execute(httpGet);
            }

            Header[] headers = response.getAllHeaders();
            Map<String, String> endheadersMap = new HashMap<>();
            for (Header header : headers) {
                endheadersMap.put(header.getName(), header.getValue());
            }
            TaskProgress taskProgress = new TaskProgress();
            taskProgress.taskid = task.taskid;
            taskProgress.processed = 999;
            String mess = JSON.toJSONString(taskProgress);
            send(mess);
            InputStream reader = response.getEntity().getContent();
            int contentLength = 1024 * 2;
            byte[] body = new byte[contentLength];
            int all = 0;
            for (; ; ) {
                int rd = reader.read(body);
                if (rd == -1) {
                    break;
                }
                all += rd;
                send(body, rd);
            }
            System.out.println("reader: " + all);
            String headjson = JSON.toJSONString(endheadersMap);
            taskProgress.taskendhead = headjson;
            taskProgress.processed = 1000;
            String messLast = JSON.toJSONString(taskProgress);
            send(messLast);
        } catch (Exception e) {
            e.printStackTrace();
            senderr(task);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ok;
    }

    public int send(String message) {
        int ok = 0;
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            ok = -1;
            throw new RuntimeException(e);
        }
        return ok;
    }

    public int send(byte[] data, int leng) {
        int ok = 0;
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data, 0, leng);
            session.getBasicRemote().sendBinary(buffer);
        } catch (IOException e) {
            ok = -1;
            throw new RuntimeException(e);
        }
        return ok;
    }
    public  int sendping(){
        try {
            session.getBasicRemote().sendPing(ByteBuffer.wrap(new byte[0]));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void main(String[] args) {
        String config="config.properties";

        String wsurl="ws://localhost:8080/CtWeb/ws";
        App.aiurl="http://localhost:8090/CtWeb/test";

        File file = new File(config);
        if (file.exists()) {
            Properties properties = new Properties();
            try (FileInputStream fileInputStream = new FileInputStream(config)) {
                properties.load(fileInputStream);
                wsurl = properties.getProperty("wsurl");
                App.aiurl = properties.getProperty("aiurl");
                System.out.println(wsurl);
                System.out.println(App.aiurl);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            Properties properties = new Properties();
            // 2. 设置属性值
            properties.setProperty("wsurl", wsurl);
            properties.setProperty("aiurl", App.aiurl);
            // 3. 写入文件
            try (FileOutputStream out = new FileOutputStream(config)) {
                // 4. 使用store方法保存配置
                properties.store(out,"test url");
                System.out.println("测试配置文件写入成功,请重新配置");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        Session sn = null;
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        int skip=0;
        for (; ;) {
            if (sn == null || !sn.isOpen()) {
                try {
                    URI uri = new URI(wsurl);
                    sn = container.connectToServer(App.class, uri);            // 保持连接
                } catch (Exception e) {
                    System.out.println("Connected is err  wait Connected");
                }
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            skip++;
            if (skip==3){
                skip=0;
                try {
                    sn.getBasicRemote().sendPing(ByteBuffer.wrap(new byte[0]));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
