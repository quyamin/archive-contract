package com.enterprise.archive.job;

import com.enterprise.archive.service.WarningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarningScheduler {

    private final WarningService warningService;

    @Scheduled(cron = "${app.warning.cron:0 10 1 * * ?}")
    public void scanWarnings() {
        log.info("开始执行合同到期/付款节点预警扫描任务");
        try {
            warningService.scan();
        } catch (Exception e) {
            log.error("预警扫描任务执行失败", e);
        }
    }
}
