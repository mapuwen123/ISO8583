package com.java8583.j8;
 
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class ISO8583Util {
	
	/**
	 * 
	 * ISO8583报文工具类
	 * 
	 */	
		public static String packet_encoding="UTF-8";//报文编码 UTF-8 GBK
		private static Map map8583Definition = null;// 8583报文128域定义器
		
		static{
			String basepath=ISO8583Util.class.getClassLoader().getResource("").getPath();
			System.out.println(basepath);
			System.out.println("Lsy8583Util使用编码配置:[encoding:"+packet_encoding+"]");
			Properties property = new Properties();
			String path =  basepath+"/config_8583.properties";
			FileInputStream fis;
			try {
				fis = new FileInputStream(path);
				property.load(fis);
				ISO8583Util.map8583Definition = new HashMap(property);
				fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 8583报文初始位图:128位01字符串
		public static String getInitBitMap(){
			String initBitMap = 
			  "00000000" + "00000000" + "00000000" + "00000000" 
			+ "00000000" + "00000000" + "00000000" + "00000000"
			+ "00000000" + "00000000" + "00000000" + "00000000" 
			+ "00000000" + "00000000" + "00000000" + "00000000" ;
			return initBitMap;
		}
		
		/**
		 * @param args
		 * @throws UnsupportedEncodingException 
		 */
		public static void main(String[] args) throws UnsupportedEncodingException {
//			try {
//				//***********************组装8583报文测试--start***********************//
//				TreeMap filedMap=new TreeMap();//报文域
//				filedMap.put("FIELD003", "1799");//交易码
//				filedMap.put("FIELD013", "2013-11-06");//交易日期
//				filedMap.put("FIELD008", "12345678901");//账号
//				filedMap.put("FIELD033", "aa索隆bb");//注意这个域是变长域!
//				filedMap.put("FIELD036", "123456");//注意这个域是变长域!
//				
//				byte[] send=make8583(filedMap);
//				System.out.println("完成组装8583报文=="+new String(send,packet_encoding)+"==");
//				//***********************组装8583报文测试--end***********************//
//				
//				
//				//***********************解析8583报文测试--start***********************//
//				Map back=analyze8583(send);
//				System.out.println("完成解析8583报文=="+back.toString()+"==");
//				//***********************解析8583报文测试--end***********************//
//			} catch (UnsupportedEncodingException e) {
//				e.printStackTrace();
//			}
			TreeMap map = new TreeMap();
			map.put("FIELD003", "810000");
			map.put("FIELD012", new SimpleDateFormat("hhmmss").format(new Date()));
			map.put("FIELD013", new SimpleDateFormat("MMdd").format(new Date()));
			map.put("FIELD032", "60071200000");
			map.put("FIELD037", "000000000189");
			map.put("FIELD039", "00");
			map.put("FIELD041", "77101001");
			map.put("FIELD042", "500000077177101");
			map.put("FIELD060", "000046");
			map.put("FIELD061", "0001");
			map.put("FIELD062", "0A45FA52BE6856828F6B7C0E07FF69890A3F4FF25C816FA9");
			String send=make8583(map);
			System.out.println("send:" + send);
			send = set8583Header(send, "0000010000020000010810");
			System.out.println("send:" + send);
			send = set8583PackSize(send);
			System.out.println("send:" + send);
			
			Map back = analyze8583(send.getBytes());
			System.out.println("back:" + back.toString());
			
		}
		
		/**
		 * 组装8583报文
		 * @param hm
		 * @return
		 */
		public static String make8583(TreeMap  filedMap){
			String whoe8583=null;
			if(filedMap==null){
				return null;
			}
			try {
				String  bitMap128=getInitBitMap();//获取初始化的128位图
				//按照8583定义器格式化各个域的内容
				Map all=formatValueTo8583(filedMap,bitMap128);
				// 获取上送报文内容
				System.out.println("all:" + all);
			    whoe8583=getWhole8583Packet(all);
			    
				return whoe8583;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return whoe8583;
		}
		/**
		 * 获取完整的8583报文体（128域）
		 * @param pacBody
		 * @return
		 */
		public static String getWhole8583Packet(Map all){
			if(all==null||all.get("formatedFiledMap")==null||all.get("bitMap128")==null){
				return null;
			}
			try {
				String  bitMap128=(String)all.get("bitMap128");
				// 128域位图二进制字符串转16位16进制
//				byte[] bitmaps= get16BitByteFromStr(bitMap128);
				String bitmaps = binaryString2hexString(bitMap128);
				TreeMap pacBody=(TreeMap)all.get("formatedFiledMap");
				StringBuffer last128=new StringBuffer();
				Iterator it=pacBody.keySet().iterator();
				for(;it.hasNext();){
					String key=(String)it.next();
					String value=(String)pacBody.get(key);
					last128.append(value);
				}
//				byte[] bitContent = last128.toString().getBytes(packet_encoding);//域值
				String bitContent = last128.toString();
				
				//组装
				String package8583=null;
				package8583=ISO8583Util.arrayApend(package8583, bitmaps);
				package8583=ISO8583Util.arrayApend(package8583, bitContent);

				return package8583;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		/**
		 * 加入报文头
		 * @param send
		 * @param head
		 * @return
		 */
		public static String set8583Header(String send, String head) {
			String headH = null;
			headH = ISO8583Util.arrayApend(head, send);
			return headH;
		}
		
		/**
		 * 加入保温长度
		 * @param send
		 * @param packSize
		 * @return
		 */
		public static String set8583PackSize(String send) {
			String all = null;
			String size = Integer.toHexString(send.length() / 2);
			if (size.length() == 1) {
				size = "000" + size;
			} else if (size.length() == 2) {
				size = "00" + size;
			} else if (size.length() == 3) {
				size = "0" + size;
			}
			all = size + send;
			return all;
		}
		
		/**
		 * 16进制转2进制
		 * @param bString
		 * @return
		 */
		public static String binaryString2hexString(String bString)  
	    {  
	        if (bString == null || bString.equals("") || bString.length() % 8 != 0)  
	            return null;  
	        StringBuffer tmp = new StringBuffer();  
	        int iTmp = 0;  
	        for (int i = 0; i < bString.length(); i += 4)  
	        {  
	            iTmp = 0;  
	            for (int j = 0; j < 4; j++)  
	            {  
	                iTmp += Integer.parseInt(bString.substring(i + j, i + j + 1)) << (4 j 1);  
	            }  
	            tmp.append(Integer.toHexString(iTmp));  
	        }  
	        return tmp.toString().substring(0, 16);  
	    }
		
		/**
		 * 2进制转16进制
		 * @param hexString
		 * @return
		 */
		public static String hexString2binaryString(String hexString)  
	    {  
	        if (hexString == null || hexString.length() % 2 != 0)  
	            return null;  
	        String bString = "", tmp;  
	        for (int i = 0; i < hexString.length(); i++)  
	        {  
	            tmp = "0000"  
	                    + Integer.toBinaryString(Integer.parseInt(hexString  
	                            .substring(i, i + 1), 16));  
	            bString += tmp.substring(tmp.length() 4);  
	        }  
	        return bString;  
	    }  
		
		public static Map formatValueTo8583(TreeMap filedMap,String  bitMap128){
			Map all=new HashMap();
			TreeMap formatedFiledMap=new TreeMap();//格式化结果
			if(filedMap!=null){
				Iterator it=filedMap.keySet().iterator();
				for(;it.hasNext();){
					String fieldName=(String)it.next();//例如FIELD005
					String fieldValue=(String)filedMap.get(fieldName);//字段值
					try{
						if (fieldValue == null) {
							System.out.println("error:报文域 {" + fieldName + "}为空值");
							fieldValue = "";
							return null;
						}
						//将域值编码转换，保证报文编码统一
						fieldValue=new String(fieldValue.getBytes(packet_encoding),packet_encoding);
						
						// 数据域名称FIELD开头的为128域
						if (fieldName.startsWith("FIELD") && fieldValue.length() >= 0) {
							String fieldNo = fieldName.substring(5, 8);//例如005
							// 组二进制位图串
							bitMap128 = change16bitMapFlag(fieldNo, bitMap128);
							// 获取域定义信息
							String[] fieldDef = ISO8583Util.map8583Definition.get("FIELD" + fieldNo).toString().split(",");
							String defType=fieldDef[0];//类型定义例string
							String defLen=fieldDef[1];//长度定义,例20
							
							boolean isFixLen=true;//是否定长判断
							
							if(defLen.startsWith("VAR")){//变长域
								isFixLen=false;
								defLen=defLen.substring(3);//获取VAR2后面的2
							}
							int fieldLen = fieldValue.getBytes(packet_encoding).length;//字段值得实际长度
							
							// 判断是否为变长域
							if (!isFixLen) {// 变长域(变长域最后组装成的效果：例如变长3位，定义var3，这里的3是指长度值占3位，字段值是123456，最后结果就是006123456)
								int defLen1 = Integer.valueOf(defLen);
								if (String.valueOf(fieldLen).length() > (10*defLen1)) {
									System.out.println("error:字段" + fieldName + "的数据定义长度的长度为" + defLen + "位,长度不能超过"+(10*defLen1));
									return null;
								}else{
									//将长度值组装入字段
									fieldValue = getVaryLengthValue(fieldValue, defLen1) + fieldValue;
									if (fieldValue.length()%2 != 0) {
										fieldValue = fieldValue + "0";
									}
								}
							} else {//定长域(定长比较好理解，一个字段规定是N位，那么字段值绝对不能超过N位，不足N位就在后面补空格)
								int defLen2 = Integer.valueOf(defLen);
								if (fieldLen > defLen2) {
									System.out.println("error:字段" + fieldName + "的数据定义长度为" + defLen + "位,长度不能超过"+defLen);
									return null;
								}else{
									if (defType.equals("ASC")) {
										fieldValue = ScalUtil.convertStringToHex(fieldValue);
									} else {
										fieldValue = getFixFieldValue(fieldValue,defLen2);//定长处理
									}
								}
							}
							if (defType.equals("BCD")) {
								
							} else if (defType.equals("ASC")) {
								
							} else if (defType.equals("BIN")) {
								
							}
							
							System.out.println("组装后报文域 {" + fieldName + "}==" + fieldValue+"==,域长度:"+fieldValue.getBytes(packet_encoding).length);
						}
						
						// 返回结果赋值
						if (filedMap.containsKey(fieldName)) {
							if (formatedFiledMap.containsKey(fieldName)) {
								formatedFiledMap.remove(fieldName);
							}
							formatedFiledMap.put(fieldName, fieldValue);
						} else {
							System.out.println("error:" +fieldName + "配置文件中不存在!");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}//end for
			}
			System.out.println("");
			all.put("formatedFiledMap", formatedFiledMap);
			all.put("bitMap128", bitMap128);
			return all;
		}
		
		

		/**
		 * 解析8583报文
		 * 
		 * @param content8583
		 */
		public static Map analyze8583(byte[] content8583) {
			System.out.println("content8583:" + new String(content8583));
			TreeMap filedMap=new TreeMap();
			try {
				// 取位图
				String bitMap16byte = new String(content8583).substring(26, 42);
				String bitMap128Str = hexString2binaryString(bitMap16byte);
				int textSize = content8583.length;
				String Text8583 = new String(content8583).substring(26, textSize);
				byte[] Text8583byte = Text8583.getBytes();
//				byte[] bitMap16byte = new byte[16];
//				System.arraycopy(content8583, 0, bitMap16byte, 0, 16);
//				// 16位图转2进制位图128位字符串
//				String bitMap128Str = hexString2binaryString(new String(bitMap16byte));
//				System.out.println("bitMap128Str:" + bitMap128Str);
				//记录当前位置,从位图后开始遍历取值 
				int pos = 16;
				// 遍历128位图，取值。注意从FIELD002开始
				for (int i = 1; i < bitMap128Str.length(); i++) {
					String filedValue = "";//字段值
					String filedName = "FIELD" + getNumThree((i+1));//FIELD005
					
					if (bitMap128Str.charAt(i) == '1') {
						// 获取域定义信息
						String[] fieldDef = ISO8583Util.map8583Definition.get(filedName).toString().split(",");
						String defType=fieldDef[0];//类型定义例string
						String defLen=fieldDef[1];//长度定义,例20
						System.out.println("defType:" + defType + "-defLen:" + defLen);
						boolean isFixLen=true;//是否定长判断
						
						if(defLen.startsWith("VAR")){//变长域
							isFixLen=false;
							defLen=defLen.substring(3);//获取VAR2后面的2
						}
						// 截取该域信息
						if (!isFixLen) {//变长域
							int defLen1 = Integer.valueOf(defLen);//VAR2后面的2
							String realLen1=new String(Text8583byte, pos, defLen1, packet_encoding);//报文中实际记录域长,例如16,023
							int realAllLen=defLen1+Integer.valueOf(realLen1);//该字段总长度（包括长度值占的长度）
							if (realAllLen%2 != 0) {
								realAllLen = realAllLen + 1;
							}
//							filedValue = new String(content8583, pos+defLen1, Integer.valueOf(realLen1), packet_encoding);
							byte[] filedValueByte=new byte[Integer.valueOf(realLen1)];
							System.arraycopy(Text8583byte, pos+defLen1, filedValueByte, 0, filedValueByte.length);
							filedValue=new String(filedValueByte,packet_encoding);
							pos += realAllLen;//记录当前位置
						} else {//定长域
							int defLen2 = Integer.valueOf(defLen);//长度值占的位数
							filedValue = new String(Text8583byte, pos, defLen2, packet_encoding);
							if (defType.equals("ASC")) {
								filedValue = ScalUtil.convertHexToString(filedValue);
							}
							pos += defLen2;//记录当前位置
						}
						filedMap.put(filedName, filedValue);
					}
				}//end for
			} catch (Exception e) {
				e.printStackTrace();
			}
			return filedMap;
		}
		
		//********************************以下是工具方法,有些没有使用到***********************************************************//

		/**
		 * 复制字符
		 * @param str
		 * @param count
		 * @return
		 */
		public static String strCopy(String str,int count){
			StringBuffer sb = new StringBuffer();
			for(int i=0;i < count;i++){
				sb.append(str);
			}
			return sb.toString();
		}
		/**
		 * 将setContent放入set（考虑到数组越界）
		 * @param set
		 * @param setContent
		 * @return
		 */
		public static byte[] setToByte(byte[] set,byte[] setContent){
			byte[] res=new byte[set.length];
			if(set==null||setContent==null){
				
			}else{
				if(set.length<setContent.length){
					
				}else{
					System.arraycopy(setContent, 0, res, 0, setContent.length);
				}
			}
			return res;
		}
		public static byte[] setToByte(byte[] set,String setContentStr){
			byte[] res=new byte[set.length];
			byte[] setContent;
			try {
				setContent = setContentStr.getBytes(packet_encoding);
				res=setToByte(res,setContent);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return res;
		}
		
		public static String getPacketLen(int len){
			String res="";
			String lenStr=String.valueOf(len);
			int lenC=4-lenStr.length();
			res=strCopy("0",lenC)+lenStr;
			return res;
		}
		public static String getPacketLen(String lenStr){
			String res="";
			if(lenStr==null){
				
			}else{
				res=getPacketLen(Integer.valueOf(lenStr));
			}
			return res;
		}
		
		
		/**
		 * 返回a和b的组合,实现累加功能
		 * @param a
		 * @param b
		 * @return
		 */
		public static String arrayApend(String a,String b){
			String c = "";
			if (a == null) {
				if (b != null) {
					c = b;
				}
			} else {
				c = a + b;
			}
			return c;
		}
		
		
		/**
		 * 改变128位图中的标志为1
		 * @param fieldNo
		 * @param res
		 * @return
		 */
		public static String change16bitMapFlag(String fieldNo, String res) {
			int indexNo=Integer.parseInt(fieldNo);
			res = res.substring(0, indexNo-1) + "1" + res.substring(indexNo);
			return res;
		}
		
		
		/**
		 * 位图操作 
		 * 
		 * 把16位图的字节数组转化成128位01字符串
		 * @param packet_header_map
		 * @return
		 */
		public static String get16BitMapStr(byte[] bitMap16){
			String bitMap128 = "";
			// 16位图转2进制位图128位字符串
			for (int i = 0; i < bitMap16.length; i++) {
				int bc = bitMap16[i];
				bc=(bc<0)?(bc+256):bc;
				String bitnaryStr=Integer.toBinaryString(bc);//二进制字符串
				// 左补零，保证是8位
				String rightBitnaryStr = strCopy("0",Math.abs(8-bitnaryStr.length())) + bitnaryStr;//位图二进制字符串
				// 先去除多余的零，然后组装128域二进制字符串
				bitMap128+=rightBitnaryStr;
			}
			return bitMap128;
		}
		
		/**
		 *  位图操作 
		 *  
		 * 把128位01字符串转化成16位图的字节数组
		 * @param packet_header_map
		 * @return
		 */
		public static byte[] get16BitByteFromStr(String str_128){
			byte[]  bit16=new byte[16];
			try {
				if(str_128==null||str_128.length()!=128){
					return null;
				}
				// 128域位图二进制字符串转16位16进制
				byte[]  tmp=str_128.getBytes(packet_encoding);
				System.out.println("++++++++++++" + new String(tmp));
				int weight;//权重
				byte[] strout = new byte[128];
				int i, j, w = 0;
				for (i = 0; i < 16; i++) {
					weight = 0x0080;
					for (j = 0; j < 8; j++) {
						strout[i] += ((tmp[w]) '0') * weight;
						weight /= 2;
						w++;
					}
					bit16[i] = strout[i];
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return bit16;
		}
		
		
		/**
		 * 从完整的8583报文中获取位图（16字节数组）
		 * @param packet
		 * @return
		 */
		public static byte[] getPacketHeaderMap(byte[] packet){
			byte[] packet_header_map = new byte[16];
			if(packet==null||packet.length<16){
				return null;
			}
			for(int i=0;i<16;i++){
				packet_header_map[i]=packet[i];
			}
			return packet_header_map;
		}
		/**
		 * 从完整的8583报文中获取16位图，转化成128位的01字符串
		 * 
		 * @param content8583
		 * @return
		 */
		public static String get16BitMapFrom8583Byte(byte[] content8583){
			// 取位图
			byte[] bitMap16 = getPacketHeaderMap(content8583);
			// 16位图转2进制位图128位字符串
			String bitMap128 = get16BitMapStr(bitMap16);
			
			return bitMap128;
		}
		

		
		//返回字段号码，例如005
		public static String getNumThree(int i){
			String len="";
			String iStr=String.valueOf(i);
			len=strCopy("0",3-iStr.length())+iStr;
			return len;
		}
		
		/**
		 * 获取字符串变长值
		 * @param valueStr
		 * @param defLen
		 * 例如getFixLengthValue("12345678", 2)返回08
		 * 例如getFixLengthValue("12345678", 3)返回008
		 * 
		 * 注意变长长度的计算：
		 * 长度的判断使用转化后的字节数组长度，因为中文在不同的编码方式下，长度是不同的，GBK是2，UTF-8是3，按字符创长度算就是1.
		 * 解析报文是按照字节来解析的，所以长度以字节长度为准，防止中文带来乱码。
		 * 
		 * 比如一个变长域:aa索隆bb，如果按照字符串计算长度那么就是6，最后是06aa索隆bb。
		 * 这样在解析时按照字节解析长度就乱了，因为按照GBK字节解析，一个汉字占2，按照UTF-8解析，一个汉字占3.
		 * 所以在计算时必须按照字节长度为准！按照我们设置的UTF-8编码结果就是10aa索隆bb.
		 * 这样在解析时长度正好是10，也就不会乱了。
		 * @return
		 */
		public static String getVaryLengthValue(String valueStr,int defLen){
			return getVaryLengthValue(valueStr,defLen,packet_encoding);
		}
		public static String getVaryLengthValue(String valueStr,int defLen,String encoding){
			String fixLen="";
			try{
				if(valueStr==null){
					return strCopy("0",defLen);
				}else{
					byte[] valueStrByte=null;
					//这里最好指定编码，不使用平台默认编码
					if(encoding==null||encoding.trim().equals("")){
						valueStrByte=valueStr.getBytes();
					}else{
						valueStrByte=valueStr.getBytes(encoding);
					}
					//长度的判断使用转化后的字节数组长度，因为中文在不同的编码方式下，长度是不同的，GBK是2，UTF-8是3，按字符创长度算就是1.
					//解析报文是按照字节来解析的，所以长度以字节长度为准，防止中文带来乱码
					if(valueStrByte.length>(10*defLen)){
						return null;
					}else{
						int len=valueStrByte.length;//字段实际长度
						String len1=String.valueOf(len);
						fixLen=strCopy("0",(defLen-len1.length()))+len1;
					}
				} 
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return fixLen;
		}
		
		/**
		 * 将字段值做定长处理，不足定长则在后面补空格
		 * @param valueStr
		 * @param defLen
		 * @return
		 */
		public static String getFixFieldValue(String valueStr,int defLen){
			return getFixFieldValue(valueStr,defLen,packet_encoding);
		}
		public static String getFixFieldValue(String valueStr,int defLen,String encoding){
			String fixLen="";
			try {
				if(valueStr==null){
					return strCopy(" ",defLen);
				}else{
					byte[] valueStrByte=null;
					//这里最好指定编码，不使用平台默认编码
					if(encoding==null||encoding.trim().equals("")){
						valueStrByte=valueStr.getBytes();
					}else{
						valueStrByte=valueStr.getBytes(encoding);
					}
					//长度的判断使用转化后的字节数组长度，因为中文在不同的编码方式下，长度是不同的，GBK是2，UTF-8是3，按字符创长度算就是1.
					//解析报文是按照字节来解析的，所以长度以字节长度为准，防止中文带来乱码
					if(valueStrByte.length>defLen){
						return null;
					}else{
						fixLen=valueStr+strCopy(" ",defLen-valueStrByte.length);
					}
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			return fixLen;
		}
		

		public static String getPacket_encoding() {
			return packet_encoding;
		}

		public static void setPacket_encoding(String packet_encoding) {
			ISO8583Util.packet_encoding = packet_encoding;
		}

		public static Map getMap8583Definition() {
			return map8583Definition;
		}

		public static void setMap8583Definition(Map map8583Definition) {
			ISO8583Util.map8583Definition = map8583Definition;
		}

}