package org.jeecg.modules.wms.vo;

import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WmsInventoryMapper.xml 用 #{d.xxxOrDefault} 取默认值，MyBatis 按 JavaBean 约定
 * 反射查找 getXxxOrDefault()。方法名少了 get 前缀会在运行期抛
 * ReflectionException（no getter for property named 'xxxOrDefault'），
 * 单测此前没有覆盖，只有真连数据库跑盘点调账时才暴露。
 */
public class WmsInventoryDimensionTest {

    @Test
    void mapperReferencedDefaultsMustBeReadableAsBeanProperties() throws Exception {
        Set<String> properties = Arrays.stream(Introspector.getBeanInfo(WmsInventoryDimension.class)
                        .getPropertyDescriptors())
                .map(PropertyDescriptor::getName)
                .collect(Collectors.toSet());

        for (String property : Arrays.asList("batchNumberOrDefault", "consignorIdOrDefault", "qualityStatusOrDefault")) {
            assertTrue(properties.contains(property),
                    "WmsInventoryDimension 必须按 JavaBean 属性暴露 " + property);
        }
    }

    @Test
    void defaultsFallBackWhenDimensionIsSparse() {
        WmsInventoryDimension sparse = new WmsInventoryDimension();

        assertTrue(sparse.getBatchNumberOrDefault().isEmpty());
        assertEquals(0L, sparse.getConsignorIdOrDefault());
        assertEquals(1, sparse.getQualityStatusOrDefault());
    }
}
