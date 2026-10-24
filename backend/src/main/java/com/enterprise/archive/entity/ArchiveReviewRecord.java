package com.enterprise.archive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("archive_review_record")
public class ArchiveReviewRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private String action;
    private String opinion;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String archiveNo;
}
