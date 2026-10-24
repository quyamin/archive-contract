package com.enterprise.archive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contract_attachment")
public class ContractAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private String fileName;
    private String originalName;
    private String filePath;
    private String fileExt;
    private Long fileSize;
    private String mimeType;
    private String fileType;
    private String checksum;
    private String contentText;
    private Long uploaderId;
    private LocalDateTime uploadTime;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
