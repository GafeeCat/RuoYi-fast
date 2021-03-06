package com.ruoyi.common.utils.gbdata;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * 生成数字签名
 *
 * @author issuser
 */
@Slf4j
public class GenerateSign {
    public static String createSign(String sid, String rid, String rtime, String appsecret) {
        String result = "";
        try {
            log.info("appsecret---------->" + appsecret);
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            byte[] keyBytes = appsecret.getBytes("UTF-8");
            log.info("keyBytes---------->" + new String(keyBytes));
            hmacSha256.init(new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256"));
            String inputString = sid + rid + rtime;
            byte[] hmacSha256Bytes = hmacSha256.doFinal(inputString.getBytes("UTF-8"));
            result = new String(Base64.encodeBase64(hmacSha256Bytes), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /*
     * 加密
     * 1.构造密钥生成器
     * 2.根据ecnodeRules规则初始化密钥生成器
     * 3.产生密钥
     * 4.创建和初始化密码器
     * 5.内容加密
     * 6.返回字符串
     */
    public static String AESEncode(String encodeRules, String content) {
        try {
            //1.构造密钥生成器，指定为AES算法,不区分大小写  
            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            //2.根据ecnodeRules规则初始化密钥生成器  
            //新增下面两行，处理Linux操作系统下随机数生成不一致的问题
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(encodeRules.getBytes());
            //生成一个128位的随机源,根据传入的字节数组
            //keygen.init(128, new SecureRandom(encodeRules.getBytes()));
            //将原来的初始化方式，改为下面的方式
            keygen.init(128, secureRandom);
            //3.产生原始对称密钥 
            SecretKey original_key = keygen.generateKey();
            //4.获得原始对称密钥的字节数组 
            byte[] raw = original_key.getEncoded();
            //5.根据字节数组生成AES密钥 
            SecretKey key = new SecretKeySpec(raw, "AES");
            //6.根据指定算法AES自成密码器 
            Cipher cipher = Cipher.getInstance("AES");
            //7.初始化密码器，第一个参数为加密(Encrypt_mode)或者解密解密(Decrypt_mode)操作，第二个参数为使用的KEY
            cipher.init(Cipher.ENCRYPT_MODE, key);
            //8.获取加密内容的字节数组(这里要设置为utf-8)不然内容中如果有中文和英文混合中文就会解密为乱码
            byte[] byte_encode = content.getBytes("utf-8");
            //9.根据密码器的初始化方式--加密：将数据加密
            byte[] byte_AES = cipher.doFinal(byte_encode);
            //10.将加密后的数据转换为字符串
            //这里用Base64Encoder中会找不到包
            //解决办法：
            //在项目的Build path中先移除JRE System Library，再添加库JRE System Library，重新编译后就一切正常了。
            String AES_encode = new String(new BASE64Encoder().encode(byte_AES));
            //11.将字符串返回 
            return AES_encode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        //如果有错就返加nulll
        return null;
    }

    /*
     * 解密
     * 解密过程：
     * 1.同加密1-4步
     * 2.将加密后的字符串反纺成byte[]数组
     * 3.将加密内容解密
     */
    public static String AESDncode(String encodeRules, String content) {
        try {
            //1.构造密钥生成器，指定为AES算法,不区分大小写  
            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            //2.根据ecnodeRules规则初始化密钥生成器   
            //新增下面两行，处理Linux操作系统下随机数生成不一致的问题
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(encodeRules.getBytes());
            //生成一个128位的随机源,根据传入的字节数组  
            //keygen.init(128, new SecureRandom(encodeRules.getBytes()));  
            //将原来的初始化方式，改为下面的方式
            keygen.init(128, secureRandom);
            //3.产生原始对称密钥  
            SecretKey original_key = keygen.generateKey();
            //4.获得原始对称密钥的字节数组  
            byte[] raw = original_key.getEncoded();
            //5.根据字节数组生成AES密钥  
            SecretKey key = new SecretKeySpec(raw, "AES");
            //6.根据指定算法AES自成密码器  
            Cipher cipher = Cipher.getInstance("AES");
            //7.初始化密码器，第一个参数为加密(Encrypt_mode)或者解密(Decrypt_mode)操作，第二个参数为使用的KEY  
            cipher.init(Cipher.DECRYPT_MODE, key);
            //8.将加密并编码后的内容解码成字节数组  
            byte[] byte_content = new BASE64Decoder().decodeBuffer(content);
            /*
             * 解密
             */
            byte[] byte_decode = cipher.doFinal(byte_content);
            String AES_decode = new String(byte_decode, "utf-8");
            return AES_decode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        //如果有错就返加nulll  
        return null;
    }

    public static void main(String[] args) {
        String encode = GenerateSign.AESEncode("111111", "abc");
        System.out.println(encode);
        String decode = GenerateSign.AESDncode("111111", encode);
        System.out.println(decode);
    }
}
