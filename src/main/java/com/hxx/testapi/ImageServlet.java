package com.hxx.testapi;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@WebServlet("/test/image")
public class ImageServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置响应内容类型为图片类型，例如JPEG
        resp.setContentType("image/jpeg");

        // 获取图片的输入流，这里以一个示例图片为例
        //String imagePath = "/uploads/1754740548718_kk.jpg"; // 更改为你的图片路径
        String imagePath = "/kk.jpg"; // 更改为你的图片路径
        //InputStream in = this.getClass().getResourceAsStream(imagePath);

        FileInputStream in = new FileInputStream("E:\\java\\CtWeb\\src\\main\\webapp\\uploads\\1754740548718_kk.jpg");

        if (in == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND); // 如果找不到图片，返回404错误
            return;
        }

        // 获取输出流，用于将图片数据写入响应体
        OutputStream out = resp.getOutputStream();

        // 读取图片数据并写入响应体
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }

        // 关闭流
        out.flush();
        in.close();
        out.close();
    }
}