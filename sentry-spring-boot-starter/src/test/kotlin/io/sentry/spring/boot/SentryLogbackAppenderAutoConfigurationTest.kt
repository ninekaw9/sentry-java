package io.sentry.spring.boot

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import io.sentry.logback.SentryAppender
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class SentryLogbackAppenderAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SentryLogbackAppenderAutoConfiguration::class.java, SentryAutoConfiguration::class.java))
        .withPropertyValues("sentry.dsn=http://key@localhost/proj")

    @BeforeTest
    fun `reset Logback context`() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.reset()
    }

    @Test
    fun `configures SentryAppender`() {
        contextRunner
            .run {
                val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
                assertThat(rootLogger.getAppenders(SentryAppender::class.java)).hasSize(1)
            }
    }

    @Test
    fun `sets SentryAppender properties`() {
        contextRunner.withPropertyValues("sentry.logging.minimum-event-level=info", "sentry.logging.minimum-breadcrumb-level=debug")
            .run {
                val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger

                val appenders = rootLogger.getAppenders(SentryAppender::class.java)
                assertThat(appenders).hasSize(1)
                val sentryAppender = appenders[0] as SentryAppender

                assertThat(sentryAppender.minimumBreadcrumbLevel).isEqualTo(Level.DEBUG)
                assertThat(sentryAppender.minimumEventLevel).isEqualTo(Level.INFO)
            }
    }

    @Test
    fun `does not configure SentryAppender when logging is disabled`() {
        contextRunner.withPropertyValues("sentry.logging.enabled=false")
            .run {
                val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
                assertThat(rootLogger.getAppenders(SentryAppender::class.java)).isEmpty()
            }
    }

    @Test
    fun `does not configure SentryAppender when appender is already configured`() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        val sentryAppender = SentryAppender()
        sentryAppender.name = "customAppender"
        sentryAppender.context = loggerContext
        sentryAppender.start()
        rootLogger.addAppender(sentryAppender)

        contextRunner
            .run {
                val appenders = rootLogger.getAppenders(SentryAppender::class.java)
                assertThat(appenders).hasSize(1)
                assertThat(appenders.first().name).isEqualTo("customAppender")
            }
    }
}

fun <T> Logger.getAppenders(clazz: Class<T>): List<Appender<ILoggingEvent>> {
    return this.iteratorForAppenders().asSequence().toList()
        .filter { it.javaClass == clazz }
}
