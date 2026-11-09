/******************************************************************************
 * Copyright (C) 2024  dyhkwong
 ******************************************************************************/

package io.nekohasekai.sagernet.fmt.juicity

import io.nekohasekai.sagernet.ktx.*
import libexclavecore.Libexclavecore
import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid

fun parseJuicity(url: String): JuicityBean {
    val link = Libexclavecore.parseURL(url)
    return JuicityBean().apply {
        name = link.fragment
        serverAddress = link.host
        // 链接里的 port 作为默认；query ports 可覆盖为范围
        val linkPort = when {
            !link.hasPort() -> error("invalid port")
            else -> link.port
        }
        serverPort = linkPort
        serverPorts = linkPort.toString()
        uuid = parseUUID(link.username)?.toHexDashString() ?: error("invalid uuid")
        password = link.password
        link.queryParameter("sni")?.also { sni = it }
        link.queryParameter("allow_insecure")?.takeIf { it == "1" }?.also { allowInsecure = true }
        link.queryParameter("pinned_certchain_sha256")?.also {
            val hash = try {
                when {
                    it.length == 64 -> it.hexToByteArray()
                    it.contains("-") || it.contains("_") -> Base64.UrlSafe.decode(it)
                    else -> Base64.decode(it)
                }
            } catch (_: Exception) {
                throw IllegalArgumentException("invalid pinned_certchain_sha256")
            }
            require(hash.size == 32) { "invalid pinned_certchain_sha256" }
            pinnedPeerCertificateChainSha256 = Base64.UrlSafe.encode(hash)
            allowInsecure = true
        }
        // 唯一端口配置：ports=3000-3100 或 ports=443
        link.queryParameter("ports")?.takeIf { it.isNotBlank() }?.also {
            serverPorts = it
            val first = JuicityBean.firstPortFrom(it)
            if (first > 0) serverPort = first
        }
        link.queryParameter("hop_interval")?.toLongOrNull()?.also { hopInterval = it }
    }
}

fun JuicityBean.toUri(): String? {
    val exportPort = JuicityBean.firstPortFrom(serverPorts).takeIf { it > 0 } ?: serverPort
    val builder = Libexclavecore.newURL("juicity").apply {
        setHostPort(serverAddress, exportPort)
        require(Uuid.parseHexDashOrNull(uuid) != null) { "invalid uuid" }
        username = uuid
        if (name.isNotEmpty()) fragment = name
    }
    if (password.isNotEmpty()) builder.password = password
    builder.addQueryParameter("congestion_control", "bbr")
    if (sni.isNotEmpty()) builder.addQueryParameter("sni", sni)
    if (pinnedPeerCertificateChainSha256.isNotEmpty()) {
        val certChainHash = pinnedPeerCertificateChainSha256.listByLineOrComma()[0]
        val hash = try {
            when {
                certChainHash.length == 64 -> certChainHash.hexToByteArray()
                certChainHash.contains("-") || certChainHash.contains("_") -> Base64.UrlSafe.decode(certChainHash)
                else -> Base64.decode(certChainHash)
            }
        } catch (_: Exception) {
            throw IllegalArgumentException("invalid pinned_certchain_sha256")
        }
        require(hash.size == 32) { "invalid pinned_certchain_sha256" }
        builder.addQueryParameter("pinned_certchain_sha256", Base64.UrlSafe.encode(hash))
    }
    if (pinnedPeerCertificateChainSha256.isNotEmpty() ||
        (allowInsecure && pinnedPeerCertificateSha256.isEmpty() &&
                pinnedPeerCertificatePublicKeySha256.isEmpty() &&
                serverNameToVerify.listByLineOrComma().isEmpty())
    ) {
        builder.addQueryParameter("allow_insecure", "1")
    }
    // 范围或与链接 port 不同时写出 ports
    if (!serverPorts.isNullOrEmpty()) {
        val p = serverPorts.trim()
        if (p.contains("-") || p.contains(",") || p != exportPort.toString()) {
            builder.addQueryParameter("ports", p)
        }
    }
    if (hopInterval != null && hopInterval > 0) {
        builder.addQueryParameter("hop_interval", hopInterval.toString())
    }
    return builder.string
}
