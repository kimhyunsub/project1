package com.attendance.backend.security;

import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.entity.EmployeeRole;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private final Long employeeId;
    private final String employeeCode;
    private final String password;
    private final EmployeeRole role;
    private final boolean active;
    private final String registeredDeviceId;

    public CustomUserDetails(Employee employee) {
        this.employeeId = employee.getId();
        this.employeeCode = employee.getEmployeeCode();
        this.password = employee.getPassword();
        this.role = employee.getRole();
        this.active = employee.isActive();
        this.registeredDeviceId = employee.getRegisteredDeviceId();
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public String getRegisteredDeviceId() {
        return registeredDeviceId;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return employeeCode;
    }
}
