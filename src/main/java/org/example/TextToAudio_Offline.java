package org.example;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.junit.Test;

import java.io.*;
import java.util.Scanner;

/**
 * 使用讯飞离线语音合成（普通版） 生成
 */

public class TextToAudio_Offline {

    public interface MscLibrary extends Library {

        // DLL文件默认路径为项目根目录，若DLL文件存放在项目外，请使用绝对路径
        MscLibrary INSTANCE = Native.load("lib\\bin\\msc_x64", MscLibrary.class);

        int MSPLogin(String username, String password, String param);

        int MSPLogout();

        String QTTSSessionBegin(String params, IntByReference errorCode);

        int QTTSTextPut(String sessionID, String textString, int textLen, String params);

        Pointer QTTSAudioGet(String sessionID, IntByReference audioLen, IntByReference synthStatus, IntByReference errorCode);

        int QTTSSessionEnd(String sessionID, String hints);
    }

    @Test
    public  void main(String[] args) throws FileNotFoundException {

        //登录参数,appid与msc库绑定,请勿随意改动
        String login_params = "appid = 你的id, work_dir = .";
        //合成参数：tts_res_path我这里用了绝对路径
        String session_begin_params = "engine_type = local, voice_name = xiaoyan, text_encoding = UTF-8, tts_res_path = fo|lib\\bin\\msc\\res\\tts\\xiaoyan.jet;fo|lib\\bin\\msc\\res\\tts\\common.jet, sample_rate = 16000, speed = 50, volume = 100, pitch = 50, rdn = 2";
        String fileName = "D:\\Users\\Desktop\\1.txt";

        try (Scanner sc = new Scanner(new FileReader(fileName))) {
            while (sc.hasNextLine()) {
                String[] line = sc.nextLine().split("\\|");
                //合成的语音文件名称
                String filename = line[1];
                //合成文本
                String text = line[0];

                String sessionId = null;
                RandomAccessFile raf = null;
                try {
                    //登录
                    int loginCode = MscLibrary.INSTANCE.MSPLogin(null, null, login_params);

                    if (loginCode != 0) {
                        //登录失败
                        return;
                    }

                    //初始session
                    IntByReference errCode = new IntByReference();
                    sessionId = MscLibrary.INSTANCE.QTTSSessionBegin(session_begin_params, errCode);

                    if (errCode.getValue() != 0) {
                        //会话失败
                        return;
                    }

                    //放入文本
                    int textPutCode = MscLibrary.INSTANCE.QTTSTextPut(sessionId, text, text.getBytes().length, null);

                    if (textPutCode != 0) {
                        //放入文本失败
                        return;
                    }

                    //写入空的头格式
                    raf = new RandomAccessFile(filename, "rw");
                    raf.write(new byte[44]);
                    int dataSize = 0;
                    IntByReference audioLen = new IntByReference();
                    IntByReference synthStatus = new IntByReference();
                    while (true) {
                        Pointer pointer = MscLibrary.INSTANCE.QTTSAudioGet(sessionId, audioLen, synthStatus, errCode);
                        if (pointer != null && audioLen.getValue() > 0) {
                            //写入合成内容
                            raf.write(pointer.getByteArray(0, audioLen.getValue()));
                            //记录数据长度
                            dataSize += audioLen.getValue();
                        }
                        //转换异常或转换结束跳出循环
                        if (errCode.getValue() != 0 || synthStatus.getValue() == 2) {
                            break;
                        }
                    }
                    if (textPutCode != 0) {
                        //获取转换数据失败
                        return;
                    }
                    //定位到文件起始位置
                    raf.seek(0);
                    //写入真实头格式
                    raf.write(getWavHeader(dataSize, 16000, 32000, 1, 16));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (sessionId != null) {
                        MscLibrary.INSTANCE.QTTSSessionEnd(sessionId, "Normal");
                    }
                    MscLibrary.INSTANCE.MSPLogout();
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }


    }


    /**
     * @param totalAudioLen 音频数据总大小
     * @param sampleRate    采样率
     * @param byteRate      位元（组）率(每秒的数据量 单位 字节/秒)   采样率(44100之类的) * 通道数(1,或者2)*每次采样得到的样本位数(16或者8) / 8;
     * @param nChannels     声道数量
     * @param weikuan       位宽
     */
    private static byte[] getWavHeader(int totalAudioLen, int sampleRate, int byteRate, int nChannels, int weikuan) {
        long totalDataLen = totalAudioLen + 36;
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) (nChannels & 0xff);
        header[23] = (byte) ((nChannels >> 8) & 0xff);

        header[24] = (byte) (sampleRate & 0xff);//采样率
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);

        header[28] = (byte) (byteRate & 0xff);//取八位
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);

        int b = weikuan * nChannels / 8;//每次采样的大小
        header[32] = (byte) (b & 0xff); // block align
        header[33] = (byte) ((b >> 8) & 0xff);

        header[34] = (byte) (weikuan & 0xff);//位宽
        header[35] = (byte) ((weikuan >> 8) & 0xff);

        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        return header;
    }



}

