/******************************************************************************
 * Copyright (C) 2024  dyhkwong
 ******************************************************************************/

package io.nekohasekai.sagernet.fmt.juicity;

import androidx.annotation.NonNull;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import org.jetbrains.annotations.NotNull;

import io.nekohasekai.sagernet.fmt.AbstractBean;
import io.nekohasekai.sagernet.fmt.KryoConverters;
import io.nekohasekai.sagernet.ktx.NetsKt;
import libexclavecore.Libexclavecore;

public class JuicityBean extends AbstractBean {

    public String uuid;
    public String password;
    public String sni;
    public Boolean allowInsecure;
    public String certificates;
    public String pinnedPeerCertificateChainSha256;
    public String pinnedPeerCertificatePublicKeySha256;
    public String pinnedPeerCertificateSha256;
    public String mtlsCertificate;
    public String mtlsCertificatePrivateKey;
    public Boolean echEnabled;
    public String echConfigList;
    public String echQueryName;
    public String serverNameToVerify;

    /**
     * 唯一端口配置：单端口 "443"，或范围 "3000-3100"，或列表 "3000,3005"。
     * serverPort 由该字段解析得到，仅作兼容。
     */
    public String serverPorts;
    /** 跳跃间隔（秒）；端口多于 1 个且 >0 时启用 */
    public Long hopInterval;

    @Override
    public void initializeDefaultValues() {
        super.initializeDefaultValues();
        if (uuid == null) uuid = "";
        if (password == null) password = "";
        if (sni == null) sni = "";
        if (allowInsecure == null) allowInsecure = false;
        if (certificates == null) certificates = "";
        if (pinnedPeerCertificateChainSha256 == null) pinnedPeerCertificateChainSha256 = "";
        if (pinnedPeerCertificatePublicKeySha256 == null) pinnedPeerCertificatePublicKeySha256 = "";
        if (pinnedPeerCertificateSha256 == null) pinnedPeerCertificateSha256 = "";
        if (mtlsCertificate == null) mtlsCertificate = "";
        if (mtlsCertificatePrivateKey == null) mtlsCertificatePrivateKey = "";
        if (echEnabled == null) echEnabled = false;
        if (echConfigList == null) echConfigList = "";
        if (echQueryName == null) echQueryName = "";
        if (serverNameToVerify == null) serverNameToVerify = "";
        if (serverPorts == null || serverPorts.isEmpty()) {
            if (serverPort != null && serverPort > 0) {
                serverPorts = String.valueOf(serverPort);
            } else {
                serverPorts = "";
            }
        }
        if (hopInterval == null) hopInterval = 0L;
        // 用 serverPorts 同步 serverPort（取第一个有效端口）
        int first = firstPortFrom(serverPorts);
        if (first > 0) {
            serverPort = first;
        }
    }

    /** 从 "443" / "3000-3100" / "a,b" 解析第一个端口 */
    public static int firstPortFrom(String ports) {
        if (ports == null) return 0;
        ports = ports.trim();
        if (ports.isEmpty()) return 0;
        String part = ports.split(",")[0].trim();
        if (part.contains("-")) {
            part = part.split("-", 2)[0].trim();
        }
        try {
            int p = Integer.parseInt(part);
            if (p > 0 && p <= 65535) return p;
        } catch (NumberFormatException ignored) {
        }
        return 0;
    }

    @Override
    public void serialize(ByteBufferOutput output) {
        output.writeInt(8);
        super.serialize(output);
        output.writeString(uuid);
        output.writeString(password);
        output.writeString(sni);
        output.writeBoolean(allowInsecure);
        output.writeString(certificates);
        output.writeString(pinnedPeerCertificateChainSha256);
        output.writeString(pinnedPeerCertificatePublicKeySha256);
        output.writeString(pinnedPeerCertificateSha256);
        output.writeString(mtlsCertificate);
        output.writeString(mtlsCertificatePrivateKey);
        output.writeString(echConfigList);
        output.writeBoolean(echEnabled);
        output.writeString(serverNameToVerify);
        output.writeString(echQueryName);
        output.writeString(serverPorts);
        output.writeLong(hopInterval);
    }

    @Override
    public void deserialize(ByteBufferInput input) {
        int version = input.readInt();
        super.deserialize(input);
        uuid = input.readString();
        password = input.readString();
        sni = input.readString();
        allowInsecure = input.readBoolean();
        if (version < 4) {
            input.readString();
        }
        if (version >= 3) {
            certificates = input.readString();
        }
        if (version >= 2) {
            pinnedPeerCertificateChainSha256 = input.readString();
            if (version == 2 && !pinnedPeerCertificateChainSha256.isEmpty()) {
                allowInsecure = true;
            }
        }
        if (version >= 3) {
            pinnedPeerCertificatePublicKeySha256 = input.readString();
            pinnedPeerCertificateSha256 = input.readString();
            mtlsCertificate = input.readString();
            mtlsCertificatePrivateKey = input.readString();
            echConfigList = input.readString();
            if (version <= 4 && !echConfigList.isEmpty()) {
                echEnabled = true;
            }
        }
        if (version >= 5) {
            echEnabled = input.readBoolean();
        }
        if (version >= 6) {
            serverNameToVerify = input.readString();
        }
        if (version >= 7) {
            echQueryName = input.readString();
        }
        if (version >= 8) {
            serverPorts = input.readString();
            hopInterval = input.readLong();
        } else {
            // 旧数据：用 serverPort 填 serverPorts
            if (serverPort != null && serverPort > 0) {
                serverPorts = String.valueOf(serverPort);
            }
            hopInterval = 0L;
        }
        int first = firstPortFrom(serverPorts);
        if (first > 0) {
            serverPort = first;
        }
    }

    @Override
    public void applyFeatureSettings(AbstractBean other) {
        if (!(other instanceof JuicityBean bean)) return;
        if (allowInsecure) {
            bean.allowInsecure = true;
        }
        if (bean.certificates == null || bean.certificates.isEmpty() && !certificates.isEmpty()) {
            bean.certificates = certificates;
        }
        if (bean.pinnedPeerCertificateChainSha256 == null || bean.pinnedPeerCertificateChainSha256.isEmpty() &&
                !pinnedPeerCertificateChainSha256.isEmpty()) {
            bean.pinnedPeerCertificateChainSha256 = pinnedPeerCertificateChainSha256;
        }
        if (bean.pinnedPeerCertificatePublicKeySha256 == null || bean.pinnedPeerCertificatePublicKeySha256.isEmpty() &&
                !pinnedPeerCertificatePublicKeySha256.isEmpty()) {
            bean.pinnedPeerCertificatePublicKeySha256 = pinnedPeerCertificatePublicKeySha256;
        }
        if (bean.pinnedPeerCertificateSha256 == null || bean.pinnedPeerCertificateSha256.isEmpty() &&
                !pinnedPeerCertificateSha256.isEmpty()) {
            bean.pinnedPeerCertificateSha256 = pinnedPeerCertificateSha256;
        }
        bean.echEnabled = echEnabled;
        bean.echConfigList = echConfigList;
        bean.echQueryName = echQueryName;
        if (serverPorts != null && !serverPorts.isEmpty()) {
            bean.serverPorts = serverPorts;
        }
        if (hopInterval != null && hopInterval > 0) {
            bean.hopInterval = hopInterval;
        }
    }

    @NotNull
    @Override
    public JuicityBean clone() {
        return KryoConverters.deserialize(new JuicityBean(), KryoConverters.serialize(this));
    }

    public static final Creator<JuicityBean> CREATOR = new CREATOR<>() {
        @NonNull
        @Override
        public JuicityBean newInstance() {
            return new JuicityBean();
        }

        @Override
        public JuicityBean[] newArray(int size) {
            return new JuicityBean[size];
        }
    };

    @Override
    public boolean isInsecure() {
        if (Libexclavecore.isLoopbackIP(serverAddress) || serverAddress.equals("localhost")) {
            return false;
        }
        if (echEnabled) {
            return false;
        }
        if (!allowInsecure) {
            return false;
        }
        if (!pinnedPeerCertificateChainSha256.isEmpty()) {
            return false;
        }
        if (!pinnedPeerCertificatePublicKeySha256.isEmpty()) {
            return false;
        }
        if (!pinnedPeerCertificateSha256.isEmpty()) {
            return false;
        }
        if (!NetsKt.listByLineOrComma(serverNameToVerify).isEmpty()) {
            return false;
        }
        return true;
    }
}
