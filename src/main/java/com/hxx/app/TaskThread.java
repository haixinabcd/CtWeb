package com.hxx.app;

import com.alibaba.fastjson2.JSON;
import com.hxx.aidata.TaskProgress;
import com.hxx.data.Task;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class TaskThread implements Runnable{
    public Task task;
    public App App;
    private static final ReentrantLock lock = new ReentrantLock();
    @Override
    public void run() {
        sendUri(task);
    }
    public int sendUri(Task task) {
        int ok = 0;
        CloseableHttpClient client = HttpClients.createDefault();
        String http =App.aiurl + task.taskuri;
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
            if (task.taskBS!=null) {
                ByteArrayEntity entity = new ByteArrayEntity(task.taskBS.toByteArray());
                httpPost.setEntity(entity);
            }
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
            ok=sendRet(response);
        } catch (Exception e) {
            e.printStackTrace();
            App.senderr(task);
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
        if (task.taskBS != null) {
            try {
                task.taskBS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            task.taskBS = null;
        }
        task = null;
        return ok;
    }

    public int sendRet(CloseableHttpResponse response) {
        int ok = -1;
        if (response==null){
            return ok;
        }
        try {
            lock.lock();
            Header[] headers = response.getAllHeaders();
            Map<String, String> endheadersMap = new HashMap<>();
            for (Header header : headers) {
                endheadersMap.put(header.getName(), header.getValue());
            }
            TaskProgress taskProgress = new TaskProgress();
            String headjson = JSON.toJSONString(endheadersMap);
            taskProgress.taskendstatus=response.getStatusLine().getStatusCode();
            taskProgress.taskid = task.taskid;
            if (response.getEntity()==null || response.getEntity().getContent()==null){
                taskProgress.processed = 1000;
                taskProgress.taskendhead = headjson;
                String messLast = JSON.toJSONString(taskProgress);
                ok = App.send(messLast);
            }else{
                taskProgress.processed = 999;
                String mess = JSON.toJSONString(taskProgress);
                ok = App.send(mess);
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
                    App.send(body, rd);
                }
                System.out.println("reader: " + all);
                taskProgress.processed = 1000;
                taskProgress.taskendhead = headjson;
                String messLast = JSON.toJSONString(taskProgress);
                ok = App.send(messLast);
            }
        } catch (Exception e) {
            e.printStackTrace();
            App.senderr(task);
        } finally {
            lock.unlock();
        }
        return ok;
    }
}
