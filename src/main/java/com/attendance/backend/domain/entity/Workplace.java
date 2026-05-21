package com.attendance.backend.domain.entity;

import com.attendance.backend.domain.entity.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "workplaces")
public class Workplace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Integer allowedRadiusMeters;

    @Column(name = "notice_message", length = 1000)
    private String noticeMessage;

    @Column(name = "work_request_approval_required", nullable = false, columnDefinition = "boolean default true")
    private boolean workRequestApprovalRequired = true;

    @Column(name = "work_request_enabled", nullable = false, columnDefinition = "boolean default true")
    private boolean workRequestEnabled = true;

    @Column(name = "enforce_single_device_login", nullable = false, columnDefinition = "boolean default true")
    private boolean enforceSingleDeviceLogin = true;

    protected Workplace() {
    }

    public Workplace(Company company,
                     String name,
                     Double latitude,
                     Double longitude,
                     Integer allowedRadiusMeters,
                     String noticeMessage,
                     boolean workRequestApprovalRequired) {
        this(company, name, latitude, longitude, allowedRadiusMeters, noticeMessage, workRequestApprovalRequired, true, true);
    }

    public Workplace(Company company,
                     String name,
                     Double latitude,
                     Double longitude,
                     Integer allowedRadiusMeters,
                     String noticeMessage,
                     boolean workRequestApprovalRequired,
                     boolean workRequestEnabled,
                     boolean enforceSingleDeviceLogin) {
        this.company = company;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadiusMeters = allowedRadiusMeters;
        this.noticeMessage = noticeMessage;
        this.workRequestApprovalRequired = workRequestApprovalRequired;
        this.workRequestEnabled = workRequestEnabled;
        this.enforceSingleDeviceLogin = enforceSingleDeviceLogin;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getName() {
        return name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getAllowedRadiusMeters() {
        return allowedRadiusMeters;
    }

    public String getNoticeMessage() {
        return noticeMessage;
    }

    public boolean isWorkRequestApprovalRequired() {
        return workRequestApprovalRequired;
    }

    public boolean isWorkRequestEnabled() {
        return workRequestEnabled;
    }

    public boolean isEnforceSingleDeviceLogin() {
        return enforceSingleDeviceLogin;
    }

    public void update(
        String name,
        Double latitude,
        Double longitude,
        Integer allowedRadiusMeters,
        String noticeMessage,
        boolean workRequestApprovalRequired,
        boolean workRequestEnabled,
        boolean enforceSingleDeviceLogin
    ) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.allowedRadiusMeters = allowedRadiusMeters;
        this.noticeMessage = noticeMessage;
        this.workRequestApprovalRequired = workRequestApprovalRequired;
        this.workRequestEnabled = workRequestEnabled;
        this.enforceSingleDeviceLogin = enforceSingleDeviceLogin;
    }
}
