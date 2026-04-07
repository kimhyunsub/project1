package com.attendance.backend.domain.entity;

import com.attendance.backend.domain.entity.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "companies")
public class Company extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CompanyPlan plan = CompanyPlan.FREE;

    @Column
    private Integer employeeLimit;

    @Column
    private Integer workplaceLimit;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    protected Company() {
    }

    public Company(
        String name,
        CompanyPlan plan,
        Integer employeeLimit,
        Integer workplaceLimit,
        Double latitude,
        Double longitude
    ) {
        this.name = name;
        this.plan = plan;
        this.employeeLimit = employeeLimit;
        this.workplaceLimit = workplaceLimit;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public CompanyPlan getPlan() {
        return plan == null ? CompanyPlan.FREE : plan;
    }

    public Integer getEmployeeLimit() {
        return employeeLimit;
    }

    public Integer getWorkplaceLimit() {
        return workplaceLimit;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void updateLimits(Integer employeeLimit, Integer workplaceLimit) {
        this.employeeLimit = employeeLimit;
        this.workplaceLimit = workplaceLimit;
    }

    public void updateLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
