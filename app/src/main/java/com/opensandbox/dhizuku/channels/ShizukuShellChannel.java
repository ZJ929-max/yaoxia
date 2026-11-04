package com.opensandbox.dhizuku.channels;

import android.content.Context;
import android.content.pm.PackageManager;

import com.opensandbox.dhizuku.core.Outcome;
import com.opensandbox.dhizuku.platform.Capability;
import com.opensandbox.dhizuku.platform.CapabilityProvider;
import com.opensandbox.dhizuku.platform.ProviderId;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;

import rikka.shizuku.Shizuku;

/**
 * shi（Shizuku）通道：以 shell 身份执行命令。
 * 工作资料的创建、把应用装进资料、指定 Profile Owner 只能走这里。
 */
public final class ShizukuShellChannel implements CapabilityProvider, ShellChannel {

    private static final String SHIZUKU_PACKAGE = "moe.shizuku.privileged.api";
    private static final int PERMISSION_REQUEST_CODE = 2001;

    private final Context context;

    public ShizukuShellChannel(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public ProviderId id() {
        return ProviderId.SHI;
    }

    @Override
    public Set<Capability> capabilities() {
        return EnumSet.of(Capability.SHELL);
    }

    @Override
    public String describe() {
        return "shi · " + SHIZUKU_PACKAGE;
    }

    @Override
    public Outcome<Boolean> isAvailable(Context ignored) {
        try {
            if (!installed()) {
                return Outcome.ok(false);
            }
            if (!Shizuku.pingBinder()) {
                return Outcome.ok(false);
            }
            return Outcome.ok(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED);
        } catch (Throwable throwable) {
            return Outcome.ok(false);
        }
    }

    @Override
    public Outcome<Void> requestPermission() {
        try {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE);
            return Outcome.ok();
        } catch (Throwable throwable) {
            return Outcome.fail(throwable);
        }
    }

    @Override
    public Outcome<String> execute(String... command) {
        try {
            Process process = Shizuku.newProcess(command, null, null);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream input = process.getInputStream()) {
                byte[] chunk = new byte[4096];
                int read;
                while ((read = input.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                }
            }
            int exitCode = process.waitFor();
            String output = buffer.toString(StandardCharsets.UTF_8.name()).trim();
            if (exitCode != 0) {
                return Outcome.fail("exit " + exitCode + (output.isEmpty() ? "" : ": " + output));
            }
            return Outcome.ok(output);
        } catch (Throwable throwable) {
            return Outcome.fail(throwable);
        }
    }

    private boolean installed() {
        try {
            context.getPackageManager().getPackageInfo(SHIZUKU_PACKAGE, 0);
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }
}
