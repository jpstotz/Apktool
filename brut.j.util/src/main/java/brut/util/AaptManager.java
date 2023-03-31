/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.util;

import brut.common.BrutException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AaptManager {

    public static File getAapt2() throws BrutException {
        return getAapt(2);
    }

    public static File getAapt1() throws BrutException {
        return getAapt(1);
    }

    private static File getAapt(Integer version) throws BrutException {
        File aaptBinary;
        String aaptVersion = getAaptBinaryName(version);

        // Set the 64 bit flag
        aaptVersion += OSDetection.is64Bit() ? "_64" : "";

        String aaptPath;
        try {
            if (OSDetection.isMacOSX()) {
                // MacOS binaries are FAT binaries for x86_64 and arm64
                if (!OSDetection.is64Bit()) {
                    throw new BrutException("32 bit OS detected. No 32 bit binaries available.");
                }
                aaptPath = "/prebuilt/macosx/" + aaptVersion;
            } else if (OSDetection.isUnix()) {
                // Linux binaries are ELF32 (80386) / ELF64 X86-64
                if (OSDetection.returnArch().contains("arm")) {
                    throw new BrutException("ARM CPU detected. Only X86 and X86-64 binaries available.");
                }
                aaptPath = "/prebuilt/linux/" + aaptVersion;
            } else if (OSDetection.isWindows()) {
                if (OSDetection.returnArch().contains("arm")) {
                    // All Windows on ARM versions should have 32 bit x86 emulation
                    aaptVersion = getAaptBinaryName(version); // use 32 bit version
                }
                aaptPath = "/prebuilt/windows/" + aaptVersion + ".exe";
            } else {
                throw new BrutException("Could not identify platform: " + OSDetection.returnOS());
            }
            aaptBinary = Jar.getResourceAsFile(aaptPath, AaptManager.class);
        } catch (BrutException ex) {
            throw new BrutException(ex);
        }

        if (aaptBinary.setExecutable(true)) {
            return aaptBinary;
        }

        throw new BrutException("Can't set aapt binary as executable");
    }

    public static String getAaptExecutionCommand(String aaptPath, File aapt) throws BrutException {
        if (! aaptPath.isEmpty()) {
            File aaptFile = new File(aaptPath);
            if (aaptFile.canRead() && aaptFile.exists()) {
                aaptFile.setExecutable(true);
                return aaptFile.getPath();
            } else {
                throw new BrutException("binary could not be read: " + aaptFile.getAbsolutePath());
            }
        } else {
            return aapt.getAbsolutePath();
        }
    }

    public static int getAaptVersion(String aaptLocation) throws BrutException {
        return getAaptVersion(new File(aaptLocation));
    }

    public static String getAaptBinaryName(Integer version) {
        return "aapt" + (version == 2 ? "2" : "");
    }

    public static int getAppVersionFromString(String version) throws BrutException {
        if (version.startsWith("Android Asset Packaging Tool (aapt) 2:")) {
            return 2;
        } else if (version.startsWith("Android Asset Packaging Tool (aapt) 2.")) {
            return 2; // Prior to Android SDK 26.0.2
        } else if (version.startsWith("Android Asset Packaging Tool, v0.")) {
            return 1;
        }

        throw new BrutException("aapt version could not be identified: " + version);
    }

    public static int getAaptVersion(File aapt) throws BrutException {
        if (!aapt.isFile()) {
            throw new BrutException("Could not identify aapt binary as executable.");
        }
        aapt.setExecutable(true);

        List<String> cmd = new ArrayList<>();
        cmd.add(aapt.getAbsolutePath());
        cmd.add("version");

        String version = OS.execAndReturn(cmd.toArray(new String[0]));

        if (version == null) {
            throw new BrutException("Could not execute aapt binary at location: " + aapt.getAbsolutePath());
        }

        return getAppVersionFromString(version);
    }
}
