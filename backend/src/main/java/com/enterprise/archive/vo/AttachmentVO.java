package com.enterprise.archive.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttachmentVO {
    private Long id;
    private Long contractId;
    private String originalName;
    private String fileExt;
    private Long fileSize;
    private String fileType;
    private String uploaderName;
    private boolean previewable;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;
}
