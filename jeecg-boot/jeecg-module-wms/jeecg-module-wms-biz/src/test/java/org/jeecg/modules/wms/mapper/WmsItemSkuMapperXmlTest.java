package org.jeecg.modules.wms.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 商品选择器联查 SQL 静态校验。
 */
public class WmsItemSkuMapperXmlTest {

    private String mapperXml() throws IOException {
        Path path = Paths.get("src/main/java/org/jeecg/modules/wms/mapper/xml/WmsItemSkuMapper.xml");
        assertTrue(Files.exists(path), "找不到 WmsItemSkuMapper.xml：" + path.toAbsolutePath());
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    void testSelectSkuPageForSelectSqlValid() throws IOException {
        String xml = mapperXml();
        assertTrue(xml.contains("id=\"selectSkuPageForSelect\""), "必须包含 selectSkuPageForSelect 查询节点");
        // wms_item_spu 表中没有 price 字段，价格必须直接取 s.price
        assertFalse(xml.contains("p.price"), "wms_item_spu 表无 price 字段，不得包含 p.price");
        assertTrue(xml.contains("s.price AS price"), "价格必须直接映射 s.price AS price");
    }
}
