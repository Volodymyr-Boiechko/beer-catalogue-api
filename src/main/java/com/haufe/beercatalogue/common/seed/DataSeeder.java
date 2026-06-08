package com.haufe.beercatalogue.common.seed;

import com.haufe.beercatalogue.manufacturer.Manufacturer;
import com.haufe.beercatalogue.manufacturer.ManufacturerRepository;
import com.haufe.beercatalogue.security.AppUser;
import com.haufe.beercatalogue.security.AppUserRepository;
import com.haufe.beercatalogue.security.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ManufacturerRepository manufacturerRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    DataSeeder(ManufacturerRepository manufacturerRepository,
               AppUserRepository appUserRepository,
               PasswordEncoder passwordEncoder) {
        this.manufacturerRepository = manufacturerRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (appUserRepository.count() > 0) {
            return;
        }

        var heineken = manufacturerRepository.save(new Manufacturer("Heineken", "Netherlands"));
        var guinness = manufacturerRepository.save(new Manufacturer("Guinness", "Ireland"));

        appUserRepository.save(new AppUser("admin", passwordEncoder.encode("admin"), UserRole.ADMIN, null));
        appUserRepository.save(new AppUser("heineken", passwordEncoder.encode("pass"), UserRole.MANUFACTURER, heineken.getId()));
        appUserRepository.save(new AppUser("guinness", passwordEncoder.encode("pass"), UserRole.MANUFACTURER, guinness.getId()));

        log.info("Demo data seeded: 2 manufacturers, 3 users");
    }
}
