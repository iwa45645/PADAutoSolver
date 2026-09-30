package com.example.padautosolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/** Android may expose the same private directory through /data/user/0 and /data/data. */
final class InventoryArchivePath {
    static String relative(File root, File file) throws IOException {
        Path base = root.getCanonicalFile().toPath();
        Path target = file.getCanonicalFile().toPath();
        if (!target.startsWith(base)) throw new IOException("Invalid inventory path");
        return base.relativize(target).toString().replace('\\', '/');
    }
}
