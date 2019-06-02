package com.example.books.library

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.web.cors.CorsConfiguration


@Configuration
@EnableWebSecurity
@EnableAutoConfiguration
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    companion object {
        val logger = LoggerFactory.getLogger(WebSecurityConfig::class.java)!!
    }

    @Bean
    fun mailSender(): JavaMailSenderImpl {
        val javaMailSender = JavaMailSenderImpl()

        javaMailSender.protocol = "smtp"
        javaMailSender.host = System.getenv("smtpHost")
        javaMailSender.port = System.getenv("smtpPort").toInt()
        javaMailSender.username = System.getenv("mailUserName")
        javaMailSender.password = System.getenv("mailPassword")

        val props = javaMailSender.javaMailProperties
        props["mail.smtp.host"] = "smtp.gmail.net.np"
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.debug"] = "true"

        return javaMailSender
    }

    override fun configure(http: HttpSecurity?) {
        http!!.authorizeRequests()
                .antMatchers("/api/login", "/api/checkName", "/api/checkPassword", "/api/checkEmail",
                        "/api/addUser", "/api/confirmAccount").permitAll()
                .antMatchers("/api/logout").authenticated()
        http.csrf().disable()
        val corsConfiguration = CorsConfiguration()
        corsConfiguration.allowCredentials = true
        corsConfiguration.allowedHeaders = arrayListOf("*")
        corsConfiguration.allowedMethods = arrayListOf("*")
        corsConfiguration.allowedOrigins = arrayListOf("http://localhost:3000")
        corsConfiguration.maxAge = 3600
        http.cors().configurationSource { corsConfiguration }
        super.configure(http)
    }

    @Bean
    override fun userDetailsService(): UserDetailsService {
        val sqlString = "SELECT NAME, PASSWORD FROM USERS WHERE STATUS = 1"
        val prepStmt = DBConnection.dbConnection!!.prepareStatement(sqlString)
        val userDetailsService = DBConnection.inMemoryUserDetailsManager
        val resultSet = prepStmt.executeQuery()
        var count = 0
        while (resultSet.next()) {
            val userDetails = User.withUsername(resultSet.getString("name")).password(resultSet.getString("password")).roles("USER").build()
            userDetailsService.createUser(userDetails)
            count++
        }
        logger.info("Created $count users")
        return userDetailsService
    }
}