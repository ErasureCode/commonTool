package com.zhouzifei.tool.util;


import com.google.common.collect.Maps;
import com.zhouzifei.tool.consts.VideoTypeConst;
import com.zhouzifei.tool.dto.VideoUrl;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

/**
 * @author 周子斐
 * parse
 * @date 2020/7/24
 * @Description
 */
@Component
public class VideoSaveUtils {


    @Autowired
    RedisUtils redisUtils;
    /**
     *
     * @param videoUrl 解析原型实体
     * @param fileLocal 保存的本地路径
     * @param fileUrl 返回的域名
     * @return
     */
    public VideoUrl saveVideo(VideoUrl videoUrl, String fileLocal, String fileUrl) {
        String domain = null;
        if (videoUrl.getDomain() != null) {
            domain = videoUrl.getDomain();
        }
        String parseUrl = videoUrl.getUrl();
        if (StringUtils.isBlank(parseUrl)) {
            return videoUrl;
        }
        int i = parseUrl.lastIndexOf("/");
        String substring = parseUrl.substring(0, i);
        videoUrl.setPrefixUrl(substring + "/");
        String playUrl = saveLocal(parseUrl, domain, videoUrl, fileLocal,fileUrl);
        videoUrl.setUrl(playUrl);
        return videoUrl;
    }

    /**
     *
     * @param url 视频的原始地址
     * @param domain 请求地址
     * @param videoUrl 返回的视频实体
     * @param fileLocal 本地路径
     * @param fileUrl 返回的域名
     * @return
     */
    public String saveLocal(String url, String domain, VideoUrl videoUrl, String fileLocal,String fileUrl) {
        String prefixType = videoUrl.getPrefixType();
        String prefixUrl = videoUrl.getPrefixUrl();
        //默认不指定key的情况下，以文件内容的hash值作为文件名
        String replace = UUID.randomUUID().toString().replace("-", "");
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhh");
        String format = dateFormat.format(new Date());
        if (prefixType.contains(".com")) {
            prefixType = prefixType.replace(".com", "");
        }
        String filePath = "/" + prefixType + "/" + format + "/";
        String fileName = replace + ".m3u8";
        HashMap<String, String> hear = Maps.newHashMap();
        hear.put("User-Agent", "Mozilla/4.0 (compatible;MSIE 7.0; Windows NT 5.1; Maxthon;)");
        hear.put("Accept-Encoding", "UTF-8");
        hear.put("referer", domain);
        try {
            HttpResponse get = HttpUtils.doGet(url, "", "GET", hear, new HashMap<>());
            // 设置字符编码
            InputStream content = get.getEntity().getContent();
            // 打开到此 URL 引用的资源的通信链接（如果尚未建立这样的连接）。
            //构造一个BufferedReader类来读取文件
            File file2 = new File(fileLocal + filePath);
            File file1 = new File(fileLocal + filePath + fileName);
            if (!file2.isDirectory()) {
                file2.mkdirs();
            }
            DataOutputStream out = new DataOutputStream(new FileOutputStream(file1));
            String result;
            InputStreamReader reader = new InputStreamReader(content, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(reader);
            prefixUrl = prefixUrl == null ? "" : prefixUrl;
            while ((result = br.readLine()) != null) {
                if (!result.contains("#")) {
                    if (!result.contains("http://")&&!result.contains("https://")) {
                        if (result.contains(VideoTypeConst.M3U8.getType())) {
                            return url;
                        } else {
                            result = prefixUrl + result + "\n";
                        }
                    }else {
                        result = result + "\n";
                    }
                } else {
                    result = result + "\n";
                }
                out.write(result.getBytes());
            }
            out.flush();
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return fileUrl + filePath + fileName;
    }
}
