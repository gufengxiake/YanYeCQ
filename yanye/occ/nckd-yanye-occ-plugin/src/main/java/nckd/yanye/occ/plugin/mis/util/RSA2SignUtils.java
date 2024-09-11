package nckd.yanye.occ.plugin.mis.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RSA2SignUtils {
	private final static Logger logger = LoggerFactory.getLogger(RSA2SignUtils.class);

	/***
	 * 参数名ASCII码从小到大排序 签名 @param req @param key @return @throws
	 */
	public static String createSign(SortedMap<Object, Object> parameters,String priKeyStr) {
		StringBuffer sb = new StringBuffer();
		StringBuffer sbkey = new StringBuffer();
		Set es = parameters.entrySet(); // 所有参与传参的参数按照accsii排序（升序）
		Iterator it = es.iterator();
		String sign = "";
		try {
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				String k = (String) entry.getKey();
				Object v = entry.getValue();
				// 空值不传递，不参与签名组串
				if (null != v && !"".equals(v)) {
					sb.append("&" + k + "=" + v);
					sbkey.append("&" + k + "=" + v);
				}
			}
			logger.info("签名字符串:" + sb.toString());

			String code = sbkey.substring(1);
			// logger.info("验签字符串:"+code);
			// MD5加密,结果转换为大写字符
			sign = RSAEncoderUtil.encodeSign(code.getBytes("UTf-8"), priKeyStr);
			logger.info("签名结果:" + sign);
		} catch (Exception e) {
			logger.error("签名报错",e);
		}
		return sign;
	}

	/***
	 * 参数名ASCII码从小到大排序 验签
	 * @return
	 */

	public static boolean checkSign(SortedMap<Object, Object> parameters, String sign,String pubKeyStr) {
		StringBuffer sb = new StringBuffer();
		StringBuffer sbkey = new StringBuffer();
		Set es = parameters.entrySet(); // 所有参与传参的参数按照accsii排序（升序）
		Iterator it = es.iterator();
		boolean b = false;
		try {
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				String k = (String) entry.getKey();
				Object v = entry.getValue();
				// 空值不传递，不参与签名组串
				if (null != v && !"".equals(v)) {
					sb.append("&" + k + "=" + v);
					sbkey.append("&" + k + "=" + v);
				}
			}
			logger.info("验签字符串:" + sb.toString());
			String code = sbkey.substring(1);
			// logger.info("验签字符串:"+code);
			// MD5加密,结果转换为大写字符

			b = RSAEncoderUtil.decodeSign(sign.getBytes("UTf-8"), code.getBytes("UTf-8"), pubKeyStr);

		} catch (Exception e) {
			logger.error("验签报错",e);
		}
		return b;

	}

}
