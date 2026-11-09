/******************************************************************************
 * Copyright (C) 2024  dyhkwong
 ******************************************************************************/

package io.nekohasekai.sagernet.ui.profile

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.preference.EditTextPreferenceModifiers
import io.nekohasekai.sagernet.fmt.juicity.JuicityBean
import io.nekohasekai.sagernet.ktx.unwrapIDN

class JuicitySettingsActivity : ProfileSettingsActivity<JuicityBean>() {

    override fun createEntity() = JuicityBean()

    override fun JuicityBean.init() {
        DataStore.profileName = name
        DataStore.serverAddress = serverAddress
        // 只使用 serverPorts 作为唯一端口配置
        DataStore.serverPorts = when {
            !serverPorts.isNullOrEmpty() -> serverPorts
            serverPort != null && serverPort > 0 -> serverPort.toString()
            else -> ""
        }
        DataStore.serverHopInterval = hopInterval ?: 0L
        DataStore.serverUserId = uuid
        DataStore.serverPassword = password
        DataStore.serverSNI = sni
        DataStore.serverAllowInsecure = allowInsecure
        DataStore.serverCertificates = certificates
        DataStore.serverPinnedCertificateChain = pinnedPeerCertificateChainSha256
        DataStore.serverPinnedCertificatePublicKey = pinnedPeerCertificatePublicKeySha256
        DataStore.serverPinnedCertificate = pinnedPeerCertificateSha256
        DataStore.serverEchEnabled = echEnabled
        DataStore.serverEchConfigList = echConfigList
        DataStore.serverEchQueryName = echQueryName
        DataStore.serverMtlsCertificate = mtlsCertificate
        DataStore.serverMtlsCertificatePrivateKey = mtlsCertificatePrivateKey
        DataStore.serverServerNameToVerify = serverNameToVerify
    }

    override fun JuicityBean.serialize() {
        name = DataStore.profileName
        serverAddress = DataStore.serverAddress.unwrapIDN()
        serverPorts = DataStore.serverPorts?.trim().orEmpty()
        val first = JuicityBean.firstPortFrom(serverPorts)
        require(first > 0) { "invalid server port" }
        serverPort = first
        hopInterval = DataStore.serverHopInterval
        uuid = DataStore.serverUserId
        password = DataStore.serverPassword
        sni = DataStore.serverSNI
        allowInsecure = DataStore.serverAllowInsecure
        certificates = DataStore.serverCertificates
        pinnedPeerCertificateChainSha256 = DataStore.serverPinnedCertificateChain
        pinnedPeerCertificatePublicKeySha256 = DataStore.serverPinnedCertificatePublicKey
        pinnedPeerCertificateSha256 = DataStore.serverPinnedCertificate
        echEnabled = DataStore.serverEchEnabled
        echConfigList = DataStore.serverEchConfigList
        echQueryName = DataStore.serverEchQueryName
        mtlsCertificate = DataStore.serverMtlsCertificate
        mtlsCertificatePrivateKey = DataStore.serverMtlsCertificatePrivateKey
        serverNameToVerify = DataStore.serverServerNameToVerify
    }

    override fun PreferenceFragmentCompat.createPreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        addPreferencesFromResource(R.xml.juicity_preferences)

        findPreference<EditTextPreference>(Key.SERVER_PASSWORD)!!.apply {
            summaryProvider = PasswordSummaryProvider
        }
        findPreference<EditTextPreference>(Key.SERVER_HOP_INTERVAL)?.apply {
            setOnBindEditTextListener(EditTextPreferenceModifiers.Number)
        }

        val echEnabled = findPreference<SwitchPreference>(Key.SERVER_ECH_ENABLED)!!
        val echConfigList = findPreference<EditTextPreference>(Key.SERVER_ECH_CONFIG_LIST)!!
        val echQueryName = findPreference<EditTextPreference>(Key.SERVER_ECH_QUERY_NAME)!!
        echConfigList.isEnabled = echEnabled.isChecked
        echQueryName.isEnabled = echEnabled.isChecked
        echEnabled.setOnPreferenceChangeListener { _, newValue ->
            echConfigList.isEnabled = newValue as Boolean
            echQueryName.isEnabled = newValue
            true
        }
    }
}
