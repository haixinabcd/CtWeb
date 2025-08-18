package com.hxx.testapi;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@MultipartConfig
@WebServlet(name = "TestServlet", value = "/test/test")
public class TestServlet extends HttpServlet {

    public static byte[] getRequestPostBytes(HttpServletRequest request)
            throws IOException {
        int contentLength = request.getContentLength();
        if(contentLength<0){
            return null;
        }
        byte buffer[] = new byte[contentLength];
        for (int i = 0; i < contentLength;) {

            int readlen = request.getInputStream().read(buffer, i,
                    contentLength - i);
            if (readlen == -1) {
                break;
            }
            i += readlen;
        }
        return buffer;
    }

    void doAll(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        // 1. 获取上传目录并确保存在
        String uploadPath = getServletContext().getRealPath("/uploads");
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) uploadDir.mkdirs();
        try {
            String name=request.getParameter("sw");
            out.println(name);
            // 2. 获取所有文件Part
            Collection<Part> fileParts = request.getParts()
                    .stream()
                    .filter(part -> part.getHeader("content-disposition")
                            .contains("filename="))
                    .collect(Collectors.toList());

            // 3. 处理每个文件
            for (Part filePart : fileParts) {
                String fileName = Paths.get(filePart.getSubmittedFileName())
                        .getFileName().toString();

                // 4. 安全校验
                if (fileName.isEmpty()) continue;
                if (!isAllowedFileType(fileName)) {
                    throw new ServletException("不允许的文件类型");
                }

                // 5. 保存文件（添加时间戳防重名）
                String savedName = System.currentTimeMillis() + "_" + fileName;
                filePart.write(uploadPath + File.separator + savedName);

                out.println("文件 " + fileName + " 上传成功<br>");
            }
            out.println("<h3>所有文件上传完成</h3>");

        } catch (Exception e) {
            out.println("<h3>上传失败: " + e.getMessage() + "</h3>");
        }
    }

    private boolean isAllowedFileType(String fileName) {
        String[] allowedTypes = {".jpg", ".png", ".pdf", ".txt"};
        return Arrays.stream(allowedTypes)
                .anyMatch(fileName.toLowerCase()::endsWith);
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

