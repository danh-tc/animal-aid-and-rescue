package com.aar.system;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tham số hệ thống (báo cáo mục 4.2), Quản lý chỉnh ở màn hình M04. */
@Entity
@Table(name = "system_parameters")
public class SystemParameter {

    @Id
    @Column(name = "param_key")
    private String key;

    @Column(name = "param_value", nullable = false)
    private String value;

    private String description;

    protected SystemParameter() {
    }

    public void changeValue(String value) {
        this.value = value;
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
    public String getDescription() { return description; }
}
