plugins {
    id("mobitarantino.android.library")
    alias(libs.plugins.metro)
}

android {
    namespace = "mobi.tarantino.proxykillswitcher.core.tunnel"

    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            ndkBuild {
                arguments +=
                    listOf(
                        "APP_PLATFORM=android-26",
                        "APP_CFLAGS=-DPKGNAME=mobi/tarantino/proxykillswitcher/core/tunnel -DCLSNAME=Socks5Tunnel",
                    )
            }
        }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/cpp/hev-socks5-tunnel/Android.mk")
        }
    }
}

dependencies {
    implementation(project(":core:di"))
}
