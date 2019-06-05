package com.example.books.library

import com.example.books.library.restcontrollers.LibrariesRestController
import com.example.books.library.restcontrollers.MembersRestController
import com.example.books.library.restcontrollers.UsersRestController
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.Authentication
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
                        "/api/addUser", "/api/confirmAccount", "/api/getItem", "/api/invitation/**",
                        "/api/getBooksByPattern**", "/api/getBookById**", "/api/getBookByISBN**").permitAll()
                .antMatchers("/api/logout").authenticated()
                .antMatchers("/api/library/{libId}/**").access("@webSecurityConfig.checkLib(authentication, #libId)")
                .antMatchers("/api/{id}/**").access("@webSecurityConfig.checkId(authentication, #id)")
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

    fun checkLib(authentication: Authentication, libId: Long): Boolean {
        logger.info("Requested librery: $libId")
        val user = UsersRestController().usersInterface.getUserByName(authentication.name) ?: return false
        val lib = LibrariesRestController().librariesInterface.getLibraryByUserId(user.id)
        logger.info("Requested library: $libId libId: $lib user: $user")
        if (lib == libId) {
            return true
        } else {
            //TODO Members rest controler
            val members = MembersRestController().membersInterface.getMembersByLibId(libId)
            for (member in members) {
                if (member.user.id == user.id) {
                    return true
                }
            }
            return false
        }
    }

    fun checkId(authentication: Authentication, id: Long): Boolean {
        logger.info("Requested id: $id")
        val user = UsersRestController().usersInterface.getUserByName(authentication.name) ?: return false
        logger.info("userId: ${user.id}")
        return user.id == id
    }
}