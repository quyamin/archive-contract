package com.enterprise.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.archive.common.BizConstants;
import com.enterprise.archive.common.UserContext;
import com.enterprise.archive.dto.ContractQuery;
import com.enterprise.archive.dto.StatsQuery;
import com.enterprise.archive.entity.BorrowApply;
import com.enterprise.archive.entity.ContractInfo;
import com.enterprise.archive.entity.SysDept;
import com.enterprise.archive.mapper.BorrowApplyMapper;
import com.enterprise.archive.mapper.ContractInfoMapper;
import com.enterprise.archive.mapper.SysDeptMapper;
import com.enterprise.archive.vo.DashboardVO;
import com.enterprise.archive.vo.StatItemVO;
import com.enterprise.archive.vo.StatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final ContractInfoMapper contractMapper;
    private final SysDeptMapper deptMapper;
    private final BorrowApplyMapper borrowMapper;
    private final DictService dictService;
    private final ConfigService configService;
    private final ContractService contractService;
    private final WarningService warningService;

    public StatsVO overview(StatsQuery query) {
        List<ContractInfo> list = filteredContracts(query);
        StatsVO vo = new StatsVO();
        vo.setTotalCount((long) list.size());
        vo.setTotalAmount(sumAmount(list));
        Map<String, String> typeLabels = dictService.labelMap("contract_type");
        Map<Long, String> deptNames = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));

        Map<String, StatAccumulator> typeMap = new LinkedHashMap<>();
        Map<String, StatAccumulator> deptMap = new LinkedHashMap<>();
        Map<String, StatAccumulator> yearMap = new TreeMap<>();
        Map<String, StatAccumulator> partnerMap = new LinkedHashMap<>();
        Map<String, StatAccumulator> amountMap = new LinkedHashMap<>();
        Map<String, StatAccumulator> monthMap = new TreeMap<>();
        String[] ranges = {"10万以下", "10万-50万", "50万-100万", "100万-500万", "500万以上"};
        for (String range : ranges) {
            amountMap.put(range, new StatAccumulator());
        }
        for (ContractInfo c : list) {
            String typeName = typeLabels.getOrDefault(c.getContractType(), c.getContractType());
            if (!StringUtils.hasText(typeName)) typeName = "未分类";
            typeMap.computeIfAbsent(typeName, k -> new StatAccumulator()).add(c.getAmount());
            String deptName = c.getDeptId() == null ? "未分配部门" : deptNames.getOrDefault(c.getDeptId(), "未知部门");
            deptMap.computeIfAbsent(deptName, k -> new StatAccumulator()).add(c.getAmount());
            if (c.getSignDate() != null) {
                yearMap.computeIfAbsent(String.valueOf(c.getSignDate().getYear()), k -> new StatAccumulator()).add(c.getAmount());
                monthMap.computeIfAbsent(c.getSignDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        k -> new StatAccumulator()).add(c.getAmount());
            }
            String partner = StringUtils.hasText(c.getPartyB()) ? c.getPartyB().trim() : "未填写";
            partnerMap.computeIfAbsent(partner, k -> new StatAccumulator()).add(c.getAmount());
            amountMap.computeIfAbsent(amountRange(c.getAmount()), k -> new StatAccumulator()).add(c.getAmount());
        }
        vo.setTypeStats(toItems(typeMap, Integer.MAX_VALUE));
        vo.setDeptStats(toItems(deptMap, Integer.MAX_VALUE));
        vo.setYearStats(toItems(yearMap, Integer.MAX_VALUE));
        vo.setAmountRangeStats(toItems(amountMap, Integer.MAX_VALUE));
        vo.setPartnerStats(toItems(partnerMap, 15));
        vo.setMonthStats(toItems(monthMap, Integer.MAX_VALUE));
        return vo;
    }

    private List<ContractInfo> filteredContracts(StatsQuery query) {
        LambdaQueryWrapper<ContractInfo> wrapper = new LambdaQueryWrapper<ContractInfo>()
                .ge(query.getStartDate() != null, ContractInfo::getSignDate, query.getStartDate())
                .le(query.getEndDate() != null, ContractInfo::getSignDate, query.getEndDate())
                .eq(query.getDeptId() != null, ContractInfo::getDeptId, query.getDeptId())
                .eq(StringUtils.hasText(query.getContractType()), ContractInfo::getContractType, query.getContractType())
                .orderByAsc(ContractInfo::getSignDate);
        return contractMapper.selectList(wrapper);
    }

    public DashboardVO dashboard() {
        DashboardVO vo = new DashboardVO();
        long total = contractMapper.selectCount(new LambdaQueryWrapper<>());
        long archived = contractMapper.selectCount(new LambdaQueryWrapper<ContractInfo>()
                .in(ContractInfo::getStatus, BizConstants.ContractStatus.ARCHIVED, BizConstants.ContractStatus.EXPIRED));
        long pending = contractMapper.selectCount(new LambdaQueryWrapper<ContractInfo>()
                .eq(ContractInfo::getStatus, BizConstants.ContractStatus.PENDING));
        int days = configService.warningAdvanceDays();
        long expiring = contractMapper.selectCount(new LambdaQueryWrapper<ContractInfo>()
                .in(ContractInfo::getStatus, BizConstants.ContractStatus.ARCHIVED, BizConstants.ContractStatus.EXPIRED)
                .between(ContractInfo::getExpireDate, LocalDate.now(), LocalDate.now().plusDays(days)));
        List<ContractInfo> all = contractMapper.selectList(new LambdaQueryWrapper<ContractInfo>()
                .select(ContractInfo::getAmount, ContractInfo::getContractType, ContractInfo::getDeptId,
                        ContractInfo::getSignDate));
        vo.setTotalContracts(total);
        vo.setArchivedCount(archived);
        vo.setPendingReviewCount(pending);
        vo.setExpiringCount(expiring);
        vo.setUnreadWarnings(warningService.unreadCount());
        vo.setTotalAmount(sumAmount(all));
        long activeBorrow = borrowMapper.selectCount(new LambdaQueryWrapper<BorrowApply>()
                .eq(BorrowApply::getApplicantId, UserContext.userId())
                .eq(BorrowApply::getStatus, BizConstants.BorrowStatus.APPROVED)
                .le(BorrowApply::getStartTime, LocalDateTime.now())
                .ge(BorrowApply::getEndTime, LocalDateTime.now()));
        vo.setActiveBorrowCount(activeBorrow);

        Map<Long, String> deptNames = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));
        Map<String, String> types = dictService.labelMap("contract_type");
        Map<String, StatAccumulator> typeMap = new LinkedHashMap<>();
        Map<String, StatAccumulator> deptMap = new LinkedHashMap<>();
        Map<String, StatAccumulator> monthMap = new TreeMap<>();
        YearMonth start = YearMonth.now().minusMonths(11);
        for (ContractInfo c : all) {
            typeMap.computeIfAbsent(types.getOrDefault(c.getContractType(), c.getContractType()),
                    k -> new StatAccumulator()).add(c.getAmount());
            deptMap.computeIfAbsent(c.getDeptId() == null ? "未分配部门"
                    : deptNames.getOrDefault(c.getDeptId(), "未知部门"), k -> new StatAccumulator()).add(c.getAmount());
            if (c.getSignDate() != null && !YearMonth.from(c.getSignDate()).isBefore(start)) {
                monthMap.computeIfAbsent(c.getSignDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        k -> new StatAccumulator()).add(c.getAmount());
            }
        }
        vo.setTypeStats(toItems(typeMap, Integer.MAX_VALUE));
        vo.setDeptStats(toItems(deptMap, Integer.MAX_VALUE));
        vo.setMonthStats(toItems(monthMap, Integer.MAX_VALUE));

        ContractQuery recentQuery = new ContractQuery();
        recentQuery.setPageNum(1);
        recentQuery.setPageSize(6);
        vo.setRecentContracts(contractService.page(recentQuery).getRecords());
        vo.setRecentWarnings(warningService.recentUnread(5));
        return vo;
    }

    private BigDecimal sumAmount(List<ContractInfo> list) {
        return list.stream().map(ContractInfo::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String amountRange(BigDecimal amount) {
        if (amount == null) return "10万以下";
        double value = amount.doubleValue();
        if (value < 100_000) return "10万以下";
        if (value < 500_000) return "10万-50万";
        if (value < 1_000_000) return "50万-100万";
        if (value < 5_000_000) return "100万-500万";
        return "500万以上";
    }

    private List<StatItemVO> toItems(Map<String, StatAccumulator> map, int limit) {
        return map.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().count, a.getValue().count))
                .limit(limit)
                .map(e -> new StatItemVO(e.getKey(), e.getValue().count, e.getValue().amount))
                .toList();
    }

    private static class StatAccumulator {
        private long count;
        private BigDecimal amount = BigDecimal.ZERO;

        void add(BigDecimal value) {
            count++;
            if (value != null) {
                amount = amount.add(value);
            }
        }
    }
}
