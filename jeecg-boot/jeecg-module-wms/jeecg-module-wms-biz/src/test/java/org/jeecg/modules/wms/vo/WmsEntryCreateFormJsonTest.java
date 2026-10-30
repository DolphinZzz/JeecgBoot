package org.jeecg.modules.wms.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 建单入参的日期反序列化。
 * 全局 Jackson 的 date-format 是 yyyy-MM-dd HH:mm:ss，前端明细表格的日期列只传
 * yyyy-MM-dd，所以生产日期/有效期至必须在字段上声明自己的格式，否则建单会直接 400。
 */
class WmsEntryCreateFormJsonTest {

    /** 与 application-dev.yml 的 spring.jackson 配置保持一致 */
    private ObjectMapper globalMapper() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.dateFormat(format);
        return builder.build();
    }

    @Test
    void parsesDateOnlyProdDateAndExpireDate() throws Exception {
        String json = "{\"entryType\":1,\"warehouseId\":1,\"consignorId\":1,"
                + "\"predictedArrivalTime\":\"2026-10-20 08:30:00\","
                + "\"details\":[{\"skuId\":100,\"count\":10,"
                + "\"prodDate\":\"2026-10-14\",\"expireDate\":\"2027-10-14\"}]}";

        WmsEntryCreateForm form = globalMapper().readValue(json, WmsEntryCreateForm.class);

        assertNotNull(form.getPredictedArrivalTime());
        assertEquals(1, form.getDetails().size());
        WmsEntryDetailForm line = form.getDetails().get(0);
        assertNotNull(line.getProdDate());
        assertNotNull(line.getExpireDate());
        assertEquals("2026-10-14", new SimpleDateFormat("yyyy-MM-dd").format(line.getProdDate()));
        assertEquals("2027-10-14", new SimpleDateFormat("yyyy-MM-dd").format(line.getExpireDate()));
    }

    @Test
    void toleratesNullDates() throws Exception {
        String json = "{\"details\":[{\"skuId\":100,\"count\":10,"
                + "\"prodDate\":null,\"expireDate\":null}]}";

        WmsEntryDetailForm line = globalMapper()
                .readValue(json, WmsEntryCreateForm.class).getDetails().get(0);

        assertNull(line.getProdDate());
        assertNull(line.getExpireDate());
    }
}
