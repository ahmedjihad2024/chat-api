package com.example.chat.admin

import com.example.chat.user.enums.Role
import com.example.chat.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class AdminBootstrapper(
    @Value($$"${app.admin.phones:}") private val adminPhones: List<String>,
    private val userRepository: UserRepository,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val phones = adminPhones.map { it.trim() }.filter { it.isNotBlank() }
        if (phones.isEmpty()) return

        phones.forEach { phone ->
            val user = userRepository.findByPhone(phone)
            if (user == null) {
                log.info("Admin bootstrap: no user yet for {} — will promote after they register", phone)
                return@forEach
            }
            if (Role.ADMIN in user.roles) return@forEach
            userRepository.save(user.copy(roles = user.roles + Role.ADMIN))
            log.info("Admin bootstrap: promoted {} to ADMIN", phone)
        }
    }
}
