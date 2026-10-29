package org.jeecg.modules.wms.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 库存分页 SQL 的静态约束。方案 3.4.4 要求按仓库/库区/库位/货主/SKU/批次/
 * 质量状态/效期状态/SN 任意组合查询，SN 维度靠 wms_item_sn 定位到
 * (库位, SKU, 批次) 组合。这里守住两点：
 * 1. 过滤条件确实存在，删掉了会在这里报出来；
 * 2. 必须用 EXISTS 而不是 JOIN —— 一个库位有多件 SN 时 JOIN 会把库存行
 *    按 SN 件数放大，数量就错了。
 */
public class WmsInventoryMapperXmlTest {

    private String mapperXml() throws IOException {
        // mapper XML 和 Mapper 接口同放在 src/main/java 下，不在 test 的 classpath 上，
        // 所以直接按模块源码路径读（surefire 的工作目录是模块根目录）。
        Path path = Paths.get("src/main/java/org/jeecg/modules/wms/mapper/xml/WmsInventoryMapper.xml");
        assertTrue(Files.exists(path), "找不到 WmsInventoryMapper.xml：" + path.toAbsolutePath());
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Test
    void snFilterMustExist() throws IOException {
        String xml = mapperXml();
        assertTrue(xml.contains("q.snCode != null and q.snCode != ''"), "缺少 SN 过滤条件");
        assertTrue(xml.contains("wms_item_sn sn"), "SN 过滤必须查 wms_item_sn");
    }

    @Test
    void snFilterMustUseExistsNotJoin() throws IOException {
        String xml = mapperXml();
        int from = xml.indexOf("q.snCode != null");
        assertTrue(from > 0);
        String block = xml.substring(from, Math.min(xml.length(), from + 400));

        assertTrue(block.contains("EXISTS"), "SN 过滤必须用 EXISTS，否则库位多件 SN 会放大库存行");
        assertFalse(block.toUpperCase().contains("JOIN WMS_ITEM_SN"),
                "SN 过滤不能用 JOIN wms_item_sn，会按 SN 件数放大库存数量");
    }
}
