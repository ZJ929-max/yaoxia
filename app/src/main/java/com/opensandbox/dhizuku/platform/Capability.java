package com.opensandbox.dhizuku.platform;

/** 本应用需要的几类能力。新增能力只在这里加一项。 */
public enum Capability {
    /** 应用管控：冻结、禁止卸载、权限授予等 */
    POLICY,
    /** 以 shell 身份执行命令：建用户、装包、设 Profile Owner */
    SHELL,
    /** 向设备所有者申请委派授权 */
    DELEGATION
}
