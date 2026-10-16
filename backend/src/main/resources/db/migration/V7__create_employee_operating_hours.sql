CREATE TABLE employee_operating_hours (
                                          id BIGSERIAL PRIMARY KEY,
                                          day_of_week VARCHAR(16) NOT NULL UNIQUE,
                                          enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                          opening_time TIME NOT NULL,
                                          closing_time TIME NOT NULL,
                                          updated_by_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
                                          updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          CONSTRAINT employee_operating_hours_day_valid CHECK (
                                              day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')
                                              )
);

CREATE TABLE employee_operating_hour_exceptions (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    exception_date DATE NOT NULL UNIQUE,
                                                    closed BOOLEAN NOT NULL DEFAULT FALSE,
                                                    opening_time TIME,
                                                    closing_time TIME,
                                                    reason VARCHAR(160),
                                                    updated_by_id BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
                                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                    CONSTRAINT employee_operating_hour_exception_times CHECK (
                                                        closed = TRUE OR (opening_time IS NOT NULL AND closing_time IS NOT NULL)
                                                        )
);

CREATE INDEX idx_employee_operating_hour_exceptions_date
    ON employee_operating_hour_exceptions (exception_date);
