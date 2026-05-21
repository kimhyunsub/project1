package com.attendance.backend.config;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class WorkRequestSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkRequestSchemaInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public WorkRequestSchemaInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!isPostgreSql()) {
            return;
        }

        ensureWorkplaceApprovalColumn();
        ensureWorkplaceEnabledColumn();
        ensureWorkplaceDeviceLoginColumn();
        ensureCompanyApprovalColumn();
        ensureCompanyEnabledColumn();
        ensureWorkRequestTable();
    }

    private boolean isPostgreSql() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("postgresql");
        }
    }

    private void ensureWorkplaceApprovalColumn() {
        jdbcTemplate.execute("""
            ALTER TABLE workplaces
            ADD COLUMN IF NOT EXISTS work_request_approval_required boolean NOT NULL DEFAULT true
            """);
    }

    private void ensureWorkplaceEnabledColumn() {
        jdbcTemplate.execute("""
            ALTER TABLE workplaces
            ADD COLUMN IF NOT EXISTS work_request_enabled boolean NOT NULL DEFAULT true
            """);
    }

    private void ensureWorkplaceDeviceLoginColumn() {
        jdbcTemplate.execute("""
            ALTER TABLE workplaces
            ADD COLUMN IF NOT EXISTS enforce_single_device_login boolean NOT NULL DEFAULT true
            """);
    }

    private void ensureCompanyApprovalColumn() {
        jdbcTemplate.execute("""
            ALTER TABLE company_settings
            ADD COLUMN IF NOT EXISTS work_request_approval_required boolean NOT NULL DEFAULT true
            """);
    }

    private void ensureCompanyEnabledColumn() {
        jdbcTemplate.execute("""
            ALTER TABLE company_settings
            ADD COLUMN IF NOT EXISTS work_request_enabled boolean NOT NULL DEFAULT true
            """);
    }

    private void ensureWorkRequestTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS work_requests (
                id bigserial PRIMARY KEY,
                employee_id bigint NOT NULL,
                company_id bigint NOT NULL,
                request_type varchar(20) NOT NULL,
                status varchar(20) NOT NULL,
                half_day_type varchar(20),
                occasion_type varchar(40),
                request_date date NOT NULL,
                early_leave_minutes integer,
                reason varchar(500),
                reviewed_by_employee_id bigint,
                reviewed_at timestamp(6),
                review_note varchar(500),
                canceled_at timestamp(6),
                created_at timestamp(6) NOT NULL DEFAULT now(),
                updated_at timestamp(6) NOT NULL DEFAULT now()
            )
            """);

        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS employee_id bigint NOT NULL");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS company_id bigint NOT NULL");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS request_type varchar(20) NOT NULL");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS status varchar(20) NOT NULL");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS half_day_type varchar(20)");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS occasion_type varchar(40)");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS request_date date NOT NULL");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS early_leave_minutes integer");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS reason varchar(500)");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS reviewed_by_employee_id bigint");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS reviewed_at timestamp(6)");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS review_note varchar(500)");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS canceled_at timestamp(6)");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS created_at timestamp(6) NOT NULL DEFAULT now()");
        jdbcTemplate.execute("ALTER TABLE work_requests ADD COLUMN IF NOT EXISTS updated_at timestamp(6) NOT NULL DEFAULT now()");

        ensureWorkRequestStatusConstraint();

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_work_requests_employee_date
            ON work_requests (employee_id, request_date DESC, created_at DESC)
            """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_work_requests_company_status_date
            ON work_requests (company_id, status, request_date DESC, created_at DESC)
            """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_work_requests_reviewed_by
            ON work_requests (reviewed_by_employee_id)
            """);
        log.info("Work request schema is ready.");
    }

    private void ensureWorkRequestStatusConstraint() {
        jdbcTemplate.execute("""
            DO $$
            DECLARE
                constraint_name text;
            BEGIN
                FOR constraint_name IN
                    SELECT conname
                    FROM pg_constraint
                    WHERE conrelid = 'work_requests'::regclass
                      AND contype = 'c'
                      AND pg_get_constraintdef(oid) ILIKE '%status%'
                LOOP
                    EXECUTE format('ALTER TABLE work_requests DROP CONSTRAINT IF EXISTS %I', constraint_name);
                END LOOP;
            END $$;
            """);

        jdbcTemplate.execute("""
            ALTER TABLE work_requests
            ADD CONSTRAINT work_requests_status_check
            CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCEL_REQUESTED', 'CANCELED'))
            """);
    }
}
