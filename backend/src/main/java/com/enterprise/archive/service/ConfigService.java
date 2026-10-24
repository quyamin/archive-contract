package com.enterprise.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.archive.common.BizException;
import com.enterprise.archive.common.UserContext;
import com.enterprise.archive.dto.ConfigUpdateRequest;
import com.enterprise.archive.entity.SysConfig;
import com.enterprise.archive.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final SysConfigMapper configMapper;

    @Value("${app.file.storage-path:./data/uploads}")
    private String defaultStoragePath;

    @Value("${app.file.max-size-mb:50}")
    private int defaultMaxSize;

    @Value("${app.file.allowed-ext:pdf,doc,docx,xls,xlsx,jpg,jpeg,png,gif}")
    private String defaultAllowedExt;

    @Value("${app.warning.advance-days:30}")
    private int defaultAdvanceDays;

    public List<SysConfig> list() {
        return configMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .orderByAsc(SysConfig::getConfigGroup, SysConfig::getId));
    }

    public void update(ConfigUpdateRequest request) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, request.getConfigKey()).last("LIMIT 1"));
        if (config == null) {
            throw new BizException("配置项不存在");
        }
        if ("file.max-size-mb".equals(request.getConfigKey())) {
            try {
                int size = Integer.parseInt(request.getConfigValue());
                if (size <= 0 || size > 1024) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                throw new BizException("文件大小限制应为1-1024之间的整数");
            }
        }
        if ("warning.advance-days".equals(request.getConfigKey())) {
            try {
                int days = Integer.parseInt(request.getConfigValue());
                if (days < 1 || days > 365) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                throw new BizException("预警提前天数应为1-365之间的整数");
            }
        }
        config.setConfigValue(request.getConfigValue());
        config.setUpdateBy(UserContext.userId());
        config.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(config);
    }

    public String getString(String key, String defaultValue) {
        SysConfig config = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key).last("LIMIT 1"));
        return config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()
                ? defaultValue : config.getConfigValue();
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(getString(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String storagePath() {
        return getString("file.storage-path", defaultStoragePath);
    }

    public int maxSizeMb() {
        return getInt("file.max-size-mb", defaultMaxSize);
    }

    public String allowedExt() {
        return getString("file.allowed-ext", defaultAllowedExt);
    }

    public int warningAdvanceDays() {
        return getInt("warning.advance-days", defaultAdvanceDays);
    }
}
