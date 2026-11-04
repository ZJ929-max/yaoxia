package com.opensandbox.dhizuku.platform;

/** 能力提供方代号。项目内的固定叫法：dhi / she / shi。 */
public enum ProviderId {
    DHI("dhi"),
    SHE("she"),
    SHI("shi");

    public final String label;

    ProviderId(String label) {
        this.label = label;
    }
}
