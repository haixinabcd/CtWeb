package com.hxx.aiservlet;

import com.alibaba.fastjson2.JSON;
import com.hxx.data.Task;
import com.hxx.data.TaskEnd;
import com.hxx.list.ListData;
import com.hxx.websocket.AiSocketServer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "OrderServlet", value = "/*")
public class OrderServlet extends HttpServlet {
    void doAll(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        Task task=new Task();
        task.taskmethod=request.getMethod();
        String uri=request.getRequestURI();
        if (uri.startsWith("/")){
            uri=uri.substring(1);
            int pos=uri.indexOf("/");
            uri=uri.substring(pos);
            String tmp=uri.substring(1);
            pos=tmp.indexOf("/");
            if (pos>=0){
                task.node=tmp.substring(0,pos);
            }
        }
        task.taskuri=uri;
        // 获取所有头部信息
        Map<String, String> headersMap = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headersMap.put(headerName,headerValue);
        }
        task.taskhead=JSON.toJSONString(headersMap);
        if (task.taskmethod.equals("GET")) {
            String queryString = request.getQueryString();
            if (queryString!=null && !queryString.isEmpty()) {
                task.taskuri = task.taskuri + "?" + queryString;
            }
        }
        int ok=ListData.addTask(task);
        ok= ListData.sendTask(task,request);
        if  (ok==-1){
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("没有可用的服务");
            return;
        }

        TaskEnd taskEnd=ListData.GetTaskEndOk(task);
        if (taskEnd!=null) {
            if (taskEnd.processed==-1){
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.println("服务异常");
                return;
            }

            Map<String, String> endheadersMap = JSON.parseObject(taskEnd.taskendhead, Map.class);
            for (Map.Entry<String, String> entry : endheadersMap.entrySet()) {
                response.setHeader(entry.getKey(),entry.getValue());
            }
            response.setStatus(taskEnd.taskendstatus);
            if (taskEnd.processed==1000){
                if (taskEnd.taskendBS==null){
                }else{
                    OutputStream out = response.getOutputStream();
                    out.write(taskEnd.taskendBS.toByteArray());
                    out.flush();
                    out.close();
                }
            }
        }
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doAll(request,response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doAll(request,response);
    }
}

