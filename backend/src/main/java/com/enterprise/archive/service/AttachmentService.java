package com.enterprise.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.archive.common.BizException;
import com.enterprise.archive.common.UserContext;
import com.enterprise.archive.entity.BorrowApply;
import com.enterprise.archive.entity.ContractAttachment;
import com.enterprise.archive.entity.ContractInfo;
import com.enterprise.archive.entity.SysUser;
import com.enterprise.archive.mapper.BorrowApplyMapper;
import com.enterprise.archive.mapper.ContractAttachmentMapper;
import com.enterprise.archive.mapper.ContractInfoMapper;
import com.enterprise.archive.mapper.SysUserMapper;
import com.enterprise.archive.security.LoginUser;
import com.enterprise.archive.vo.AttachmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final Set<String> PREVIEWABLE = Set.of("pdf", "jpg", "jpeg", "png", "gif", "docx");

    private final ContractAttachmentMapper attachmentMapper;
    private final ContractInfoMapper contractMapper;
    private final BorrowApplyMapper borrowMapper;
    private final SysUserMapper userMapper;
    private final ConfigService configService;

    @Transactional
    public List<AttachmentVO> upload(Long contractId, MultipartFile[] files, String fileType) {
        ContractInfo contract = requireContract(contractId);
        ensureUploadable(contract);
        if (files == null || files.length == 0) {
            throw new BizException("请选择需要上传的文件");
        }
        String allowed = configService.allowedExt().toLowerCase(Locale.ROOT);
        Set<String> allowedSet = new HashSet<>(Arrays.asList(allowed.split(",")));
        long maxBytes = (long) configService.maxSizeMb() * 1024 * 1024;
        List<AttachmentVO> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            if (file.getSize() > maxBytes) {
                throw new BizException("文件《" + safeOriginalName(file) + "》超过" + configService.maxSizeMb() + "MB限制");
            }
            String originalName = safeOriginalName(file);
            String ext = extensionOf(originalName);
            if (!allowedSet.contains(ext)) {
                throw new BizException("不支持的文件格式：" + ext + "，允许格式：" + allowed);
            }
            try {
                result.add(store(contract, file, originalName, ext, fileType));
            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                log.error("附件保存失败", e);
                throw new BizException("附件保存失败，请重试");
            }
        }
        if (result.isEmpty()) {
            throw new BizException("未检测到有效文件");
        }
        return result;
    }

    private AttachmentVO store(ContractInfo contract, MultipartFile file, String originalName,
                               String ext, String fileType) throws IOException {
        Path baseDir = storageBase();
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path targetDir = baseDir.resolve(dateDir).normalize();
        if (!targetDir.startsWith(baseDir)) {
            throw new BizException("文件存储路径非法");
        }
        Files.createDirectories(targetDir);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = targetDir.resolve(storedName).normalize();
        if (!target.startsWith(baseDir)) {
            throw new BizException("文件存储路径非法");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        String contentText = extractText(target, ext);
        ContractAttachment attachment = new ContractAttachment();
        attachment.setContractId(contract.getId());
        attachment.setFileName(storedName);
        attachment.setOriginalName(originalName);
        attachment.setFilePath(target.toString());
        attachment.setFileExt(ext);
        attachment.setFileSize(file.getSize());
        attachment.setMimeType(file.getContentType());
        attachment.setFileType(StringUtils.hasText(fileType) ? fileType : "OTHER");
        attachment.setChecksum(sha256(target));
        attachment.setContentText(contentText);
        attachment.setUploaderId(UserContext.userId());
        attachment.setUploadTime(LocalDateTime.now());
        attachmentMapper.insert(attachment);
        return toVO(attachment, UserContext.get().getRealName());
    }

    private void ensureUploadable(ContractInfo contract) {
        LoginUser user = UserContext.get();
        boolean owner = Objects.equals(contract.getCreateBy(), user.getId())
                || Objects.equals(contract.getOwnerId(), user.getId());
        if (!owner && !user.isAdmin() && !user.has("contract:view:all")) {
            throw new BizException(403, "您无权操作该合同附件");
        }
        if (!Set.of("DRAFT", "REJECTED").contains(contract.getStatus())) {
            throw new BizException("当前合同状态不允许上传附件，已提交审核或归档的合同请走变更流程");
        }
    }

    public List<AttachmentVO> listByContract(Long contractId) {
        List<ContractAttachment> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<ContractAttachment>()
                        .eq(ContractAttachment::getContractId, contractId)
                        .orderByAsc(ContractAttachment::getId));
        Map<Long, String> names = new HashMap<>();
        if (!attachments.isEmpty()) {
            Set<Long> userIds = new HashSet<>();
            attachments.forEach(a -> userIds.add(a.getUploaderId()));
            for (SysUser user : userMapper.selectBatchIds(userIds)) {
                names.put(user.getId(), user.getRealName());
            }
        }
        return attachments.stream()
                .map(a -> toVO(a, names.getOrDefault(a.getUploaderId(), ""))).toList();
    }

    private AttachmentVO toVO(ContractAttachment attachment, String uploaderName) {
        AttachmentVO vo = new AttachmentVO();
        vo.setId(attachment.getId());
        vo.setContractId(attachment.getContractId());
        vo.setOriginalName(attachment.getOriginalName());
        vo.setFileExt(attachment.getFileExt());
        vo.setFileSize(attachment.getFileSize());
        vo.setFileType(attachment.getFileType());
        vo.setUploaderName(uploaderName);
        vo.setUploadTime(attachment.getUploadTime());
        vo.setPreviewable(PREVIEWABLE.contains(attachment.getFileExt()));
        return vo;
    }

    public FileResource loadResource(Long attachmentId, boolean download) {
        ContractAttachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BizException("附件不存在或已删除");
        }
        ContractInfo contract = requireContract(attachment.getContractId());
        assertCanRead(contract, download);
        Path path = Paths.get(attachment.getFilePath()).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw new BizException(500, "附件文件丢失，请联系档案管理员核查");
        }
        return new FileResource(attachment, path);
    }

    private void assertCanRead(ContractInfo contract, boolean download) {
        LoginUser user = UserContext.get();
        if (download) {
            if (!UserContext.hasPerm("contract:download")) {
                throw new BizException(403, "您没有附件下载权限");
            }
            if (isArchived(contract)) {
                if (!user.has("archive:download:any") && !hasActiveBorrow(contract.getId(), user.getId())) {
                    throw new BizException(403, "该档案未开放下载，请先提交借阅申请并审批通过");
                }
            } else if (!Objects.equals(contract.getCreateBy(), user.getId())
                    && !Objects.equals(contract.getOwnerId(), user.getId())
                    && !user.has("contract:view:all")) {
                throw new BizException(403, "您无权下载该合同附件");
            }
            return;
        }
        if (!UserContext.hasPerm("attachment:preview")) {
            throw new BizException(403, "您没有附件预览权限");
        }
        if (!isArchived(contract) && !Objects.equals(contract.getCreateBy(), user.getId())
                && !Objects.equals(contract.getOwnerId(), user.getId())
                && !user.has("contract:view:all")) {
            throw new BizException(403, "您无权查看该合同附件");
        }
    }

    public boolean canDownload(ContractInfo contract, Long userId) {
        if (!UserContext.hasPerm("contract:download")) {
            return false;
        }
        LoginUser user = UserContext.get();
        if (isArchived(contract)) {
            return user.has("archive:download:any") || hasActiveBorrow(contract.getId(), userId);
        }
        return Objects.equals(contract.getCreateBy(), userId)
                || Objects.equals(contract.getOwnerId(), userId)
                || user.has("contract:view:all");
    }

    public boolean hasActiveBorrow(Long contractId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return borrowMapper.selectCount(new LambdaQueryWrapper<BorrowApply>()
                .eq(BorrowApply::getContractId, contractId)
                .eq(BorrowApply::getApplicantId, userId)
                .eq(BorrowApply::getStatus, "APPROVED")
                .le(BorrowApply::getStartTime, now)
                .ge(BorrowApply::getEndTime, now)) > 0;
    }

    public void delete(Long id) {
        ContractAttachment attachment = attachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BizException("附件不存在");
        }
        ContractInfo contract = requireContract(attachment.getContractId());
        ensureUploadable(contract);
        attachmentMapper.deleteById(id);
    }

    private ContractInfo requireContract(Long contractId) {
        ContractInfo contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new BizException("合同不存在");
        }
        return contract;
    }

    private boolean isArchived(ContractInfo contract) {
        return "ARCHIVED".equals(contract.getStatus()) || "EXPIRED".equals(contract.getStatus());
    }

    private Path storageBase() {
        return Paths.get(configService.storagePath()).toAbsolutePath().normalize();
    }

    private String safeOriginalName(MultipartFile file) {
        String name = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "未命名文件"));
        name = name.replace("\\", "/");
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf('/') + 1);
        }
        if (name.contains("..") || name.isBlank()) {
            throw new BizException("文件名不合法");
        }
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private String extensionOf(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            throw new BizException("文件缺少扩展名");
        }
        return name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, len);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            return null;
        }
    }

    private String extractText(Path path, String ext) {
        if (!Set.of("pdf", "doc", "docx").contains(ext)) {
            return null;
        }
        try {
            String text;
            if ("pdf".equals(ext)) {
                try (PDDocument document = PDDocument.load(path.toFile())) {
                    if (document.getNumberOfPages() > 200) {
                        return null;
                    }
                    text = new PDFTextStripper().getText(document);
                }
            } else if ("docx".equals(ext)) {
                try (InputStream in = Files.newInputStream(path);
                     XWPFDocument document = new XWPFDocument(in);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    text = extractor.getText();
                }
            } else {
                try (InputStream in = Files.newInputStream(path);
                     HWPFDocument document = new HWPFDocument(in);
                     WordExtractor extractor = new WordExtractor(document)) {
                    text = extractor.getText();
                }
            }
            if (text == null) {
                return null;
            }
            text = text.replaceAll("\\s+", " ").trim();
            return text.length() > 200_000 ? text.substring(0, 200_000) : text;
        } catch (Exception e) {
            log.warn("附件文本抽取失败: {}", path, e);
            return null;
        }
    }

    public record FileResource(ContractAttachment attachment, Path path) {
    }
}
