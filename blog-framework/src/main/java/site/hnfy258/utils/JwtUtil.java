package site.hnfy258.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT工具类
 */
public class JwtUtil {

    //JWT有效期
    public static final Long JWT_TTL = 60 * 60 *1000L;// 60 * 60 *1000  一个小时
    //设置签名信息(秘钥明文)
    public static final String JWT_KEY = "sangeng";

    /**
     * 生成uuid
     *
     * @return {@link String}
     */
    public static String getUUID(){
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        return token;
    }

    /**
     * 生成jtw，无过期时间，则使用设置的时间
     * @param subject token中要存放的数据（json格式）
     * @return
     */
    public static String createJWT(String subject) {
        return getJwtBuilder(subject, JwtUtil.JWT_TTL, getUUID());
    }

    /**
     * 生成jtw，有过期时间。默认为1个小时
     * @param subject token中要存放的数据（json格式）
     * @param ttlMillis token超时时间
     * @return
     */
    public static String createJWT(String subject, Long ttlMillis) {
        return getJwtBuilder(subject, ttlMillis, getUUID());// 设置过期时间
    }


    /**
     * 生成jtw，自定义过期时间。
     * @param id
     * @param subject
     * @param ttlMillis
     * @return
     */
    public static String createJWT(String id, String subject, Long ttlMillis) {
        return getJwtBuilder(subject, ttlMillis, id);// 设置过期时间
    }


    /**
     * 创建JWT
     *
     * @param subject   主题
     * @param ttlMillis ttl米尔斯
     * @param uuid      uuid
     * @return {@link String}
     */
    private static String getJwtBuilder(String subject, Long ttlMillis, String uuid) {
//        1.根据签名信息生成加密后的秘钥 secretKey
        SecretKey secretKey = generalKey();
//        2.获取到当前时间戳
        long nowMillis = System.currentTimeMillis();
//        3.签发时间
        Date now = new Date(nowMillis);
//        4.JWT的过期时间：当前时间戳+JWT过期时间
        long expMillis = nowMillis + ttlMillis;
        Date expDate = new Date(expMillis);

        return Jwts.builder()
//                唯一ID
                .setId(uuid)
//                主题：可以是JSON数据
                .setSubject(subject)
//                签发者
                .setIssuer("sg")
//                签发时间
                .setIssuedAt(now)
//                使用HS256对称加密算法签名，第二个参数为秘钥，根据密钥进行加密
                .signWith(SignatureAlgorithm.HS256, secretKey)
//                设置JWT过期时间
                .setExpiration(expDate)
//                对JWT的三部分进行拼接
                .compact();
    }

    /**
     * 根据签名信息生成加密后的秘钥 secretKey
     * @return
     */
    public static SecretKey generalKey() {
        byte[] encodedKey = Base64.getDecoder().decode(JwtUtil.JWT_KEY);
        SecretKey key = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
        return key;
    }

    /**
     * 解析JWT
     *
     * @param jwt
     * @return
     * @throws Exception
     */
    public static Claims parseJWT(String jwt) throws Exception {
//        根据签名信息生成加密后的秘钥 secretKey
        SecretKey secretKey = generalKey();
        return Jwts.parser()
//                根据密钥进行解密
                .setSigningKey(secretKey)
//                JWT对象
                .parseClaimsJws(jwt)
                .getBody();
    }
}


