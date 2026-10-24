package com.enterprise.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.archive.common.BizConstants;
import com.enterprise.archive.common.BizException;
import com.enterprise.archive.common.PageResult;
import com.enterprise.archive.common.UserContext;
import com.enterprise.archive.dto.BorrowApplyRequest;
import com.enterprise.archive.dto.BorrowReviewRequest;
import com.enterprise.archive.entity.*;
import com.enterprise.archive.mapper.*;
import com.enterprise.archive.security.LoginUser;
import com.enterprise.archive.vo.BorrowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowApplyMapper borrowMapper;
    private final ContractInfoMapper contractMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    @Transactional
    public Long apply(BorrowApplyRequest request) {
        ContractInfo contract = contractMapper.selectById(request.getContractId());
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        if (!BizConstants.ContractStatus.ARCHIVED_STATUS.contains(contract.getStatus())) {
            throw new BizException("仅已归档合同可以申请借阅");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BizException("借阅结束时间必须晚于开始时间");
        }
        if (request.getEndTime().isBefore(now)) {
            throw new BizException("借阅结束时间不能早于当前时间");
        }
        if (request.getStartTime().isBefore(now.minusMinutes(10))) {
            throw new BizException("借阅开始时间不能早于当前时间");
        }
        if (Duration.between(request.getStartTime(), request.getEndTime()).toDays() > 90) {
            throw new BizException("单次借阅期限不能超过90天");
        }
        Long duplicate = borrowMapper.selectCount(new LambdaQueryWrapper<BorrowApply>()
                .eq(BorrowApply::getContractId, contract.getId())
                .eq(BorrowApply::getApplicantId, UserContext.userId())
                .in(BorrowApply::getStatus, BizConstants.BorrowStatus.PENDING, BizConstants.BorrowStatus.APPROVED)
                .gt(BorrowApply::getEndTime, now));
        if (duplicate > 0) {
            throw new BizException("该合同已有待审批或生效中的借阅申请");
        }
        BorrowApply borrow = new BorrowApply();
        borrow.setBorrowNo("JY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", new Random().nextInt(1000)));
        borrow.setContractId(contract.getId());
        borrow.setApplicantId(UserContext.userId());
        borrow.setPurpose(request.getPurpose());
        borrow.setStartTime(request.getStartTime());
        borrow.setEndTime(request.getEndTime());
        borrow.setStatus(BizConstants.BorrowStatus.PENDING);
        borrowMapper.insert(borrow);
        return borrow.getId();
    }

    @Transactional
    public void review(Long id, BorrowReviewRequest request) {
        BorrowApply borrow = borrowMapper.selectById(id);
        if (borrow == null) {
            throw new BizException("借阅申请不存在");
        }
        if (!BizConstants.BorrowStatus.PENDING.equals(borrow.getStatus())) {
            throw new BizException("该借阅申请已处理");
        }
        if (BizConstants.ReviewAction.APPROVE.equals(request.getAction())) {
            borrow.setStatus(BizConstants.BorrowStatus.APPROVED);
        } else if (BizConstants.ReviewAction.REJECT.equals(request.getAction())) {
            if (!StringUtils.hasText(request.getOpinion())) {
                throw new BizException("驳回时必须填写审批意见");
            }
            borrow.setStatus(BizConstants.BorrowStatus.REJECTED);
        } else {
            throw new BizException("审批动作不合法");
        }
        borrow.setApproverId(UserContext.userId());
        borrow.setApproveTime(LocalDateTime.now());
        borrow.setApproveOpinion(request.getOpinion());
        borrowMapper.updateById(borrow);
    }

    @Transactional
    public void returnBorrow(Long id) {
        BorrowApply borrow = borrowMapper.selectById(id);
        if (borrow == null) {
            throw new BizException("借阅申请不存在");
        }
        if (!BizConstants.BorrowStatus.APPROVED.equals(borrow.getStatus())) {
            throw new BizException("当前借阅状态不允许归还");
        }
        LoginUser user = UserContext.get();
        if (!Objects.equals(borrow.getApplicantId(), user.getId()) && !user.has("borrow:approve")) {
            throw new BizException(403, "只能归还本人的借阅申请");
        }
        borrow.setStatus(BizConstants.BorrowStatus.RETURNED);
        borrow.setReturnTime(LocalDateTime.now());
        borrowMapper.updateById(borrow);
    }

    public PageResult<BorrowVO> page(long pageNum, long pageSize, String status, boolean mine, String keyword) {
        LambdaQueryWrapper<BorrowApply> wrapper = new LambdaQueryWrapper<>();
        LoginUser user = UserContext.get();
        if (mine && !user.has("borrow:approve")) {
            wrapper.eq(BorrowApply::getApplicantId, user.getId());
        }
        wrapper.eq(StringUtils.hasText(status), BorrowApply::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w.like(BorrowApply::getBorrowNo, keyword)
                        .or().like(BorrowApply::getPurpose, keyword))
                .orderByDesc(BorrowApply::getId);
        Page<BorrowApply> page = borrowMapper.selectPage(
                new Page<>(Math.max(1, pageNum), Math.min(Math.max(1, pageSize), 100)), wrapper);
        return PageResult.of(enrich(page.getRecords()), page.getTotal(), page.getCurrent(), page.getSize());
    }

    private List<BorrowVO> enrich(List<BorrowApply> records) {
        if (records.isEmpty()) return new ArrayList<>();
        Set<Long> userIds = new HashSet<>();
        Set<Long> contractIds = new HashSet<>();
        records.forEach(b -> {
            if (b.getApplicantId() != null) userIds.add(b.getApplicantId());
            if (b.getApproverId() != null) userIds.add(b.getApproverId());
            contractIds.add(b.getContractId());
        });
        Map<Long, SysUser> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, SysDept> depts = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, Function.identity()));
        Map<Long, ContractInfo> contracts = contractMapper.selectBatchIds(contractIds).stream()
                .collect(Collectors.toMap(ContractInfo::getId, Function.identity()));
        Long currentUserId = UserContext.userId();
        boolean canReview = UserContext.hasPerm("borrow:approve");
        return records.stream().map(b -> {
            BorrowVO vo = new BorrowVO();
            vo.setId(b.getId());
            vo.setBorrowNo(b.getBorrowNo());
            vo.setContractId(b.getContractId());
            ContractInfo contract = contracts.get(b.getContractId());
            if (contract != null) {
                vo.setContractNo(contract.getContractNo());
                vo.setContractName(contract.getContractName());
            }
            vo.setApplicantId(b.getApplicantId());
            SysUser applicant = users.get(b.getApplicantId());
            vo.setApplicantName(applicant == null ? "" : applicant.getRealName());
            if (applicant != null && depts.containsKey(applicant.getDeptId())) {
                vo.setApplicantDept(depts.get(applicant.getDeptId()).getDeptName());
            }
            vo.setPurpose(b.getPurpose());
            vo.setStartTime(b.getStartTime());
            vo.setEndTime(b.getEndTime());
            vo.setStatus(b.getStatus());
            vo.setApproverId(b.getApproverId());
            SysUser approver = users.get(b.getApproverId());
            vo.setApproverName(approver == null ? "" : approver.getRealName());
            vo.setApproveOpinion(b.getApproveOpinion());
            vo.setApproveTime(b.getApproveTime());
            vo.setReturnTime(b.getReturnTime());
            vo.setCreateTime(b.getCreateTime());
            vo.setCanApprove(canReview && BizConstants.BorrowStatus.PENDING.equals(b.getStatus()));
            vo.setCanReturn(Objects.equals(b.getApplicantId(), currentUserId)
                    && BizConstants.BorrowStatus.APPROVED.equals(b.getStatus()));
            return vo;
        }).toList();
    }

    public int expireBorrows() {
        List<BorrowApply> expired = borrowMapper.selectList(new LambdaQueryWrapper<BorrowApply>()
                .eq(BorrowApply::getStatus, BizConstants.BorrowStatus.APPROVED)
                .lt(BorrowApply::getEndTime, LocalDateTime.now()));
        for (BorrowApply borrow : expired) {
            borrow.setStatus(BizConstants.BorrowStatus.EXPIRED);
            borrowMapper.updateById(borrow);
        }
        return expired.size();
    }
}
