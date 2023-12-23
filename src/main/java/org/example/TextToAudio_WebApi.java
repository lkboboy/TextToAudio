package org.example;


import Util.XunFeiUtil;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Base64;
import java.util.Scanner;

/**
 * 使用讯飞在线语音合成（流式版）API生成
 */

public class TextToAudio_WebApi {

    private static final Logger log = LoggerFactory.getLogger(TextToAudio_WebApi.class);

    @Test
    public void textToAudio() throws IOException {
        String fileName = "D:\\Users\\Desktop\\1.txt";
        try (Scanner sc = new Scanner(new FileReader(fileName))) {
            while (sc.hasNextLine()) {
                String[] line = sc.nextLine().split("\\|");
                //合成的语音文件名称
                String filename = line[1];
                //合成文本
                String text = line[0];

                //调用微服务接口获取音频base64
                String result = "";
                try {
                    result = XunFeiUtil.convertText(text);
                } catch (Exception e) {
                    log.error("【文字转语音接口调用异常】", e);
                }

                //音频数据
                byte[] audioByte = Base64.getDecoder().decode(result);
                FileOutputStream output = new FileOutputStream("d:\\"+filename+".wav");
                OutputStream os = new BufferedOutputStream(output);
                try {
                    //音频流
                    os.write(audioByte);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    os.flush();
                    os.close();
                }

            }
        }

    }
}

