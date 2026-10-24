package com.enterprise.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.archive.common.BizConstants;
import com.enterprise.archive.common.BizException;
import com.enterprise.archive.common.PageResult;
import com.enterprise.archive.common.UserContext;
import com.enterprise.archive.dto.*;
import com.enterprise.archive.entity.*;
import com.enterprise.archive.mapper.*;
import com.enterprise.archive.security.LoginUser;
import com.enterprise.archive.vo.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.enterprise.archive.common.BizConstants.ChangeStatus;
import static com.enterprise.archive.common.BizConstants.ContractStatus;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractInfoMapper contractMapper;
    private final ContractPaymentNodeMapper paymentNodeMapper;
    private final ContractAttachmentMapper attachmentMapper;
    private final ArchiveReviewRecordMapper reviewMapper;
    private final ArchiveChangeRecordMapper changeMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final BorrowApplyMapper borrowMapper;
    private final DictService dictService;
    private final ConfigService configService;
    private final AttachmentService attachmentService;
    private final ObjectMapper objectMapper;

    public PageResult<ContractVO> page(ContractQuery query) {
        LambdaQueryWrapper<ContractInfo> wrapper = buildWrapper(query);
        long pageNum = Math.max(1, query.getPageNum());
        long pageSize = Math.min(Math.max(1, query.getPageSize()), 100);
        Page<ContractInfo> page = contractMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<ContractVO> records = enrich(page.getRecords(), false);
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<ContractVO> reviewPage(ContractQuery query) {
        query.setStatus(ContractStatus.PENDING);
        return page(query);
    }

    private LambdaQueryWrapper<ContractInfo> buildWrapper(ContractQuery query) {
        LambdaQueryWrapper<ContractInfo> wrapper = new LambdaQueryWrapper<>();
        applyDataScope(wrapper);
        wrapper.like(StringUtils.hasText(query.getContractNo()), ContractInfo::getContractNo, query.getContractNo())
                .like(StringUtils.hasText(query.getContractName()), ContractInfo::getContractName, query.getContractName())
                .like(StringUtils.hasText(query.getPartyA()), ContractInfo::getPartyA, query.getPartyA())
                .like(StringUtils.hasText(query.getPartyB()), ContractInfo::getPartyB, query.getPartyB())
                .like(StringUtils.hasText(query.getArchiveNo()), ContractInfo::getArchiveNo, query.getArchiveNo())
                .ge(query.getSignDateStart() != null, ContractInfo::getSignDate, query.getSignDateStart())
                .le(query.getSignDateEnd() != null, ContractInfo::getSignDate, query.getSignDateEnd())
                .ge(query.getAmountMin() != null, ContractInfo::getAmount, query.getAmountMin())
                .le(query.getAmountMax() != null, ContractInfo::getAmount, query.getAmountMax())
                .eq(query.getDeptId() != null, ContractInfo::getDeptId, query.getDeptId())
                .eq(query.getOwnerId() != null, ContractInfo::getOwnerId, query.getOwnerId())
                .eq(StringUtils.hasText(query.getContractType()), ContractInfo::getContractType, query.getContractType())
                .orderByDesc(ContractInfo::getId);

        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(ContractInfo::getStatus, query.getStatus());
        } else if (StringUtils.hasText(query.getCategory())) {
            switch (query.getCategory()) {
                case "draft" -> wrapper.eq(ContractInfo::getStatus, ContractStatus.DRAFT);
                case "reviewing" -> wrapper.eq(ContractInfo::getStatus, ContractStatus.PENDING);
                case "rejected" -> wrapper.eq(ContractInfo::getStatus, ContractStatus.REJECTED);
                case "archived" -> wrapper.eq(ContractInfo::getStatus, ContractStatus.ARCHIVED);
                case "expired" -> wrapper.eq(ContractInfo::getStatus, ContractStatus.EXPIRED);
                default -> {
                }
            }
        }
        if (Boolean.TRUE.equals(query.getExpiringSoon())) {
            int days = configService.warningAdvanceDays();
            wrapper.in(ContractInfo::getStatus, ContractStatus.ARCHIVED, ContractStatus.EXPIRED)
                    .between(ContractInfo::getExpireDate, LocalDate.now(), LocalDate.now().plusDays(days));
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(ContractInfo::getContractNo, keyword)
                    .or().like(ContractInfo::getContractName, keyword)
                    .or().like(ContractInfo::getPartyA, keyword)
                    .or().like(ContractInfo::getPartyB, keyword)
                    .or().like(ContractInfo::getArchiveNo, keyword));
        }
        if (StringUtils.hasText(query.getAttachmentKeyword())) {
            String keyword = "%" + query.getAttachmentKeyword().trim() + "%";
            wrapper.apply("EXISTS (SELECT 1 FROM contract_attachment ca WHERE ca.contract_id = contract_info.id "
                    + "AND ca.deleted = 0 AND (ca.original_name LIKE {0} OR ca.content_text LIKE {0}))", keyword);
        }
        return wrapper;
    }

    private void applyDataScope(LambdaQueryWrapper<ContractInfo> wrapper) {
        LoginUser user = UserContext.get();
        if (user.has("contract:view:all")) {
            return;
        }
        if (user.has("contract:add")) {
            Long deptId = user.getDeptId();
            wrapper.and(w -> {
                w.eq(ContractInfo::getCreateBy, user.getId())
                        .or().eq(ContractInfo::getOwnerId, user.getId());
                if (deptId != null) {
                    w.or().eq(ContractInfo::getDeptId, deptId);
                }
            });
            return;
        }
        wrapper.in(ContractInfo::getStatus, ContractStatus.ARCHIVED, ContractStatus.EXPIRED);
    }

    public ContractVO detail(Long id) {
        ContractInfo contract = requireContract(id);
        assertVisible(contract);
        return enrich(List.of(contract), true).get(0);
    }

    public List<ContractVO> exportList(ContractQuery query) {
        query.setPageNum(1);
        query.setPageSize(100000);
        LambdaQueryWrapper<ContractInfo> wrapper = buildWrapper(query);
        wrapper.last("LIMIT 10000");
        return enrich(contractMapper.selectList(wrapper), false);
    }

    private List<ContractVO> enrich(List<ContractInfo> contracts, boolean withDetail) {
        if (contracts == null || contracts.isEmpty()) {
            return new ArrayList<>();
        }
        LoginUser user = UserContext.get();
        Map<String, String> typeMap = dictService.labelMap("contract_type");
        Map<String, String> statusMap = dictService.labelMap("contract_status");
        Map<String, String> securityMap = dictService.labelMap("security_level");
        Map<Long, String> deptNames = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));
        Set<Long> userIds = new HashSet<>();
        contracts.forEach(c -> {
            if (c.getOwnerId() != null) userIds.add(c.getOwnerId());
            if (c.getCreateBy() != null) userIds.add(c.getCreateBy());
            if (c.getArchiveBy() != null) userIds.add(c.getArchiveBy());
        });
        Map<Long, String> userNames = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        List<Long> contractIds = contracts.stream().map(ContractInfo::getId).toList();
        Map<Long, Integer> attachmentCounts = new HashMap<>();
        attachmentMapper.selectList(new LambdaQueryWrapper<ContractAttachment>()
                        .in(ContractAttachment::getContractId, contractIds))
                .forEach(a -> attachmentCounts.merge(a.getContractId(), 1, Integer::sum));
        Set<Long> borrowedContractIds = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();
        borrowMapper.selectList(new LambdaQueryWrapper<BorrowApply>()
                        .in(BorrowApply::getContractId, contractIds)
                        .eq(BorrowApply::getApplicantId, user.getId())
                        .eq(BorrowApply::getStatus, BizConstants.BorrowStatus.APPROVED)
                        .le(BorrowApply::getStartTime, now)
                        .ge(BorrowApply::getEndTime, now))
                .forEach(b -> borrowedContractIds.add(b.getContractId()));

        List<ContractVO> result = new ArrayList<>();
        int advanceDays = configService.warningAdvanceDays();
        for (ContractInfo contract : contracts) {
            ContractVO vo = new ContractVO();
            copyBase(contract, vo);
            vo.setDeptName(deptNames.getOrDefault(contract.getDeptId(), ""));
            vo.setOwnerName(userNames.getOrDefault(contract.getOwnerId(), ""));
            vo.setCreateByName(userNames.getOrDefault(contract.getCreateBy(), ""));
            vo.setArchiveByName(userNames.getOrDefault(contract.getArchiveBy(), ""));
            vo.setContractTypeName(typeMap.getOrDefault(contract.getContractType(), contract.getContractType()));
            vo.setStatusName(statusMap.getOrDefault(contract.getStatus(), contract.getStatus()));
            vo.setSecurityLevelName(securityMap.getOrDefault(contract.getSecurityLevel(), contract.getSecurityLevel()));
            vo.setAttachmentCount(attachmentCounts.getOrDefault(contract.getId(), 0));
            if (contract.getExpireDate() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), contract.getExpireDate());
                vo.setExpireDays(days);
                vo.setExpiringSoon(days >= 0 && days <= advanceDays
                        && ContractStatus.ARCHIVED_STATUS.contains(contract.getStatus()));
            } else {
                vo.setExpiringSoon(false);
            }
            boolean archived = ContractStatus.ARCHIVED_STATUS.contains(contract.getStatus());
            boolean downloadable = UserContext.hasPerm("contract:download")
                    && (user.has("archive:download:any")
                    || (!archived && (Objects.equals(contract.getCreateBy(), user.getId())
                    || Objects.equals(contract.getOwnerId(), user.getId()) || user.has("contract:view:all")))
                    || (archived && borrowedContractIds.contains(contract.getId())));
            vo.setCanDownload(downloadable);
            boolean owner = Objects.equals(contract.getCreateBy(), user.getId())
                    || Objects.equals(contract.getOwnerId(), user.getId());
            boolean editableStatus = ContractStatus.DRAFT.equals(contract.getStatus())
                    || ContractStatus.REJECTED.equals(contract.getStatus());
            vo.setCanEdit(editableStatus && (owner || user.isAdmin()));
            result.add(vo);

            if (withDetail) {
                vo.setAttachments(attachmentService.listByContract(contract.getId()));
                vo.setPaymentNodes(paymentNodeMapper.selectList(new LambdaQueryWrapper<ContractPaymentNode>()
                                .eq(ContractPaymentNode::getContractId, contract.getId())
                                .orderByAsc(ContractPaymentNode::getPlanPayDate, ContractPaymentNode::getId))
                        .stream().map(n -> toNodeDTO(n, advanceDays)).toList());
                List<ArchiveReviewRecord> reviews = reviewMapper.selectList(new LambdaQueryWrapper<ArchiveReviewRecord>()
                        .eq(ArchiveReviewRecord::getContractId, contract.getId())
                        .orderByDesc(ArchiveReviewRecord::getReviewTime));
                vo.setReviews(reviews.stream().map(r -> toReviewVO(r, userNames)).toList());
                List<ArchiveChangeRecord> changes = changeMapper.selectList(new LambdaQueryWrapper<ArchiveChangeRecord>()
                        .eq(ArchiveChangeRecord::getContractId, contract.getId())
                        .orderByDesc(ArchiveChangeRecord::getApplyTime));
                vo.setChanges(changes.stream().map(c -> toChangeVO(c, userNames, null, null)).toList());
            }
        }
        return result;
    }

    private ContractVO.PaymentNodeDTO toNodeDTO(ContractPaymentNode node, int advanceDays) {
        ContractVO.PaymentNodeDTO dto = new ContractVO.PaymentNodeDTO();
        dto.setId(node.getId());
        dto.setNodeName(node.getNodeName());
        dto.setPlanPayDate(node.getPlanPayDate());
        dto.setAmount(node.getAmount());
        dto.setPaidStatus(node.getPaidStatus());
        dto.setRemark(node.getRemark());
        if (node.getPaidStatus() == null || node.getPaidStatus() == 0) {
            long days = node.getPlanPayDate() == null ? -1 : ChronoUnit.DAYS.between(LocalDate.now(), node.getPlanPayDate());
            dto.setExpiringSoon(days >= 0 && days <= advanceDays);
        }
        return dto;
    }

    private ReviewVO toReviewVO(ArchiveReviewRecord record, Map<Long, String> userNames) {
        ReviewVO vo = new ReviewVO();
        vo.setId(record.getId());
        vo.setContractId(record.getContractId());
        vo.setAction(record.getAction());
        vo.setOpinion(record.getOpinion());
        vo.setReviewerId(record.getReviewerId());
        vo.setReviewerName(userNames.getOrDefault(record.getReviewerId(), ""));
        vo.setReviewTime(record.getReviewTime());
        vo.setArchiveNo(record.getArchiveNo());
        return vo;
    }

    private ChangeVO toChangeVO(ArchiveChangeRecord record, Map<Long, String> userNames,
                                Map<Long, String> contractNos, Map<Long, String> contractNames) {
        ChangeVO vo = new ChangeVO();
        vo.setId(record.getId());
        vo.setContractId(record.getContractId());
        if (contractNos != null) {
            vo.setContractNo(contractNos.get(record.getContractId()));
            vo.setContractName(contractNames.get(record.getContractId()));
        }
        vo.setChangeType(record.getChangeType());
        vo.setChangeReason(record.getChangeReason());
        vo.setBeforeContent(record.getBeforeContent());
        vo.setAfterContent(record.getAfterContent());
        vo.setStatus(record.getStatus());
        vo.setApplicantId(record.getApplicantId());
        vo.setApplicantName(userNames.getOrDefault(record.getApplicantId(), ""));
        vo.setApplyTime(record.getApplyTime());
        vo.setApproverId(record.getApproverId());
        vo.setApproverName(userNames.getOrDefault(record.getApproverId(), ""));
        vo.setApproveTime(record.getApproveTime());
        vo.setApproveOpinion(record.getApproveOpinion());
        return vo;
    }

    private void copyBase(ContractInfo contract, ContractVO vo) {
        vo.setId(contract.getId());
        vo.setContractNo(contract.getContractNo());
        vo.setContractName(contract.getContractName());
        vo.setPartyA(contract.getPartyA());
        vo.setPartyB(contract.getPartyB());
        vo.setSignDate(contract.getSignDate());
        vo.setEffectiveDate(contract.getEffectiveDate());
        vo.setExpireDate(contract.getExpireDate());
        vo.setAmount(contract.getAmount());
        vo.setContractType(contract.getContractType());
        vo.setDeptId(contract.getDeptId());
        vo.setOwnerId(contract.getOwnerId());
        vo.setSecurityLevel(contract.getSecurityLevel());
        vo.setStatus(contract.getStatus());
        vo.setArchiveNo(contract.getArchiveNo());
        vo.setArchiveTime(contract.getArchiveTime());
        vo.setRemark(contract.getRemark());
        vo.setVersion(contract.getVersion());
        vo.setCreateBy(contract.getCreateBy());
        vo.setCreateTime(contract.getCreateTime());
    }

    public Long save(ContractSaveRequest request) {
        validateDates(request.getEffectiveDate(), request.getExpireDate());
        ContractInfo duplicate = contractMapper.selectOne(new LambdaQueryWrapper<ContractInfo>()
                .eq(ContractInfo::getContractNo, request.getContractNo()).last("LIMIT 1"));
        if (duplicate != null && !duplicate.getId().equals(request.getId())) {
            throw new BizException("合同编号已存在");
        }
        requireDept(request.getDeptId());
        requireUser(request.getOwnerId());
        ContractInfo contract;
        if (request.getId() == null) {
            contract = new ContractInfo();
            contract.setStatus(ContractStatus.DRAFT);
            contract.setVersion(0);
        } else {
            contract = requireContract(request.getId());
            assertEditable(contract);
            if (!Set.of(ContractStatus.DRAFT, ContractStatus.REJECTED).contains(contract.getStatus())) {
                throw new BizException("仅草稿或已驳回合同允许编辑，归档合同请走变更审批流程");
            }
        }
        contract.setContractNo(request.getContractNo().trim());
        contract.setContractName(request.getContractName());
        contract.setPartyA(request.getPartyA());
        contract.setPartyB(request.getPartyB());
        contract.setSignDate(request.getSignDate());
        contract.setEffectiveDate(request.getEffectiveDate());
        contract.setExpireDate(request.getExpireDate());
        contract.setAmount(request.getAmount());
        contract.setContractType(request.getContractType());
        contract.setDeptId(request.getDeptId());
        contract.setOwnerId(request.getOwnerId());
        contract.setSecurityLevel(request.getSecurityLevel());
        contract.setRemark(request.getRemark());
        if (request.getId() == null) {
            contractMapper.insert(contract);
            if (contract.getCreateBy() == null) {
                // 自动填充器在未经过拦截器时兜底
                contract.setCreateBy(UserContext.userId());
                contractMapper.updateById(contract);
            }
        } else {
            contractMapper.updateById(contract);
        }
        savePaymentNodes(contract.getId(), request.getPaymentNodes());
        return contract.getId();
    }

    private void savePaymentNodes(Long contractId, List<PaymentNodeDTO> nodes) {
        paymentNodeMapper.delete(new LambdaQueryWrapper<ContractPaymentNode>()
                .eq(ContractPaymentNode::getContractId, contractId));
        if (nodes == null) {
            return;
        }
        for (PaymentNodeDTO dto : nodes) {
            if (!StringUtils.hasText(dto.getNodeName()) || dto.getPlanPayDate() == null) {
                continue;
            }
            ContractPaymentNode node = new ContractPaymentNode();
            node.setContractId(contractId);
            node.setNodeName(dto.getNodeName());
            node.setPlanPayDate(dto.getPlanPayDate());
            node.setAmount(dto.getAmount() == null ? BigDecimal.ZERO : dto.getAmount());
            node.setPaidStatus(dto.getPaidStatus() == null ? 0 : dto.getPaidStatus());
            node.setRemark(dto.getRemark());
            paymentNodeMapper.insert(node);
        }
    }

    @Transactional
    public void submit(Long id) {
        ContractInfo contract = requireContract(id);
        assertEditable(contract);
        if (!ContractStatus.DRAFT.equals(contract.getStatus()) && !ContractStatus.REJECTED.equals(contract.getStatus())) {
            throw new BizException("当前合同状态不允许提交归档");
        }
        validateForArchive(contract);
        contract.setStatus(ContractStatus.PENDING);
        contractMapper.updateById(contract);

        ArchiveReviewRecord review = new ArchiveReviewRecord();
        review.setContractId(id);
        review.setAction(BizConstants.ReviewAction.SUBMIT);
        review.setOpinion("提交归档审核");
        review.setReviewerId(UserContext.userId());
        review.setReviewTime(LocalDateTime.now());
        reviewMapper.insert(review);
    }

    @Transactional
    public void review(Long id, ReviewRequest request) {
        ContractInfo contract = requireContract(id);
        if (!ContractStatus.PENDING.equals(contract.getStatus())) {
            throw new BizException("仅审核中的合同可以执行审核");
        }
        String action = request.getAction();
        if (!BizConstants.ReviewAction.APPROVE.equals(action) && !BizConstants.ReviewAction.REJECT.equals(action)) {
            throw new BizException("审核动作不合法");
        }
        if (BizConstants.ReviewAction.REJECT.equals(action) && !StringUtils.hasText(request.getOpinion())) {
            throw new BizException("驳回时必须填写审核意见");
        }
        ArchiveReviewRecord review = new ArchiveReviewRecord();
        review.setContractId(id);
        review.setAction(action);
        review.setOpinion(request.getOpinion());
        review.setReviewerId(UserContext.userId());
        review.setReviewTime(LocalDateTime.now());
        if (BizConstants.ReviewAction.APPROVE.equals(action)) {
            validateForArchive(contract);
            String archiveNo = generateArchiveNo();
            contract.setStatus(ContractStatus.ARCHIVED);
            contract.setArchiveNo(archiveNo);
            contract.setArchiveTime(LocalDateTime.now());
            contract.setArchiveBy(UserContext.userId());
            review.setArchiveNo(archiveNo);
            contractMapper.updateById(contract);
        } else {
            contract.setStatus(ContractStatus.REJECTED);
            contractMapper.updateById(contract);
        }
        reviewMapper.insert(review);
    }

    private synchronized String generateArchiveNo() {
        String prefix = "DA" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        ContractInfo last = contractMapper.selectOne(new LambdaQueryWrapper<ContractInfo>()
                .likeRight(ContractInfo::getArchiveNo, prefix)
                .orderByDesc(ContractInfo::getArchiveNo)
                .last("LIMIT 1"));
        int seq = 1;
        if (last != null && last.getArchiveNo() != null && last.getArchiveNo().length() > prefix.length()) {
            try {
                seq = Integer.parseInt(last.getArchiveNo().substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return prefix + String.format("%04d", seq);
    }

    private void validateForArchive(ContractInfo contract) {
        if (!StringUtils.hasText(contract.getContractName()) || !StringUtils.hasText(contract.getPartyA())
                || !StringUtils.hasText(contract.getPartyB()) || contract.getSignDate() == null
                || contract.getAmount() == null || !StringUtils.hasText(contract.getContractType())
                || contract.getDeptId() == null || contract.getOwnerId() == null
                || !StringUtils.hasText(contract.getSecurityLevel())) {
            throw new BizException("合同基础信息不完整，请补充后再提交归档");
        }
        Long count = attachmentMapper.selectCount(new LambdaQueryWrapper<ContractAttachment>()
                .eq(ContractAttachment::getContractId, contract.getId()));
        if (count == null || count == 0) {
            throw new BizException("请至少上传一份合同附件后再提交归档");
        }
    }

    @Transactional
    public void deleteDraft(Long id) {
        ContractInfo contract = requireContract(id);
        assertEditable(contract);
        if (!ContractStatus.DRAFT.equals(contract.getStatus())
                && !ContractStatus.REJECTED.equals(contract.getStatus())) {
            throw new BizException("仅草稿或已驳回合同可以删除，已归档档案禁止删除");
        }
        paymentNodeMapper.delete(new LambdaQueryWrapper<ContractPaymentNode>()
                .eq(ContractPaymentNode::getContractId, id));
        contractMapper.deleteById(id);
    }

    @Transactional
    public void applyChange(ChangeApplyRequest request) {
        ContractInfo contract = requireContract(request.getContractId());
        assertVisible(contract);
        if (!ContractStatus.ARCHIVED_STATUS.contains(contract.getStatus())) {
            throw new BizException("仅已归档合同可以申请变更");
        }
        LoginUser user = UserContext.get();
        boolean related = Objects.equals(contract.getCreateBy(), user.getId())
                || Objects.equals(contract.getOwnerId(), user.getId());
        if (!related && !user.has("contract:view:all")) {
            throw new BizException(403, "您无权对该档案发起变更");
        }
        Long pending = changeMapper.selectCount(new LambdaQueryWrapper<ArchiveChangeRecord>()
                .eq(ArchiveChangeRecord::getContractId, contract.getId())
                .eq(ArchiveChangeRecord::getStatus, ChangeStatus.PENDING));
        if (pending > 0) {
            throw new BizException("该合同已有待审批的变更申请");
        }
        if (request.getChangeFields() == null || request.getChangeFields().isEmpty()) {
            throw new BizException("请填写需要变更的内容");
        }
        Map<String, Object> beforeMap = contractSnapshot(contract);
        ContractInfo target = cloneContract(contract);
        applyChangeFields(target, request.getChangeFields());
        validateDates(target.getEffectiveDate(), target.getExpireDate());
        if (target.getAmount() != null && target.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("合同金额不能小于0");
        }
        Map<String, Object> afterMap = contractSnapshot(target);

        ArchiveChangeRecord record = new ArchiveChangeRecord();
        record.setContractId(contract.getId());
        record.setChangeType(request.getChangeType());
        record.setChangeReason(request.getChangeReason());
        try {
            record.setBeforeContent(objectMapper.writeValueAsString(beforeMap));
            record.setAfterContent(objectMapper.writeValueAsString(afterMap));
        } catch (Exception e) {
            throw new BizException("变更内容序列化失败");
        }
        record.setStatus(ChangeStatus.PENDING);
        record.setApplicantId(UserContext.userId());
        record.setApplyTime(LocalDateTime.now());
        changeMapper.insert(record);
    }

    private ContractInfo cloneContract(ContractInfo source) {
        ContractInfo target = new ContractInfo();
        org.springframework.beans.BeanUtils.copyProperties(source, target);
        return target;
    }

    private void applyChangeFields(ContractInfo target, Map<String, Object> fields) {
        Set<String> allowed = Set.of("contractName", "partyA", "partyB", "signDate", "effectiveDate",
                "expireDate", "amount", "contractType", "deptId", "ownerId", "securityLevel", "remark");
        for (String key : fields.keySet()) {
            if (!allowed.contains(key)) {
                throw new BizException("不支持变更字段：" + key);
            }
        }
        try {
            if (fields.containsKey("contractName")) target.setContractName(String.valueOf(fields.get("contractName")));
            if (fields.containsKey("partyA")) target.setPartyA(String.valueOf(fields.get("partyA")));
            if (fields.containsKey("partyB")) target.setPartyB(String.valueOf(fields.get("partyB")));
            if (fields.containsKey("contractType")) target.setContractType(String.valueOf(fields.get("contractType")));
            if (fields.containsKey("securityLevel")) target.setSecurityLevel(String.valueOf(fields.get("securityLevel")));
            if (fields.containsKey("remark")) target.setRemark(fields.get("remark") == null ? null : String.valueOf(fields.get("remark")));
            if (fields.containsKey("signDate")) target.setSignDate(parseDate(fields.get("signDate")));
            if (fields.containsKey("effectiveDate")) target.setEffectiveDate(parseDate(fields.get("effectiveDate")));
            if (fields.containsKey("expireDate")) target.setExpireDate(parseDate(fields.get("expireDate")));
            if (fields.containsKey("amount")) target.setAmount(new BigDecimal(String.valueOf(fields.get("amount"))));
            if (fields.containsKey("deptId")) {
                Long deptId = Long.valueOf(String.valueOf(fields.get("deptId")));
                requireDept(deptId);
                target.setDeptId(deptId);
            }
            if (fields.containsKey("ownerId")) {
                Long ownerId = Long.valueOf(String.valueOf(fields.get("ownerId")));
                requireUser(ownerId);
                target.setOwnerId(ownerId);
            }
            if (!StringUtils.hasText(target.getContractName()) || !StringUtils.hasText(target.getPartyA())
                    || !StringUtils.hasText(target.getPartyB())) {
                throw new BizException("合同名称、甲乙双方不能为空");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("变更内容格式不正确");
        }
    }

    private LocalDate parseDate(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return LocalDate.parse(String.valueOf(value).substring(0, 10));
    }

    private Map<String, Object> contractSnapshot(ContractInfo contract) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contractName", contract.getContractName());
        map.put("partyA", contract.getPartyA());
        map.put("partyB", contract.getPartyB());
        map.put("signDate", contract.getSignDate() == null ? null : contract.getSignDate().toString());
        map.put("effectiveDate", contract.getEffectiveDate() == null ? null : contract.getEffectiveDate().toString());
        map.put("expireDate", contract.getExpireDate() == null ? null : contract.getExpireDate().toString());
        map.put("amount", contract.getAmount());
        map.put("contractType", contract.getContractType());
        map.put("deptId", contract.getDeptId());
        map.put("ownerId", contract.getOwnerId());
        map.put("securityLevel", contract.getSecurityLevel());
        map.put("remark", contract.getRemark());
        return map;
    }

    public PageResult<ChangeVO> changePage(long pageNum, long pageSize, String status, boolean onlyMine) {
        LambdaQueryWrapper<ArchiveChangeRecord> wrapper = new LambdaQueryWrapper<>();
        LoginUser user = UserContext.get();
        // 无变更审批权限的用户只能看到本人发起的变更申请，避免越权查看档案变更内容。
        if (!user.has("change:review")) {
            wrapper.eq(ArchiveChangeRecord::getApplicantId, user.getId());
        }
        wrapper.eq(StringUtils.hasText(status), ArchiveChangeRecord::getStatus, status)
                .orderByDesc(ArchiveChangeRecord::getId);
        Page<ArchiveChangeRecord> page = changeMapper.selectPage(
                new Page<>(Math.max(1, pageNum), Math.min(Math.max(1, pageSize), 100)), wrapper);
        List<ChangeVO> records = enrichChanges(page.getRecords());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private List<ChangeVO> enrichChanges(List<ArchiveChangeRecord> records) {
        if (records.isEmpty()) return new ArrayList<>();
        Set<Long> userIds = new HashSet<>();
        Set<Long> contractIds = new HashSet<>();
        records.forEach(r -> {
            if (r.getApplicantId() != null) userIds.add(r.getApplicantId());
            if (r.getApproverId() != null) userIds.add(r.getApproverId());
            contractIds.add(r.getContractId());
        });
        Map<Long, String> userNames = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        Map<Long, ContractInfo> contracts = contractMapper.selectBatchIds(contractIds).stream()
                .collect(Collectors.toMap(ContractInfo::getId, Function.identity()));
        Map<Long, String> contractNos = new HashMap<>();
        Map<Long, String> contractNames = new HashMap<>();
        contracts.forEach((id, c) -> {
            contractNos.put(id, c.getContractNo());
            contractNames.put(id, c.getContractName());
        });
        return records.stream().map(r -> toChangeVO(r, userNames, contractNos, contractNames)).toList();
    }

    @Transactional
    public void reviewChange(Long id, ChangeReviewRequest request) {
        ArchiveChangeRecord record = changeMapper.selectById(id);
        if (record == null) {
            throw new BizException("变更申请不存在");
        }
        if (!ChangeStatus.PENDING.equals(record.getStatus())) {
            throw new BizException("该变更申请已处理");
        }
        if (BizConstants.ReviewAction.APPROVE.equals(request.getAction())) {
            ContractInfo contract = requireContract(record.getContractId());
            if (!ContractStatus.ARCHIVED_STATUS.contains(contract.getStatus())) {
                throw new BizException("合同当前状态不允许变更");
            }
            try {
                Map<String, Object> after = objectMapper.readValue(record.getAfterContent(),
                        new TypeReference<Map<String, Object>>() {
                        });
                applyChangeFields(contract, after);
                validateDates(contract.getEffectiveDate(), contract.getExpireDate());
                contractMapper.updateById(contract);
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                throw new BizException("变更内容应用失败");
            }
            record.setStatus(ChangeStatus.APPROVED);
        } else if (BizConstants.ReviewAction.REJECT.equals(request.getAction())) {
            record.setStatus(ChangeStatus.REJECTED);
            if (!StringUtils.hasText(request.getOpinion())) {
                throw new BizException("驳回时必须填写审批意见");
            }
        } else {
            throw new BizException("审批动作不合法");
        }
        record.setApproverId(UserContext.userId());
        record.setApproveTime(LocalDateTime.now());
        record.setApproveOpinion(request.getOpinion());
        changeMapper.updateById(record);
    }

    private void assertVisible(ContractInfo contract) {
        LoginUser user = UserContext.get();
        if (user.has("contract:view:all")) {
            return;
        }
        if (Objects.equals(contract.getCreateBy(), user.getId())
                || Objects.equals(contract.getOwnerId(), user.getId())) {
            return;
        }
        if (ContractStatus.ARCHIVED_STATUS.contains(contract.getStatus()) && user.has("contract:view")) {
            return;
        }
        if (user.has("contract:add") && user.getDeptId() != null
                && Objects.equals(contract.getDeptId(), user.getDeptId())) {
            return;
        }
        throw new BizException(403, "您无权查看该合同档案");
    }

    private void assertEditable(ContractInfo contract) {
        LoginUser user = UserContext.get();
        boolean owner = Objects.equals(contract.getCreateBy(), user.getId())
                || Objects.equals(contract.getOwnerId(), user.getId());
        if (!owner && !user.isAdmin()) {
            throw new BizException(403, "仅合同负责人或系统管理员可编辑该合同");
        }
    }

    private ContractInfo requireContract(Long id) {
        ContractInfo contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BizException("合同不存在或已删除");
        }
        return contract;
    }

    private void requireDept(Long deptId) {
        if (deptId == null || deptMapper.selectById(deptId) == null) {
            throw new BizException("归属部门不存在");
        }
    }

    private void requireUser(Long userId) {
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("合同负责人不存在");
        }
    }

    private void validateDates(LocalDate effectiveDate, LocalDate expireDate) {
        if (effectiveDate != null && expireDate != null && effectiveDate.isAfter(expireDate)) {
            throw new BizException("生效日期不能晚于到期日期");
        }
    }
}
