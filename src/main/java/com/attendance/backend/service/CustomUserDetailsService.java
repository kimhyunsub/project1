package com.attendance.backend.service;

import com.attendance.backend.domain.entity.Employee;
import com.attendance.backend.domain.repository.EmployeeRepository;
import com.attendance.backend.exception.ResourceNotFoundException;
import com.attendance.backend.security.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public CustomUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Employee employee = employeeRepository.findByEmployeeCode(username)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        if (!employee.isActive()) {
            throw new ResourceNotFoundException("사용이 중지된 계정입니다.");
        }
        return new CustomUserDetails(employee);
    }
}
