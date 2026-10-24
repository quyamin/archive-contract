package com.enterprise.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.archive.common.BizConstants;
import com.enterprise.archive.common.BizException;
import com.enterprise.archive.common.PageResult;
import com.enterprise.archive.common.UserContext;
import com.enterprise.archive.dto.WarningQuery;
import com.enterprise.archive.entity.ContractInfo;
import com.enterprise.archive.entity.ContractPaymentNode;
import com.enterprise.archive.entity.SysWarning;
import com.enterprise.archive.mapper.ContractInfoMapper;
import com.enterprise.archive.mapper.ContractPaymentNodeMapper;
import com.enterprise.archive.mapper.SysUserMapper;
import com.enterprise.archive.mapper.SysWarningMapper;
import com.enterprise.archive.vo.WarningVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarningService {

    private final SysWarningMapper warningMapper;
    private final ContractInfoMapper contractMapper;
    private final ContractPaymentNodeMapper paymentNodeMapper;
    private final SysUserMapper userMapper;
    private final ConfigService configService;
    private final BorrowService borrowService;

    @Transactional
    public Map<String, Object> scan() {
        int advanceDays = configService.warningAdvanceDays();
        LocalDate today = LocalDate.now();
        int expiredContracts = expireContracts(today);
        int borrowExpired = borrowService.expireBorrows();

        Set<Long> receivers = new LinkedHashSet<>(userMapper.selectUserIdsByPerm("archive:review"));

        List<ContractInfo> contracts = contractMapper.selectList(new LambdaQueryWrapper<ContractInfo>()
                .in(ContractInfo::getStatus, BizConstants.ContractStatus.ARCHIVED, BizConstants.ContractStatus.EXPIRED));
        int expiryWarnings = 0;
        for (ContractInfo contract : contracts) {
            if (contract.getExpireDate() == null) continue;
            long days = ChronoUnit.DAYS.between(today, contract.getExpireDate());
            if (days >= 0 && days <= advanceDays) {
                String message = "合同《" + contract.getContractName() + "》将于" + contract.getExpireDate()
                        + "到期，剩余" + days + "天，请及时跟进续签或归档处理。";
                String bizKey = "EXPIRY-" + contract.getId() + "-" + contract.getExpireDate();
                Set<Long> targets = new HashSet<>(receivers);
                if (contract.getOwnerId() != null) targets.add(contract.getOwnerId());
                expiryWarnings += createWarnings(contract.getId(), BizConstants.WarningType.EXPIRY,
                        contract.getExpireDate(), message, bizKey, targets);
            }
        }

        List<Long> archivedIds = contracts.stream().map(ContractInfo::getId).toList();
        int payWarnings = 0;
        if (!archivedIds.isEmpty()) {
            List<ContractPaymentNode> nodes = paymentNodeMapper.selectList(new LambdaQueryWrapper<ContractPaymentNode>()
                    .in(ContractPaymentNode::getContractId, archivedIds)
                    .eq(ContractPaymentNode::getPaidStatus, 0)
                    .between(ContractPaymentNode::getPlanPayDate, today, today.plusDays(advanceDays)));
            Map<Long, ContractInfo> contractMap = contracts.stream()
                    .collect(Collectors.toMap(ContractInfo::getId, c -> c));
            for (ContractPaymentNode node : nodes) {
                ContractInfo contract = contractMap.get(node.getContractId());
                if (contract == null) continue;
                long days = ChronoUnit.DAYS.between(today, node.getPlanPayDate());
                String message = "合同《" + contract.getContractName() + "》付款节点【" + node.getNodeName()
                        + "】将于" + node.getPlanPayDate() + "到达，剩余" + days + "天，请及时处理付款事项。";
                String bizKey = "PAYMENT-" + node.getId() + "-" + node.getPlanPayDate();
                Set<Long> targets = new HashSet<>(receivers);
                if (contract.getOwnerId() != null) targets.add(contract.getOwnerId());
                if (contract.getCreateBy() != null) targets.add(contract.getCreateBy());
                payWarnings += createWarnings(contract.getId(), BizConstants.WarningType.PAYMENT,
                        node.getPlanPayDate(), message, bizKey, targets);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expiryWarnings", expiryWarnings);
        result.put("paymentWarnings", payWarnings);
        result.put("expiredContracts", expiredContracts);
        result.put("expiredBorrows", borrowExpired);
        result.put("scanTime", LocalDateTime.now());
        log.info("预警扫描完成: {}", result);
        return result;
    }

    private int expireContracts(LocalDate today) {
        List<ContractInfo> toExpire = contractMapper.selectList(new LambdaQueryWrapper<ContractInfo>()
                .eq(ContractInfo::getStatus, BizConstants.ContractStatus.ARCHIVED)
                .lt(ContractInfo::getExpireDate, today));
        for (ContractInfo contract : toExpire) {
            contract.setStatus(BizConstants.ContractStatus.EXPIRED);
            contractMapper.updateById(contract);
        }
        return toExpire.size();
    }

    private int createWarnings(Long contractId, String type, LocalDate warningDate, String message,
                               String bizKey, Set<Long> receiverIds) {
        int created = 0;
        for (Long receiverId : receiverIds) {
            if (receiverId == null) continue;
            Long exists = warningMapper.selectCount(new LambdaQueryWrapper<SysWarning>()
                    .eq(SysWarning::getBizKey, bizKey)
                    .eq(SysWarning::getReceiverId, receiverId));
            if (exists > 0) continue;
            SysWarning warning = new SysWarning();
            warning.setContractId(contractId);
            warning.setWarningType(type);
            warning.setWarningDate(warningDate);
            warning.setMessage(message);
            warning.setBizKey(bizKey);
            warning.setReceiverId(receiverId);
            warning.setReadStatus(0);
            warningMapper.insert(warning);
            created++;
        }
        return created;
    }

    public PageResult<WarningVO> page(WarningQuery query) {
        LambdaQueryWrapper<SysWarning> wrapper = new LambdaQueryWrapper<SysWarning>()
                .eq(SysWarning::getReceiverId, UserContext.userId())
                .eq(StringUtils.hasText(query.getWarningType()), SysWarning::getWarningType, query.getWarningType())
                .eq(query.getReadStatus() != null, SysWarning::getReadStatus, query.getReadStatus())
                .like(StringUtils.hasText(query.getKeyword()), SysWarning::getMessage, query.getKeyword())
                .orderByAsc(SysWarning::getReadStatus)
                .orderByDesc(SysWarning::getId);
        Page<SysWarning> page = warningMapper.selectPage(
                new Page<>(Math.max(1, query.getPageNum()), Math.min(Math.max(1, query.getPageSize()), 100)), wrapper);
        return PageResult.of(enrich(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    private List<WarningVO> enrich(List<SysWarning> warnings) {
        if (warnings.isEmpty()) return new ArrayList<>();
        Set<Long> contractIds = warnings.stream().map(SysWarning::getContractId).collect(Collectors.toSet());
        Map<Long, ContractInfo> contracts = contractMapper.selectBatchIds(contractIds).stream()
                .collect(Collectors.toMap(ContractInfo::getId, c -> c));
        return warnings.stream().map(w -> {
            WarningVO vo = new WarningVO();
            vo.setId(w.getId());
            vo.setContractId(w.getContractId());
            ContractInfo contract = contracts.get(w.getContractId());
            if (contract != null) {
                vo.setContractNo(contract.getContractNo());
                vo.setContractName(contract.getContractName());
            }
            vo.setWarningType(w.getWarningType());
            vo.setWarningDate(w.getWarningDate());
            vo.setMessage(w.getMessage());
            vo.setReadStatus(w.getReadStatus());
            vo.setReadTime(w.getReadTime());
            vo.setCreateTime(w.getCreateTime());
            return vo;
        }).toList();
    }

    public long unreadCount() {
        return warningMapper.selectCount(new LambdaQueryWrapper<SysWarning>()
                .eq(SysWarning::getReceiverId, UserContext.userId())
                .eq(SysWarning::getReadStatus, 0));
    }

    public List<WarningVO> recentUnread(int limit) {
        List<SysWarning> warnings = warningMapper.selectList(new LambdaQueryWrapper<SysWarning>()
                .eq(SysWarning::getReceiverId, UserContext.userId())
                .eq(SysWarning::getReadStatus, 0)
                .orderByDesc(SysWarning::getId)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 20)));
        return enrich(warnings);
    }

    public void markRead(Long id) {
        SysWarning warning = warningMapper.selectById(id);
        if (warning == null || !Objects.equals(warning.getReceiverId(), UserContext.userId())) {
            throw new BizException("预警消息不存在");
        }
        if (warning.getReadStatus() == 0) {
            warning.setReadStatus(1);
            warning.setReadTime(LocalDateTime.now());
            warningMapper.updateById(warning);
        }
    }

    public void markAllRead() {
        List<SysWarning> unread = warningMapper.selectList(new LambdaQueryWrapper<SysWarning>()
                .eq(SysWarning::getReceiverId, UserContext.userId())
                .eq(SysWarning::getReadStatus, 0));
        for (SysWarning warning : unread) {
            warning.setReadStatus(1);
            warning.setReadTime(LocalDateTime.now());
            warningMapper.updateById(warning);
        }
    }
}
