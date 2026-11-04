package com.opensandbox.dhizuku;

// 事务号必须显式写死：Dhizuku 的 UserService 启动/销毁时
// 会向本接口发 FIRST_CALL_TRANSACTION+1 / +2，对应下面第 1、2 号方法。
interface IRemoteDpm {
    int getApiLevel() = 0;
    void onServiceCreated() = 1;
    void onServiceDestroyed() = 2;
    void setSuspended(in String[] packages, boolean suspended) = 3;
    String[] filterSuspended(in String[] packages) = 4;
    boolean isDeviceOwner() = 5;
}
