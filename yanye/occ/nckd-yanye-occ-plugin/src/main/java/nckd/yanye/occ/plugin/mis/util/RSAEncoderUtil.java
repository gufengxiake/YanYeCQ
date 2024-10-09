package nckd.yanye.occ.plugin.mis.util;


import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RSAEncoderUtil {
	private final static Logger logger = LoggerFactory.getLogger(RSAEncoderUtil.class);
	private static String KEY_ALGORTHM = "RSA";
	private static String CIPHER_ALGORITHM="RSA/ECB/PKCS1Padding";



	/**
	 * 签名
	 *
	 * @param data
	 * @return
	 */
	public static String encodeSign(byte[] data, String privateKey) {
		// byte[] keys=Base64.decodeBase64(PapConfig.PRIVATEKEY);//解码
		byte[] keys = Base64.decodeBase64(privateKey);// 解码
		PKCS8EncodedKeySpec pk8 = new PKCS8EncodedKeySpec(keys);
		String result="";
		try {
			KeyFactory keyfac = KeyFactory.getInstance("RSA");
			PrivateKey pkey = keyfac.generatePrivate(pk8);
			Signature signature = Signature.getInstance("SHA256WithRSA");
			signature.initSign(pkey);
			signature.update(data);
			result= Base64.encodeBase64String(signature
					.sign());
		} catch (Exception e) {
			logger.error("签名报错",e);
		}
		return result;

	}

	/***
	 * 签名验证
	 *
	 * @param signdata
	 * @param data
	 * @return
	 */
	public static boolean decodeSign(byte[] signdata, byte[] data,
			String publicKey) {
		// byte[] keys=Base64.decodeBase64(PapConfig.PUBLICKEY);
		byte[] keys = Base64.decodeBase64(publicKey);
		byte[] sign = Base64.decodeBase64(signdata);// 签名二次解码
		boolean b=false;
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keys);
		try {
			KeyFactory keyfac = KeyFactory.getInstance("RSA");
			PublicKey pubKey = keyfac.generatePublic(keySpec);
			Signature signature = Signature.getInstance("SHA256WithRSA");
			signature.initVerify(pubKey);
			signature.update(data);
			b= signature.verify(sign);
		} catch (Exception e) {
			logger.error("验签报错",e);
		}
		return b;
	}

    /**
     * 加密，三步走。
     *
     * @param publicKey
     * @param plainText
     * @return
     * @throws InvalidKeySpecException
     */
    public static String RSAEncode(String publicKey, byte[] plainText) throws Exception{

    	byte[] keys = Base64.decodeBase64(publicKey);
    	X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keys);
    	KeyFactory keyfac = KeyFactory.getInstance(KEY_ALGORTHM);
		PublicKey pubKey = keyfac.generatePublic(keySpec);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);

        return Base64.encodeBase64String(cipher.doFinal(plainText));

    }

    /**
     * 解密，三步走。
     *
     * @param privateKey
     * @param encodedText
     * @return
     * @throws InvalidKeySpecException
     */
    public static String RSADecode(String privateKey, byte[] encodedText) throws Exception{

    	byte[] keys = Base64.decodeBase64(privateKey);// 解码
		PKCS8EncodedKeySpec pk8 = new PKCS8EncodedKeySpec(keys);
		KeyFactory keyfac = KeyFactory.getInstance(KEY_ALGORTHM);
		PrivateKey pkey = keyfac.generatePrivate(pk8);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, pkey);
        return new String(cipher.doFinal(encodedText));

    }

}
