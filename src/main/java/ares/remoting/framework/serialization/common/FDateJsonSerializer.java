package ares.remoting.framework.serialization.common;

import com.alibaba.fastjson.serializer.JSONSerializer;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * @Description:
 * @author: yuhongxi
 * @date:2019/11/7
 */
public class FDateJsonSerializer extends JSONSerializer {

    private static final FastDateFormat DATE_FORMAT=FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

}
