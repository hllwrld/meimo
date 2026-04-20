package com.stx.meimo.util

object ServerConfig {

    const val DEFAULT_DOMAIN = "sexyai.ai"

    val presetDomainList = listOf(
        "sexyai.ai",
        "sexyai.top",
        "meimoai3.com",
        "meimoai13.com"
    )

    private val _customDomains = mutableSetOf<String>()

    val availableDomains: List<String>
        get() = presetDomainList + _customDomains.filter { it !in presetDomainList }.sorted()

    @Volatile
    var currentDomain: String = DEFAULT_DOMAIN
        private set

    val webUrl: String get() = "https://$currentDomain"
    val apiBaseUrl: String get() = "https://$currentDomain/api/"

    fun init(prefs: AppPreferences) {
        _customDomains.clear()
        _customDomains.addAll(prefs.customDomains)
        currentDomain = prefs.serverDomain
    }

    fun setDomain(domain: String, prefs: AppPreferences) {
        currentDomain = domain
        prefs.serverDomain = domain
    }

    fun addCustomDomain(domain: String, prefs: AppPreferences) {
        if (domain.isBlank() || domain in presetDomainList) return
        _customDomains.add(domain)
        prefs.customDomains = _customDomains.toSet()
    }

    fun removeCustomDomain(domain: String, prefs: AppPreferences) {
        if (domain in presetDomainList) return
        _customDomains.remove(domain)
        prefs.customDomains = _customDomains.toSet()
        if (currentDomain == domain) {
            setDomain(DEFAULT_DOMAIN, prefs)
        }
    }
}
