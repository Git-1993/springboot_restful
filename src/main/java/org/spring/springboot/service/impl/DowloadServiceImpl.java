package org.spring.springboot.service.impl;


import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.spring.springboot.oss.OssUtil;
import org.spring.springboot.service.DowloadService;
import org.spring.springboot.util.MSOfficeGeneratorUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.jws.WebService;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

@Service
public class DowloadServiceImpl implements DowloadService {

    public String dowloadPaper(String url) {
        MSOfficeGeneratorUtils officeUtils = new MSOfficeGeneratorUtils(false); // 将生成过程设置为不可见
        int imgIndex = 1;
        Map<String, String> imgMap = new HashMap<String, String>(); //存放图片标识符及物理路径  {"image_1","D:\img.png"};
        try {
            //Document document = Jsoup.parse(input, "UTF-8", "");
            Document document = Jsoup.connect(url).timeout(60000).get();
            Elements elements = document.select("img");

            for (Element img : elements){
                img.after("${image_" + imgIndex + "}"); // 为img添加同级p标签，内容为<p>${image_imgIndexNumber}</p>
                String src = img.attr("src");
                //System.out.println(src);
                // 下载图片到本地
                download(src,"image_"+imgIndex,"D:\\imgs\\");
                // 保存图片标识符及物理路径
                imgMap.put("${image_" + imgIndex++ + "}", src);
                // 删除Img标签
                img.remove();
            }
            // 将html代码写到html文件中
            FileWriter fw = new FileWriter("D:\\tt.html");
            fw.write(document.html(), 0, document.html().length());// 写入文件
            fw.flush();
            fw.close();

            String newFileName = "D:\\ww.doc";
            // temp_A3_2L.doc为A3两栏的模板，这里模板不动，复制了一个副本 用于写入数据
            FileUtils.copyFile(new File("D:\\temp_A3_2L1.docx"),new File(newFileName));
            // html文件转为word
            officeUtils.html2Word("D:\\tt.html",newFileName);


            // 替换标识符为图片
            for (Map.Entry<String, String> entry : imgMap.entrySet()){
                officeUtils.replaceText2Image(entry.getKey(), entry.getValue());
            }
            officeUtils.saveAs(newFileName);    // 保存
            officeUtils.close(); // 关闭Office Word创建的文档
            officeUtils.quit(); // 退出Office Word程序

            // 这里可以删除本地图片 略去
            delAllFile("D:\\imgs");
            //上传文件到Oss服务器
            OssUtil.testUpload("ww1.doc","D:\\ww.doc");
            //删除本地的试卷
            File file = new File("D:\\ww.doc");
            file.delete();
            imgIndex = 1;
            imgMap.clear();

        } catch (IOException e) {
            e.printStackTrace();
        }
        //返回试卷Objectkey
        return "ww1.doc";
    }

    // 删除指定文件夹下所有文件
    // param path 文件夹完整绝对路径
    public static boolean delAllFile(String path) {
        boolean flag = false;
        File file = new File(path);
        if (!file.exists()) {
            return flag;
        }
        if (!file.isDirectory()) {
            return flag;
        }
        String[] tempList = file.list();
        File temp = null;
        for (int i = 0; i < tempList.length; i++) {
            if (path.endsWith(File.separator)) {
                temp = new File(path + tempList[i]);
            } else {
                temp = new File(path + File.separator + tempList[i]);
            }
            if (temp.isFile()) {
                temp.delete();
            }
            if (temp.isDirectory()) {
                delAllFile(path + "/" + tempList[i]);// 先删除文件夹里面的文件
                delFolder(path + "/" + tempList[i]);// 再删除空文件夹
                flag = true;
            }
        }
        return flag;
    }
// 删除文件夹
// param folderPath 文件夹完整绝对路径

    public static void delFolder(String folderPath) {
        try {
            delAllFile(folderPath); // 删除完里面所有内容
            String filePath = folderPath;
            filePath = filePath.toString();
            java.io.File myFilePath = new java.io.File(filePath);
            myFilePath.delete(); // 删除空文件夹
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 下载图片
     * @param urlString  路径
     * @param filename   保存的文件名
     * @param savePath   保存路径
     */
    public static void download(String urlString, String filename, String savePath) {
        InputStream is = null;
        OutputStream os = null;
        try {
            // 构造URL
            URL url = new URL(urlString);
            // 打开连接
            URLConnection con = url.openConnection();
            //设置请求超时为5s
            con.setConnectTimeout(5*1000);
            // 输入流
            is = con.getInputStream();

            // 1K的数据缓冲
            byte[] bs = new byte[1024];
            // 读取到的数据长度
            int len;
            // 输出的文件流
            File sf=new File(savePath);
            if(!sf.exists()){
                sf.mkdirs();
            }
            os = new FileOutputStream(sf.getPath() + "\\" + filename);
            // 开始读取
            while ((len = is.read(bs)) != -1) {
                os.write(bs, 0, len);
            }
            // 完毕，关闭所有链接
            os.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // 查1



//    public User queryOne(@PathParam("uid") int uid,@PathParam("id") int id) {
//        System.out.println("in restful server... uid=" + uid +", id=" + id);
//        return new User(uid, "张三");
//    }/queryOne/{uid}/{id}"
}
