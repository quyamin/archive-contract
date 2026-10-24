package com.enterprise.archive.common;

import java.util.Set;

public final class BizConstants {

    private BizConstants() {
    }

    public static final class ContractStatus {
        public static final String DRAFT = "DRAFT";
        public static final String PENDING = "PENDING";
        public static final String REJECTED = "REJECTED";
        public static final String ARCHIVED = "ARCHIVED";
        public static final String EXPIRED = "EXPIRED";

        public static final Set<String> ARCHIVED_STATUS = Set.of(ARCHIVED, EXPIRED);
    }

    public static final class ReviewAction {
        public static final String SUBMIT = "SUBMIT";
        public static final String APPROVE = "APPROVE";
        public static final String REJECT = "REJECT";
    }

    public static final class ChangeStatus {
        public static final String PENDING = "PENDING";
        public static final String APPROVED = "APPROVED";
        public static final String REJECTED = "REJECTED";
    }

    public static final class BorrowStatus {
        public static final String PENDING = "PENDING";
        public static final String APPROVED = "APPROVED";
        public static final String REJECTED = "REJECTED";
        public static final String RETURNED = "RETURNED";
        public static final String EXPIRED = "EXPIRED";
    }

    public static final class WarningType {
        public static final String EXPIRY = "EXPIRY";
        public static final String PAYMENT = "PAYMENT";
    }
}
