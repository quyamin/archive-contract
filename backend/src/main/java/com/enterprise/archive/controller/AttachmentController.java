package com.enterprise.archive.controller;

import com.enterprise.archive.common.Result;
import com.enterprise.archive.common.annotation.OpLog;
import com.enterprise.archive.common.annotation.RequirePerm;
import com.enterprise.archive.service.AttachmentService;
import com.enterprise.archive.vo.AttachmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/contract/{contractId}")
    @RequirePerm("contract:view")
    public Result<List<AttachmentVO>> list(@PathVariable Long contractId) {
        return Result.ok(attachmentService.listByContract(contractId));
    }

    @OpLog(module = "附件管理", action = "上传附件")
    @PostMapping("/upload")
    @RequirePerm("attachment:upload")
    public Result<List<AttachmentVO>> upload(@RequestParam Long contractId,
                                             @RequestParam(defaultValue = "OTHER") String fileType,
                                             @RequestParam("files") MultipartFile[] files) {
        return Result.ok(attachmentService.upload(contractId, files, fileType));
    }

    @OpLog(module = "附件管理", action = "删除附件")
    @DeleteMapping("/{id}")
    @RequirePerm("attachment:delete")
    public Result<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/preview")
    @RequirePerm("attachment:preview")
    public ResponseEntity<Resource> preview(@PathVariable Long id) {
        return buildResponse(id, false);
    }

    @OpLog(module = "附件管理", action = "下载附件")
    @GetMapping("/{id}/download")
    @RequirePerm("contract:download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return buildResponse(id, true);
    }

    private ResponseEntity<Resource> buildResponse(Long id, boolean download) {
        AttachmentService.FileResource file = attachmentService.loadResource(id, download);
        Resource resource = new FileSystemResource(file.path());
        String fileName = file.attachment().getOriginalName();
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = (download ? "attachment" : "inline") + "; filename*=UTF-8''" + encoded;
        String contentType = file.attachment().getMimeType();
        if (contentType == null || contentType.isBlank()) {
            try {
                contentType = Files.probeContentType(file.path());
            } catch (Exception ignored) {
            }
        }
        MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.attachment().getFileSize() == null ? 0 : file.attachment().getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resource);
    }
}
