package dev.mattramotar.meeseeks.runtime.internal.db

import java.util.Properties

internal object QuartzProps {
    private const val JOB_STORE_DS = "org.quartz.jobStore.dataSource"
    private const val DS_PREFIX = "org.quartz.dataSource"

    fun jdbcUrlFromQuartzProps(props: Properties): String {
        val dsName = props.getProperty(JOB_STORE_DS)
            ?: error("$JOB_STORE_DS not set in quartz.properties!")
        val key = "$DS_PREFIX.$dsName.URL"
        return props.getProperty(key)
            ?: error("$key not set in quartz.properties!")
    }

    fun setJdbcUrl(props: Properties, jdbcUrl: String) {
        val dsName = props.getProperty(JOB_STORE_DS)
            ?: error("$JOB_STORE_DS not set in quartz.properties!")
        props["$DS_PREFIX.$dsName.URL"] = jdbcUrl
    }

    fun assertNoMismatchedSystemProperty(jdbcUrlFromProps: String) {
        val sysProp = System.getProperty("meeseeks.quartz.jdbc") ?: return
        require(sysProp == jdbcUrlFromProps) {
            "Mismatched JDBC URLs: meeseeks.quartz.jdbc='$sysProp' vs quartz.properties='$jdbcUrlFromProps'. " +
                "Remove the system property or make them identical."
        }
    }
}