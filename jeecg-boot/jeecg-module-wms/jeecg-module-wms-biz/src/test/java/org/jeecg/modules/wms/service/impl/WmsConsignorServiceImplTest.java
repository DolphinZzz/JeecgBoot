package org.jeecg.modules.wms.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.entity.WmsConsignor;
import org.jeecg.modules.wms.mapper.WmsConsignorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 货主档案的两条硬规则：
 * 1. 编码由服务端发号且不能重复（编码会写进历史单据，被删除的编号也不允许复用）；
 * 2. 已被业务数据引用的货主不能删，只能停用。
 */
@ExtendWith(MockitoExtension.class)
public class WmsConsignorServiceImplTest {

    @Mock
    private WmsConsignorMapper consignorMapper;

    @InjectMocks
    private WmsConsignorServiceImpl consignorService;

    @BeforeEach
    void setUp() {
        // ServiceImpl 的 baseMapper 不是构造注入，@InjectMocks 不会填，必须手动塞
        ReflectionTestUtils.setField(consignorService, "baseMapper", consignorMapper);
    }

    @Test
    void saveGeneratesNextCodeWhenAbsent() {
        when(consignorMapper.selectMaxConsignorCode("HZ")).thenReturn("HZ000036");

        consignorService.save(consignor("新货主"));

        ArgumentCaptor<WmsConsignor> captor = ArgumentCaptor.forClass(WmsConsignor.class);
        verify(consignorMapper).insert(captor.capture());
        WmsConsignor saved = captor.getValue();
        assertEquals("HZ000037", saved.getConsignorCode(), "编码应在库内最大序号上 +1");
        assertEquals(1, saved.getEnableStatus(), "未指定时默认启用");
        assertEquals(1, saved.getIsDel());
        assertEquals("新货主", saved.getConsignorName());
    }

    @Test
    void saveStartsWithFirstCodeOnEmptyTable() {
        when(consignorMapper.selectMaxConsignorCode("HZ")).thenReturn(null);

        consignorService.save(consignor("首家货主"));

        ArgumentCaptor<WmsConsignor> captor = ArgumentCaptor.forClass(WmsConsignor.class);
        verify(consignorMapper).insert(captor.capture());
        assertEquals("HZ000001", captor.getValue().getConsignorCode());
    }

    @Test
    void saveRejectsDuplicateCode() {
        WmsConsignor entity = consignor("重复编码").setConsignorCode("HZ000001");
        when(consignorMapper.countByCode("HZ000001", null)).thenReturn(1L);

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> consignorService.save(entity));
        assertTrue(ex.getMessage().contains("货主编码已存在"), ex.getMessage());
        verify(consignorMapper, never()).insert(any(WmsConsignor.class));
    }

    @Test
    void saveRejectsBlankName() {
        WmsConsignor entity = consignor("   ");
        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> consignorService.save(entity));
        assertTrue(ex.getMessage().contains("货主名称"), ex.getMessage());
    }

    @Test
    void updateDoesNotOverwriteCode() {
        WmsConsignor entity = consignor("改名货主").setId(3).setConsignorCode("HZ999999");

        consignorService.updateById(entity);

        ArgumentCaptor<WmsConsignor> captor = ArgumentCaptor.forClass(WmsConsignor.class);
        verify(consignorMapper).updateById(captor.capture());
        assertNull(captor.getValue().getConsignorCode(), "编码是对外标识，编辑时一律不写库");
        assertEquals("改名货主", captor.getValue().getConsignorName());
    }

    @Test
    void deleteBlockedWhenReferenced() {
        when(consignorMapper.countReferences(3L)).thenReturn(2L);

        JeecgBootException ex = assertThrows(JeecgBootException.class, () -> consignorService.checkDeletable(3));
        assertTrue(ex.getMessage().contains("不能删除"), ex.getMessage());
    }

    @Test
    void deleteAllowedWhenUnreferenced() {
        when(consignorMapper.countReferences(3L)).thenReturn(0L);
        assertDoesNotThrow(() -> consignorService.checkDeletable(3));
    }

    private WmsConsignor consignor(String name) {
        return new WmsConsignor().setConsignorName(name);
    }
}
